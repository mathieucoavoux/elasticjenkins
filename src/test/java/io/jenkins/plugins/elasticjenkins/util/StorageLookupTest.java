package io.jenkins.plugins.elasticjenkins.util;

import io.jenkins.plugins.elasticjenkins.TestsUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class StorageLookupTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule public JenkinsRule j = new JenkinsRule(){

        private List<WebClient> clients = new ArrayList<WebClient>();
        @Override
        public void before() throws Throwable {
            super.before();
            ElasticJenkinsUtil.setJenkinsHealthCheckEnable(false);
        }

        @Override
        public void after() throws Exception {
            super.after();
            if(TestEnvironment.get() != null) {
                try {
                    TestEnvironment.get().dispose();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    TestsUtil testsUtil = new TestsUtil(j, temporaryFolder);



    @Before
    public void setUp() throws IOException, InterruptedException {
        testsUtil.reset();
        ElasticJenkinsUtil.setLogStorageType("cloud");
    }


    @Test
    public void testGetLogStorage() throws IOException {
        Object storageType1 = StorageLookup.getLogStorage();
        assertTrue(storageType1 == null);
        ElasticJenkinsUtil.setLogStorageType("elasticsearch");
        Object storageType2 = StorageLookup.getLogStorage();
        assertEquals("io.jenkins.plugins.elasticjenkins.util.ElasticManager",storageType2.getClass().getName());
    }
}
