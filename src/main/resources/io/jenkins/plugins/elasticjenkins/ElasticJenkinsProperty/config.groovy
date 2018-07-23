package io.jenkins.plugins.elasticjenkins;

import static io.jenkins.plugins.elasticjenkins.ElasticJenkinsProperty.DescriptorImpl.ELASTIC_JENKINS_BLOCK_NAME;

def f = namespace(lib.FormTagLib);

f.optionalBlock(name: QUEUE_PERSIST_BLOCK_NAME, title: _('elasticjenkins.project'), field:"enabled",checked: instance != null) { }
