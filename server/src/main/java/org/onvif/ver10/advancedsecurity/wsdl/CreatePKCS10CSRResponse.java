
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
 *         &lt;element name="PKCS10CSR" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}Base64DERencodedASN1Value"/&gt;
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
    "pkcs10CSR"
})
@XmlRootElement(name = "CreatePKCS10CSRResponse")
public class CreatePKCS10CSRResponse {

    @XmlElement(name = "PKCS10CSR", required = true)
    protected byte[] pkcs10CSR;

    /**
     * Gets the value of the pkcs10CSR property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getPKCS10CSR() {
        return pkcs10CSR;
    }

    /**
     * Sets the value of the pkcs10CSR property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setPKCS10CSR(byte[] value) {
        this.pkcs10CSR = value;
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
