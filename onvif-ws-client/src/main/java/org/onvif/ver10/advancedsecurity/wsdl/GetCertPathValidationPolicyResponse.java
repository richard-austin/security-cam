
package org.onvif.ver10.advancedsecurity.wsdl;

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
 *         &lt;element name="CertPathValidationPolicy" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CertPathValidationPolicy"/&gt;
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
    "certPathValidationPolicy"
})
@XmlRootElement(name = "GetCertPathValidationPolicyResponse")
public class GetCertPathValidationPolicyResponse {

    @XmlElement(name = "CertPathValidationPolicy", required = true)
    protected CertPathValidationPolicy certPathValidationPolicy;

    /**
     * Gets the value of the certPathValidationPolicy property.
     * 
     * @return
     *     possible object is
     *     {@link CertPathValidationPolicy }
     *     
     */
    public CertPathValidationPolicy getCertPathValidationPolicy() {
        return certPathValidationPolicy;
    }

    /**
     * Sets the value of the certPathValidationPolicy property.
     * 
     * @param value
     *     allowed object is
     *     {@link CertPathValidationPolicy }
     *     
     */
    public void setCertPathValidationPolicy(CertPathValidationPolicy value) {
        this.certPathValidationPolicy = value;
    }

}
