
package org.onvif.ver10.accessrules.wsdl;

import java.util.ArrayList;
import java.util.List;
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
 *         &lt;element name="NextStartReference" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="AccessProfileInfo" type="{http://www.onvif.org/ver10/accessrules/wsdl}AccessProfileInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
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
    "nextStartReference",
    "accessProfileInfo"
})
@XmlRootElement(name = "GetAccessProfileInfoListResponse")
public class GetAccessProfileInfoListResponse {

    @XmlElement(name = "NextStartReference")
    protected String nextStartReference;
    @XmlElement(name = "AccessProfileInfo")
    protected List<AccessProfileInfo> accessProfileInfo;

    /**
     * Gets the value of the nextStartReference property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNextStartReference() {
        return nextStartReference;
    }

    /**
     * Sets the value of the nextStartReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNextStartReference(String value) {
        this.nextStartReference = value;
    }

    /**
     * Gets the value of the accessProfileInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessProfileInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessProfileInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessProfileInfo }
     * 
     * 
     */
    public List<AccessProfileInfo> getAccessProfileInfo() {
        if (accessProfileInfo == null) {
            accessProfileInfo = new ArrayList<AccessProfileInfo>();
        }
        return this.accessProfileInfo;
    }

}
