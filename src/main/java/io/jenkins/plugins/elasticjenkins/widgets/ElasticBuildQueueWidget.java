package io.jenkins.plugins.elasticjenkins.widgets;

import hudson.Functions;
import hudson.model.ModelObject;
import org.jenkinsci.Symbol;

import hudson.Extension;
import hudson.widgets.Widget;
import org.kohsuke.stapler.Header;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;


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
    public String getItemCount() {
        return "{ \"count\" : \"0\" }";
    }
}
