package io.jenkins.plugins.elasticjenkins;

import hudson.model.Computer;
import hudson.model.Node;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;

public class ElasticJenkinsIntegrationTest {

    //@Rule
    //public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule j = new JenkinsRule(){

        private List<WebClient> clients = new ArrayList<WebClient>();

        @Override
        public void after() throws Exception {
            //super.after();
            if(TestEnvironment.get() != null) {
                try {
                    TestEnvironment.get().dispose();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    private static String master = "MASTERNAME";
    private static String clusterName = "CLUSTER_NAME";


    public static String masterId = "MASTER123";
    public static String hostname = "HOSTNAME1";
    private static String title = "PROJECT_NAME";
    public static String logIndex = "test_jenkins_logs";
    public static String buildsIndex = "test_jenkins_builds";
    public static String queueIndex = "test_jenkins_queues";
    public static String clusterIndex = "test_jenkins_manage_clusters";
    public static String clusterType = "clusters";
    public static String mappingIndex = "test_jenkins_manage_mapping";
    public static String mappingHealth = "test_jenkins_manage_health";

    //public static String url = "http://192.168.66.1:9200";
    public static String url = System.getProperty("elasticSearchUrl") != null ? System.getProperty("elasticSearchUrl") : "http://192.168.66.1:9200";
    private Computer computer;
    private Node node;
    //TestsUtil testsUtil = new TestsUtil(story, temporaryFolder);



    @Before
    public void setUp() throws IOException, InterruptedException {
        //testsUtil.reset();

    }

    @Test
    public void testStartJenkins() throws IOException, InterruptedException {
        ElasticJenkinsUtil.writeProperties(master,clusterName , url,"UTF-8",logIndex,buildsIndex,
                queueIndex,clusterIndex,mappingIndex,mappingHealth );


        TestsUtil.deleteIndices();

        TestsUtil.createIndex();
        ElasticJenkinsManagement elasticJenkinsManagement = new ElasticJenkinsManagement();
        elasticJenkinsManagement.addJenkinsMaster(master,clusterName,hostname,masterId,clusterIndex);
        Thread.sleep(2000);
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class,"p");
                p.setDefinition(new CpsFlowDefinition("node { wrap([$class: 'ElasticJenkinsWrapper', 'stepName' : 'create_ec2_instance']) { echo 'OK' } }",true));
                WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
                assertNotNull(b1);
                story.j.waitForCompletion(b1);
                File logFile = b1.getLogFile();

                story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b1));
            }
        });
    }
}
