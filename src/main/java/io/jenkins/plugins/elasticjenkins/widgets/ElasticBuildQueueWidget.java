package io.jenkins.plugins.elasticjenkins.widgets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;

import hudson.Extension;
import hudson.widgets.Widget;

import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.List;


@Extension(ordinal=100)
public class ElasticBuildQueueWidget extends Widget{



    public ElasticBuildQueueWidget() {}


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
