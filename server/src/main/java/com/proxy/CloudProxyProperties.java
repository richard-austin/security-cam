package com.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import grails.config.Config;
import grails.core.GrailsApplication;
import java.io.FileReader;

public final class CloudProxyProperties {
    GrailsApplication grailsApplication;
    private String PRODUCT_KEY_PATH;
    private String CLOUD_PROXY_ACTIVE_MQ_URL;
    private String ACTIVE_MQ_INIT_QUEUE;
    private String LOG_LEVEL;
    private JsonObject cloudCreds;
    static private CloudProxyProperties theInstance;

    private CloudProxyProperties()
    {
    }

    static CloudProxyProperties getInstance()
    {
        return theInstance;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) throws Exception {
        this.grailsApplication = grailsApplication;
        CloudProxyProperties.theInstance = this;
        setupConfigParams();
    }

    private void setupConfigParams() throws Exception {
        Config config = grailsApplication.getConfig();
        PRODUCT_KEY_PATH = config.getProperty("cloudProxy.productKeyPath");
        CLOUD_PROXY_ACTIVE_MQ_URL = config.getProperty("cloudProxy.cloudActiveMQUrl");
        ACTIVE_MQ_INIT_QUEUE = config.getProperty("cloudProxy.activeMQInitQueue");
        LOG_LEVEL = config.getProperty("cloudProxy.logLevel");
        cloudCreds = getCloudCreds();
    }

    public JsonObject getCloudCreds() throws Exception {
        JsonObject json;
        Config config = grailsApplication.getConfig();
        try {
            Gson gson = new Gson();
            json = gson.fromJson(new FileReader(config.toProperties().getProperty("appHomeDirectory")+"/cloud-creds.json"), JsonObject.class);
        }
        catch(Exception ex) {
            throw new Exception("Error when getting Cloud credentials");
        }
        return json;
    }
    public String getMQ_CLOUD_PROXY_KEYSTORE_PATH() {return cloudCreds.get("mqClientKSPath").getAsString();}

    public String getMQ_CLOUD_PROXY_KEYSTORE_PASSWORD() {
        return cloudCreds.get("mqClientKSPW").getAsString();
    }
    public String getMQ_USER() {return cloudCreds.get("mqUser").getAsString();}
    public String getMQ_PASSWORD() {return cloudCreds.get("mqPw").getAsString();}
    public String getPRODUCT_KEY_PATH() { return PRODUCT_KEY_PATH; }
    public String getCLOUD_PROXY_ACTIVE_MQ_URL() {return CLOUD_PROXY_ACTIVE_MQ_URL;}
    public String getACTIVE_MQ_INIT_QUEUE() {return ACTIVE_MQ_INIT_QUEUE;}
    public String getLOG_LEVEL() { return LOG_LEVEL; }
}

