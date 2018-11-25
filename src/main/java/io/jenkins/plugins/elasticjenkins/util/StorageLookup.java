package io.jenkins.plugins.elasticjenkins.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageLookup {

    private static final Logger LOGGER = Logger.getLogger(StorageLookup.class.getName());

    public static Object getLogStorage() {
        switch (ElasticJenkinsUtil.getLogStorageType()) {
            case "elasticsearch":
                return new ElasticManager();
            case "cloud":
                return null;
            default:
                return null;
        }

    }
}
