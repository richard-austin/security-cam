
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
 *         &lt;element name="CertificateIDs" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CertificateIDs"/&gt;
 *         &lt;element name="Alias" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "certificateIDs",
    "alias"
})
@XmlRootElement(name = "CreateCertificationPath")
public class CreateCertificationPath {

    @XmlElement(name = "CertificateIDs", required = true)
    protected CertificateIDs certificateIDs;
    @XmlElement(name = "Alias")
    protected String alias;

    /**
     * Gets the value of the certificateIDs property.
     * 
     * @return
     *     possible object is
     *     {@link CertificateIDs }
     *     
     */
    public CertificateIDs getCertificateIDs() {
        return certificateIDs;
    }

    /**
     * Sets the value of the certificateIDs property.
     * 
     * @param value
     *     allowed object is
     *     {@link CertificateIDs }
     *     
     */
    public void setCertificateIDs(CertificateIDs value) {
        this.certificateIDs = value;
    }

    /**
     * Gets the value of the alias property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Sets the value of the alias property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAlias(String value) {
        this.alias = value;
    }

}
