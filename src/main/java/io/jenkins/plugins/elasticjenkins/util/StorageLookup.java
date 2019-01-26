package io.jenkins.plugins.elasticjenkins.util;

import io.jenkins.plugins.elasticjenkins.ElasticJenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageLookup {

    private static final Logger LOGGER = Logger.getLogger(StorageLookup.class.getName());

    private static Object getLogStorage() {
        switch (ElasticJenkinsUtil.getLogStorageType()) {
            case "elasticsearch":
                return new ElasticManager();
            case "cloud":
                return null;
            default:
                return null;
        }

    }

    private static Object getConfigurationStorage() {
        switch (ElasticJenkinsUtil.getConfigurationStorageType()) {
            case "elasticsearch":
                return new ElasticManager();
            case "cloud":
                return null;
            default:
                return null;
        }
    }

    private static Object getClusterStorage() {
        switch (ElasticJenkinsUtil.getClusterStorageType()) {
            case "elasticsearch":
                return new ElasticManager();
            case "cloud":
                return null;
            default:
                return null;
        }
    }

    public static Object getStorage(Class interfaceName) {
        switch (interfaceName.getSimpleName()) {
            case "LogStorageInterface":
                return getLogStorage();
            case "ConfigurationStorageInterface":
                return getConfigurationStorage();
            case "ClusterStorageInterface":
                return getClusterStorage();
            default:
                LOGGER.log(Level.SEVERE,"I do NOT know this interface !!");
                return null;
        }
    }
}
