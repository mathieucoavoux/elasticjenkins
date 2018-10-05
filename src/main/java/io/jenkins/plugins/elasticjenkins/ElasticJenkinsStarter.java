package io.jenkins.plugins.elasticjenkins;


import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsStarter extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsStarter.class.getName());


    protected ElasticManager elasticManager = new ElasticManager();

    public ElasticJenkinsStarter() {

    }


    @Override
    public void onOnline(Computer c, TaskListener listener) {
        //Need to store the value into a variable otherwise when getStartupTime will be called it will call the currentTimeMillis again
        Long currentTime = System.currentTimeMillis();
        ElasticJenkinsUtil.setStartupTime(currentTime);
        //Check if the properties is configured already
        if (ElasticJenkinsUtil.getMasterName() == null || ElasticJenkinsUtil.getClusterName() == null ) {
            LOGGER.log(Level.WARNING,"ElasticJenkins is not defined yet. Please configure it and restart the server");
            return;
        }
        //Create index
        ElasticJenkinsUtil.createHealthIndex();
        String healthId = elasticManager.addMasterStartup();
        if (healthId == null ) {
            LOGGER.log(Level.SEVERE, "Cannot add the master in the healthcheck index. Please ensure your backend is working correctly and you have configured the plugin properly");
            return;
        }
        ElasticJenkinsUtil.setStartupTimeId(healthId);


        elasticManager.updateHealthFlag(healthId);
        List<String> listHealthIds = elasticManager.getUnavailableNode();
        if (listHealthIds.size() == 0) {
            return;
        }
        //Mark all ID to recover
        Map<String, Boolean> map = elasticManager.markToRecover(listHealthIds);
        //Check if all ID has been marked correctly
        //Log an error for every entry which can not be marked to recover
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (entry.getValue()) {
                boolean success = elasticManager.recoverBuilds(entry.getKey());


                if(success) {
                    elasticManager.markCompletedRecover(entry.getKey(), "SUCCESS");
                }else {
                    elasticManager.markCompletedRecover(entry.getKey(), "FAILED");
                }
            } else {
                LOGGER.log(Level.SEVERE, "Can not mark to recover :" + entry.getKey());
            }
        }
    }

}
