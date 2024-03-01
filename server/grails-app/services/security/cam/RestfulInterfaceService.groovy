package security.cam

import grails.core.GrailsApplication
import security.cam.enums.RestfulResponseStatusEnum
import security.cam.interfaceobjects.RestfulResponse
import server.Camera

class CameraParams {
    public def storage = [:]

    def propertyMissing(String name, value) { storage[name] = value }

    def propertyMissing(String name) { storage[name] }
}

class RestfulInterfaceService {
    LogService logService
    GrailsApplication grailsApplication
    CamService camService

    /**
     * Attempt to send a RESTFul request to a given host to perform a service
     *
     * @param address : network address of the target camera
     * @param uri : uri for the camera service
     * @param params : map of any key:value parameters needed in the API call, null if there are none
     * @return : RestfulResponse object containing details of the result of the request, including an object containing the data received back from the host if any
     */
    RestfulResponse sendRequest(String address, String uri, String params = null, boolean isPOST = false, int timeOut = 6) {
        def result = new RestfulResponse()
        result.status = RestfulResponseStatusEnum.FAIL
        result.responseObject = null
        result.responseCode = -1
        result.errorMsg = null

        HttpURLConnection conn = null
        InputStream is = null
        Reader inp = null
        String url = buildURL(address, uri, isPOST ? null : params)

        try {
            Camera cam = camService.getCamera(address)
            def creds = cam.credentials()
            String userPass = "${creds.userName}:${creds.password}"
            String basicAuth = "Basic " + userPass.bytes.encodeBase64().toString()
            URL u = new URL(url)
            conn = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY)
            conn.setRequestMethod(isPOST ? "POST" : "GET")
            conn.setRequestProperty("Authorization", basicAuth)
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Accept-Charset", "UTF-8")
            conn.setDoOutput(true)
            conn.setConnectTimeout(timeOut * 1000)
            conn.setReadTimeout(timeOut * 1000)

            if(isPOST)
            {
                OutputStream os = conn.getOutputStream()
                byte[] ip = params.getBytes("utf-8")
                os.write(ip, 0, ip.length)
            }
            result.responseCode = conn.getResponseCode()

            if (result.responseCode == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream()
            } else {
                is = conn.getErrorStream()
            }

            // get the response or error message from the relevant stream and store as a string
            def bis = new BufferedInputStream(is)
            final char[] buffer = new char[0x10000]
            StringBuilder out = new StringBuilder()
            inp = new InputStreamReader(bis, "UTF-8")
            int readCount = 1
            while (readCount > 0) {
                readCount = inp.read(buffer, 0, buffer.length)
                if (readCount > 0) {
                    out.append(buffer, 0, readCount)
                }
            }
            // if the response was good, de-serialise the JSON payload into the response object
            if (result.responseCode == HttpURLConnection.HTTP_OK) {
                result.status = RestfulResponseStatusEnum.PASS
                result.responseObject = createMap(out.toString())
                if(result.responseObject==null)
                    result.responseObject=[response:out.toString()]  // Used when setting rather than reading values.
            } else {
                logService.cam.error("API [${uri}] request failed to [${address}]. Error message: ${out.toString()}")
                result.status =result.status
                result.responseCode = conn.getResponseCode()
                result.errorMsg = out.toString()
            }
        }
        catch (MalformedURLException | SocketTimeoutException | ConnectException e) {
            logService.cam.error(e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.status = RestfulResponseStatusEnum.CONNECT_FAIL
            result.errorMsg = e.getMessage()
        }
        catch (FileNotFoundException e) {
            logService.cam.error(e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.status = RestfulResponseStatusEnum.SERVICE_NOT_FOUND
            result.errorMsg = e.getMessage()
        }
        catch (UnknownHostException e) {
            logService.cam.error('Unknown host ' + e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.status = RestfulResponseStatusEnum.CONNECT_FAIL
            result.userError = true
            result.errorMsg = 'Unknown host ' + e.getMessage()
        }
        catch (IOException e) {
            logService.cam.error(e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.responseObject = e.getClass().getName()
            result.status = RestfulResponseStatusEnum.ERROR_RESPONSE
            result.errorMsg = e.getMessage()
        }
        catch (Exception e) {
            logService.cam.error(e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.status = RestfulResponseStatusEnum.FAIL
            result.errorMsg = e.getMessage()
        }
        finally {
            if (conn && conn.getErrorStream()) {
                conn.getErrorStream().close()
            }
            if (is) {
                is.close()
            }
            if (inp) {
                inp.close()
            }
            conn.disconnect()
        }
        return result
    }

    /**
     * createMap: Create an object from the list of data returned by the cameras which can be returned as
     *             JSON to the client
     * @param valueList : List of data returned from the camera, i.e: -
     *                   var show_0="1"; var place_0="1"; var format_0="0"; var type_0="0"; var x_0="976";
     * @return: map of attributes and their values.
     */
    private static def createMap(String valueList) {
        CameraParams cp = null
        if(valueList.contains("\r\n"))
        {
            cp = new CameraParams()
            String[] params = valueList.split("\r\n")
            params.each { val ->
                String thisVal = val.replace("var ", "")
                String attr, attrValue
                attr = thisVal.substring(0, thisVal.indexOf("="))
                attrValue = thisVal.substring(thisVal.indexOf("\"") + 1, thisVal.indexOf("\"", thisVal.indexOf("\"") + 1))

                cp[attr] = attrValue
            }
        }
        return cp?.storage
    }

    /**
     * Build the URL to call the required service at the given host, including parameters if any.
     *
     * @param address : network address of the host (e.g. IP address, FQDN)
     * @param uri : uri for the camera service required
     * @return : the constructed URL
     */
    private static String buildURL(String address, String uri, String params = null) {
        StringBuilder url = new StringBuilder()
        url.append('http://')
        url.append(address)
        url.append('/')
        url.append(uri)
        if (params) {
            url.append('?')
            url.append(params)
        }
        return url.toString()
    }

    /**
     * buildJSON: Build a JSON string of the input parameters given in the map
     * @param params Parameters as key/value pairs
     */
    private static String buildJSON(Map<String, String> params) {
        StringBuilder json = new StringBuilder()
        json.append("{")

        if (params && params.size() > 0) {

            boolean setComma = false
            params.each { it ->
                if (it.value != null) {
                    if (setComma)
                        json.append(", ")
                    else
                        setComma = true

                    json.append("\"$it.key\": \"$it.value\"")
                }
            }
        }
        json.append("}")
        return json.toString()
    }
}
