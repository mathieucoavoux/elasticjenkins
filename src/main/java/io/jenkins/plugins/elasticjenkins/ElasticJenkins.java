package io.jenkins.plugins.elasticjenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * The @see hudson.model.TransientProjectActionFactory helps to call @see hudson.model.Action
 * We use it in this plugin to call ElasticJenkinsAction
 *
 * @author Mathieu Coavoux
 */
@Extension
public class ElasticJenkins extends TransientProjectActionFactory {
    @Override
    public Collection<? extends Action> createFor(AbstractProject target) {
        return Collections.singleton(new ElasticJenkinsAction(target));
    }
}
