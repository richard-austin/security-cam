
package org.onvif.ver10.pacs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.accesscontrol.wsdl.AccessPointInfoBase;
import org.onvif.ver10.accesscontrol.wsdl.AreaInfoBase;


/**
 * 
 *         General datastructure referenced by a token.
 *         Should be used as extension base.
 *       
 * 
 * <p>Java class for DataEntity complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataEntity"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="token" use="required" type="{http://www.onvif.org/ver10/pacs}ReferenceToken" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataEntity")
@XmlSeeAlso({
    AccessPointInfoBase.class,
    AreaInfoBase.class
})
public class DataEntity {

    @XmlAttribute(name = "token", required = true)
    protected String token;

    /**
     * Gets the value of the token property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the value of the token property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToken(String value) {
        this.token = value;
    }

}
