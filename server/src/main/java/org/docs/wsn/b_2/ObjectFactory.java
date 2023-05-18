
package org.docs.wsn.b_2;

import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.b_2.DestroyPullPoint;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessage;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse;
import org.oasis_open.docs.wsn.b_2.GetMessages;
import org.oasis_open.docs.wsn.b_2.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.PauseFailedFaultType;
import org.oasis_open.docs.wsn.b_2.PauseSubscription;
import org.oasis_open.docs.wsn.b_2.PauseSubscriptionResponse;
import org.oasis_open.docs.wsn.b_2.QueryExpressionType;
import org.oasis_open.docs.wsn.b_2.Renew;
import org.oasis_open.docs.wsn.b_2.RenewResponse;
import org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType;
import org.oasis_open.docs.wsn.b_2.ResumeSubscription;
import org.oasis_open.docs.wsn.b_2.ResumeSubscriptionResponse;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType;
import org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType;
import org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType;
import org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType;
import org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType;
import org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType;
import org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.b_2.UnsubscribeResponse;
import org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType;
import org.oasis_open.docs.wsn.b_2.UseRaw;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.wsaddressing.W3CEndpointReference;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.oasis_open.docs.wsn.b_2 package. 
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

    private final static QName _TopicExpression_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "TopicExpression");
    private final static QName _FixedTopicSet_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "FixedTopicSet");
    private final static QName _TopicExpressionDialect_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "TopicExpressionDialect");
    private final static QName _ConsumerReference_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "ConsumerReference");
    private final static QName _Filter_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "Filter");
    private final static QName _SubscriptionPolicy_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "SubscriptionPolicy");
    private final static QName _CreationTime_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "CreationTime");
    private final static QName _SubscriptionReference_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "SubscriptionReference");
    private final static QName _Topic_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "Topic");
    private final static QName _ProducerReference_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "ProducerReference");
    private final static QName _NotificationMessage_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "NotificationMessage");
    private final static QName _CurrentTime_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "CurrentTime");
    private final static QName _TerminationTime_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "TerminationTime");
    private final static QName _ProducerProperties_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "ProducerProperties");
    private final static QName _MessageContent_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "MessageContent");
    private final static QName _SubscribeCreationFailedFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "SubscribeCreationFailedFault");
    private final static QName _InvalidFilterFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "InvalidFilterFault");
    private final static QName _TopicExpressionDialectUnknownFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "TopicExpressionDialectUnknownFault");
    private final static QName _InvalidTopicExpressionFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "InvalidTopicExpressionFault");
    private final static QName _TopicNotSupportedFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "TopicNotSupportedFault");
    private final static QName _MultipleTopicsSpecifiedFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "MultipleTopicsSpecifiedFault");
    private final static QName _InvalidProducerPropertiesExpressionFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "InvalidProducerPropertiesExpressionFault");
    private final static QName _InvalidMessageContentExpressionFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "InvalidMessageContentExpressionFault");
    private final static QName _UnrecognizedPolicyRequestFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnrecognizedPolicyRequestFault");
    private final static QName _UnsupportedPolicyRequestFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnsupportedPolicyRequestFault");
    private final static QName _NotifyMessageNotSupportedFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "NotifyMessageNotSupportedFault");
    private final static QName _UnacceptableInitialTerminationTimeFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnacceptableInitialTerminationTimeFault");
    private final static QName _NoCurrentMessageOnTopicFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "NoCurrentMessageOnTopicFault");
    private final static QName _UnableToGetMessagesFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnableToGetMessagesFault");
    private final static QName _UnableToDestroyPullPointFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnableToDestroyPullPointFault");
    private final static QName _UnableToCreatePullPointFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnableToCreatePullPointFault");
    private final static QName _UnacceptableTerminationTimeFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnacceptableTerminationTimeFault");
    private final static QName _UnableToDestroySubscriptionFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "UnableToDestroySubscriptionFault");
    private final static QName _PauseFailedFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "PauseFailedFault");
    private final static QName _ResumeFailedFault_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "ResumeFailedFault");
    private final static QName _SubscribeInitialTerminationTime_QNAME = new QName("http://docs.oasis-open.org/wsn/b-2", "InitialTerminationTime");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.oasis_open.docs.wsn.b_2
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.Subscribe }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.Subscribe createSubscribe() {
        return new org.oasis_open.docs.wsn.b_2.Subscribe();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType createNotificationMessageHolderType() {
        return new org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.TopicExpressionType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.TopicExpressionType createTopicExpressionType() {
        return new org.oasis_open.docs.wsn.b_2.TopicExpressionType();
    }

    /**
     * Create an instance of {@link NotificationProducerRP }
     * 
     */
    public NotificationProducerRP createNotificationProducerRP() {
        return new NotificationProducerRP();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.FilterType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.FilterType createFilterType() {
        return new org.oasis_open.docs.wsn.b_2.FilterType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType createSubscriptionPolicyType() {
        return new org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType();
    }

    /**
     * Create an instance of {@link SubscriptionManagerRP }
     * 
     */
    public SubscriptionManagerRP createSubscriptionManagerRP() {
        return new SubscriptionManagerRP();
    }

    /**
     * Create an instance of {@link Notify }
     * 
     */
    public Notify createNotify() {
        return new Notify();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.QueryExpressionType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.QueryExpressionType createQueryExpressionType() {
        return new org.oasis_open.docs.wsn.b_2.QueryExpressionType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UseRaw }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UseRaw createUseRaw() {
        return new UseRaw();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.Subscribe.SubscriptionPolicy }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.Subscribe.SubscriptionPolicy createSubscribeSubscriptionPolicy() {
        return new org.oasis_open.docs.wsn.b_2.Subscribe.SubscriptionPolicy();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.SubscribeResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.SubscribeResponse createSubscribeResponse() {
        return new SubscribeResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.GetCurrentMessage }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.GetCurrentMessage createGetCurrentMessage() {
        return new GetCurrentMessage();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse createGetCurrentMessageResponse() {
        return new GetCurrentMessageResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType createSubscribeCreationFailedFaultType() {
        return new org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType();
    }

    /**
     * Create an instance of {@link InvalidFilterFaultType }
     * 
     */
    public InvalidFilterFaultType createInvalidFilterFaultType() {
        return new InvalidFilterFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType createTopicExpressionDialectUnknownFaultType() {
        return new org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType createInvalidTopicExpressionFaultType() {
        return new org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType createTopicNotSupportedFaultType() {
        return new org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType();
    }

    /**
     * Create an instance of {@link MultipleTopicsSpecifiedFaultType }
     * 
     */
    public MultipleTopicsSpecifiedFaultType createMultipleTopicsSpecifiedFaultType() {
        return new MultipleTopicsSpecifiedFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType createInvalidProducerPropertiesExpressionFaultType() {
        return new org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType();
    }

    /**
     * Create an instance of {@link InvalidMessageContentExpressionFaultType }
     * 
     */
    public InvalidMessageContentExpressionFaultType createInvalidMessageContentExpressionFaultType() {
        return new InvalidMessageContentExpressionFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType createUnrecognizedPolicyRequestFaultType() {
        return new org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType createUnsupportedPolicyRequestFaultType() {
        return new org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType();
    }

    /**
     * Create an instance of {@link NotifyMessageNotSupportedFaultType }
     * 
     */
    public NotifyMessageNotSupportedFaultType createNotifyMessageNotSupportedFaultType() {
        return new NotifyMessageNotSupportedFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType createUnacceptableInitialTerminationTimeFaultType() {
        return new org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType();
    }

    /**
     * Create an instance of {@link NoCurrentMessageOnTopicFaultType }
     * 
     */
    public NoCurrentMessageOnTopicFaultType createNoCurrentMessageOnTopicFaultType() {
        return new NoCurrentMessageOnTopicFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.GetMessages }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.GetMessages createGetMessages() {
        return new GetMessages();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.GetMessagesResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.GetMessagesResponse createGetMessagesResponse() {
        return new GetMessagesResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.DestroyPullPoint }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.DestroyPullPoint createDestroyPullPoint() {
        return new DestroyPullPoint();
    }

    /**
     * Create an instance of {@link DestroyPullPointResponse }
     * 
     */
    public DestroyPullPointResponse createDestroyPullPointResponse() {
        return new DestroyPullPointResponse();
    }

    /**
     * Create an instance of {@link UnableToGetMessagesFaultType }
     * 
     */
    public UnableToGetMessagesFaultType createUnableToGetMessagesFaultType() {
        return new UnableToGetMessagesFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType createUnableToDestroyPullPointFaultType() {
        return new org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.CreatePullPoint }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.CreatePullPoint createCreatePullPoint() {
        return new CreatePullPoint();
    }

    /**
     * Create an instance of {@link CreatePullPointResponse }
     * 
     */
    public CreatePullPointResponse createCreatePullPointResponse() {
        return new CreatePullPointResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType createUnableToCreatePullPointFaultType() {
        return new org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.Renew }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.Renew createRenew() {
        return new Renew();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.RenewResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.RenewResponse createRenewResponse() {
        return new RenewResponse();
    }

    /**
     * Create an instance of {@link UnacceptableTerminationTimeFaultType }
     * 
     */
    public UnacceptableTerminationTimeFaultType createUnacceptableTerminationTimeFaultType() {
        return new UnacceptableTerminationTimeFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.Unsubscribe }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.Unsubscribe createUnsubscribe() {
        return new Unsubscribe();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnsubscribeResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnsubscribeResponse createUnsubscribeResponse() {
        return new UnsubscribeResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType createUnableToDestroySubscriptionFaultType() {
        return new org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.PauseSubscription }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.PauseSubscription createPauseSubscription() {
        return new PauseSubscription();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.PauseSubscriptionResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.PauseSubscriptionResponse createPauseSubscriptionResponse() {
        return new PauseSubscriptionResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.ResumeSubscription }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.ResumeSubscription createResumeSubscription() {
        return new ResumeSubscription();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.ResumeSubscriptionResponse }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.ResumeSubscriptionResponse createResumeSubscriptionResponse() {
        return new ResumeSubscriptionResponse();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.PauseFailedFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.PauseFailedFaultType createPauseFailedFaultType() {
        return new org.oasis_open.docs.wsn.b_2.PauseFailedFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType createResumeFailedFaultType() {
        return new org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType();
    }

    /**
     * Create an instance of {@link org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType.Message }
     * 
     */
    public org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType.Message createNotificationMessageHolderTypeMessage() {
        return new org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType.Message();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicExpressionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicExpressionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "TopicExpression")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.TopicExpressionType> createTopicExpression(org.oasis_open.docs.wsn.b_2.TopicExpressionType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.TopicExpressionType>(_TopicExpression_QNAME, org.oasis_open.docs.wsn.b_2.TopicExpressionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "FixedTopicSet", defaultValue = "true")
    public JAXBElement<Boolean> createFixedTopicSet(Boolean value) {
        return new JAXBElement<Boolean>(_FixedTopicSet_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "TopicExpressionDialect")
    public JAXBElement<String> createTopicExpressionDialect(String value) {
        return new JAXBElement<String>(_TopicExpressionDialect_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link W3CEndpointReference }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link W3CEndpointReference }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "ConsumerReference")
    public JAXBElement<W3CEndpointReference> createConsumerReference(W3CEndpointReference value) {
        return new JAXBElement<W3CEndpointReference>(_ConsumerReference_QNAME, W3CEndpointReference.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.FilterType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.FilterType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "Filter")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.FilterType> createFilter(org.oasis_open.docs.wsn.b_2.FilterType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.FilterType>(_Filter_QNAME, FilterType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "SubscriptionPolicy")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType> createSubscriptionPolicy(org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.SubscriptionPolicyType>(_SubscriptionPolicy_QNAME, SubscriptionPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "CreationTime")
    public JAXBElement<XMLGregorianCalendar> createCreationTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_CreationTime_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link W3CEndpointReference }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link W3CEndpointReference }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "SubscriptionReference")
    public JAXBElement<W3CEndpointReference> createSubscriptionReference(W3CEndpointReference value) {
        return new JAXBElement<W3CEndpointReference>(_SubscriptionReference_QNAME, W3CEndpointReference.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicExpressionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicExpressionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "Topic")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.TopicExpressionType> createTopic(org.oasis_open.docs.wsn.b_2.TopicExpressionType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.TopicExpressionType>(_Topic_QNAME, TopicExpressionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link W3CEndpointReference }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link W3CEndpointReference }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "ProducerReference")
    public JAXBElement<W3CEndpointReference> createProducerReference(W3CEndpointReference value) {
        return new JAXBElement<W3CEndpointReference>(_ProducerReference_QNAME, W3CEndpointReference.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "NotificationMessage")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType> createNotificationMessage(org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType>(_NotificationMessage_QNAME, NotificationMessageHolderType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "CurrentTime")
    public JAXBElement<XMLGregorianCalendar> createCurrentTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_CurrentTime_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "TerminationTime")
    public JAXBElement<XMLGregorianCalendar> createTerminationTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_TerminationTime_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.QueryExpressionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.QueryExpressionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "ProducerProperties")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.QueryExpressionType> createProducerProperties(org.oasis_open.docs.wsn.b_2.QueryExpressionType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.QueryExpressionType>(_ProducerProperties_QNAME, org.oasis_open.docs.wsn.b_2.QueryExpressionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.QueryExpressionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.QueryExpressionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "MessageContent")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.QueryExpressionType> createMessageContent(org.oasis_open.docs.wsn.b_2.QueryExpressionType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.QueryExpressionType>(_MessageContent_QNAME, QueryExpressionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "SubscribeCreationFailedFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType> createSubscribeCreationFailedFault(org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType>(_SubscribeCreationFailedFault_QNAME, SubscribeCreationFailedFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvalidFilterFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InvalidFilterFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "InvalidFilterFault")
    public JAXBElement<InvalidFilterFaultType> createInvalidFilterFault(InvalidFilterFaultType value) {
        return new JAXBElement<InvalidFilterFaultType>(_InvalidFilterFault_QNAME, InvalidFilterFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "TopicExpressionDialectUnknownFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType> createTopicExpressionDialectUnknownFault(org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType>(_TopicExpressionDialectUnknownFault_QNAME, TopicExpressionDialectUnknownFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "InvalidTopicExpressionFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType> createInvalidTopicExpressionFault(org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType>(_InvalidTopicExpressionFault_QNAME, InvalidTopicExpressionFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "TopicNotSupportedFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType> createTopicNotSupportedFault(org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.TopicNotSupportedFaultType>(_TopicNotSupportedFault_QNAME, TopicNotSupportedFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MultipleTopicsSpecifiedFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link MultipleTopicsSpecifiedFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "MultipleTopicsSpecifiedFault")
    public JAXBElement<MultipleTopicsSpecifiedFaultType> createMultipleTopicsSpecifiedFault(MultipleTopicsSpecifiedFaultType value) {
        return new JAXBElement<MultipleTopicsSpecifiedFaultType>(_MultipleTopicsSpecifiedFault_QNAME, MultipleTopicsSpecifiedFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "InvalidProducerPropertiesExpressionFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType> createInvalidProducerPropertiesExpressionFault(org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.InvalidProducerPropertiesExpressionFaultType>(_InvalidProducerPropertiesExpressionFault_QNAME, InvalidProducerPropertiesExpressionFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvalidMessageContentExpressionFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InvalidMessageContentExpressionFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "InvalidMessageContentExpressionFault")
    public JAXBElement<InvalidMessageContentExpressionFaultType> createInvalidMessageContentExpressionFault(InvalidMessageContentExpressionFaultType value) {
        return new JAXBElement<InvalidMessageContentExpressionFaultType>(_InvalidMessageContentExpressionFault_QNAME, InvalidMessageContentExpressionFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnrecognizedPolicyRequestFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType> createUnrecognizedPolicyRequestFault(org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.UnrecognizedPolicyRequestFaultType>(_UnrecognizedPolicyRequestFault_QNAME, UnrecognizedPolicyRequestFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnsupportedPolicyRequestFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType> createUnsupportedPolicyRequestFault(org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.UnsupportedPolicyRequestFaultType>(_UnsupportedPolicyRequestFault_QNAME, UnsupportedPolicyRequestFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NotifyMessageNotSupportedFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NotifyMessageNotSupportedFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "NotifyMessageNotSupportedFault")
    public JAXBElement<NotifyMessageNotSupportedFaultType> createNotifyMessageNotSupportedFault(NotifyMessageNotSupportedFaultType value) {
        return new JAXBElement<NotifyMessageNotSupportedFaultType>(_NotifyMessageNotSupportedFault_QNAME, NotifyMessageNotSupportedFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnacceptableInitialTerminationTimeFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType> createUnacceptableInitialTerminationTimeFault(org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.UnacceptableInitialTerminationTimeFaultType>(_UnacceptableInitialTerminationTimeFault_QNAME, UnacceptableInitialTerminationTimeFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NoCurrentMessageOnTopicFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NoCurrentMessageOnTopicFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "NoCurrentMessageOnTopicFault")
    public JAXBElement<NoCurrentMessageOnTopicFaultType> createNoCurrentMessageOnTopicFault(NoCurrentMessageOnTopicFaultType value) {
        return new JAXBElement<NoCurrentMessageOnTopicFaultType>(_NoCurrentMessageOnTopicFault_QNAME, NoCurrentMessageOnTopicFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnableToGetMessagesFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UnableToGetMessagesFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnableToGetMessagesFault")
    public JAXBElement<UnableToGetMessagesFaultType> createUnableToGetMessagesFault(UnableToGetMessagesFaultType value) {
        return new JAXBElement<UnableToGetMessagesFaultType>(_UnableToGetMessagesFault_QNAME, UnableToGetMessagesFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnableToDestroyPullPointFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType> createUnableToDestroyPullPointFault(org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType>(_UnableToDestroyPullPointFault_QNAME, UnableToDestroyPullPointFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnableToCreatePullPointFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType> createUnableToCreatePullPointFault(org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType>(_UnableToCreatePullPointFault_QNAME, UnableToCreatePullPointFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UnacceptableTerminationTimeFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UnacceptableTerminationTimeFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnacceptableTerminationTimeFault")
    public JAXBElement<UnacceptableTerminationTimeFaultType> createUnacceptableTerminationTimeFault(UnacceptableTerminationTimeFaultType value) {
        return new JAXBElement<UnacceptableTerminationTimeFaultType>(_UnacceptableTerminationTimeFault_QNAME, UnacceptableTerminationTimeFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "UnableToDestroySubscriptionFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType> createUnableToDestroySubscriptionFault(org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType>(_UnableToDestroySubscriptionFault_QNAME, UnableToDestroySubscriptionFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.PauseFailedFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.PauseFailedFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "PauseFailedFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.PauseFailedFaultType> createPauseFailedFault(org.oasis_open.docs.wsn.b_2.PauseFailedFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.PauseFailedFaultType>(_PauseFailedFault_QNAME, PauseFailedFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "ResumeFailedFault")
    public JAXBElement<org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType> createResumeFailedFault(org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType value) {
        return new JAXBElement<org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType>(_ResumeFailedFault_QNAME, ResumeFailedFaultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/wsn/b-2", name = "InitialTerminationTime", scope = org.oasis_open.docs.wsn.b_2.Subscribe.class)
    public JAXBElement<String> createSubscribeInitialTerminationTime(String value) {
        return new JAXBElement<String>(_SubscribeInitialTerminationTime_QNAME, String.class, Subscribe.class, value);
    }

}
