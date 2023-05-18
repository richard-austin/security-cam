
package org.onvif.ver20.ptz.wsdl;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;

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
 *         &lt;element name="PTZConfigurationToken" type="{http://www.onvif.org/ver10/schema}ReferenceToken"/&gt;
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
    "ptzConfigurationToken"
})
@XmlRootElement(name = "GetConfiguration")
public class GetConfiguration {

    @XmlElement(name = "PTZConfigurationToken", required = true)
    protected String ptzConfigurationToken;

    /**
     * Gets the value of the ptzConfigurationToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPTZConfigurationToken() {
        return ptzConfigurationToken;
    }

    /**
     * Sets the value of the ptzConfigurationToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPTZConfigurationToken(String value) {
        this.ptzConfigurationToken = value;
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
