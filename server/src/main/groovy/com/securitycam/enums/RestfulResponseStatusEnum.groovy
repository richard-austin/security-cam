package com.securitycam.enums

enum RestfulResponseStatusEnum {
	PASS('Pass'),													// Restful Service completed successfully
	FAIL('An undefined error occurred'),							// An undefined error occurred
	CERTIFICATE_ERROR('Certificate error'),							// Certificate error when setting up the trust manager
	CONNECT_FAIL('Connect failure'),								// Could not connect to this host, bad URL or attempt timed out
	SERVICE_NOT_FOUND('This service could not be found'),			// Requested service could not be found on the server
	ERROR_RESPONSE('Bad response code from the server'),				// a response code other than 200 was received
	JSON_PARSING_ERROR('The data from the camera is invalid') 		// Could not parse the response from the camera

	private String name	
	private RestfulResponseStatusEnum(String name){
		this.name = name
	}
	
	String toString() {
		return name
	}
}
