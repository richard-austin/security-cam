
package org.onvif.ver10.recording.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.schema.RecordingConfiguration;


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
 *         &lt;element name="RecordingConfiguration" type="{http://www.onvif.org/ver10/schema}RecordingConfiguration"/&gt;
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
    "recordingConfiguration"
})
@XmlRootElement(name = "GetRecordingConfigurationResponse")
public class GetRecordingConfigurationResponse {

    @XmlElement(name = "RecordingConfiguration", required = true)
    protected RecordingConfiguration recordingConfiguration;

    /**
     * Gets the value of the recordingConfiguration property.
     * 
     * @return
     *     possible object is
     *     {@link RecordingConfiguration }
     *     
     */
    public RecordingConfiguration getRecordingConfiguration() {
        return recordingConfiguration;
    }

    /**
     * Sets the value of the recordingConfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link RecordingConfiguration }
     *     
     */
    public void setRecordingConfiguration(RecordingConfiguration value) {
        this.recordingConfiguration = value;
    }

}
