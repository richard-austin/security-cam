
package org.onvif.ver10.advancedsecurity.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


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
 *         &lt;element name="KeyPair" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}Base64DERencodedASN1Value"/&gt;
 *         &lt;element name="Alias" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="EncryptionPassphraseID" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}PassphraseID" minOccurs="0"/&gt;
 *         &lt;element name="EncryptionPassphrase" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "keyPair",
    "alias",
    "encryptionPassphraseID",
    "encryptionPassphrase"
})
@XmlRootElement(name = "UploadKeyPairInPKCS8")
public class UploadKeyPairInPKCS8 {

    @XmlElement(name = "KeyPair", required = true)
    protected byte[] keyPair;
    @XmlElement(name = "Alias")
    protected String alias;
    @XmlElement(name = "EncryptionPassphraseID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String encryptionPassphraseID;
    @XmlElement(name = "EncryptionPassphrase")
    protected String encryptionPassphrase;

    /**
     * Gets the value of the keyPair property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getKeyPair() {
        return keyPair;
    }

    /**
     * Sets the value of the keyPair property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setKeyPair(byte[] value) {
        this.keyPair = value;
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

    /**
     * Gets the value of the encryptionPassphraseID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEncryptionPassphraseID() {
        return encryptionPassphraseID;
    }

    /**
     * Sets the value of the encryptionPassphraseID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEncryptionPassphraseID(String value) {
        this.encryptionPassphraseID = value;
    }

    /**
     * Gets the value of the encryptionPassphrase property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEncryptionPassphrase() {
        return encryptionPassphrase;
    }

    /**
     * Sets the value of the encryptionPassphrase property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEncryptionPassphrase(String value) {
        this.encryptionPassphrase = value;
    }

}
