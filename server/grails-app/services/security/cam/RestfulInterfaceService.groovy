package security.cam

import groovy.json.JsonException
import groovy.json.JsonSlurper
import security.cam.enums.RestfulResponseStatusEnum
import security.cam.interfaceobjects.RestfulResponse

import javax.net.ssl.*
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class RestfulInterfaceService {
	LogService logService

	boolean initialised = false

	void initialise()
	{
		if(!initialised) {
//			// Create a trust manager that does not validate certificate chains
//			TrustManager[] trustAllCerts = [new X509TrustManager() {
//				@Override
//				void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
//				}
//
//				@Override
//				void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
//				}
//
//				@Override
//				X509Certificate[] getAcceptedIssuers() {
//					return null
//				}
//			}
//			]

//			KeyStore keyStore = KeyStore.getInstance("JKS")
//			keyStore.load(new FileInputStream(systemPropertiesService.RESTFUL_KEYSTORE_PATH), systemPropertiesService.RESTFUL_KEYSTORE_PASSWORD.toCharArray())
//
//			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SUNX509")
//			kmf.init(keyStore, systemPropertiesService.RESTFUL_KEY_PASSWORD.toCharArray())
//			KeyManager[] keyManagers = kmf.getKeyManagers()
//			SSLContext sc = SSLContext.getInstance("TLS")
//
//			sc.init(keyManagers, trustAllCerts, new SecureRandom())
//
//			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
			initialised = true
			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				boolean verify(String hostname, SSLSession session) {
					return true
				}
			}

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
		}
	}

	/**
	* Attempt to send a RESTFul request to a given host to perform a service
	* 
	* @param address    : network address of the target camera
	* @param uri        : uri for the camera service
	* @param params     : map of any key:value parameters needed in the API call, null if there are none
	* @return 		    : RestfulResponse object containing details of the result of the request, including an object containing the data received back from the host if any
	*/
	RestfulResponse sendRequest(String address, String uri, String params = null) {
		def result = new RestfulResponse()
		result.status = RestfulResponseStatusEnum.FAIL
		result.responseObject = null
		result.responseCode = -1
		result.errorMsg = null

		initialise()

		HttpURLConnection conn = null
		InputStream is = null
		Reader inp = null
		String url = buildURL(address, uri, params)
		//String jsonParams = buildJSON(params)

		try {
			String username ="admin"
			String password = "R@nc1dTapsB0ttom"
			String userPass = "${username}:${password}"
			String basicAuth = "Basic " + userPass.bytes.encodeBase64().toString()
			URL u = new URL(url)
			conn = (HttpURLConnection)u.openConnection(Proxy.NO_PROXY)
			conn.setRequestMethod("GET")
			conn.setRequestProperty("Authorization", basicAuth)
			conn.setRequestProperty("Content-Type", "application/json; utf-8")
			conn.setRequestProperty("Accept", "application/json")
			conn.setRequestProperty("Accept-Charset", "UTF-8")
			conn.setDoOutput(true)
			conn.setConnectTimeout(6 * 1000)
			conn.setReadTimeout(6 * 1000)

//			OutputStream os = conn.getOutputStream()
//			byte[] byteParams = jsonParams.getBytes()
//			os.write(byteParams)

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
				result.responseObject = out.toString()
			}
			else {
				logService.cam.error("API [${uri}] request failed to [${address}]. Error message: ${out.toString()}")
				result.status = RestfulResponseStatusEnum.ERROR_RESPONSE
				result.responseCode = conn.getResponseCode()
				result.errorMsg = RestfulResponseStatusEnum.ERROR_RESPONSE.toString()
			}
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
		catch (UnknownHostException e)
		{
			logService.cam.error('Unknown host ' + e.getMessage())
			result.status = RestfulResponseStatusEnum.CONNECT_FAIL
			result.userError = true
			result.errorMsg = 'Unknown host ' + e.getMessage()
		}
		catch (IOException e) {
			logService.cam.error(e.getMessage())
			result.responseCode = conn.getResponseCode()
			result.status = RestfulResponseStatusEnum.ERROR_RESPONSE
			result.errorMsg = RestfulResponseStatusEnum.ERROR_RESPONSE.toString()
		}
		catch (Exception e) {
			logService.cam.error(e.getMessage())
			result.responseCode = conn.getResponseCode()
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
	* @param address    : network address of the host (e.g. IP address, FQDN)
	* @param uri        : uri for the camera service required
	* @return 		    : the constructed URL
	*/	
	private static String buildURL(String address, String uri, String params=null) {
		StringBuilder url = new StringBuilder()
		url.append('http://')
		url.append(address)
		url.append('/')
		url.append(uri)
		if(params)
		{
			url.append('?')
			url.append(params)
		}
		return url.toString()
	}

	/**
	 * buildJSON: Build a JSON string of the input parameters given in the map
	 * @param params Parameters as key/value pairs
	 */
	private static String buildJSON(Map<String, String> params)
	{
		StringBuilder json = new StringBuilder()
		json.append("{")

		if (params && params.size() > 0) {

			boolean setComma = false
			params.each{it ->
				if (it.value != null) {
					if(setComma)
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