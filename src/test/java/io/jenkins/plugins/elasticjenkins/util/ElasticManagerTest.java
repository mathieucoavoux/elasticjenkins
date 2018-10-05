package io.jenkins.plugins.elasticjenkins.util;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class ElasticManagerTest  {

    private static Logger LOGGER = Logger.getLogger(ElasticManagerTest.class.getName());

    @Rule public JenkinsRule j = new JenkinsRule();

    private static String testFolder = "elasticjenkins";

    public static String root = (System.getProperty("java.io.tmpdir"))+testFolder;
    public static File testFile = new File(root);



    public static String master = "MASTERNAME";
    public static String clusterName = "CLUSTER_NAME";
    public String uniqueId = "20170428";
    public String title = "PROJECT_NAME";
    public String logIndex = "test_jenkins_logs";
    public String buildsIndex = "test_jenkins_builds";
    public String queueIndex = "test_jenkins_queues";
    public String clusterIndex = "test_jenkins_manage_clusters";
    public String mappingIndex = "test_jenkins_manage_mapping";
    public String mappingHealth = "test_jenkins_manage_health";

    public static String url = "http://192.168.44.1:9200";

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

    public void deleteIndices() throws IOException, InterruptedException {
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
        ElasticJenkinsUtil.writeProperties(master,clusterName , url,"UTF-8",logIndex,buildsIndex,queueIndex,clusterIndex,mappingIndex,mappingHealth );

    }

    /**
     * All tests are in the same function as with encountered an issue
     * with JenkinsRules when splitting tests in different methods
     * We received an NullPointerException on some system.
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void allTests() throws IOException, InterruptedException {

        deleteIndices();

        createIndex();
        //ElasticJenkinsManagement elasticJenkinsManagement = new ElasticJenkinsManagement();
        //elasticJenkinsManagement.addJenkinsMaster(master,clusterName,"TEST_SERVER",master,clusterIndex);
        //Thread.sleep(2000);
        //Add build
        //addBuild();

        //Update build
        //updateBuild();

        //Search by id
        //searchById();

        //Get pagination
        //testGetPaginateBuildHistory();

        //Test add project mapping
        //testAddProjectMapping();

        //testFindByParameter();

        //testGetProjectId();

        //testAddMasterStartupAndFlag();

        //testGetUnavailableNode();

        //testMarkToRecover();

        //testRecoverBuilds();

        testGetNodesByCluster();

        deleteIndices();

    }

    public void createIndex() {
        ElasticManager em = new ElasticManager();
        ElasticJenkinsUtil.createManageIndex();
        ElasticJenkinsUtil.createHealthIndex();
    }


    public void addBuild() throws IOException, InterruptedException {

        Run<?,?> build = generateBuild("1");
        ElasticManager em = new ElasticManager();
        String index = "myproject/myserver";
        String type = "builds";
        String idElastic = em.addBuild(index,build);
        assertTrue(idElastic.equals("1_"+index+"_"+master));
        deleleTest(index,type,"1_"+index+"_"+master);
    }


    public void updateBuild() throws IOException, InterruptedException {
        Thread.sleep(10000);
        Run<?,?> build = generateBuild("1");
        ElasticManager em = new ElasticManager();
        String index = "jenkins_test";
        String type = "builds";
        String fileName = "testUpdate.log";
        String idElastic = em.addBuild(index,build);
        List<String> logs = new ArrayList<>();
        logs.add("éer");
        logs.add("Line2");
        File file = new File(root+"/"+fileName);
        if(file.exists()) file.delete();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath(),true));
        for(String row : logs)
            writer.append(row);
        writer.close();
        PowerMockito.when(build.getLogFile()).thenReturn(file);
        String idUpdated = em.updateBuild(idElastic,"COMPLETED",file);
        assertEquals("1_"+index+"_"+master,idUpdated);
        String idLog = em.searchById(idUpdated).getLogId();
        assertTrue(idLog != null);
        deleteTest(logIndex,idLog);
        deleleTest(index,type,"1_"+index+"_"+master);
        if(file.exists()) file.delete();

    }

    public void searchById() throws IOException, InterruptedException {
        Thread.sleep(10000);
        Run<?,?> build = generateBuild("1");
        ElasticManager em = new ElasticManager();
        String index = "jenkins_test";
        String type = "builds";
        String fileName = "testUpdate2.log";
        String idElastic = em.addBuild(index,build);
        assertTrue(idElastic.equals("1_"+index+"_"+master));

        List<String> logs = new ArrayList<>();

        File file = new File(root+"/"+fileName);

        if(file.exists()) file.delete();

        logs.add("[[ \"test\" == \"test\" ]] && echo OK;");
        logs.add("#Javascript");
        logs.add("if('OK' == 'OK'){alert('OK');}");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath(),true));
        for(String row : logs) {
            writer.append(row+"\n");
        }
        writer.flush();
        writer.close();
        PowerMockito.when(build.getLogFile()).thenReturn(file);
        String idUpdated = em.updateBuild(idElastic,"COMPLETED",file);
        assertEquals(idElastic,idUpdated);
        GenericBuild genericBuild = em.searchById(idElastic);
        String idLog = genericBuild.getLogId();
        assertTrue(idLog != null);
        List<String> outputList = Files.readAllLines(em.getLogOutput(URLDecoder.decode(idLog,"UTF-8"),"1").toPath());
        for(int ind=0;ind<outputList.size();ind++) {
            String lineElastic =URLDecoder.decode(outputList.get(ind),"UTF-8");
            assertEquals(logs.get(ind),lineElastic);
        }

        deleteTest(logIndex,idLog);
        deleleTest(index,type,"1_"+index+"_"+master);
        if(file.exists()) file.delete();
    }

    public void testGetPaginateBuildHistory() throws IOException, InterruptedException {
        ElasticManager em = new ElasticManager();

        String type = "builds";
        Run<?,?> build = generateBuild("1");
        String hash = ElasticJenkinsUtil.getHash(build.getUrl().split(build.getId())[0]);
        String projectId = em.addProjectMapping(hash,URLEncoder.encode(build.getUrl().split(build.getId())[0],ElasticJenkinsUtil.getProperty("elasticCharset")));
        for(int i=1;i<10;i++) {
            em.addBuild(projectId,generateBuild(Integer.toString(i)));
        }
        //Let elasticsearch save the entries correctly
        Thread.sleep(2000);
        List<GenericBuild> list = em.getPaginateBuildHistory(hash, "clusters" , 2, "5");
        assertEquals("4",list.get(0).getId());
        assertEquals("3",list.get(1).getId());
        for(int i=1;i<10;i++) {
            deleleTest("jenkins_builds",type,Integer.toString(i)+"_"+master);
        }
    }

    public void testAddProjectMapping() throws UnsupportedEncodingException {
        ElasticManager em = new ElasticManager();
        String index = "jenkins_test";
        String type = "builds";
        em.addProjectMapping(ElasticJenkinsUtil.getHash(index),URLEncoder.encode(index,"UTF-8"));

    }

    public void testFindByParameter() {
        ElasticManager em = new ElasticManager();
        List<GenericBuild> list = em.findByParameter("44b1edab813647ffecac78d81c4b8d22", "MyMaster2","Bonjour2");
    }


    public void testGetProjectId() throws IOException, InterruptedException {
        ElasticManager elasticManager = new ElasticManager();
        Run<?,?> build = generateBuild("1");
        String hash = ElasticJenkinsUtil.getHash(build.getUrl().split(build.getId())[0]);
        elasticManager.addProjectMapping(hash,URLEncoder.encode(build.getUrl().split(build.getId())[0],ElasticJenkinsUtil.getProperty("elasticCharset")));
        Thread.sleep(2000);
        String projectId = elasticManager.getProjectId(hash);
        assertTrue(projectId != null);
    }

    public void testAddMasterStartupAndFlag() {
        //Startup time
        Long startupTime = 1538245515043L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        //Add jenkins startup in the health index
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        String uri = url + "/" + mappingHealth + "/health/" + id;
        String json = ElasticJenkinsUtil.elasticGet(uri);
        String myMaster = JsonPath.parse(json).read("$._source.jenkinsMasterName");
        assertEquals(master,myMaster);
    }


    public void testGetUnavailableNode() throws InterruptedException {
        //Add a new jenkins startup
        Long startupTime = 1538245515044L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        //Update the health flag to a previous date
        String uri = url + "/" + mappingHealth + "/health/" + id+"/_update";
        String json = "{" +
                        "\"doc\" : {" +
                            "\"lastFlag\" : 1538245515045" +
                            "}"+
                        "}";
        ElasticJenkinsUtil.elasticPost(uri,json);
        //Let some time to Elasticsearch to update the entry
        Thread.sleep(3000);
        //Check if the master is raised as unavailable
        List<String> list = elasticManager.getUnavailableNode();
        assertEquals(1,list.size());
        assertEquals(id,list.get(0));
    }

    public void testMarkToRecover() {
        //Add a new jenkins startup
        Long startupTime = 1538245515045L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        List<String> list = new ArrayList<>();
        list.add(id);
        Map<String,Boolean> map = elasticManager.markToRecover(list);
        assertEquals(1,map.size());
        assertTrue(map.get(id));
    }

    public void testRecoverBuilds() {
        //Add a new jenkins startup
        Long startupTime = 1538245515046L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        //Add an entry in the manage_cluster
        String uriCluster = url+"/"+clusterIndex+"/clusters/";
        String jsonCluster = " {" +
                            "\"jenkinsMasterName\" : \""+master+"\","+
                            "\"clusterName\" : \""+clusterName+"\","+
                            "\"hostname\" : \"MYHOST\""+
                      "}";
        String jsonResponseCluster = ElasticJenkinsUtil.elasticPost(uriCluster,jsonCluster);
        String masterId = JsonPath.parse(jsonResponseCluster).read("$._id");
        //Create a new GenericBuild
        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setId("1");
        genericBuild.setName("Myproject");
        genericBuild.setStartupTime(startupTime);
        genericBuild.setStatus("ENQUEUED");
        genericBuild.setJenkinsMasterName(master);
        genericBuild.setJenkinsMasterId(masterId);
        Gson gson = new Gson();
        String jsonGeneric = gson.toJson(genericBuild);
        String uriQueue = url+"/"+queueIndex+"/queues/";
        //Save this build in the queue index
        ElasticJenkinsUtil.elasticPost(uriQueue,jsonGeneric);
        //Recover the build
        boolean result = elasticManager.recoverBuilds(id);
        assertTrue(result);
    }

    public void testGetNodesByCluster() throws InterruptedException {
        String uri = url+"/"+clusterIndex+"/clusters";
        String jsonMaster1 = "{"+
                                "\"jenkinsMasterName\" : \"master1\","+
                                "\"clusterName\" : \""+clusterName+"\","+
                                "\"hostname\" : \"server1\""+
                            "}";
        String jsonMaster2 = "{"+
                "\"jenkinsMasterName\" : \"master2\","+
                "\"clusterName\" : \""+clusterName+"\","+
                "\"hostname\" : \"server2\""+
                "}";
        ElasticJenkinsUtil.elasticPost(uri,jsonMaster1);
        ElasticJenkinsUtil.elasticPost(uri,jsonMaster2);
        Thread.sleep(3000);
        ElasticManager elasticManager = new ElasticManager();
        String nodesList = elasticManager.getNodesByCluster(clusterName);
        assertEquals("master1 master2",nodesList);
    }
}
