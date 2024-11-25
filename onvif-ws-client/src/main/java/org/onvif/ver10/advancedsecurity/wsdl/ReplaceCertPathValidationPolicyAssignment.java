
package org.onvif.ver10.advancedsecurity.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
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
 *         &lt;element name="OldCertPathValidationPolicyID" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CertPathValidationPolicyID"/&gt;
 *         &lt;element name="NewCertPathValidationPolicyID" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CertPathValidationPolicyID"/&gt;
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
    "oldCertPathValidationPolicyID",
    "newCertPathValidationPolicyID"
})
@XmlRootElement(name = "ReplaceCertPathValidationPolicyAssignment")
public class ReplaceCertPathValidationPolicyAssignment {

    @XmlElement(name = "OldCertPathValidationPolicyID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String oldCertPathValidationPolicyID;
    @XmlElement(name = "NewCertPathValidationPolicyID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String newCertPathValidationPolicyID;

    /**
     * Gets the value of the oldCertPathValidationPolicyID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOldCertPathValidationPolicyID() {
        return oldCertPathValidationPolicyID;
    }

    /**
     * Sets the value of the oldCertPathValidationPolicyID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOldCertPathValidationPolicyID(String value) {
        this.oldCertPathValidationPolicyID = value;
    }

    /**
     * Gets the value of the newCertPathValidationPolicyID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNewCertPathValidationPolicyID() {
        return newCertPathValidationPolicyID;
    }

    /**
     * Sets the value of the newCertPathValidationPolicyID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNewCertPathValidationPolicyID(String value) {
        this.newCertPathValidationPolicyID = value;
    }

}
