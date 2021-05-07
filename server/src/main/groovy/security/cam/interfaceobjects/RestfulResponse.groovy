package security.cam.interfaceobjects

import security.cam.enums.RestfulResponseStatusEnum

class RestfulResponse {
	RestfulResponseStatusEnum status
	Integer responseCode
	boolean userError = false
	String errorMsg
	def responseObject  // Response from the call
	
	String toString() {
		return """
status         : ${status}
responseCode   : ${responseCode}
userError      : ${userError}
errorMsg       : ${errorMsg}
responseObject : ${responseObject}
"""
	}
}
