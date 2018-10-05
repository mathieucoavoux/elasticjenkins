package io.jenkins.plugins.elasticjenkins.entity;

public class ProjectMapping {

    public String getProjectHash() {
        return projectHash;
    }

    public void setProjectHash(String projectHash) {
        this.projectHash = projectHash;
    }

    public String getProjectEncodedName() {
        return projectEncodedName;
    }

    public void setProjectEncodedName(String projectEncodedName) {
        this.projectEncodedName = projectEncodedName;
    }

    protected String projectHash;
    protected String projectEncodedName;

}
