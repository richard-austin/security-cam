
package org.onvif.ver10.media.wsdl;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;
import org.onvif.ver10.schema.MediaUri;

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
 *         &lt;element name="MediaUri" type="{http://www.onvif.org/ver10/schema}MediaUri"/&gt;
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
    "mediaUri"
})
@XmlRootElement(name = "GetSnapshotUriResponse")
public class GetSnapshotUriResponse {

    @XmlElement(name = "MediaUri", required = true)
    protected MediaUri mediaUri;

    /**
     * Gets the value of the mediaUri property.
     * 
     * @return
     *     possible object is
     *     {@link MediaUri }
     *     
     */
    public MediaUri getMediaUri() {
        return mediaUri;
    }

    /**
     * Sets the value of the mediaUri property.
     * 
     * @param value
     *     allowed object is
     *     {@link MediaUri }
     *     
     */
    public void setMediaUri(MediaUri value) {
        this.mediaUri = value;
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
