package security.cam.interfaceobjects

import security.cam.enums.PassFail

class CommandResponse {
    public PassFail status
    int          errno
    String       response
    String       error
    Boolean      userError // use this to signal some errors were added to the command object

    CommandResponse() {
        userError = false
        status = PassFail.PASS
        userError = false
    }

    String toString() {
        String retval = """
        \tStatus      = ${status}
        \tErrNo       = ${errno}
        \tResponse    = ${response}
        \tError       = ${error}
        """.toString()
        return retval
    }
}
