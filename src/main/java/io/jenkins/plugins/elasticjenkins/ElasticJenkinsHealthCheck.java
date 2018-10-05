package io.jenkins.plugins.elasticjenkins;

import hudson.Extension;
import hudson.model.PeriodicWork;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsHealthCheck extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsHealthCheck.class.getName());

    protected ElasticManager elasticManager = new ElasticManager();


    @Override
    public long getRecurrencePeriod() {
        return 10000;
    }

    @Override
    protected void doRun() throws Exception {
        //Update flag
        elasticManager.updateHealthFlag(ElasticJenkinsUtil.getStartupTimeId());
        //Check if any node is not available
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
