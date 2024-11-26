package org.onvif.ver10.device.wsdl;

import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 4.0.0
 * Generated source version: 4.0.0
 *
 */
@WebServiceClient(name = "DeviceService",
                  wsdlLocation = "null",
                  targetNamespace = "http://www.onvif.org/ver10/device/wsdl")
public class DeviceService extends jakarta.xml.ws.Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.onvif.org/ver10/device/wsdl", "DeviceService");
    public final static QName DevicePort = new QName("http://www.onvif.org/ver10/device/wsdl", "DevicePort");
    static {
        WSDL_LOCATION = null;
    }

    public DeviceService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public DeviceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public DeviceService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public DeviceService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public DeviceService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public DeviceService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns Device
     */
    @WebEndpoint(name = "DevicePort")
    public Device getDevicePort() {
        return super.getPort(DevicePort, Device.class);
    }

    /**
     *
     * @param features
     *     A list of {@link jakarta.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Device
     */
    @WebEndpoint(name = "DevicePort")
    public Device getDevicePort(WebServiceFeature... features) {
        return super.getPort(DevicePort, Device.class, features);
    }

}
