
package org.oasis_open.docs.wsn.bw_2;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.3.2
 * Generated source version: 3.3.2
 */

@WebFault(name = "PauseFailedFault", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
public class PauseFailedFault extends Exception {

    private org.oasis_open.docs.wsn.b_2.PauseFailedFaultType pauseFailedFault;

    public PauseFailedFault() {
        super();
    }

    public PauseFailedFault(String message) {
        super(message);
    }

    public PauseFailedFault(String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public PauseFailedFault(String message, org.oasis_open.docs.wsn.b_2.PauseFailedFaultType pauseFailedFault) {
        super(message);
        this.pauseFailedFault = pauseFailedFault;
    }

    public PauseFailedFault(String message, org.oasis_open.docs.wsn.b_2.PauseFailedFaultType pauseFailedFault, java.lang.Throwable cause) {
        super(message, cause);
        this.pauseFailedFault = pauseFailedFault;
    }

    public org.oasis_open.docs.wsn.b_2.PauseFailedFaultType getFaultInfo() {
        return this.pauseFailedFault;
    }
}
