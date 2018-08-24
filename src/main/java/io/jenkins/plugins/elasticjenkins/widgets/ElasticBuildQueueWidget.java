package io.jenkins.plugins.elasticjenkins.widgets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hudson.Functions;
import hudson.model.ModelObject;
import io.jenkins.plugins.elasticjenkins.ElasticJenkins;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import jnr.ffi.annotations.In;
import org.jenkinsci.Symbol;

import hudson.Extension;
import hudson.widgets.Widget;
import org.kohsuke.stapler.Header;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.List;


@Extension(ordinal=100) @Symbol("elasticqueue")
public class ElasticBuildQueueWidget extends Widget{
//public class ElasticBuildQueueWidget<O extends ModelObject,T> extends Widget{

    /*
    public final String baseUrl;

    public final O owner;

    public ElasticBuildQueueWidget(O owner) {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        this.baseUrl = Functions.getNearestAncestorUrl(currentRequest,owner);
        this.owner = owner;
    }
    */

    public ElasticBuildQueueWidget() {}

    public String doAjax(StaplerRequest req, StaplerResponse rsp,
                       @Header("n") String n) {
        rsp.setContentType("application/json");
        return "{ \"msg\" : \"hello world\" }";
    }

    public String getDisplayName() {
        return "Build queue widget";
    }

    @Override
    public String getUrlName() {
        return "ejqueue";
    }

    @JavaScriptMethod
    public Integer getItemCount() {
        ElasticManager elasticManager = new ElasticManager();
        return elasticManager.getCountCurrentItem();
    }

    @JavaScriptMethod
    public Integer getBuildCount() {
        ElasticManager elasticManager = new ElasticManager();
        return elasticManager.getCountCurrentBuilds();
    }

    @JavaScriptMethod
    public String getLastQueuedItemsJson() {
        ElasticManager elasticManager = new ElasticManager();
        Gson gson = new GsonBuilder().create();
        return gson.toJson(elasticManager.getLastCurrentItems());

    }

    public List<GenericBuild> getLastQueuedItems() {
        ElasticManager elasticManager = new ElasticManager();
        return elasticManager.getLastCurrentItems();
    }


    @JavaScriptMethod
    public String getLastCurrentBuildsJson() {
        ElasticManager elasticManager = new ElasticManager();
        Gson gson = new GsonBuilder().create();
        return gson.toJson(elasticManager.getLastCurrentBuilds());
    }

    public List<GenericBuild> getLastCurrentBuilds() {
        ElasticManager elasticManager = new ElasticManager();
        return elasticManager.getLastCurrentBuilds();
    }

}
