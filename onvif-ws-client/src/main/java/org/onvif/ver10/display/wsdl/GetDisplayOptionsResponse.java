
package org.onvif.ver10.display.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.schema.CodingCapabilities;
import org.onvif.ver10.schema.LayoutOptions;
import org.w3c.dom.Element;


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
 *         &lt;element name="LayoutOptions" type="{http://www.onvif.org/ver10/schema}LayoutOptions" minOccurs="0"/&gt;
 *         &lt;element name="CodingCapabilities" type="{http://www.onvif.org/ver10/schema}CodingCapabilities"/&gt;
 *         &lt;any processContents='lax' maxOccurs="unbounded" minOccurs="0"/&gt;
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
    "layoutOptions",
    "codingCapabilities",
    "any"
})
@XmlRootElement(name = "GetDisplayOptionsResponse")
public class GetDisplayOptionsResponse {

    @XmlElement(name = "LayoutOptions")
    protected LayoutOptions layoutOptions;
    @XmlElement(name = "CodingCapabilities", required = true)
    protected CodingCapabilities codingCapabilities;
    @XmlAnyElement(lax = true)
    protected List<Object> any;

    /**
     * Gets the value of the layoutOptions property.
     * 
     * @return
     *     possible object is
     *     {@link LayoutOptions }
     *     
     */
    public LayoutOptions getLayoutOptions() {
        return layoutOptions;
    }

    /**
     * Sets the value of the layoutOptions property.
     * 
     * @param value
     *     allowed object is
     *     {@link LayoutOptions }
     *     
     */
    public void setLayoutOptions(LayoutOptions value) {
        this.layoutOptions = value;
    }

    /**
     * Gets the value of the codingCapabilities property.
     * 
     * @return
     *     possible object is
     *     {@link CodingCapabilities }
     *     
     */
    public CodingCapabilities getCodingCapabilities() {
        return codingCapabilities;
    }

    /**
     * Sets the value of the codingCapabilities property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodingCapabilities }
     *     
     */
    public void setCodingCapabilities(CodingCapabilities value) {
        this.codingCapabilities = value;
    }

    /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Element }
     * {@link Object }
     * 
     * 
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

}
