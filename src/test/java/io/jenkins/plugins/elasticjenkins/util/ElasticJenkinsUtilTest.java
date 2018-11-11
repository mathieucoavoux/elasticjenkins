package io.jenkins.plugins.elasticjenkins.util;

import io.jenkins.plugins.elasticjenkins.ElasticJenkinsManagement;
import io.jenkins.plugins.elasticjenkins.TestsUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElasticJenkinsUtilTest {

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

    private static String master = "MASTERNAME2";
    private static String clusterName = "CLUSTER_NAME2";

    TestsUtil testsUtil = new TestsUtil(j, temporaryFolder);


    private static String testFolder = "elasticjenkins";

    public static String root = (System.getProperty("java.io.tmpdir"))+testFolder;
    public static File testFile = new File(root);

    @BeforeClass
    public static void initialize() throws IOException {
        if(! testFile.exists()) {
            if(!testFile.mkdirs())
                throw new IOException("Can not execute since the directory is not writtable: "+root);
        }
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        testsUtil.reset();
    }


    @Test
    public void testGetElasticsearchStatus() throws InterruptedException {
        String status = ElasticJenkinsUtil.getElasticSearchStatus(TestsUtil.url);
        assertTrue(status.equals("yellow") || status.equals("green"));
    }

    @Test
    public void testGetIdByMaster() throws InterruptedException, IOException {
        ElasticJenkinsManagement ejm = new ElasticJenkinsManagement();
        assertTrue(ejm.addJenkinsMaster(master,clusterName,TestsUtil.hostname,"MASTER456",TestsUtil.clusterIndex));
        //Leave a short period to let Elasticsearch saving the entry correctly
        Thread.sleep(2000);
        String masterId2 = ElasticJenkinsUtil.getIdByMaster(master);
        assertTrue(masterId2 != null);
        testsUtil.deleteTest(TestsUtil.clusterIndex,TestsUtil.clusterType,masterId2);
        Thread.sleep(2000);
        String masterId = ElasticJenkinsUtil.getIdByMaster(master);
        assertEquals(null,masterId);

    }

    @Test
    public void testGetCurrentMasterId() throws InterruptedException {
        //The master is already added in the setup
        assertEquals(TestsUtil.masterId,ElasticJenkinsUtil.getCurrentMasterId());


    }
}
