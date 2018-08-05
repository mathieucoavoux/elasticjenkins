package io.jenkins.plugins.elasticjenkins;


import hudson.model.AbstractProject;
import hudson.model.Action;
import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.List;

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

    @JavaScriptMethod
    public List<GenericBuild> getPaginatedHistory(@Nonnull String type,
                                                  @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        //TODO: Add a parameter for the cluster name and master name
        //TODO: Add a method to search by parameters
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());
       return  elasticManager.getPaginateBuildHistory(index,type,paginationSize,paginationStart);
    }
}
