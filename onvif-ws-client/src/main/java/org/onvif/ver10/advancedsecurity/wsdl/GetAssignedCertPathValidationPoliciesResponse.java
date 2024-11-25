
package org.onvif.ver10.advancedsecurity.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
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
 *         &lt;element name="CertPathValidationPolicyID" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CertPathValidationPolicyID" maxOccurs="unbounded" minOccurs="0"/&gt;
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
    "certPathValidationPolicyID"
})
@XmlRootElement(name = "GetAssignedCertPathValidationPoliciesResponse")
public class GetAssignedCertPathValidationPoliciesResponse {

    @XmlElementRef(name = "CertPathValidationPolicyID", namespace = "http://www.onvif.org/ver10/advancedsecurity/wsdl", type = JAXBElement.class, required = false)
    protected List<JAXBElement<String>> certPathValidationPolicyID;

    /**
     * Gets the value of the certPathValidationPolicyID property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the certPathValidationPolicyID property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCertPathValidationPolicyID().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<JAXBElement<String>> getCertPathValidationPolicyID() {
        if (certPathValidationPolicyID == null) {
            certPathValidationPolicyID = new ArrayList<JAXBElement<String>>();
        }
        return this.certPathValidationPolicyID;
    }

}
