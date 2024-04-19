package com.proxy;

import org.apache.activemq.ActiveMQSslConnectionFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.X509Certificate;

/**
 * ActiveMQSslConnectionFactoryNoTrustStore: All trusting ActiveMQSslConnectionFactory. using this in place of
 *                                           ActiveMQSslConnectionFactory eliminates the need for a trust store.
 *                                           The requirement for a trust store made it difficult to use security keys
 *                                           auto generated on installation. Rather you would have to have keys created
 *                                           pre-build and put into the GitHub repos for the NVR, Cloud and ActiveMQ
 *                                           as NVR and Cloud would require the ActiveMQ key in their trust stores.
 */
public class ActiveMQSslConnectionFactoryNoTrustStore extends ActiveMQSslConnectionFactory {
    ActiveMQSslConnectionFactoryNoTrustStore(String url) {
        super(url);
    }

    public ActiveMQSslConnectionFactoryNoTrustStore() {
    }

    @Override
    protected TrustManager[] createTrustManager() {
        return new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)  {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {

            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        }};
    }
}
