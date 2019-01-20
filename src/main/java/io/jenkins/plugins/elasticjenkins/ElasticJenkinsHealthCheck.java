package io.jenkins.plugins.elasticjenkins;

import hudson.Extension;
import hudson.model.PeriodicWork;
import io.jenkins.plugins.elasticjenkins.util.ClusterStorageInterface;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import io.jenkins.plugins.elasticjenkins.util.StorageProxyFactory;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsHealthCheck extends PeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsHealthCheck.class.getName());

    private ElasticManager elasticManager;
    private ClusterStorageInterface clusterStorage;

    @Override
    public long getRecurrencePeriod() {
        return 10000;
    }

    @Override
    protected void doRun() throws Exception {
        if(!ElasticJenkinsUtil.getJenkinsHealthCheckEnable())
            return;
        if (ElasticJenkinsUtil.getMasterName() == null || ElasticJenkinsUtil.getClusterName() == null ) {
            return;
        }
        if(clusterStorage == null)
            this.clusterStorage = (ClusterStorageInterface) StorageProxyFactory.newInstance(ClusterStorageInterface.class);
        //Check if elasticManager has been initialized already
        //if(elasticManager == null)
        //    this.elasticManager = new ElasticManager();
        //Update flag
        clusterStorage.updateHealthFlag(ElasticJenkinsUtil.getStartupTimeId());
        //Check if any node is not available
        List<String> listHealthIds = clusterStorage.getUnavailableNode();
        if (listHealthIds.size() == 0) {
            return;
        }
        //Mark all ID to recover
        Map<String, Boolean> map = clusterStorage.markToRecover(listHealthIds);
        //Check if all ID has been marked correctly
        //Log an error for every entry which can not be marked to recover
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (entry.getValue()) {
                boolean success = clusterStorage.recoverBuilds(entry.getKey());


                if (success) {
                    clusterStorage.markCompletedRecover(entry.getKey(), "SUCCESS");
                } else {
                    clusterStorage.markCompletedRecover(entry.getKey(), "FAILED");
                }
            } else {
                LOGGER.log(Level.SEVERE, "Can not mark to recover :" + entry.getKey());
            }
        }
    }

}
