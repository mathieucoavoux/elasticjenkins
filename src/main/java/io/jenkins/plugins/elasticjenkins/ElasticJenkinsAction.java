package io.jenkins.plugins.elasticjenkins;


import com.google.gson.Gson;
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
        return "/plugin/elasticjenkins/24x24/elasticjenkins.png";
    }

    //@CheckForNull
    //@Override
    public String getDisplayName() {
        return "Elastic build history";
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
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());
       return  elasticManager.getPaginateBuildHistory(index,type,paginationSize,paginationStart);
    }

    @JavaScriptMethod
    public String getPagninatedHistoryJson(@Nonnull String type,
                                           @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        //TODO: Add a parameter for the cluster name and master name
        //TODO: Add a method to search by parameters
        //TODO: Check if new builds came and update the list in a nice fashion
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());
        Gson gson = new Gson();
        return gson.toJson(elasticManager.getPaginateBuildHistory(index,type,paginationSize,paginationStart));
    }

    @JavaScriptMethod
    public String getNewResultsJson(@Nonnull String type,@Nonnull String lastFetch) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());
        Gson gson = new Gson();
        return gson.toJson(elasticManager.getNewResults(index,type,lastFetch));
    }
}
