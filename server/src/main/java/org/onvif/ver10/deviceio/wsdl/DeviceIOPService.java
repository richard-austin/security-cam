package org.onvif.ver10.deviceio.wsdl;

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
@WebServiceClient(name = "DeviceIOPService",
                  wsdlLocation = "null",
                  targetNamespace = "http://www.onvif.org/ver10/deviceIO/wsdl")
public class DeviceIOPService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.onvif.org/ver10/deviceIO/wsdl", "DeviceIOPService");
    public final static QName DeviceIOPort = new QName("http://www.onvif.org/ver10/deviceIO/wsdl", "DeviceIOPort");
    static {
        WSDL_LOCATION = null;
    }

    public DeviceIOPService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public DeviceIOPService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public DeviceIOPService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public DeviceIOPService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public DeviceIOPService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public DeviceIOPService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns DeviceIOPort
     */
    @WebEndpoint(name = "DeviceIOPort")
    public DeviceIOPort getDeviceIOPort() {
        return super.getPort(DeviceIOPort, DeviceIOPort.class);
    }

    /**
     *
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns DeviceIOPort
     */
    @WebEndpoint(name = "DeviceIOPort")
    public DeviceIOPort getDeviceIOPort(WebServiceFeature... features) {
        return super.getPort(DeviceIOPort, DeviceIOPort.class, features);
    }

}
