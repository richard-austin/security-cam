
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
 *         &lt;element name="CertificationPath" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CertificationPath"/&gt;
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
    "certificationPath"
})
@XmlRootElement(name = "GetCertificationPathResponse")
public class GetCertificationPathResponse {

    @XmlElement(name = "CertificationPath", required = true)
    protected CertificationPath certificationPath;

    /**
     * Gets the value of the certificationPath property.
     * 
     * @return
     *     possible object is
     *     {@link CertificationPath }
     *     
     */
    public CertificationPath getCertificationPath() {
        return certificationPath;
    }

    /**
     * Sets the value of the certificationPath property.
     * 
     * @param value
     *     allowed object is
     *     {@link CertificationPath }
     *     
     */
    public void setCertificationPath(CertificationPath value) {
        this.certificationPath = value;
    }

}
