
package org.onvif.ver10.search.wsdl;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;
import org.onvif.ver10.schema.RecordingInformation;

import javax.xml.bind.annotation.*;


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
 *         &lt;element name="RecordingInformation" type="{http://www.onvif.org/ver10/schema}RecordingInformation"/&gt;
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
    "recordingInformation"
})
@XmlRootElement(name = "GetRecordingInformationResponse")
public class GetRecordingInformationResponse {

    @XmlElement(name = "RecordingInformation", required = true)
    protected RecordingInformation recordingInformation;

    /**
     * Gets the value of the recordingInformation property.
     * 
     * @return
     *     possible object is
     *     {@link RecordingInformation }
     *     
     */
    public RecordingInformation getRecordingInformation() {
        return recordingInformation;
    }

    /**
     * Sets the value of the recordingInformation property.
     * 
     * @param value
     *     allowed object is
     *     {@link RecordingInformation }
     *     
     */
    public void setRecordingInformation(RecordingInformation value) {
        this.recordingInformation = value;
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
