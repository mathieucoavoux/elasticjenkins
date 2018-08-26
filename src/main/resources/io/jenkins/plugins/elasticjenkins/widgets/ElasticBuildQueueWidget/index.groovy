package jenkins.widgets.BuildQueueWidget;

import static io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil.isInitialized
import static io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil.isEmpty

def t = namespace(lib.JenkinsTagLib.class);
st=namespace("jelly:stapler")
if(isInitialized && ! isEmpty) {
    include(my, "panel.jelly")

    include(my, "panel_builds.jelly")
}