package io.jenkins.plugins.elasticjenkins;

import hudson.model.*;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManagerTest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ElasticJenkinsActionTest {
    private static Logger LOGGER = Logger.getLogger(ElasticManagerTest.class.getName());

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static String testFolder = "elasticjenkins";

    public static String root = (System.getProperty("java.io.tmpdir"))+testFolder;
    public static File testFile = new File(root);



    public static String master = "MASTERNAME";
    public static String clusterName = "CLUSTER_NAME";
    public String uniqueId = "20170428";
    public String title = "PROJECT_NAME";
    public String logIndex = "jenkins_logs";
    public String buildsIndex = "jenkins_builds";
    public String queueIndex = "jenkins_queues";
    public String clusterIndex = "jenkins_manage_clusters";
    public String mappingIndex = "jenkins_manage_mapping";

    public static String url = "http://localhost:9200";

    public Computer computer;
    public Node node;


    public void deleleTest(String index, String type,String id) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url+"/"+index+"/"+type+"/"+id);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }

    public void deleteTest(String index,String suffix) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url+"/"+index+"/"+suffix);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }


    public Run<?,?> generateBuild(String id) throws IOException {
        computer = j.jenkins.createComputer();
        node = computer.getNode();
        j.jenkins.setNumExecutors(2);

        Long queueId = new Long(1);
        String url = "/"+title+"/"+id;

        Run build = Mockito.mock(Run.class);

        StringParameterValue p1 = new StringParameterValue("parameter1","éeù");
        StringParameterValue p2 = new StringParameterValue("parameter2","value2");
        ParametersAction pa =  new ParametersAction(p1,p2);


        List<ParametersAction> list = new ArrayList<ParametersAction>();
        list.add(pa);
        //Create Mock Job
        Job job = Mockito.mock(Job.class);
        job.setDisplayName(title);

        Mockito.when(build.getUrl()).thenReturn(url);
        Mockito.when(build.getId()).thenReturn(id);
        Mockito.when(build.getActions(ParametersAction.class)).thenReturn(list);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(build.getQueueId()).thenReturn(queueId);
        Mockito.when(build.getDisplayName()).thenReturn(title);

        return build;
    }

    public AbstractProject<?,?> generateProject(String id) {
        AbstractItem item = Mockito.mock(AbstractItem.class);
        AbstractProject<?,?> project = Mockito.mock(AbstractProject.class);
        String url = "/"+title+"/"+id;
        Mockito.when(project.getUrl()).thenReturn(url);
        return project;
    }

    @BeforeClass
    public static void initialize() throws IOException {
        if(! testFile.exists()) {
            if(!testFile.mkdirs())
                throw new IOException("Can not execute since the directory is not writtable: "+root);
        }
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        //ElasticJenkinsUtil.writeProperties(master,url,"UTF-16",logIndex);
        ElasticJenkinsUtil.writeProperties(master,clusterName , url,"UTF-8",logIndex,buildsIndex,queueIndex,clusterIndex,mappingIndex );
    }

    @Test
    public void allTest() {
        //testGetBuildByParameters();
    }

    public void testGetBuildByParameters() {
        AbstractProject<?,?> project = generateProject("76");
        ElasticJenkinsAction elasticJenkinsAction = new ElasticJenkinsAction(project);
        elasticJenkinsAction.getBuildByParameters("builds","cluster","Bonjour2");
    }
}
