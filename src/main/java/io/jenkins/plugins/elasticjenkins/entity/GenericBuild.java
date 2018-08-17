package io.jenkins.plugins.elasticjenkins.entity;



import hudson.console.AnnotatedLargeText;
import hudson.console.ConsoleAnnotator;
import hudson.model.AbstractProject;
import io.jenkins.plugins.elasticjenkins.util.ElasticLogHandler;
import org.apache.commons.jelly.XMLOutput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

public class GenericBuild {

    /**
     * Id of the build
     */
    @Nonnull
    protected String id;

    /**
     * Jenkins job name
     */
    @Nonnull
    protected String name;

    /**
     * When the build start
     */
    @Nonnull
    protected Long startDate;

    /**
     * When the job is completed
     */
    @Nullable
    protected Long endDate;

    /**
     * Parameters set for the built
     */
    @Nullable
    protected List<Parameters> parameters;

    /**
     * Log output of the job
     */
    @Nullable
    protected String logId;

    /**
     * Status of the job
     */
    @Nonnull
    protected String status;

    @Nonnull
    public String getJenkinsMasterName() {
        return jenkinsMasterName;
    }

    public void setJenkinsMasterName(@Nonnull String jenkinsMasterName) {
        this.jenkinsMasterName = jenkinsMasterName;
    }

    @Nonnull
    protected String jenkinsMasterName;
    /**
     * Who has executed the job
     */
    @Nullable
    protected String launchedByName;



    /**
     * The id of the launcher
     */
    @Nullable
    protected String launchedById;

    /**
     * Where the job has been executed
     */
    @Nonnull
    protected String executedOn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public List<Parameters> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameters> parameters) {
        this.parameters = parameters;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLaunchedByName() {
        return launchedByName;
    }

    public void setLaunchedByName(String launchedByName) {
        this.launchedByName = launchedByName;
    }

    @Nullable
    public String getLaunchedById() {
        return launchedById;
    }

    public void setLaunchedById(@Nullable String launchedById) {
        this.launchedById = launchedById;
    }

    public String getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(String executedOn) {
        this.executedOn = executedOn;
    }

}
