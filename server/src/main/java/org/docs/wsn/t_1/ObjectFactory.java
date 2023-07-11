
package org.docs.wsn.t_1;

import org.oasis_open.docs.wsn.t_1.Documentation;
import org.oasis_open.docs.wsn.t_1.QueryExpressionType;
import org.oasis_open.docs.wsn.t_1.TopicNamespaceType;
import org.oasis_open.docs.wsn.t_1.TopicSetType;
import org.oasis_open.docs.wsn.t_1.TopicType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.oasis_open.docs.wsn.t_1 package. 
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

    private final static QName _TopicSet_QNAME = new QName("http://docs.oasis-open.org/wsn/t-1", "TopicSet");
    private final static QName _TopicNamespace_QNAME = new QName("http://docs.oasis-open.org/wsn/t-1", "TopicNamespace");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.oasis_open.docs.wsn.t_1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.t_1.TopicNamespaceType }
     * 
     */
    public org.oasis_open.docs.wsn.t_1.TopicNamespaceType createTopicNamespaceType() {
        return new org.oasis_open.docs.wsn.t_1.TopicNamespaceType();
    }

    /**
     * Create an instance of {@link TopicSetType }
     * 
     */
    public TopicSetType createTopicSetType() {
        return new TopicSetType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.t_1.Documentation }
     * 
     */
    public org.oasis_open.docs.wsn.t_1.Documentation createDocumentation() {
        return new Documentation();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.t_1.QueryExpressionType }
     * 
     */
    public org.oasis_open.docs.wsn.t_1.QueryExpressionType createQueryExpressionType() {
        return new QueryExpressionType();
    }

    /**
     * Create an instance of {@link TopicType }
     * 
     */
    public TopicType createTopicType() {
        return new TopicType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.t_1.TopicNamespaceType.Topic }
     * 
     */
    public org.oasis_open.docs.wsn.t_1.TopicNamespaceType.Topic createTopicNamespaceTypeTopic() {
        return new org.oasis_open.docs.wsn.t_1.TopicNamespaceType.Topic();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TopicSetType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TopicSetType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/t-1", name = "TopicSet")
    public JAXBElement<TopicSetType> createTopicSet(TopicSetType value) {
        return new JAXBElement<TopicSetType>(_TopicSet_QNAME, TopicSetType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.t_1.TopicNamespaceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.t_1.TopicNamespaceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/t-1", name = "TopicNamespace")
    public JAXBElement<org.oasis_open.docs.wsn.t_1.TopicNamespaceType> createTopicNamespace(org.oasis_open.docs.wsn.t_1.TopicNamespaceType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.t_1.TopicNamespaceType>(_TopicNamespace_QNAME, TopicNamespaceType.class, null, value);
    }

}
