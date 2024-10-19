
package org.onvif.ver10.schema;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for WideDynamicRangeOptions complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WideDynamicRangeOptions"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Mode" type="{http://www.onvif.org/ver10/schema}WideDynamicMode" maxOccurs="unbounded"/&gt;
 *         &lt;element name="Level" type="{http://www.onvif.org/ver10/schema}FloatRange"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WideDynamicRangeOptions", propOrder = {
    "mode",
    "level"
})
public class WideDynamicRangeOptions {

    @XmlElement(name = "Mode", required = true)
    @XmlSchemaType(name = "string")
    protected List<WideDynamicMode> mode;
    @XmlElement(name = "Level", required = true)
    protected FloatRange level;

    /**
     * Gets the value of the mode property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mode property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMode().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WideDynamicMode }
     * 
     * 
     */
    public List<WideDynamicMode> getMode() {
        if (mode == null) {
            mode = new ArrayList<WideDynamicMode>();
        }
        return this.mode;
    }

    /**
     * Gets the value of the level property.
     * 
     * @return
     *     possible object is
     *     {@link FloatRange }
     *     
     */
    public FloatRange getLevel() {
        return level;
    }

    /**
     * Sets the value of the level property.
     * 
     * @param value
     *     allowed object is
     *     {@link FloatRange }
     *     
     */
    public void setLevel(FloatRange value) {
        this.level = value;
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
