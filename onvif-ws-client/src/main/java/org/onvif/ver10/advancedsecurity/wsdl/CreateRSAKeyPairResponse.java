
package org.onvif.ver10.advancedsecurity.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.Duration;


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
 *         &lt;element name="KeyID" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}KeyID"/&gt;
 *         &lt;element name="EstimatedCreationTime" type="{http://www.w3.org/2001/XMLSchema}duration"/&gt;
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
    "keyID",
    "estimatedCreationTime"
})
@XmlRootElement(name = "CreateRSAKeyPairResponse")
public class CreateRSAKeyPairResponse {

    @XmlElement(name = "KeyID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String keyID;
    @XmlElement(name = "EstimatedCreationTime", required = true)
    protected Duration estimatedCreationTime;

    /**
     * Gets the value of the keyID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKeyID() {
        return keyID;
    }

    /**
     * Sets the value of the keyID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKeyID(String value) {
        this.keyID = value;
    }

    /**
     * Gets the value of the estimatedCreationTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getEstimatedCreationTime() {
        return estimatedCreationTime;
    }

    /**
     * Sets the value of the estimatedCreationTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setEstimatedCreationTime(Duration value) {
        this.estimatedCreationTime = value;
    }

}
