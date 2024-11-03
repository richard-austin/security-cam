package com.securitycam.proxies;

import com.google.gson.*;
import com.securitycam.configuration.Config;
import com.securitycam.services.LogService;
import org.springframework.context.annotation.Bean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class CloudProxyProperties {
    Config config;
    LogService logService;
    private String PRODUCT_KEY_PATH;
    private String ACTIVE_MQ_INIT_QUEUE;
    private String LOG_LEVEL;
    private JsonObject cloudCreds;

    public CloudProxyProperties(Config config, LogService logService) {
        this.config = config;
        this.logService = logService;
        try {
            setupConfigParams();
        }
        catch (Exception e) {
            logService.getCam().error(e.getMessage()+ " when setting up CloudProxyProperties");
        }
    }

    private void setupConfigParams() throws Exception {
        PRODUCT_KEY_PATH = config.getCloudProxy().getProductKeyPath();
        ACTIVE_MQ_INIT_QUEUE = config.getCloudProxy().getActiveMQInitQueue();
        LOG_LEVEL = config.getCloudProxy().getLogLevel();
        cloudCreds = getCloudCreds();
    }

    public JsonObject getCloudCreds() throws Exception {
        JsonObject json;
        try {
            Gson gson = new Gson();
            json = gson.fromJson(new FileReader(config.getCamerasHomeDirectory() + "/cloud-creds.json"), JsonObject.class);
        } catch (Exception ex) {
            throw new Exception("Error when getting Cloud credentials: "+ex.getMessage());
        }
        return json;
    }

    public void setCloudCreds(String username, String password, String mqHost) throws Exception {
        JsonObject creds = getCloudCreds();
        // Update the ActiveMQW username and password if the new values are not blank
        if(!Objects.equals(username, "") && !Objects.equals(password, "")) {
            creds.remove("mqUser");
            JsonElement userName = new JsonPrimitive(username);
            creds.add("mqUser", userName);
            creds.remove("mqPw");
            creds.add("mqPw", new JsonPrimitive(password));
        }
        creds.remove("mqHost");
        creds.add("mqHost", new JsonPrimitive(mqHost));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(creds);

        JsonElement je = JsonParser.parseString(json);
        String prettyJsonString = gson.toJson(je);

        String fileName = config.getCamerasHomeDirectory() + "/cloud-creds.json";
        File file = new File(fileName);
        boolean b1 = file.setWritable(true);
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(prettyJsonString);
        boolean b2 = file.setWritable(false);
        writer.close();
        // Load in the new parameters
        setupConfigParams();
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

    public String getMQ_HOST() {
        var mqHost = cloudCreds.get("mqHost");
        return mqHost != null ? mqHost.getAsString() : "<none>";
    }

    public String getPRODUCT_KEY_PATH() {
        return PRODUCT_KEY_PATH;
    }

    public String getCLOUD_PROXY_ACTIVE_MQ_URL() {
        // Take the cloudActiveMQUrl in application.yml and replace the host with that which was set in
        //  Update ActiveMQ Credentials, leave it if it was never set
        URI uri;
        try {
            uri = new URI(Objects.requireNonNull(config.getCloudProxy()).getCloudActiveMQUrl());
            if (!Objects.equals(getMQ_HOST(), "<none>"))
                uri = new URI(uri.getScheme().toLowerCase(), getMQ_HOST() + ":" + uri.getPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException ignore) {
            return "";
        }
        return uri.toString();
    }

    public String getACTIVE_MQ_INIT_QUEUE() {
        return ACTIVE_MQ_INIT_QUEUE;
    }

    public String getLOG_LEVEL() {
        return LOG_LEVEL;
    }

}
