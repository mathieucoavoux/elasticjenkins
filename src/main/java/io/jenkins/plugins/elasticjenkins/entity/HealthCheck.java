package io.jenkins.plugins.elasticjenkins.entity;

public class HealthCheck {
    public String getJenkinsMasterName() {
        return jenkinsMasterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Long getStartupTime() {
        return startupTime;
    }

    private String jenkinsMasterName;
    protected String clusterName;
    private Long startupTime;
}
