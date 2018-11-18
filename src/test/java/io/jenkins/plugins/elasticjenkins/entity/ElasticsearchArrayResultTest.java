package io.jenkins.plugins.elasticjenkins.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import hudson.model.ParameterValue;
import io.jenkins.plugins.elasticjenkins.TestsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class ElasticsearchArrayResultTest {

    String url = TestsUtil.url;

    @Before
    public void setUp() throws IOException, InterruptedException {
        TestsUtil.deleteIndices();
        TestsUtil.createManageIndex();
    }

    private GenericBuild generateBuild() {
        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setJenkinsMasterId(TestsUtil.masterId);
        genericBuild.setJenkinsMasterName(TestsUtil.master);
        genericBuild.setStatus("SUCCESS");
        genericBuild.setStartupTime(0L);
        genericBuild.setName("ProjectName");
        genericBuild.setQueuedSince(0L);
        genericBuild.setId("1");
        return genericBuild;
    }

    @Test
    public void testArray() throws InterruptedException {
        Gson gson = new GsonBuilder().create();
        //Get Shards of the Elasticsearch cluster
        String uriBuild = url+"/"+TestsUtil.buildsIndex+"/builds/";
        String buildJson = gson.toJson(generateBuild());
        TestsUtil.elasticPost(uriBuild,buildJson);

        String uri = uriBuild+"_search";
        String jsonResponse = TestsUtil.elasticGet(uri);
        int retry = 0;
        int maxRetry = 20;
        while(jsonResponse == null && retry < maxRetry) {
            Thread.sleep(1000);
            retry = retry + 1;
            jsonResponse = TestsUtil.elasticGet(uri);
        }
        Type elasticsearchArrayResulType = new TypeToken<ElasticsearchArrayResult<Object>>(){}.getType();
        ElasticsearchArrayResult<Object> list = gson.fromJson(jsonResponse,elasticsearchArrayResulType);
        assertTrue(list.shards != null);
        assertTrue(list.shards.getTotal() != null);
        assertTrue(list.shards.getTotal() != "0");

        assertTrue(list.hits != null);
        int retry2 = 0;
        int maxRetry2 = 20;
        while (! "1".equals(list.hits.getTotal()) && retry2 < maxRetry2) {
            list = gson.fromJson(jsonResponse,elasticsearchArrayResulType);
            retry2 = retry2 + 1;
        }
        assertEquals("1",list.hits.getTotal());
    }

}
