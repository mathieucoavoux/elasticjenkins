package io.jenkins.plugins.elasticjenkins;

import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.slaves.ComputerLauncher;
import jenkins.model.RunAction2;

import javax.annotation.CheckForNull;

public class ElasticJenkinsBuildAction extends ComputerLauncher implements RunAction2 {

    protected transient AbstractBuild<?,?> build;

    @Override
    public void onAttached(Run<?, ?> r) {
        build =  (AbstractBuild<?,?>) r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        build =  (AbstractBuild<?,?>) r;
    }

    //@CheckForNull
    @Override
    public String getIconFileName() {
        return "";
    }

    //@CheckForNull
    @Override
    public String getDisplayName() {
        return "";
    }

    //@CheckForNull
    @Override
    public String getUrlName() {
        return "ej2";
    }

    public String getName() {
        return "ElasticJenkinsBuildActionName";
    }

    public AbstractBuild getBuild() { return build;}

    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {

        @Override
        public String getDisplayName() {
            return "ElasticJennkisBuildActionImpl";

        }
    }
}
