package io.jenkins.plugins.elasticjenkins;


import hudson.model.AbstractProject;
import hudson.model.Action;

import javax.annotation.CheckForNull;

public class ElasticJenkinsAction implements Action {
    private AbstractProject<?,?> project;

    ElasticJenkinsAction(AbstractProject<?,?> project) {
        this.project = project;
    }

    //@CheckForNull
    //@Override
    public String getIconFileName() {
        return "notepad.png";
    }

    //@CheckForNull
    //@Override
    public String getDisplayName() {
        return "ElasticJenkinsAction1";
    }

    //@CheckForNull
    //@Override
    public String getUrlName() {
        return "ej1";
    }

    public AbstractProject<?,?> getProject() {
        return project;
    }
}
