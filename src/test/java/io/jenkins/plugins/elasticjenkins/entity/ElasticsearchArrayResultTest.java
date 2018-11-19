package io.jenkins.plugins.elasticjenkins.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import io.jenkins.plugins.elasticjenkins.TestsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
        genericBuild.setExecutedOn("master");
        genericBuild.setLaunchedByName("Mat");
        genericBuild.setProjectId("myProjectId");
        genericBuild.setStartDate(0L);
        genericBuild.setUrl("Project1");
        genericBuild.setEndDate(0L);
        genericBuild.setLogId("123");
        ParameterValue parameterValue = new ParameterValue("MyName","MyDescription") {
            @CheckForNull
            @Override
            public Object getValue() {
                return "MyValue";
            }
        };
        ParametersAction parametersAction = new ParametersAction(parameterValue);
        List<ParametersAction> list = new ArrayList<ParametersAction>();
        list.add(parametersAction);
        genericBuild.setParametersAction(list);
        return genericBuild;
    }

    @Test
    public void testArray() throws InterruptedException {
        //Gson gson = new GsonBuilder().create();
        Gson gson = new GsonBuilder().registerTypeAdapter(ParameterValue.class,TestsUtil.getPVDeserializer()).create();
        //Get Shards of the Elasticsearch cluster
        String uriBuild = url+"/"+TestsUtil.buildsIndex+"/builds/";
        String buildJson = gson.toJson(generateBuild());
        TestsUtil.elasticPost(uriBuild,buildJson);

        String uri = uriBuild+"_search?request_cache=false";
        String jsonResponse = TestsUtil.elasticGet(uri);
        int retry = 0;
        int maxRetry = 20;
        while(jsonResponse == null && retry < maxRetry) {
            Thread.sleep(1000);
            retry = retry + 1;
            jsonResponse = TestsUtil.elasticGet(uri);
        }
        Type elasticsearchArrayResulType = new TypeToken<ElasticsearchArrayResult<GenericBuild>>(){}.getType();
        ElasticsearchArrayResult<GenericBuild> list = gson.fromJson(jsonResponse,elasticsearchArrayResulType);
        assertTrue(list.shards != null);
        assertTrue(list.shards.getTotal() != null);
        assertTrue(list.shards.getTotal() != "0");
        assertTrue(list.shards.getSuccessful() != null);
        assertTrue(list.shards.getSuccessful() != "0");
        assertTrue(list.shards.getSkipped() != null);
        assertTrue(list.shards.getFailed() != null);

        assertTrue(list.hits != null);
        int retry2 = 0;
        int maxRetry2 = 20;
        while (! "1".equals(list.hits.getTotal()) && retry2 < maxRetry2) {
            jsonResponse = TestsUtil.elasticGet(uri);
            list = gson.fromJson(jsonResponse,elasticsearchArrayResulType);
            retry2 = retry2 + 1;
            Thread.sleep(1000);
        }
        assertEquals("1",list.hits.getTotal());
        List<ElasticsearchResult<GenericBuild>> listER = list.hits.getHits();
        assertTrue(listER.size() == 1);
        ElasticsearchResult<GenericBuild> er = listER.get(0);
        assertTrue(er.getId() != null);
        assertTrue(er.getResult() != null);
        assertTrue(er.getIndex() != null);
        assertTrue(er.getPrimaryTerm() != null);
        assertTrue(er.getSeqNo() != null);
        assertTrue(er.getType() != null);
        assertTrue(er.getVersion() != null);
        GenericBuild genericBuild = er.getSource();
        assertEquals("1",genericBuild.getId());
        assertEquals(TestsUtil.masterId,genericBuild.getJenkinsMasterId());
        assertEquals(TestsUtil.master,genericBuild.getJenkinsMasterName());
        assertEquals("SUCCESS",genericBuild.getStatus());
        assertTrue(genericBuild.getStartupTime() == 0L);
        assertEquals("ProjectName",genericBuild.getName());
        assertTrue(genericBuild.getQueuedSince() == 0L);
        assertEquals("master",genericBuild.getExecutedOn());
        assertEquals("Mat",genericBuild.getLaunchedByName());
        assertEquals("myProjectId",genericBuild.getProjectId());
        assertTrue(genericBuild.getStartDate() == 0L);
        assertEquals("Project1",genericBuild.getUrl());
        assertTrue(genericBuild.getEndDate() == 0L);
        assertEquals("123",genericBuild.getLogId());
    }

}
