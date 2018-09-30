package io.jenkins.plugins.elasticjenkins;


import hudson.Extension;
import hudson.slaves.ComputerListener;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsStarter extends ComputerListener {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsStarter.class.getName());


    protected ElasticManager elasticManager = new ElasticManager();

    public ElasticJenkinsStarter() {
        //Need to store the value into a variable otherwise when getStartupTime will be called it will call the currentTimeMillis again
        Long currentTime = System.currentTimeMillis();
        ElasticJenkinsUtil.setStartupTime(currentTime);
        LOGGER.log(Level.INFO,"Startup time:"+currentTime);
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

        ElasticJenkinsHealthCheck elasticJenkinsHealthCheck = new ElasticJenkinsHealthCheck(healthId);
        Timer timer = new Timer(true);
        timer.schedule(elasticJenkinsHealthCheck, 0, 10 * 1000);
    }



}
