package io.jenkins.plugins.elasticjenkins;


import com.google.gson.Gson;
import hudson.model.AbstractProject;
import hudson.model.Action;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticLogHandler;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.apache.commons.jelly.XMLOutput;
import org.apache.http.HttpRequest;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
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
        String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);

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
        String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
        Gson gson = new Gson();

        String masters = master;
        if(viewType.equals("cluster"))
            masters = elasticManager.getNodesByCluster(cluster);

        return gson.toJson(elasticManager.getPaginateBuildHistory(index,type,masters , paginationSize, paginationStart));
    }

    @JavaScriptMethod
    public String getNewResultsJson(@Nonnull String type,@Nonnull String viewType,@Nonnull String lastFetch) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
        Gson gson = new Gson();
        String masters = master;
        if(viewType.equals("cluster"))
            masters = elasticManager.getNodesByCluster(cluster);

        return gson.toJson(elasticManager.getNewResults(index,type,lastFetch,masters ));
    }

    @JavaScriptMethod
    public String filterResult(@Nonnull String type,@Nonnull String viewType,@Nonnull String filterType, @Nonnull String filterValue) {
        if(filterType.equals("parameters")) {
            return getBuildByParameters(type,viewType,filterValue);
        }

        return null;
    }


    public String getBuildByParameters(@Nonnull String type,@Nonnull String viewType,@Nonnull String parameter) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
        Gson gson = new Gson();
        String masters = master;
        if(viewType.equals("cluster"))
            masters = elasticManager.getNodesByCluster(cluster);
        return gson.toJson(elasticManager.findByParameter(index,type,masters,parameter));
    }

    @JavaScriptMethod
    public void writeOutput(XMLOutput out,String id) throws IOException, SAXException {
        ElasticManager elasticManager = new ElasticManager();
        //Get log id
        String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
        String suffix = elasticManager.getLogOutputId(index,"builds",id);
        List<String> list = elasticManager.getLogOutput(URLDecoder.decode(suffix,"UTF-8"));
        for(String row: list) {
            out.write(row+"\n");
        }

        out.flush();

    }

    public HttpResponse doGetLog(StaplerRequest request) {
        return HttpResponses.forwardToView(this,"log_output.jelly");
    }
}
