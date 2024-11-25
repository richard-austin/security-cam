
package org.onvif.ver10.search.wsdl;

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
 *         &lt;element name="SearchToken" type="{http://www.onvif.org/ver10/schema}JobToken"/&gt;
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
    "searchToken"
})
@XmlRootElement(name = "FindRecordingsResponse")
public class FindRecordingsResponse {

    @XmlElement(name = "SearchToken", required = true)
    protected String searchToken;

    /**
     * Gets the value of the searchToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSearchToken() {
        return searchToken;
    }

    /**
     * Sets the value of the searchToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSearchToken(String value) {
        this.searchToken = value;
    }

}
