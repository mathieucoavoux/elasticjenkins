package io.jenkins.plugins.elasticjenkins;


import com.google.gson.Gson;
import hudson.Extension;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.Run;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import jenkins.model.TransientActionFactory;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.xml.sax.SAXException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsWorkflowAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsWorkflowAction.class.getName());

    public final WorkflowJob target;

    private ElasticJenkinsWorkflowAction(WorkflowJob job) {
        this.target = job;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/elasticjenkins/24x24/elasticjenkins.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Elastic build history";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "ej2";
    }

    @Extension
    public static class Factory extends TransientActionFactory<WorkflowJob> {

        @Override
        public Class<WorkflowJob> type() {
            return WorkflowJob.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull WorkflowJob target) {
            return Collections.singleton(new ElasticJenkinsWorkflowAction(target));
        }
    }

    @JavaScriptMethod
    public List<GenericBuild> getPaginatedHistory(@Nonnull String viewType,
                                                  @Nonnull Integer paginationSize, @Nonnull String paginationStart) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(target.getUrl().split("/$")[0]);

        return  elasticManager.getPaginateBuildHistory(index, viewType , paginationSize, paginationStart);
    }


    @JavaScriptMethod
    public String getPagninatedHistoryJson(@Nonnull String type,
                                           @Nonnull String viewType,
                                           @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        ElasticManager elasticManager = new ElasticManager();
        String index = ElasticJenkinsUtil.getHash(target.getUrl().split("/$")[0]);
        Gson gson = new Gson();

        return gson.toJson(elasticManager.getPaginateBuildHistory(index, viewType , paginationSize, paginationStart));
    }

    @JavaScriptMethod
    public String getNewResultsJson(@Nonnull String type,@Nonnull String viewType,@Nonnull String lastFetch) {
        ElasticManager elasticManager = new ElasticManager();
        String projectHash = ElasticJenkinsUtil.getHash(target.getUrl().split("/$")[0]);
        Gson gson = new Gson();


        return gson.toJson(elasticManager.getNewResults(projectHash, lastFetch,viewType ));
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
        String projectHash = ElasticJenkinsUtil.getHash(target.getUrl().split("/$")[0]);
        Gson gson = new Gson();
        return gson.toJson(elasticManager.findByParameter(projectHash, viewType,parameter));
    }

    @JavaScriptMethod
    public void writeOutput(XMLOutput out, String id) throws IOException, SAXException {
        ElasticManager elasticManager = new ElasticManager();
        //Get log id
        String index = ElasticJenkinsUtil.getHash(target.getUrl().split("/$")[0]);
        String suffix = elasticManager.getLogOutputId(id);

        File logOutput = elasticManager.getLogOutput(URLDecoder.decode(suffix,"UTF-8"),id);
        LOGGER.log(Level.FINEST,"Log output:"+logOutput.getPath());
        //Get last WorkfowRun to enable color if Ansi color plugin is installed
        new AnnotatedLargeText<>(logOutput,Charset.defaultCharset(),true,target.getLastBuild()).writeHtmlTo(0,out.asWriter());

        logOutput.delete();


    }

    public void doProgressiveHtml(StaplerRequest req, StaplerResponse rsp) throws IOException {

        ElasticManager elasticManager = new ElasticManager();


    }


    public HttpResponse doGetLog(StaplerRequest request) {
        return HttpResponses.forwardToView(this,"log_output.jelly");
    }
}
