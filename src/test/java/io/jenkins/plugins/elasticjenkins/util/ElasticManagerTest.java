package io.jenkins.plugins.elasticjenkins.util;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.TestsUtil;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.powermock.api.mockito.PowerMockito;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;


public class ElasticManagerTest  {


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

    private static String master = "MASTERNAME";
    private static String clusterName = "CLUSTER_NAME";

    TestsUtil testsUtil = new TestsUtil(j, temporaryFolder);



    @Before
    public void setUp() throws IOException, InterruptedException {
        testsUtil.reset();
    }

    @Test
    public void addBuild() throws IOException {

        Run<?,?> build = testsUtil.generateBuild("1");
        ElasticManager em = new ElasticManager();
        String index = "myproject_myserver";
        String type = "builds";
        String idElastic = em.addBuild(index,build);
        assertTrue(idElastic.equals("1_"+index+"_"+TestsUtil.masterId));
        testsUtil.deleteTest(index,type,"1_"+index+"_"+TestsUtil.masterId);
    }

    @Test
    public void updateBuild() throws IOException, InterruptedException {
        Thread.sleep(10000);
        Run<?,?> build = testsUtil.generateBuild("1");
        ElasticManager em = new ElasticManager();
        String index = "jenkins_test";
        String type = "builds";
        String fileName = "testUpdate.log";
        File fileTest = new File("C:\\Users\\Mathieu\\Jenkins\\plugins\\elasticjenkins\\work\\jobs\\Abc\\builds\\27\\log");
        String idElastic = em.addBuild(index,build);
        List<String> logs = new ArrayList<>();
        logs.add("éer");
        logs.add("Line2");
        File subfolder = temporaryFolder.newFolder(TestsUtil.testFolder);
        File file = new File(subfolder.getPath()+"/"+fileName);
        if(file.exists()) file.delete();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath(),true));
        for(String row : logs)
            writer.append(row);
        writer.close();
        PowerMockito.when(build.getLogFile()).thenReturn(fileTest);
        String idUpdated = em.updateBuild(idElastic,"COMPLETED",fileTest, Charset.defaultCharset());
        assertEquals("1_"+index+"_"+TestsUtil.masterId,idUpdated);
        String idLog = em.searchById(idUpdated).getLogId();
        assertTrue(idLog != null);
        testsUtil.deleteTest(TestsUtil.logIndex,idLog);
        testsUtil.deleteTest(index,type,"1_"+index+"_"+TestsUtil.masterId);
        if(file.exists()) file.delete();

    }

    @Test
    public void searchById() throws IOException, InterruptedException {
        Thread.sleep(10000);
        Run<?,?> build = testsUtil.generateBuild("1");
        ElasticManager em = new ElasticManager();
        String index = "jenkins_test";
        String type = "builds";
        String fileName = "testUpdate2.log";
        String idElastic = em.addBuild(index,build);
        assertEquals(idElastic, "1_" + index + "_" + TestsUtil.masterId);

        List<String> logs = new ArrayList<>();

        File subfolder = temporaryFolder.newFolder(TestsUtil.testFolder);
        File file = new File(subfolder.getPath()+"/"+fileName);

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
        String idUpdated = em.updateBuild(idElastic,"COMPLETED",file, Charset.defaultCharset());
        assertEquals(idElastic,idUpdated);
        GenericBuild genericBuild = em.searchById(idElastic);
        String idLog = genericBuild.getLogId();
        assertTrue(idLog != null);
        List<String> outputList = Files.readAllLines(em.getLogOutput(URLDecoder.decode(idLog,"UTF-8"),"1").toPath());
        for(int ind=0;ind<outputList.size();ind++) {
            String lineElastic =URLDecoder.decode(outputList.get(ind),"UTF-8");
            assertEquals(logs.get(ind),lineElastic);
        }

        testsUtil.deleteTest(TestsUtil.logIndex,idLog);
        testsUtil.deleteTest(index,type,"1_"+index+"_"+master);
        if(file.exists()) file.delete();
    }

    @Test
    public void testGetPaginateBuildHistory() throws IOException, InterruptedException {
        ElasticManager em = new ElasticManager();

        String type = "builds";
        Run<?,?> build = testsUtil.generateBuild("1");
        String hash = ElasticJenkinsUtil.getHash(build.getUrl().split(build.getId())[0]);
        String projectId = em.addProjectMapping(hash,URLEncoder.encode(build.getUrl().split(build.getId())[0],ElasticJenkinsUtil.getCharset()));
        for(int i=1;i<10;i++) {
            em.addBuild(projectId,testsUtil.generateBuild(Integer.toString(i)));
        }
        //Let elasticsearch save the entries correctly
        Thread.sleep(2000);
        List<GenericBuild> list = em.getPaginateBuildHistory(hash, "clusters" , 2, "5");
        assertEquals("4",list.get(0).getId());
        assertEquals("3",list.get(1).getId());
        for(int i=1;i<10;i++) {
            testsUtil.deleteTest("jenkins_builds",type,Integer.toString(i)+"_"+master);
        }
    }

    @Test
    public void testAddProjectMapping() throws UnsupportedEncodingException {
        ElasticManager em = new ElasticManager();
        String index = "jenkins_test";
        String type = "builds";
        em.addProjectMapping(ElasticJenkinsUtil.getHash(index),URLEncoder.encode(index,"UTF-8"));

    }

    @Test
    public void testFindByParameter() {
        ElasticManager em = new ElasticManager();
        List<GenericBuild> list = em.findByParameter("44b1edab813647ffecac78d81c4b8d22", "MyMaster2","Bonjour2");
    }

    @Test
    public void  testGetProjectId() throws IOException, InterruptedException {
        ElasticManager elasticManager = new ElasticManager();
        Run<?,?> build = testsUtil.generateBuild("1");
        String hash = ElasticJenkinsUtil.getHash(build.getUrl().split(build.getId())[0]);
        elasticManager.addProjectMapping(hash,URLEncoder.encode(build.getUrl().split(build.getId())[0],ElasticJenkinsUtil.getCharset()));
        Thread.sleep(2000);
        String projectId = elasticManager.getProjectId(hash);
        assertTrue(projectId != null);
    }

    @Test
    public void testAddMasterStartupAndFlag() {
        //Startup time
        Long startupTime = 1538245515043L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        //Add jenkins startup in the health index
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        String uri = TestsUtil.url + "/" + TestsUtil.mappingHealth + "/health/" + id;
        String json = ElasticJenkinsUtil.elasticGet(uri);
        String myMaster = JsonPath.parse(json).read("$._source.jenkinsMasterName");
        assertEquals(master,myMaster);
    }


    @Test
    public void testGetUnavailableNode() throws InterruptedException {
        //Add a new jenkins startup
        Long startupTime = 1538245515044L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        //Update the health flag to a previous date
        String uri = TestsUtil.url + "/" + TestsUtil.mappingHealth + "/health/" + id+"/_update";
        String json = "{" +
                        "\"doc\" : {" +
                            "\"lastFlag\" : 1538245515045" +
                            "}"+
                        "}";
        ElasticJenkinsUtil.elasticPost(uri,json);
        //Loop until the entry has been updated
        String searchUri = TestsUtil.url + "/" + TestsUtil.mappingHealth + "/health/_search";
        String searchJson = "{"+
                            "   \"query\" : {"+
                                    "   \"bool\" : {"+
                                            "   \"must\" : ["+
                                                    "   { \"match\" : { \"lastFlag\" : 1538245515045}}"+
                                                    "]"+
                                            "}"+
                                    "}"+
                            "}";
        String jsonResponse = ElasticJenkinsUtil.elasticPost(searchUri,searchJson);
        int total = 0;
        if(jsonResponse != null)
            total = JsonPath.parse(jsonResponse).read("$.hits.total");
        int maxRetry = 10;
        int retry = 0;
        while(total != 1 && retry < maxRetry) {
            //Let some time to Elasticsearch to update the entry
            Thread.sleep(1000);
            jsonResponse = ElasticJenkinsUtil.elasticPost(searchUri,searchJson);
            if(jsonResponse != null)
                total = JsonPath.parse(ElasticJenkinsUtil.elasticPost(searchUri,searchJson)).read("$.hits.total");
            retry = retry + 1;
        }
        //Check if the master is raised as unavailable
        List<String> list = elasticManager.getUnavailableNode();
        assertEquals(1,list.size());
        assertEquals(id,list.get(0));
    }

    @Test
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

    @Test
    public void testRecoverBuilds() {
        //Add a new jenkins startup
        Long startupTime = 1538245515046L;
        ElasticJenkinsUtil.setStartupTime(startupTime);
        ElasticManager elasticManager = new ElasticManager();
        String id = elasticManager.addMasterStartup();
        //Add an entry in the manage_cluster
        String uriCluster = TestsUtil.url+"/"+TestsUtil.clusterIndex+"/clusters/";
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
        String uriQueue = TestsUtil.url+"/"+TestsUtil.queueIndex+"/queues/";
        //Save this build in the queue index
        ElasticJenkinsUtil.elasticPost(uriQueue,jsonGeneric);
        //Recover the build
        boolean result = elasticManager.recoverBuilds(id);
        assertTrue(result);
    }

    @Test
    public void testGetNodesByCluster() throws InterruptedException {
        //Delete previous master if they exists

        String uri = TestsUtil.url+"/"+TestsUtil.clusterIndex+"/clusters";
        String jsonMaster1 = "{"+
                                "\"jenkinsMasterName\" : \"MASTERNAME\","+
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
        assertTrue(nodesList.matches(".*master2.*"));
        assertTrue(nodesList.matches(".*MASTERNAME.*"));
    }
}
