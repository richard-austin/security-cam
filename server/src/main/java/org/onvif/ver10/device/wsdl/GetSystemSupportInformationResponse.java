
package org.onvif.ver10.device.wsdl;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;
import org.onvif.ver10.schema.SupportInformation;

import javax.xml.bind.annotation.*;


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
 *         &lt;element name="SupportInformation" type="{http://www.onvif.org/ver10/schema}SupportInformation"/&gt;
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
    "supportInformation"
})
@XmlRootElement(name = "GetSystemSupportInformationResponse")
public class GetSystemSupportInformationResponse {

    @XmlElement(name = "SupportInformation", required = true)
    protected SupportInformation supportInformation;

    /**
     * Gets the value of the supportInformation property.
     * 
     * @return
     *     possible object is
     *     {@link SupportInformation }
     *     
     */
    public SupportInformation getSupportInformation() {
        return supportInformation;
    }

    /**
     * Sets the value of the supportInformation property.
     * 
     * @param value
     *     allowed object is
     *     {@link SupportInformation }
     *     
     */
    public void setSupportInformation(SupportInformation value) {
        this.supportInformation = value;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     * 
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, JAXBToStringStyle.DEFAULT_STYLE);
    }

}
