package io.jenkins.plugins.elasticjenkins;

import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestsUtil {

    JenkinsRule j;
    TemporaryFolder testFile;
    RestartableJenkinsRule story;

    public TestsUtil(JenkinsRule j,TemporaryFolder testFile) {
        this.j = j;
        this.testFile = testFile;
    }

    public TestsUtil(RestartableJenkinsRule story,TemporaryFolder testFile) {
        this.story = story;
        this.testFile = testFile;
    }

    public static String testFolder = "elasticjenkins";

    public static String master = "MASTERNAME";
    public static String clusterName = "CLUSTER_NAME";
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

    public void reset() throws IOException, InterruptedException {
        ElasticJenkinsUtil.writeProperties(master,clusterName , url,"UTF-8",logIndex,buildsIndex,
                queueIndex,clusterIndex,mappingIndex,mappingHealth );
        deleteIndices();

        createIndex();
        ElasticJenkinsManagement elasticJenkinsManagement = new ElasticJenkinsManagement();
        elasticJenkinsManagement.addJenkinsMaster(master,clusterName,hostname,masterId,clusterIndex);
        Thread.sleep(2000);
    }

    public AbstractProject<?,?> generateProject(String id) {
        AbstractItem item = Mockito.mock(AbstractItem.class);
        AbstractProject<?,?> project = Mockito.mock(AbstractProject.class);
        String url = "/"+title+"/"+id;
        Mockito.when(project.getUrl()).thenReturn(url);
        return project;
    }

    public WorkflowJob genereatePipelineProject(String resourceName) throws IOException {
        WorkflowJob project = j.createProject(WorkflowJob.class,"p");
        //project.setDefinition(new CpsFlowDefinition("pipeline { agent any; stages { stage('test') { sh 'echo OK' } } }", true));
        project.setDefinition(new CpsFlowDefinition(loadResource(resourceName),true));
        return project;

    }

    public Run<?,?> generateBuild(String id) throws IOException {
        computer = j.jenkins.createComputer();
        node = computer.getNode();
        j.jenkins.setNumExecutors(2);

        Long queueId = 1L;
        String url = "/"+title+"/"+id;

        Run build = Mockito.mock(Run.class);

        StringParameterValue p1 = new StringParameterValue("parameter1","éeù");
        StringParameterValue p2 = new StringParameterValue("parameter2","value2");
        ParametersAction pa =  new ParametersAction(p1,p2);


        List<ParametersAction> list = new ArrayList<ParametersAction>();
        list.add(pa);
        //Create Mock Job
        Job job = PowerMockito.mock(Job.class);
        FreeStyleProject freeStyleProject = PowerMockito.mock(FreeStyleProject.class);
        job.setDisplayName(title);
        freeStyleProject.setDisplayName(title);
        FreeStyleProject myProject = j.createFreeStyleProject();
        Mockito.when(build.getUrl()).thenReturn(url);
        Mockito.when(build.getId()).thenReturn(id);
        Mockito.when(build.getActions(ParametersAction.class)).thenReturn(list);
        Mockito.when(build.getParent()).thenReturn(myProject);
        Mockito.when(build.getQueueId()).thenReturn(queueId);
        Mockito.when(build.getDisplayName()).thenReturn(title);

        return build;
    }


    public void deleteTest(String index, String type, String id) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url+"/"+index+"/"+type+"/"+id);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }

    public void deleteTest(String index, String suffix) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url+"/"+index+"/"+suffix);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }



    public static void deleteIndices() throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(logIndex);
        list.add(buildsIndex);
        list.add(queueIndex);
        list.add(clusterIndex);
        list.add(mappingIndex);
        list.add(mappingHealth);
        for(String uri : list) {
            HttpDelete httpDelete = new HttpDelete(url+"/"+uri);
            httpDelete.setHeader("Accept","application/json");
            HttpClientBuilder builder = HttpClientBuilder.create();
            CloseableHttpClient client = builder.build();
            CloseableHttpResponse response = client.execute(httpDelete);
        }
        Thread.sleep(2000);
    }

    public static void createIndex() {
        ElasticManager em = new ElasticManager();
        ElasticJenkinsUtil.createManageIndex();
        ElasticJenkinsUtil.createHealthIndex();
    }

    private String loadResource(String name) {
        try {
            return new String(IOUtils.toByteArray(getClass().getResourceAsStream(name)));
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }
}
