package io.jenkins.plugins.elasticjenkins.entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElasticMasterTest {

    @Test
    public void testElasticMaster() {
        ElasticMaster elasticMaster = new ElasticMaster();
        elasticMaster.setHostname("myHostname");
        elasticMaster.setClusterName("myClusterName");
        elasticMaster.setJenkinsMasterName("myJenkinsMaster");
        assertEquals("myHostname",elasticMaster.getHostname());
        assertEquals("myClusterName",elasticMaster.getClusterName());
        assertEquals("myJenkinsMaster",elasticMaster.getJenkinsMasterName());
    }
}
