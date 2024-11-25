
package org.onvif.ver10.advancedsecurity.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.w3c.dom.Element;


/**
 * <p>Java class for CRL complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CRL"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="CRLID" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}CRLID"/&gt;
 *         &lt;element name="Alias" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="CRLContent" type="{http://www.onvif.org/ver10/advancedsecurity/wsdl}Base64DERencodedASN1Value"/&gt;
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
@XmlType(name = "CRL", propOrder = {
    "crlid",
    "alias",
    "crlContent",
    "any"
})
public class CRL {

    @XmlElement(name = "CRLID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String crlid;
    @XmlElement(name = "Alias", required = true)
    protected String alias;
    @XmlElement(name = "CRLContent", required = true)
    protected byte[] crlContent;
    @XmlAnyElement(lax = true)
    protected List<Object> any;

    /**
     * Gets the value of the crlid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCRLID() {
        return crlid;
    }

    /**
     * Sets the value of the crlid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCRLID(String value) {
        this.crlid = value;
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
     * Gets the value of the crlContent property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getCRLContent() {
        return crlContent;
    }

    /**
     * Sets the value of the crlContent property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setCRLContent(byte[] value) {
        this.crlContent = value;
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
