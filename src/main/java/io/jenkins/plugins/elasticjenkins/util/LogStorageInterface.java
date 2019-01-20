package io.jenkins.plugins.elasticjenkins.util;

import hudson.console.AnnotatedLargeText;

import javax.annotation.Nonnull;
import java.io.File;

public interface LogStorageInterface extends StorageInterface {

    /**
     * Get the log output in JSON format based on the log id
     * @param id: id of the log
     * @return the content of the log
     */
    String getLogOutputId(@Nonnull String id);

    /**
     * Get the file of a log output based on a suffix and an id
     * @param suffix: Suffix of the url where the log is stored
     * @param id: id of the log to generate a temporary file
     * @return: the file generated
     */
    File getLogOutput(@Nonnull String suffix, @Nonnull String id);


}
