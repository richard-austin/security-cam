package server

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationErrors
import okhttp3.internal.http.HttpMethod
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import security.cam.LogService
import security.cam.RestfulInterfaceService
import security.cam.UtilsService
import security.cam.ValidationErrorService
import security.cam.commands.CameraParamsCommand
import security.cam.commands.SetCameraParamsCommand
import security.cam.enums.PassFail
import security.cam.enums.RestfulResponseStatusEnum
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse



class UtilsController {
    UtilsService utilsService
    RestfulInterfaceService restfulInterfaceService
    LogService logService
    ValidationErrorService validationErrorService

    /**
     * getTemperature: Get the core temperature (Raspberry pi only). This is called at intervals to keep the session alive
     * @return: The temperature as a string. On non Raspberry pi systems an error is returned.
     */
    @Secured(['ROLE_CLIENT'])
    def getTemperature() {
        ObjectCommandResponse response = utilsService.getTemperature()

        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.responseObject as JSON
    }

    /**
     * getVersion: Get the version number from application.yml config
     * @return: The version strig
     */
    @Secured(['ROLE_CLIENT'])
    def getVersion() {
        ObjectCommandResponse response = utilsService.getVersion()
        if (response.status != PassFail.PASS)
            render(status: 500, text: response.error)
        else
            render response.responseObject as JSON
    }

    /**
     * setIP: Set the file myip to contain our current public ip address.
     * @return: Our public ip address
     */
    @Secured(['ROLE_CLIENT'])
    def setIP() {
        ObjectCommandResponse response
        response = utilsService.setIP()
        render response.responseObject as JSON
    }

    @Secured(['ROLE_CLIENT'])
    def cameraParams(CameraParamsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'cameraParams')
            logService.cam.error "cameraParams: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            RestfulResponse response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params)

            if (response.status != RestfulResponseStatusEnum.PASS) {
                result.status = PassFail.FAIL
                result.error = response.errorMsg
                result.userError = response.userError
                render(status: 500, text: result)
            } else
                render response.responseObject as JSON
        }
    }

    @Secured(['ROLE_CLIENT'])
    def setCameraParams(SetCameraParamsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        if (cmd.hasErrors()) {
            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'cameraParams')
            logService.cam.error "setCameraParams: Validation error: " + errorsMap.toString()
            render(status: 400, text: errorsMap as JSON)
        } else {
            RestfulResponse response = restfulInterfaceService.sendRequest(cmd.address, cmd.uri, cmd.params, true)

            if (response.status != RestfulResponseStatusEnum.PASS) {
                logService.cam.error "setCameraParams: error: " + response.errorMsg
                result.status = PassFail.FAIL
                result.error = response.errorMsg
                result.userError = response.userError
                render(status: 500, result as JSON)
            } else
                render response.responseObject as JSON
        }
    }
    private String server = "localhost";
    private int port = 8080;

//    @RequestMapping("/**")
//    ResponseEntity mirrorRest(@RequestBody(required = false) String body,
//                              HttpMethod method, HttpServletRequest request, HttpServletResponse response)
//            throws URISyntaxException {
//        String requestUrl = request.getRequestURI();
//
//        URI uri = new URI("http", null, server, port, null, null, null);
//        uri = UriComponentsBuilder.fromUri(uri)
//                .path(requestUrl)
//                .query(request.getQueryString())
//                .build(true).toUri();
//
//        HttpHeaders headers = new HttpHeaders();
//        Enumeration<String> headerNames = request.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            headers.set(headerName, request.getHeader(headerName));
//        }
//
//        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
//        RestTemplate restTemplate = new RestTemplate();
//        try {
//            return restTemplate.exchange(uri, method, httpEntity, String.class);
//        } catch(HttpStatusCodeException e) {
//            return ResponseEntity.status(e.getRawStatusCode())
//                    .headers(e.getResponseHeaders())
//                    .body(e.getResponseBodyAsString());
//        }
//    }
//

    @Secured(['ROLE_CLIENT'])
    @GetMapping(value="/Utils/proxy/x/y")
    def proxy() {
        def x = request.getRequestURI()
        def y = request.getRequestURL()
        def z = x
        def t = y

        def q = request.JSON

        RestfulResponse response = sendRequest(request)

        render(status: response.responseCode, text: response.responseObject.toString())
     }

    /**
     * Attempt to send a RESTFul request to a given host to perform a service
     *
     * @param address : network address of the target camera
     * @param uri : uri for the camera service
     * @param params : map of any key:value parameters needed in the API call, null if there are none
     * @return : RestfulResponse object containing details of the result of the request, including an object containing the data received back from the host if any
     */
    private def sendRequest(HttpServletRequest req, boolean isPOST = false) {
        def result = new RestfulResponse()
        result.status = RestfulResponseStatusEnum.FAIL
        result.responseObject = null
        result.responseCode = -1
        result.errorMsg = null

        HttpURLConnection conn = null
        InputStream is = null
        Reader inp = null
        String url = buildURL('192.168.0.30', '', isPOST ? null : '')

        try {
            String username = grailsApplication.config.camerasAdminUserName
            String password = grailsApplication.config.camerasAdminPassword
            String userPass = "${username}:${password}"
            String basicAuth = "Basic " + userPass.bytes.encodeBase64().toString()
            URL u = new URL(url)
            conn = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY)
            for (Enumeration<?> names = req.getHeaderNames(); names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                for (Enumeration<?> values = req.getHeaders(name); values.hasMoreElements(); ) {
                    conn.setRequestProperty(name, (String) values.nextElement());
                }
            }
              conn.setRequestProperty("Authorization", basicAuth)
            conn.setDoOutput(true)
            conn.setConnectTimeout(6 * 1000)
            conn.setReadTimeout(6 * 1000)
            //conn.setRe('Referrer-Policy', 'strict-origin')

            if(isPOST)
            {
                OutputStream os = conn.getOutputStream()
                byte[] ip = params.getBytes("utf-8")
                os.write(ip, 0, ip.length)
            }
            result.responseCode = conn.getResponseCode()
            def headers = conn.getHeaderFields()
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
            result.responseObject = out.toString()

            def headerFields = conn.getHeaderFields()
            def x = headerFields
         }
        catch (MalformedURLException | SocketTimeoutException | ConnectException e) {
            logService.cam.error(e.getMessage())
            result.status = RestfulResponseStatusEnum.CONNECT_FAIL
            result.errorMsg = RestfulResponseStatusEnum.CONNECT_FAIL.toString()
        }
        catch (FileNotFoundException e) {
            logService.cam.error(e.getMessage())
            result.status = RestfulResponseStatusEnum.SERVICE_NOT_FOUND
            result.errorMsg = RestfulResponseStatusEnum.SERVICE_NOT_FOUND.toString()
        }
        catch (UnknownHostException e) {
            logService.cam.error('Unknown host ' + e.getMessage())
            result.status = RestfulResponseStatusEnum.CONNECT_FAIL
            result.userError = true
            result.errorMsg = 'Unknown host ' + e.getMessage()
        }
        catch (IOException e) {
            logService.cam.error(e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.status = RestfulResponseStatusEnum.ERROR_RESPONSE
            result.errorMsg = RestfulResponseStatusEnum.ERROR_RESPONSE.toString()
        }
        catch (Exception e) {
            logService.cam.error(e.getMessage())
            result.responseCode = 500 //conn.getResponseCode()
            result.status = RestfulResponseStatusEnum.FAIL
            result.errorMsg = RestfulResponseStatusEnum.FAIL.toString()
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

}
