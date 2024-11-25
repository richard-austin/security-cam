
package org.onvif.ver10.search.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.schema.SearchState;


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
 *         &lt;element name="State" type="{http://www.onvif.org/ver10/schema}SearchState"/&gt;
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
    "state"
})
@XmlRootElement(name = "GetSearchStateResponse")
public class GetSearchStateResponse {

    @XmlElement(name = "State", required = true)
    @XmlSchemaType(name = "string")
    protected SearchState state;

    /**
     * Gets the value of the state property.
     * 
     * @return
     *     possible object is
     *     {@link SearchState }
     *     
     */
    public SearchState getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     * 
     * @param value
     *     allowed object is
     *     {@link SearchState }
     *     
     */
    public void setState(SearchState value) {
        this.state = value;
    }

}
