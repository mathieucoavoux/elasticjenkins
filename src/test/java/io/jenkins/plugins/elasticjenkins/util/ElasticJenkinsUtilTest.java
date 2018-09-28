package io.jenkins.plugins.elasticjenkins.util;

import io.jenkins.plugins.elasticjenkins.ElasticJenkinsManagement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElasticJenkinsUtilTest {

    public static String url = "http://localhost:9200";
    public static String master = "MASTERNAME";
    public static String clusterName = "CLUSTERNAME";
    public static String hostname = "MYHOST";
    private static String indexJenkinsIndexCluster = "jenkins_manage_clusters";
    private static String indexJenkinsIndexMapping = "jenkins_manage_mapping";
    private static String indexLog = "jenkins_logs";
    public static String buildsIndex = "jenkins_builds";
    public static String queueIndex = "jenkins_queues";
    public static String clusterIndex = "jenkins_manage_clusters";
    public static String mappingIndex = "jenkins_manage_mapping";
    public static String mappingHealth = "jenkins_manage_health";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    public void deleleTest(String id) throws IOException {
        String uri = url+"/"+indexJenkinsIndexCluster+"/clusters/"+id;
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }

    @AfterClass
    public static void tearDown() throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(indexLog);
        list.add(buildsIndex);
        list.add(queueIndex);
        list.add(clusterIndex);
        list.add(mappingIndex);
        for(String uri : list) {
            HttpDelete httpDelete = new HttpDelete(url+"/"+uri);
            httpDelete.setHeader("Accept","application/json");
            HttpClientBuilder builder = HttpClientBuilder.create();
            CloseableHttpClient client = builder.build();
            CloseableHttpResponse response = client.execute(httpDelete);
        }
        Thread.sleep(2000);
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        ElasticJenkinsUtil.writeProperties(master,clusterName , url,"UTF-8",indexLog,buildsIndex,queueIndex,clusterIndex,mappingIndex,mappingHealth );

    }

    @Test
    public void testGetElasticsearchStatus() throws InterruptedException {
        String status = ElasticJenkinsUtil.getElasticsearchStatus(url);
        assertTrue(status.equals("yellow") || status.equals("green"));
    }

    @Test
    public void testGetIdByMaster() throws InterruptedException, IOException {
        ElasticJenkinsManagement ejm = new ElasticJenkinsManagement();
        assertTrue(ejm.addJenkinsMaster(master,clusterName,hostname,null,clusterIndex));
        //Leave a short period to let Elasticsearch saving the entry correctly
        Thread.sleep(2000);
        String masterId2 = ElasticJenkinsUtil.getIdByMaster(master);
        assertTrue(masterId2 != null);
        deleleTest(masterId2);
        Thread.sleep(2000);
        String masterId = ElasticJenkinsUtil.getIdByMaster(master);
        assertEquals(null,masterId);

    }

    @Test
    public void testGetCurrentMasterId() throws InterruptedException {
        ElasticManager em = new ElasticManager();
        ElasticJenkinsManagement elasticJenkinsManagement = new ElasticJenkinsManagement();
        if(elasticJenkinsManagement.addJenkinsMaster(master,clusterName,"TEST_SERVER","MASTERID123",clusterIndex)) {
            //As elastic search may takes time to create the server
            Thread.sleep(2000);
            assertEquals("MASTERID123",ElasticJenkinsUtil.getCurentMasterId());
        }

    }
}
