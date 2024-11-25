
package org.onvif.ver10.recording.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.schema.TrackConfiguration;


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
 *         &lt;element name="RecordingToken" type="{http://www.onvif.org/ver10/schema}RecordingReference"/&gt;
 *         &lt;element name="TrackToken" type="{http://www.onvif.org/ver10/schema}TrackReference"/&gt;
 *         &lt;element name="TrackConfiguration" type="{http://www.onvif.org/ver10/schema}TrackConfiguration"/&gt;
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
    "recordingToken",
    "trackToken",
    "trackConfiguration"
})
@XmlRootElement(name = "SetTrackConfiguration")
public class SetTrackConfiguration {

    @XmlElement(name = "RecordingToken", required = true)
    protected String recordingToken;
    @XmlElement(name = "TrackToken", required = true)
    protected String trackToken;
    @XmlElement(name = "TrackConfiguration", required = true)
    protected TrackConfiguration trackConfiguration;

    /**
     * Gets the value of the recordingToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecordingToken() {
        return recordingToken;
    }

    /**
     * Sets the value of the recordingToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecordingToken(String value) {
        this.recordingToken = value;
    }

    /**
     * Gets the value of the trackToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrackToken() {
        return trackToken;
    }

    /**
     * Sets the value of the trackToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrackToken(String value) {
        this.trackToken = value;
    }

    /**
     * Gets the value of the trackConfiguration property.
     * 
     * @return
     *     possible object is
     *     {@link TrackConfiguration }
     *     
     */
    public TrackConfiguration getTrackConfiguration() {
        return trackConfiguration;
    }

    /**
     * Sets the value of the trackConfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link TrackConfiguration }
     *     
     */
    public void setTrackConfiguration(TrackConfiguration value) {
        this.trackConfiguration = value;
    }

}
