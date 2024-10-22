package org.onvif.ver10.receiver.wsdl;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.URL;

/**
 * This class was generated by Apache CXF 3.3.2
 * Generated source version: 3.3.2
 *
 */
@WebServiceClient(name = "ReceiverService",
                  wsdlLocation = "null",
                  targetNamespace = "http://www.onvif.org/ver10/receiver/wsdl")
public class ReceiverService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.onvif.org/ver10/receiver/wsdl", "ReceiverService");
    public final static QName ReceiverPort = new QName("http://www.onvif.org/ver10/receiver/wsdl", "ReceiverPort");
    static {
        WSDL_LOCATION = null;
    }

    public ReceiverService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public ReceiverService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ReceiverService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public ReceiverService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public ReceiverService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public ReceiverService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns ReceiverPort
     */
    @WebEndpoint(name = "ReceiverPort")
    public ReceiverPort getReceiverPort() {
        return super.getPort(ReceiverPort, ReceiverPort.class);
    }

    /**
     *
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ReceiverPort
     */
    @WebEndpoint(name = "ReceiverPort")
    public ReceiverPort getReceiverPort(WebServiceFeature... features) {
        return super.getPort(ReceiverPort, ReceiverPort.class, features);
    }

}