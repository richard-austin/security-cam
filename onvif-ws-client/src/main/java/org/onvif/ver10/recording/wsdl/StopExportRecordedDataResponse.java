
package org.onvif.ver10.recording.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.onvif.ver10.schema.ArrayOfFileProgress;
import org.w3c.dom.Element;


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
 *         &lt;element name="Progress" type="{http://www.w3.org/2001/XMLSchema}float"/&gt;
 *         &lt;element name="FileProgressStatus" type="{http://www.onvif.org/ver10/schema}ArrayOfFileProgress"/&gt;
 *         &lt;any processContents='lax'/&gt;
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
    "progress",
    "fileProgressStatus",
    "any"
})
@XmlRootElement(name = "StopExportRecordedDataResponse")
public class StopExportRecordedDataResponse {

    @XmlElement(name = "Progress")
    protected float progress;
    @XmlElement(name = "FileProgressStatus", required = true)
    protected ArrayOfFileProgress fileProgressStatus;
    @XmlAnyElement(lax = true)
    protected Object any;

    /**
     * Gets the value of the progress property.
     * 
     */
    public float getProgress() {
        return progress;
    }

    /**
     * Sets the value of the progress property.
     * 
     */
    public void setProgress(float value) {
        this.progress = value;
    }

    /**
     * Gets the value of the fileProgressStatus property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfFileProgress }
     *     
     */
    public ArrayOfFileProgress getFileProgressStatus() {
        return fileProgressStatus;
    }

    /**
     * Sets the value of the fileProgressStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfFileProgress }
     *     
     */
    public void setFileProgressStatus(ArrayOfFileProgress value) {
        this.fileProgressStatus = value;
    }

    /**
     * Gets the value of the any property.
     * 
     * @return
     *     possible object is
     *     {@link Element }
     *     {@link Object }
     *     
     */
    public Object getAny() {
        return any;
    }

    /**
     * Sets the value of the any property.
     * 
     * @param value
     *     allowed object is
     *     {@link Element }
     *     {@link Object }
     *     
     */
    public void setAny(Object value) {
        this.any = value;
    }

}
