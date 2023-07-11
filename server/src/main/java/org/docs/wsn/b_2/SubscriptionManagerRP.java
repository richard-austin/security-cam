
package org.docs.wsn.b_2;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.wsaddressing.W3CEndpointReference;


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
 *         &lt;element ref="{http://docs.oasis-open.org/wsn/b-2}ConsumerReference"/&gt;
 *         &lt;element ref="{http://docs.oasis-open.org/wsn/b-2}Filter" minOccurs="0"/&gt;
 *         &lt;element ref="{http://docs.oasis-open.org/wsn/b-2}SubscriptionPolicy" minOccurs="0"/&gt;
 *         &lt;element ref="{http://docs.oasis-open.org/wsn/b-2}CreationTime" minOccurs="0"/&gt;
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
    "consumerReference",
    "filter",
    "subscriptionPolicy",
    "creationTime"
})
@XmlRootElement(name = "SubscriptionManagerRP")
public class SubscriptionManagerRP {

    @XmlElement(name = "ConsumerReference", required = true)
    protected W3CEndpointReference consumerReference;
    @XmlElement(name = "Filter")
    protected org.oasis_open.docs.wsn.b_2.FilterType filter;
    @XmlElement(name = "SubscriptionPolicy")
    protected org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType subscriptionPolicy;
    @XmlElement(name = "CreationTime")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar creationTime;

    /**
     * Gets the value of the consumerReference property.
     * 
     * @return
     *     possible object is
     *     {@link W3CEndpointReference }
     *     
     */
    public W3CEndpointReference getConsumerReference() {
        return consumerReference;
    }

    /**
     * Sets the value of the consumerReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link W3CEndpointReference }
     *     
     */
    public void setConsumerReference(W3CEndpointReference value) {
        this.consumerReference = value;
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link org.oasis_open.docs.wsn.b_2.FilterType }
     *     
     */
    public org.oasis_open.docs.wsn.b_2.FilterType getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link org.oasis_open.docs.wsn.b_2.FilterType }
     *     
     */
    public void setFilter(FilterType value) {
        this.filter = value;
    }

    /**
     * Gets the value of the subscriptionPolicy property.
     * 
     * @return
     *     possible object is
     *     {@link org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType }
     *     
     */
    public org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType getSubscriptionPolicy() {
        return subscriptionPolicy;
    }

    /**
     * Sets the value of the subscriptionPolicy property.
     * 
     * @param value
     *     allowed object is
     *     {@link org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType }
     *     
     */
    public void setSubscriptionPolicy(SubscriptionPolicyType value) {
        this.subscriptionPolicy = value;
    }

    /**
     * Gets the value of the creationTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getCreationTime() {
        return creationTime;
    }

    /**
     * Sets the value of the creationTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setCreationTime(XMLGregorianCalendar value) {
        this.creationTime = value;
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
