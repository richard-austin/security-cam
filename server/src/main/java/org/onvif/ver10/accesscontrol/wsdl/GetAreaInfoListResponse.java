
package org.onvif.ver10.accesscontrol.wsdl;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


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
 *         &lt;element name="AreaInfo" type="{http://www.onvif.org/ver10/accesscontrol/wsdl}AreaInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
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
    "areaInfo"
})
@XmlRootElement(name = "GetAreaInfoListResponse")
public class GetAreaInfoListResponse {

    @XmlElement(name = "NextStartReference")
    protected String nextStartReference;
    @XmlElement(name = "AreaInfo")
    protected List<AreaInfo> areaInfo;

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
     * Gets the value of the areaInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the areaInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAreaInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AreaInfo }
     * 
     * 
     */
    public List<AreaInfo> getAreaInfo() {
        if (areaInfo == null) {
            areaInfo = new ArrayList<AreaInfo>();
        }
        return this.areaInfo;
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
