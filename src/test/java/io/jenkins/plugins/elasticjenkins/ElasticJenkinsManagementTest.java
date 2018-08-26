package io.jenkins.plugins.elasticjenkins;

import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ElasticJenkinsManagementTest {

    public static String url = "http://localhost:9200";
    public static String master = "MASTERNAME";
    public static String clusterName = "CLUSTERNAME";
    public static String hostname = "MYHOST";
    private static String indexLog = "jenkins_logs";
    public String buildsIndex = "jenkins_builds";
    public String queueIndex = "jenkins_queues";
    public static String jenkinsManageIndexCluster = "jenkins_manage_clusters";
    public static String jenkinsManageIndexMapping = "jenkins_manage_mapping";
    public static String jenkinsManageType = "clusters";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    public void deleteCluster(String id) throws IOException {
        String uri = url+"/"+jenkinsManageIndexCluster+"/"+jenkinsManageType+"/"+id;
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        CloseableHttpResponse response = client.execute(httpDelete);
    }

    @Test
    public void testDoConfigure() throws IllegalAccessException, NoSuchFieldException, IOException, InterruptedException {
        ElasticJenkinsManagement elasticJenkinsManagement = new ElasticJenkinsManagement();
        HttpResponse responseOK = elasticJenkinsManagement.doConfigure(master,url,"UTF-8",clusterName,indexLog,buildsIndex,queueIndex,false,jenkinsManageIndexCluster,jenkinsManageIndexMapping);
        //Check response
        Field statusCodeField = responseOK.getClass().getDeclaredField("statusCode");
        statusCodeField.setAccessible(true);
        assertEquals(302,statusCodeField.getInt(responseOK));
        Field urlField = responseOK.getClass().getDeclaredField("url");
        urlField.setAccessible(true);
        assertEquals(".?success",urlField.get(responseOK));
        //Wait few seconds to let Elasticsearch save the master
        Thread.sleep(2000);
        //Get Id of the master to delete it
        ElasticManager elasticManager = new ElasticManager();
        List<String> listIds = elasticManager.getMasterIdByNameAndCluster(master,clusterName);
        for(String id : listIds) {
            deleteCluster(id);
        }

    }
}
