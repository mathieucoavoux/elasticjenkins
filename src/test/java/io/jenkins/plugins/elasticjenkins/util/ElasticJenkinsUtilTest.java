package io.jenkins.plugins.elasticjenkins.util;

import io.jenkins.plugins.elasticjenkins.ElasticJenkinsManagement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElasticJenkinsUtilTest {

    public static String url = "http://localhost:9200";
    public static String master = "MASTERNAME";
    public static String clusterName = "CLUSTERNAME";
    public static String hostname = "MYHOST";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    public void deleleTest(String id) throws IOException {
        String uri = url+"/jenkins_manage/clusters/"+id;
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }

    @Before
    public void setUp() throws IOException {
        ElasticJenkinsUtil.writeProperties(master,url,"UTF-16");

    }

    @Test
    public void testGetElasticsearchStatus() throws InterruptedException {
        String status = ElasticJenkinsUtil.getElasticsearchStatus(url);
        assertTrue(status.equals("yellow") || status.equals("green"));
    }

    @Test
    public void testGetIdByMaster() throws InterruptedException, IOException {
        String masterId = ElasticJenkinsUtil.getIdByMaster(master);
        assertEquals(null,masterId);
        ElasticJenkinsManagement ejm = new ElasticJenkinsManagement();
        assertTrue(ejm.addJenkinsMaster(master,clusterName,hostname,masterId));
        //Leave a short period to let Elasticsearch saving the entry correctly
        Thread.sleep(2000);
        String masterId2 = ElasticJenkinsUtil.getIdByMaster(master);
        System.out.println("master2"+masterId2);
        assertTrue(masterId2 != null);
        deleleTest(masterId2);
    }
}
