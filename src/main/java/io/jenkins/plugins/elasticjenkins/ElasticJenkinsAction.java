package io.jenkins.plugins.elasticjenkins;


import com.google.gson.Gson;
import hudson.model.AbstractProject;
import hudson.model.Action;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsAction implements Action {
    private AbstractProject<?,?> project;

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsManagement.class.getName());

    protected String master = ElasticJenkinsUtil.getProperty("masterName");
    protected String cluster = ElasticJenkinsUtil.getProperty("clusterName");

    ElasticJenkinsAction(AbstractProject<?,?> project) {
        this.project = project;
    }

    public String getIconFileName() {
        return "/plugin/elasticjenkins/24x24/elasticjenkins.png";
    }

    public String getDisplayName() {
        return "Elastic build history";
    }

    public String getUrlName() {
        return "ej1";
    }

    public AbstractProject<?,?> getProject() {
        return project;
    }

    @JavaScriptMethod
    public List<GenericBuild> getPaginatedHistory(@Nonnull String type,
                                                  @Nonnull String viewType,
                                                  @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());

        String masters = master;
        if(viewType.equals("cluster"))
            masters = elasticManager.getNodesByCluster(cluster);
        LOGGER.log(Level.FINEST,"Cluster: "+masters);
       return  elasticManager.getPaginateBuildHistory(index,type,masters , paginationSize, paginationStart);
    }

    @JavaScriptMethod
    public String getPagninatedHistoryJson(@Nonnull String type,
                                           @Nonnull String viewType,
                                           @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        //TODO: Add a parameter for the cluster name and master name
        //TODO: Add a method to search by parameters
        //TODO: Check if new builds came and update the list in a nice fashion
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());
        Gson gson = new Gson();

        String masters = master;
        if(viewType.equals("cluster"))
            masters = elasticManager.getNodesByCluster(cluster);

        return gson.toJson(elasticManager.getPaginateBuildHistory(index,type,masters , paginationSize, paginationStart));
    }

    @JavaScriptMethod
    public String getNewResultsJson(@Nonnull String type,@Nonnull String viewType,@Nonnull String lastFetch) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl());
        Gson gson = new Gson();
        String masters = master;
        if(viewType.equals("cluster"))
            masters = elasticManager.getNodesByCluster(cluster);

        return gson.toJson(elasticManager.getNewResults(index,type,lastFetch,masters ));
    }
}
