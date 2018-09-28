package io.jenkins.plugins.elasticjenkins;

import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.entity.Parameters;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsRecover {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsRecover.class.getName());

    /**
     * Reschedule a build
     * @param genericBuild: the generic build to reschedule
     * @param id: the Elasticsearch id of the previous build to recover. This will be used to generate the unique log file name
     * @param master: the master name where the build has been scheduled previously. This will be used to generate the unique log file name
     * @return: true if the build has been rescheduled correctly
     */
    public boolean rescheduleBuild(GenericBuild genericBuild,String id,String master) {
        if(genericBuild == null || id == null || master == null)
            return false;
        //Check the type of project to reschedule
        if(Jenkins.getInstance().getItemByFullName(genericBuild.getName()) instanceof AbstractProject<?,?> == true) {
            //Recover freestyle job
            return rescheduleAbstractProject(genericBuild,id,master);
        }else if(Jenkins.getInstance().getItemByFullName(genericBuild.getName()) instanceof WorkflowJob == true){
            //Recover workflow job
            return rescheduleWorkflowJob(genericBuild,id,master);
        }else{
            LOGGER.log(Level.SEVERE, "Can not recover the job with the elasticsearch id:"+id);
            LOGGER.log(Level.SEVERE,"Please resubmit manually");
        }
        return false;
    }

    protected boolean rescheduleAbstractProject(GenericBuild genericBuild,String id,String master) {
        AbstractProject<?,?> project = (AbstractProject<?,?>) Jenkins.getInstance().getItemByFullName(genericBuild.getName());
        if (project != null && project.getFullName().equals(genericBuild.getName())) {
            //Cause of the reschedule user
            Cause c = new Cause.UserIdCause(); //This will probably return null
            project.scheduleBuild2(0,c,genericBuild.getParameters());
        }
        return false;
    }

    protected boolean rescheduleWorkflowJob(GenericBuild genericBuild,String id,String master) {

        return false;
    }
}
