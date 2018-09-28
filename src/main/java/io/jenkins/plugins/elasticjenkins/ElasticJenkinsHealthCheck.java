package io.jenkins.plugins.elasticjenkins;

import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchArrayResult;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.entity.HealthCheck;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsHealthCheck extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsHealthCheck.class.getName());

    protected String id;
    protected ElasticManager elasticManager = new ElasticManager();

    public ElasticJenkinsHealthCheck(String id) {
        this.id = id;
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Run task at:" + new Date());
        //Update the id with the current time
        elasticManager.updateHealthFlag(id);
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
