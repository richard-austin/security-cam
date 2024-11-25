
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
 *         &lt;element name="Passphrase" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="PassphraseAlias" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "passphrase",
    "passphraseAlias"
})
@XmlRootElement(name = "UploadPassphrase")
public class UploadPassphrase {

    @XmlElement(name = "Passphrase", required = true)
    protected String passphrase;
    @XmlElement(name = "PassphraseAlias")
    protected String passphraseAlias;

    /**
     * Gets the value of the passphrase property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassphrase() {
        return passphrase;
    }

    /**
     * Sets the value of the passphrase property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassphrase(String value) {
        this.passphrase = value;
    }

    /**
     * Gets the value of the passphraseAlias property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassphraseAlias() {
        return passphraseAlias;
    }

    /**
     * Sets the value of the passphraseAlias property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassphraseAlias(String value) {
        this.passphraseAlias = value;
    }

}
