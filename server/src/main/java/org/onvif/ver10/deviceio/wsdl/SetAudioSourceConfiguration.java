
package org.onvif.ver10.deviceio.wsdl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;
import org.onvif.ver10.schema.AudioSourceConfiguration;
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
 *         &lt;element name="Configuration" type="{http://www.onvif.org/ver10/schema}AudioSourceConfiguration"/&gt;
 *         &lt;element name="ForcePersistence" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
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
    "configuration",
    "forcePersistence",
    "any"
})
@XmlRootElement(name = "SetAudioSourceConfiguration")
public class SetAudioSourceConfiguration {

    @XmlElement(name = "Configuration", required = true)
    protected AudioSourceConfiguration configuration;
    @XmlElement(name = "ForcePersistence")
    protected boolean forcePersistence;
    @XmlAnyElement(lax = true)
    protected List<Object> any;

    /**
     * Gets the value of the configuration property.
     * 
     * @return
     *     possible object is
     *     {@link AudioSourceConfiguration }
     *     
     */
    public AudioSourceConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the value of the configuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link AudioSourceConfiguration }
     *     
     */
    public void setConfiguration(AudioSourceConfiguration value) {
        this.configuration = value;
    }

    /**
     * Gets the value of the forcePersistence property.
     * This getter has been renamed from isForcePersistence() to getForcePersistence() by cxf-xjc-boolean plugin.
     * 
     */
    public boolean getForcePersistence() {
        return forcePersistence;
    }

    /**
     * Sets the value of the forcePersistence property.
     * 
     */
    public void setForcePersistence(boolean value) {
        this.forcePersistence = value;
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

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     * 
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, JAXBToStringStyle.DEFAULT_STYLE);
    }

}
