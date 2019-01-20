package io.jenkins.plugins.elasticjenkins;

import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ElasticJenkinsRecoverTest {

    public Long startupTime1 = 1538245515043L;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule public JenkinsRule j = new JenkinsRule(){

        private List<WebClient> clients = new ArrayList<WebClient>();

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
        //Add startupTime
        ElasticJenkinsUtil.setStartupTime(startupTime1);
    }

    @Test
    public void testRun() {
        //This is an empty test since everything is done in the setUp
    }


}
