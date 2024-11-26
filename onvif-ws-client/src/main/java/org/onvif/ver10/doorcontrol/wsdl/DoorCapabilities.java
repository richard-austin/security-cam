
package org.onvif.ver10.doorcontrol.wsdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * 
 *             DoorCapabilities reflect optional functionality of a particular physical entity.
 *             Different door instances may have different set of capabilities.
 *             This information may change during device operation, e.g. if hardware settings are
 *             changed.
 *             The following capabilities are available:
 *           
 * 
 * <p>Java class for DoorCapabilities complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DoorCapabilities"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;any processContents='lax' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="Access" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="AccessTimingOverride" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="Lock" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="Unlock" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="Block" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="DoubleLock" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="LockDown" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="LockOpen" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="DoorMonitor" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="LockMonitor" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="DoubleLockMonitor" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="Alarm" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="Tamper" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="Fault" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;anyAttribute processContents='lax'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DoorCapabilities", propOrder = {
    "any"
})
public class DoorCapabilities {

    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "Access")
    protected Boolean access;
    @XmlAttribute(name = "AccessTimingOverride")
    protected Boolean accessTimingOverride;
    @XmlAttribute(name = "Lock")
    protected Boolean lock;
    @XmlAttribute(name = "Unlock")
    protected Boolean unlock;
    @XmlAttribute(name = "Block")
    protected Boolean block;
    @XmlAttribute(name = "DoubleLock")
    protected Boolean doubleLock;
    @XmlAttribute(name = "LockDown")
    protected Boolean lockDown;
    @XmlAttribute(name = "LockOpen")
    protected Boolean lockOpen;
    @XmlAttribute(name = "DoorMonitor")
    protected Boolean doorMonitor;
    @XmlAttribute(name = "LockMonitor")
    protected Boolean lockMonitor;
    @XmlAttribute(name = "DoubleLockMonitor")
    protected Boolean doubleLockMonitor;
    @XmlAttribute(name = "Alarm")
    protected Boolean alarm;
    @XmlAttribute(name = "Tamper")
    protected Boolean tamper;
    @XmlAttribute(name = "Fault")
    protected Boolean fault;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
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
     * {@link Object }
     * {@link Element }
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
     * Gets the value of the access property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAccess() {
        return access;
    }

    /**
     * Sets the value of the access property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAccess(Boolean value) {
        this.access = value;
    }

    /**
     * Gets the value of the accessTimingOverride property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAccessTimingOverride() {
        return accessTimingOverride;
    }

    /**
     * Sets the value of the accessTimingOverride property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAccessTimingOverride(Boolean value) {
        this.accessTimingOverride = value;
    }

    /**
     * Gets the value of the lock property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isLock() {
        return lock;
    }

    /**
     * Sets the value of the lock property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLock(Boolean value) {
        this.lock = value;
    }

    /**
     * Gets the value of the unlock property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isUnlock() {
        return unlock;
    }

    /**
     * Sets the value of the unlock property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUnlock(Boolean value) {
        this.unlock = value;
    }

    /**
     * Gets the value of the block property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBlock() {
        return block;
    }

    /**
     * Sets the value of the block property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBlock(Boolean value) {
        this.block = value;
    }

    /**
     * Gets the value of the doubleLock property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDoubleLock() {
        return doubleLock;
    }

    /**
     * Sets the value of the doubleLock property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDoubleLock(Boolean value) {
        this.doubleLock = value;
    }

    /**
     * Gets the value of the lockDown property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isLockDown() {
        return lockDown;
    }

    /**
     * Sets the value of the lockDown property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLockDown(Boolean value) {
        this.lockDown = value;
    }

    /**
     * Gets the value of the lockOpen property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isLockOpen() {
        return lockOpen;
    }

    /**
     * Sets the value of the lockOpen property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLockOpen(Boolean value) {
        this.lockOpen = value;
    }

    /**
     * Gets the value of the doorMonitor property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDoorMonitor() {
        return doorMonitor;
    }

    /**
     * Sets the value of the doorMonitor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDoorMonitor(Boolean value) {
        this.doorMonitor = value;
    }

    /**
     * Gets the value of the lockMonitor property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isLockMonitor() {
        return lockMonitor;
    }

    /**
     * Sets the value of the lockMonitor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLockMonitor(Boolean value) {
        this.lockMonitor = value;
    }

    /**
     * Gets the value of the doubleLockMonitor property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDoubleLockMonitor() {
        return doubleLockMonitor;
    }

    /**
     * Sets the value of the doubleLockMonitor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDoubleLockMonitor(Boolean value) {
        this.doubleLockMonitor = value;
    }

    /**
     * Gets the value of the alarm property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAlarm() {
        return alarm;
    }

    /**
     * Sets the value of the alarm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAlarm(Boolean value) {
        this.alarm = value;
    }

    /**
     * Gets the value of the tamper property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTamper() {
        return tamper;
    }

    /**
     * Sets the value of the tamper property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTamper(Boolean value) {
        this.tamper = value;
    }

    /**
     * Gets the value of the fault property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isFault() {
        return fault;
    }

    /**
     * Sets the value of the fault property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFault(Boolean value) {
        this.fault = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
