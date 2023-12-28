package com.osstelecom.db.inventory.manager.client.netcompass.configuration;

import com.osstelecom.db.inventory.manager.http.client.configuration.ConfigurationManager;
import com.osstelecom.db.inventory.manager.http.client.configuration.NetcompassClientConfiguration;

public class NetcompassConfigurationManager {

    public NetcompassClientConfiguration loadConfiguration(String profile) {
        return loadConfiguration(profile, null);
    }

    public NetcompassClientConfiguration loadConfiguration(String profile, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = "netcompass-client.yaml";
        }
        if (profile == null) {
            profile = "";
        } else {
            profile = profile + "/";
        }
        String configPath = "config/" + profile + fileName;
        return new ConfigurationManager().loadConfiguration(configPath);
    }
}