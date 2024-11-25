
package org.onvif.ver10.search.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import org.onvif.ver10.schema.MetadataFilter;
import org.onvif.ver10.schema.SearchScope;


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
 *         &lt;element name="StartPoint" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="EndPoint" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *         &lt;element name="Scope" type="{http://www.onvif.org/ver10/schema}SearchScope"/&gt;
 *         &lt;element name="MetadataFilter" type="{http://www.onvif.org/ver10/schema}MetadataFilter"/&gt;
 *         &lt;element name="MaxMatches" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="KeepAliveTime" type="{http://www.w3.org/2001/XMLSchema}duration"/&gt;
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
    "startPoint",
    "endPoint",
    "scope",
    "metadataFilter",
    "maxMatches",
    "keepAliveTime"
})
@XmlRootElement(name = "FindMetadata")
public class FindMetadata {

    @XmlElement(name = "StartPoint", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startPoint;
    @XmlElement(name = "EndPoint")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endPoint;
    @XmlElement(name = "Scope", required = true)
    protected SearchScope scope;
    @XmlElement(name = "MetadataFilter", required = true)
    protected MetadataFilter metadataFilter;
    @XmlElement(name = "MaxMatches")
    protected Integer maxMatches;
    @XmlElement(name = "KeepAliveTime", required = true)
    protected Duration keepAliveTime;

    /**
     * Gets the value of the startPoint property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getStartPoint() {
        return startPoint;
    }

    /**
     * Sets the value of the startPoint property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setStartPoint(XMLGregorianCalendar value) {
        this.startPoint = value;
    }

    /**
     * Gets the value of the endPoint property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getEndPoint() {
        return endPoint;
    }

    /**
     * Sets the value of the endPoint property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setEndPoint(XMLGregorianCalendar value) {
        this.endPoint = value;
    }

    /**
     * Gets the value of the scope property.
     * 
     * @return
     *     possible object is
     *     {@link SearchScope }
     *     
     */
    public SearchScope getScope() {
        return scope;
    }

    /**
     * Sets the value of the scope property.
     * 
     * @param value
     *     allowed object is
     *     {@link SearchScope }
     *     
     */
    public void setScope(SearchScope value) {
        this.scope = value;
    }

    /**
     * Gets the value of the metadataFilter property.
     * 
     * @return
     *     possible object is
     *     {@link MetadataFilter }
     *     
     */
    public MetadataFilter getMetadataFilter() {
        return metadataFilter;
    }

    /**
     * Sets the value of the metadataFilter property.
     * 
     * @param value
     *     allowed object is
     *     {@link MetadataFilter }
     *     
     */
    public void setMetadataFilter(MetadataFilter value) {
        this.metadataFilter = value;
    }

    /**
     * Gets the value of the maxMatches property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMaxMatches() {
        return maxMatches;
    }

    /**
     * Sets the value of the maxMatches property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxMatches(Integer value) {
        this.maxMatches = value;
    }

    /**
     * Gets the value of the keepAliveTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * Sets the value of the keepAliveTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setKeepAliveTime(Duration value) {
        this.keepAliveTime = value;
    }

}
