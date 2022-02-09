package com.proxy;

import grails.config.Config;
import grails.core.GrailsApplication;

public final class CloudProxyProperties {
    GrailsApplication grailsApplication;
    static private CloudProxyProperties theInstance;

    private CloudProxyProperties()
    {
    }

    static CloudProxyProperties getInstance()
    {
        return theInstance;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
        CloudProxyProperties.theInstance = this;
        setupConfigParams();
    }

    private void setupConfigParams()
    {
        Config config = grailsApplication.getConfig();
        TRUSTSTORE_PATH = config.getProperty("cloudProxy.trustStorePath");
        CLOUD_PROXY_KEYSTORE_PATH = config.getProperty("cloudProxy.keyStorePath");
        TRUSTSTORE_PASSWORD = config.getProperty("cloudProxy.trustStorePassword");
        CLOUD_PROXY_KEYSTORE_PASSWORD = config.getProperty("cloudProxy.keyStorePassword");
        PRODUCT_KEY_PATH = config.getProperty("cloudProxy.productKeyPath");
    }

    static final int REQUEST_TIMEOUT_SECS = 300;
    private String TRUSTSTORE_PATH;
    private String CLOUD_PROXY_KEYSTORE_PATH ;
    private String CLOUD_PROXY_KEYSTORE_PASSWORD ;
    private String TRUSTSTORE_PASSWORD;
    private String PRODUCT_KEY_PATH;

    public String getTRUSTSTORE_PATH() {
        return TRUSTSTORE_PATH;
    }

    public String getCLOUD_PROXY_KEYSTORE_PATH() {
        return CLOUD_PROXY_KEYSTORE_PATH;
    }

    public String getCLOUD_PROXY_KEYSTORE_PASSWORD() {
        return CLOUD_PROXY_KEYSTORE_PASSWORD;
    }

    public String getTRUSTSTORE_PASSWORD() {
        return TRUSTSTORE_PASSWORD;
    }


    public String getPRODUCT_KEY_PATH() { return PRODUCT_KEY_PATH; }
}

