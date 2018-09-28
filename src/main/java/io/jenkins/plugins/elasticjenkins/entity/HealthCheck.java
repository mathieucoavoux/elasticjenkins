package io.jenkins.plugins.elasticjenkins.entity;

public class HealthCheck {
    public String getJenkinsMasterName() {
        return jenkinsMasterName;
    }

    public void setJenkinsMasterName(String jenkinsMasterName) {
        this.jenkinsMasterName = jenkinsMasterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Long getLastFlag() {
        return lastFlag;
    }

    public void setLastFlag(Long lastFlag) {
        this.lastFlag = lastFlag;
    }

    public Long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(Long startupTime) {
        this.startupTime = startupTime;
    }

    public String getRecoverBy() {
        return recoverBy;
    }

    public void setRecoverBy(String recoverBy) {
        this.recoverBy = recoverBy;
    }

    public String getRecoverStatus() {
        return recoverStatus;
    }

    public void setRecoverStatus(String recoverStatus) {
        this.recoverStatus = recoverStatus;
    }

    protected String jenkinsMasterName;
    protected String clusterName;
    protected Long lastFlag;
    protected Long startupTime;
    protected String recoverBy;
    protected String recoverStatus;
}
