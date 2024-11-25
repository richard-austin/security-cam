
package org.onvif.ver10.recording.wsdl;

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
 *         &lt;element name="OperationToken" type="{http://www.onvif.org/ver10/schema}ReferenceToken"/&gt;
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
    "operationToken"
})
@XmlRootElement(name = "StopExportRecordedData")
public class StopExportRecordedData {

    @XmlElement(name = "OperationToken", required = true)
    protected String operationToken;

    /**
     * Gets the value of the operationToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOperationToken() {
        return operationToken;
    }

    /**
     * Sets the value of the operationToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOperationToken(String value) {
        this.operationToken = value;
    }

}
