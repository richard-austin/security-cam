
package org.onvif.ver10.recording.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.schema.RecordingJobConfiguration;


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
 *         &lt;element name="JobConfiguration" type="{http://www.onvif.org/ver10/schema}RecordingJobConfiguration"/&gt;
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
    "jobConfiguration"
})
@XmlRootElement(name = "CreateRecordingJob")
public class CreateRecordingJob {

    @XmlElement(name = "JobConfiguration", required = true)
    protected RecordingJobConfiguration jobConfiguration;

    /**
     * Gets the value of the jobConfiguration property.
     * 
     * @return
     *     possible object is
     *     {@link RecordingJobConfiguration }
     *     
     */
    public RecordingJobConfiguration getJobConfiguration() {
        return jobConfiguration;
    }

    /**
     * Sets the value of the jobConfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link RecordingJobConfiguration }
     *     
     */
    public void setJobConfiguration(RecordingJobConfiguration value) {
        this.jobConfiguration = value;
    }

}
