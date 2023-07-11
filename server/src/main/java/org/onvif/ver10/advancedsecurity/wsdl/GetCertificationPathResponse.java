
package org.onvif.ver10.advancedsecurity.wsdl;

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
