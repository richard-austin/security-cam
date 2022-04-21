package org.onvif.ver10.recording.wsdl;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.3.2
 * Generated source version: 3.3.2
 *
 */
@WebServiceClient(name = "RecordingService",
                  wsdlLocation = "null",
                  targetNamespace = "http://www.onvif.org/ver10/recording/wsdl")
public class RecordingService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.onvif.org/ver10/recording/wsdl", "RecordingService");
    public final static QName RecordingPort = new QName("http://www.onvif.org/ver10/recording/wsdl", "RecordingPort");
    static {
        WSDL_LOCATION = null;
    }

    public RecordingService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public RecordingService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public RecordingService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public RecordingService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public RecordingService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public RecordingService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns RecordingPort
     */
    @WebEndpoint(name = "RecordingPort")
    public RecordingPort getRecordingPort() {
        return super.getPort(RecordingPort, RecordingPort.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns RecordingPort
     */
    @WebEndpoint(name = "RecordingPort")
    public RecordingPort getRecordingPort(WebServiceFeature... features) {
        return super.getPort(RecordingPort, RecordingPort.class, features);
    }

}