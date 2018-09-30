package io.jenkins.plugins.elasticjenkins;

import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.queue.QueueTaskFuture;
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
     * @return: true if the build has been rescheduled correctly
     */
    public boolean rescheduleBuild(GenericBuild genericBuild,String id) {
        if(genericBuild == null || id == null )
            return false;
        //Check the type of project to reschedule
        if(Jenkins.getInstance().getItemByFullName(genericBuild.getName()) instanceof AbstractProject<?,?> == true) {
            //Recover freestyle job
            return rescheduleAbstractProject(genericBuild,id);
        }else if(Jenkins.getInstance().getItemByFullName(genericBuild.getName()) instanceof WorkflowJob == true){
            //Recover workflow job
            return rescheduleWorkflowJob(genericBuild,id);
        }else{
            LOGGER.log(Level.SEVERE,"Job not supported:"+Jenkins.getInstance().getItemByFullName(genericBuild.getName()).getClass().getName());
            LOGGER.log(Level.SEVERE, "Can not recover the job with the elasticsearch id:"+id);
            LOGGER.log(Level.SEVERE,"Please resubmit manually");
        }
        return false;
    }

    protected boolean rescheduleAbstractProject(GenericBuild genericBuild,String id) {
        AbstractProject<?,?> project = (AbstractProject<?,?>) Jenkins.getInstance().getItemByFullName(genericBuild.getName());
        if (project != null && project.getFullName().equals(genericBuild.getName())) {
            //Cause of the reschedule user
            Cause c = new Cause.UserIdCause(); //This will probably return null
            QueueTaskFuture queueTaskFuture = project.scheduleBuild2(0,c,genericBuild.getParameters());
            if(queueTaskFuture != null)
                return true;
        }
        return false;
    }

    protected boolean rescheduleWorkflowJob(GenericBuild genericBuild,String id) {
        WorkflowJob project = (WorkflowJob) Jenkins.getInstance().getItemByFullName(genericBuild.getName());
        if (project != null && project.getFullName().equals(genericBuild.getName())) {
            //Cause of the reschedule user
            Cause c = new Cause.UserIdCause(); //This will probably return null
            List<ParametersAction> listParam = genericBuild.getParameters();
            ParametersAction[] actions = new ParametersAction[listParam.size()];
            actions = listParam.toArray(actions);
            QueueTaskFuture queueTaskFuture = project.scheduleBuild2(0,actions);
            if(queueTaskFuture != null)
                return true;
        }
        return false;
    }
}
