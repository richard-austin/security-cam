package com.proxy;

import com.google.gson.*;
import grails.config.Config;
import grails.core.GrailsApplication;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public final class CloudProxyProperties {
    GrailsApplication grailsApplication;
    private String PRODUCT_KEY_PATH;
    private String CLOUD_PROXY_ACTIVE_MQ_URL;
    private String ACTIVE_MQ_INIT_QUEUE;
    private String LOG_LEVEL;
    private JsonObject cloudCreds;
    static private CloudProxyProperties theInstance;

    private CloudProxyProperties() {
    }

    public static CloudProxyProperties getInstance() {
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
            json = gson.fromJson(new FileReader(config.toProperties().getProperty("camerasHomeDirectory") + "/cloud-creds.json"), JsonObject.class);
        } catch (Exception ex) {
            throw new Exception("Error when getting Cloud credentials");
        }
        return json;
    }

    public void setCloudCreds(String username, String password) throws Exception {
        JsonObject creds = getCloudCreds();
        creds.remove("mqUser");
        JsonElement userName = new JsonPrimitive(username);
        creds.add("mqUser", userName);
        creds.remove("mqPw");
        creds.add("mqPw", new JsonPrimitive(password));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(creds);

        JsonElement je = JsonParser.parseString(json);
        String prettyJsonString = gson.toJson(je);

        String fileName = grailsApplication.getConfig().getProperty("camerasHomeDirectory") + "/cloud-creds.json";
        File file = new File(fileName);
        boolean b1 = file.setWritable(true);
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(prettyJsonString);
        boolean b2 = file.setWritable(false);
        writer.close();
    }

    public String getMQ_CLOUD_PROXY_KEYSTORE_PATH() {
        return cloudCreds.get("mqClientKSPath").getAsString();
    }

    public String getMQ_CLOUD_PROXY_KEYSTORE_PASSWORD() {
        return cloudCreds.get("mqClientKSPW").getAsString();
    }

    public String getMQ_USER() {
        return cloudCreds.get("mqUser").getAsString();
    }

    public String getMQ_PASSWORD() {
        return cloudCreds.get("mqPw").getAsString();
    }

    public String getPRODUCT_KEY_PATH() {
        return PRODUCT_KEY_PATH;
    }

    public String getCLOUD_PROXY_ACTIVE_MQ_URL() {
        return CLOUD_PROXY_ACTIVE_MQ_URL;
    }

    public String getACTIVE_MQ_INIT_QUEUE() {
        return ACTIVE_MQ_INIT_QUEUE;
    }

    public String getLOG_LEVEL() {
        return LOG_LEVEL;
    }
}

