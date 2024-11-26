
package org.oasis_open.docs.wsn.bw_2;

import jakarta.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 4.0.0
 * Generated source version: 4.0.0
 */

@WebFault(name = "UnacceptableInitialTerminationTimeFault", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
public class UnacceptableInitialTerminationTimeFault extends Exception {

    private org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType faultInfo;

    public UnacceptableInitialTerminationTimeFault() {
        super();
    }

    public UnacceptableInitialTerminationTimeFault(String message) {
        super(message);
    }

    public UnacceptableInitialTerminationTimeFault(String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public UnacceptableInitialTerminationTimeFault(String message, org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType unacceptableInitialTerminationTimeFault) {
        super(message);
        this.faultInfo = unacceptableInitialTerminationTimeFault;
    }

    public UnacceptableInitialTerminationTimeFault(String message, org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType unacceptableInitialTerminationTimeFault, java.lang.Throwable cause) {
        super(message, cause);
        this.faultInfo = unacceptableInitialTerminationTimeFault;
    }

    public org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType getFaultInfo() {
        return this.faultInfo;
    }
}
