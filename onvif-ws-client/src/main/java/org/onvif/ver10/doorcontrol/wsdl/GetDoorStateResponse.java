
package org.onvif.ver10.doorcontrol.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="DoorState" type="{http://www.onvif.org/ver10/doorcontrol/wsdl}DoorState"/&gt;
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
    "doorState"
})
@XmlRootElement(name = "GetDoorStateResponse")
public class GetDoorStateResponse {

    @XmlElement(name = "DoorState", required = true)
    protected DoorState doorState;

    /**
     * Gets the value of the doorState property.
     * 
     * @return
     *     possible object is
     *     {@link DoorState }
     *     
     */
    public DoorState getDoorState() {
        return doorState;
    }

    /**
     * Sets the value of the doorState property.
     * 
     * @param value
     *     allowed object is
     *     {@link DoorState }
     *     
     */
    public void setDoorState(DoorState value) {
        this.doorState = value;
    }

}
