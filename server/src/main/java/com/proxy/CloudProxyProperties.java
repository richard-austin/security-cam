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
        MQ_TRUSTSTORE_PATH = config.getProperty("cloudProxy.mqTrustStorePath");
        MQ_CLOUD_PROXY_KEYSTORE_PATH = config.getProperty("cloudProxy.mqKeyStorePath");
        MQ_TRUSTSTORE_PASSWORD = config.getProperty("cloudProxy.trustStorePassword");
        MQ_CLOUD_PROXY_KEYSTORE_PASSWORD = config.getProperty("cloudProxy.keyStorePassword");
        PRODUCT_KEY_PATH = config.getProperty("cloudProxy.productKeyPath");
        CLOUD_PROXY_ACTIVE_MQ_URL = config.getProperty("cloudProxy.cloudActiveMQUrl");
        ACTIVE_MQ_INIT_QUEUE = config.getProperty("cloudProxy.activeMQInitQueue");
        LOG_LEVEL = config.getProperty("cloudProxy.logLevel");
    }

    static final int REQUEST_TIMEOUT_SECS = 300;
    private String MQ_TRUSTSTORE_PATH;
    private String MQ_CLOUD_PROXY_KEYSTORE_PATH;
    private String MQ_CLOUD_PROXY_KEYSTORE_PASSWORD;
    private String MQ_TRUSTSTORE_PASSWORD;
    private String PRODUCT_KEY_PATH;
    private String CLOUD_PROXY_ACTIVE_MQ_URL;
    private String ACTIVE_MQ_INIT_QUEUE;
    private String LOG_LEVEL;

    public String getMQ_TRUSTSTORE_PATH() {return MQ_TRUSTSTORE_PATH;}

    public String getMQ_CLOUD_PROXY_KEYSTORE_PATH() {return MQ_CLOUD_PROXY_KEYSTORE_PATH;}

    public String getMQ_CLOUD_PROXY_KEYSTORE_PASSWORD() {
        return MQ_CLOUD_PROXY_KEYSTORE_PASSWORD;
    }

    public String getMQ_TRUSTSTORE_PASSWORD() {
        return MQ_TRUSTSTORE_PASSWORD;
    }

    public String getPRODUCT_KEY_PATH() { return PRODUCT_KEY_PATH; }
    public String getCLOUD_PROXY_ACTIVE_MQ_URL() {return CLOUD_PROXY_ACTIVE_MQ_URL;}
    public String getACTIVE_MQ_INIT_QUEUE() {return ACTIVE_MQ_INIT_QUEUE;}
    public String getLOG_LEVEL() { return LOG_LEVEL; }
}

