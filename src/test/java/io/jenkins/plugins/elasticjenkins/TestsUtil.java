package io.jenkins.plugins.elasticjenkins;

import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;



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
    public static String charset = "UTF-8";

    //public static String url = "http://192.168.66.1:9200";
    public static String url = System.getProperty("elasticSearchUrl") != null ? System.getProperty("elasticSearchUrl") : "http://localhost:9200";
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

    public String loadResource(String name) {
        try {
            return new String(IOUtils.toByteArray(getClass().getResourceAsStream(name)));
        } catch (Throwable t) {
            throw new RuntimeException("Could not read resource:[" + name + "].");
        }
    }

    public static String elasticPost(@Nonnull String uri, @Nonnull String json) {
        String result = null;
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Accept","application/json");
        httpPost.setHeader("Content-type","application/json");

        StringEntity entity = new StringEntity(json,charset);
        httpPost.setEntity(entity);
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            result = EntityUtils.toString(response.getEntity());



        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
    public static String elasticGet(@Nonnull String uri) {
        String result = null;
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            result = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 302) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static void createManageIndex() {
        String uri = url+"/"+clusterIndex;
        String uri2 = url+"/"+mappingIndex;
        String uriBuilds = url+"/"+buildsIndex;
        String uriQueues = url+"/"+queueIndex;
        String json = "{\n" +
                "    \"settings\" : {\n" +
                "        \"index\" : {\n" +
                "            \"number_of_shards\" : 3, \n" +
                "            \"number_of_replicas\" : 2 \n" +
                "        }\n" +
                "    }\n" +
                "}";
        String mappingBuilds = "{\n" +
                "\t\"settings\" : {\n" +
                "\t\t\"index\" : {\n" +
                "\t\t\t\"number_of_shards\" : 3, \n" +
                "\t\t\t\"number_of_replicas\" : 0 \n" +
                "\t\t}\n" +
                "\t},\n" +
                "  \"mappings\": {\n" +
                "    \"builds\": {\n" +
                "      \"properties\": {\n" +
                "            \"startDate\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "        },\n" +
                "        \"endDate\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "        },\n" +
                "            \"startupTime\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "            },\n" +
                "        \"id\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"name\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        },\n" +
                "       \"url\" : {\n" +
                "           \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"queuedSince\" : {\n" +
                "          \"type\" : \"long\"\n" +
                "        },\n" +
                "        \"logId\" : {\n" +
                "          \"type\" : \"text\"\n" +
                "        },\n" +
                "        \"status\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"jenkinsMasterName\" : {\n" +
                "          \"type\" : \"text\",\n" +
                "           \"fields\" : {\n" +
                "               \"keyword\" : {\n" +
                "                   \"type\" : \"keyword\",\n"+
                "                   \"ignore_above\" : 256\n"+
                "               }\n" +
                "           }\n" +
                "        },\n" +
                "        \"projectId\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"launchedByName\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"parameters\" : {\n" +
                "          \"properties\" : {\n" +
                "            \"name\" : {\n" +
                "              \"type\" : \"keyword\" \n" +
                "            },\n" +
                "            \"value\" : {\n" +
                "              \"type\" : \"keyword\" \n" +
                "            },\n" +
                "            \"description\" : {\n" +
                "              \"type\" : \"keyword\" \n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        elasticPut(uri2,json);
        elasticPut(uriBuilds,mappingBuilds);
        elasticPut(uriBuilds,uri);
        elasticPut(uriQueues,json);
    }

    private static String elasticPut(@Nonnull String uri, @Nonnull String json) {
        String result = null;
        HttpPut httpPut = new HttpPut(uri);
        httpPut.setHeader("Accept","application/json");
        httpPut.setHeader("Content-type","application/json");
        StringEntity entity = null;
        try {
            entity = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        httpPut.setEntity(entity);

        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpPut);
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
