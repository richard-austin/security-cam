
package org.onvif.ver10.accessrules.wsdl;

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
 *         &lt;element name="AccessProfile" type="{http://www.onvif.org/ver10/accessrules/wsdl}AccessProfile"/&gt;
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
    "accessProfile"
})
@XmlRootElement(name = "CreateAccessProfile")
public class CreateAccessProfile {

    @XmlElement(name = "AccessProfile", required = true)
    protected AccessProfile accessProfile;

    /**
     * Gets the value of the accessProfile property.
     * 
     * @return
     *     possible object is
     *     {@link AccessProfile }
     *     
     */
    public AccessProfile getAccessProfile() {
        return accessProfile;
    }

    /**
     * Sets the value of the accessProfile property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessProfile }
     *     
     */
    public void setAccessProfile(AccessProfile value) {
        this.accessProfile = value;
    }

}
