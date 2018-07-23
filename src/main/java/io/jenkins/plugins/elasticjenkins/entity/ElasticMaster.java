package io.jenkins.plugins.elasticjenkins.entity;

public class ElasticMaster {

    protected String jenkinsMasterName;

    protected String clusterName;

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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    protected String hostname;
}
