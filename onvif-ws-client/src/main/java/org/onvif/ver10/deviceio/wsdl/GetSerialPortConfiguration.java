
package org.onvif.ver10.deviceio.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SerialPortToken" type="{http://www.onvif.org/ver10/schema}ReferenceToken"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "serialPortToken"
})
@XmlRootElement(name = "GetSerialPortConfiguration")
public class GetSerialPortConfiguration {

    @XmlElement(name = "SerialPortToken", required = true)
    protected String serialPortToken;

    /**
     * Gets the value of the serialPortToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSerialPortToken() {
        return serialPortToken;
    }

    /**
     * Sets the value of the serialPortToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSerialPortToken(String value) {
        this.serialPortToken = value;
    }

}
