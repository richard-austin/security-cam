
package org.docs.wsrf.bf_2;

import org.oasis_open.docs.wsrf.bf_2.BaseFaultType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.oasis_open.docs.wsrf.bf_2 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _BaseFault_QNAME = new QName("http://docs.oasis-open.org/wsrf/bf-2", "BaseFault");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.oasis_open.docs.wsrf.bf_2
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsrf.bf_2.BaseFaultType }
     * 
     */
    public org.oasis_open.docs.wsrf.bf_2.BaseFaultType createBaseFaultType() {
        return new org.oasis_open.docs.wsrf.bf_2.BaseFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsrf.bf_2.BaseFaultType.ErrorCode }
     * 
     */
    public org.oasis_open.docs.wsrf.bf_2.BaseFaultType.ErrorCode createBaseFaultTypeErrorCode() {
        return new org.oasis_open.docs.wsrf.bf_2.BaseFaultType.ErrorCode();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsrf.bf_2.BaseFaultType.Description }
     * 
     */
    public org.oasis_open.docs.wsrf.bf_2.BaseFaultType.Description createBaseFaultTypeDescription() {
        return new org.oasis_open.docs.wsrf.bf_2.BaseFaultType.Description();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsrf.bf_2.BaseFaultType.FaultCause }
     * 
     */
    public org.oasis_open.docs.wsrf.bf_2.BaseFaultType.FaultCause createBaseFaultTypeFaultCause() {
        return new org.oasis_open.docs.wsrf.bf_2.BaseFaultType.FaultCause();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsrf.bf_2.BaseFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsrf.bf_2.BaseFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsrf/bf-2", name = "BaseFault")
    public JAXBElement<org.oasis_open.docs.wsrf.bf_2.BaseFaultType> createBaseFault(org.oasis_open.docs.wsrf.bf_2.BaseFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsrf.bf_2.BaseFaultType>(_BaseFault_QNAME, BaseFaultType.class, null, value);
    }

}
