package io.jenkins.plugins.elasticjenkins;

import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import java.util.logging.Logger;
/*
public class ElasticJenkinsProperty extends JobProperty<Job<?,?>> {

    protected static final Logger LOGGER = Logger.getLogger(ElasticJenkinsProperty.class.getName());

    private String displayName;

    @DataBoundConstructor
    public ElasticJenkinsProperty() {}

    @CheckForNull
    public String getDisplayName() { return displayName;}

    @DataBoundSetter
    public void setDisplayName(String displayName) { this.displayName = displayName;}

    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public static final String ELASTIC_JENKINS_BLOCK_NAME = "elasticJenkinsPropertyName";

        @Override
        public String getDisplayName() { return "Elastic jenkins descriptor name";}

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ElasticJenkinsProperty ejp = req.bindJSON(ElasticJenkinsProperty.class,formData.getJSONObject(ELASTIC_JENKINS_BLOCK_NAME));
            return (ejp == null) ? null : ejp;
        }
    }
}
*/