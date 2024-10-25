package com.securitycam.interfaceobjects

import com.securitycam.enums.RestfulResponseStatusEnum

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
