package jenkins.widgets.BuildQueueWidget;

def t = namespace(lib.JenkinsTagLib.class)
st=namespace("jelly:stapler")

include(my,"panel.jelly")

include(my,"panel_builds.jelly")