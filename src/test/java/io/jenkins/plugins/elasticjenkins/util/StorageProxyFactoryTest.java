package io.jenkins.plugins.elasticjenkins.util;

import io.jenkins.plugins.elasticjenkins.TestsUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StorageProxyFactoryTest {

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
        ElasticJenkinsUtil.setLogStorageType("elasticsearch");
        ElasticJenkinsUtil.setConfigurationStorageType("elasticsearch");
    }

    @Test
    public void testNewInstance() throws ClassNotFoundException, ConfigurationException {
        LogStorageInterface myObject = (LogStorageInterface)  StorageProxyFactory.newInstance(LogStorageInterface.class);
        assertTrue(myObject.getLogOutputId(null) == null);
        ConfigurationStorageInterface myObject2 = (ConfigurationStorageInterface) StorageProxyFactory.newInstance(ConfigurationStorageInterface.class);
        assertTrue(myObject2.getCountCurrentBuilds() == 0);
    }
}
