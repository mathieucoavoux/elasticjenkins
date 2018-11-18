package io.jenkins.plugins.elasticjenkins.entity;

import hudson.model.ParametersAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    @Nonnull
    protected Long startupTime;

    public Long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(Long startupTime) {
        this.startupTime = startupTime;
    }


    @Nullable
    public Long getQueuedSince() {
        return queuedSince;
    }

    public void setQueuedSince(@Nullable Long queuedSince) {
        this.queuedSince = queuedSince;
    }

    @Nullable
    protected Long queuedSince;


    /**
     * Parameters set for the built
     */
    @Nullable
    protected List<ParametersAction> parameters;

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


    @Nonnull
    public String getJenkinsMasterId() {
        return jenkinsMasterId;
    }

    public void setJenkinsMasterId(@Nonnull String jenkinsMasterId) {
        this.jenkinsMasterId = jenkinsMasterId;
    }

    @Nonnull
    protected String jenkinsMasterId;

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(@Nonnull String projectId) {
        this.projectId = projectId;
    }

    @Nonnull
    protected String projectId;
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

    @Nonnull
    public String getUrl() {
        return url;
    }

    public void setUrl(@Nonnull String url) {
        this.url = url;
    }

    /**
     * The URL to access the job. Used in Kibana
     */
    @Nonnull
    protected String url;

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

    public List<ParametersAction> getParameters() {
        return parameters;
    }

    public void setParametersAction(List<ParametersAction> parameters) {
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
