
package org.onvif.ver10.accesscontrol.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;


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
 *         &lt;element name="AccessPointState" type="{http://www.onvif.org/ver10/accesscontrol/wsdl}AccessPointState"/&gt;
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
    "accessPointState"
})
@XmlRootElement(name = "GetAccessPointStateResponse")
public class GetAccessPointStateResponse {

    @XmlElement(name = "AccessPointState", required = true)
    protected AccessPointState accessPointState;

    /**
     * Gets the value of the accessPointState property.
     * 
     * @return
     *     possible object is
     *     {@link AccessPointState }
     *     
     */
    public AccessPointState getAccessPointState() {
        return accessPointState;
    }

    /**
     * Sets the value of the accessPointState property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessPointState }
     *     
     */
    public void setAccessPointState(AccessPointState value) {
        this.accessPointState = value;
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
