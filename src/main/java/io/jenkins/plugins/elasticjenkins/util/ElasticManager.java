package io.jenkins.plugins.elasticjenkins.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.jayway.jsonpath.JsonPath;
import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.entity.Parameters;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticManager {

    private static final Logger LOGGER = Logger.getLogger(ElasticManager.class.getName());
    private static String jenkinsManageIndex = "jenkins_manage";
    private static String jenkinsManageType = "clusters";

    protected String url = ElasticJenkinsUtil.getProperty("persistenceStore");
    protected String master = ElasticJenkinsUtil.getProperty("masterName");

    protected Gson gson = new GsonBuilder().create();

    public String addBuild(@Nonnull String index, @Nonnull String type,
                           @Nonnull Run<?,?> build) {
        Gson gson = new GsonBuilder().create();

        //We set build values in a generic build to avoid to serialize unnecessary values
        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setName(ElasticJenkinsUtil.convertUrlToFullName(build.getUrl()));
        genericBuild.setId(build.getId());
        //genericBuild.setStartDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(build.getStartTimeInMillis())));

        //genericBuild.setParameters(build.getActions(ParametersAction.class));
        List<ParametersAction> parametersActions = build.getActions(ParametersAction.class);
        List<Parameters> listParameters = new ArrayList<>();
        for(ParametersAction parametersAction : parametersActions) {
            List<ParameterValue> parameterValues = parametersAction.getAllParameters();
            for(ParameterValue parameterValue : parameterValues) {
                Parameters parameters = new Parameters();
                parameters.setName(parameterValue.getName());
                parameters.setValue(parameterValue.getValue());
                parameters.setDescription(parameterValue.getDescription());
                listParameters.add(parameters);
            }

        }
        genericBuild.setParameters(listParameters);
        genericBuild.setLaunchedByName(User.current().getDisplayName());
        genericBuild.setLaunchedById(User.current().getId());
        genericBuild.setJenkinsMasterName(master);
        try {
            genericBuild.setExecutedOn(Executor.currentExecutor().getOwner().getHostName());
        } catch (IOException e) {
            LOGGER.log(Level.INFO,"We can not retrieve the hostname of the slave");
        } catch (InterruptedException e) {
            LOGGER.log(Level.INFO,"We can not retrieve the hostname of the slave");
        } catch (NullPointerException e) {
            LOGGER.log(Level.INFO,"Executor is empty. We must be in a testing mode");
        }
        genericBuild.setStatus("EXECUTING");
        //Convert the generic build to a Json string
        String json = gson.toJson(genericBuild);

        //Post the json to Elasticsearch
        String eId = build.getId()+"_"+master;
        String uri = url+"/"+index+"/"+type+"/"+eId;
        String elasticSearchId = null;
        try {
            StringEntity entity = new StringEntity(json);

            ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,entity),ElasticsearchResult.class);
            if (esr.getResult().equals("created")) elasticSearchId = esr.get_id();
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"Generic build is not serializable into JSON. Build: "
                    +genericBuild.getId()+" project:"+genericBuild.getName()+" will not be saved");
        }
        return elasticSearchId;
    }

    public String updateBuild(@Nonnull String index, @Nonnull String type,
                              @Nonnull Run<?,?> build, @Nonnull String id) {
        String elasticSearchId = "";
        Gson gson = new GsonBuilder().create();

        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setName(ElasticJenkinsUtil.convertUrlToFullName(build.getUrl()));
        genericBuild.setId(build.getId());
        genericBuild.setStartDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(build.getStartTimeInMillis())));

        /*
        List<ParametersAction> parametersActions = build.getActions(ParametersAction.class);
        for(ParametersAction parametersAction : parametersActions) {
            parametersAction.
        }
        */
        //genericBuild.setParameters();
        genericBuild.setLaunchedByName(User.current().getDisplayName());
        genericBuild.setLaunchedById(User.current().getId());
        genericBuild.setStatus("COMPLETED");

        /*
        List<String> listExample = new ArrayList<String>();
        listExample.add("Line1");
        listExample.add("Line2");
        genericBuild.setLogOutput(listExample);
*/
        try {
            //byte[] bytes = Files.readAllBytes(build.getLogFile().toPath());
            //genericBuild.setLogOutput(bytes);

            List<String> list = Files.readAllLines(build.getLogFile().toPath());
            List<String> update = new ArrayList<>();
            //genericBuild.setLogOutput(list);
            for(String oneLine : list) {
                //TODO: Set the Charset in the management console
                update.add(URLEncoder.encode(oneLine,"UTF-16"));
                LOGGER.log(Level.INFO,"Line:"+oneLine);
            }
            genericBuild.setLogOutput(update);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
        }

        String json = gson.toJson(genericBuild);

        String uri = url+"/"+index+"/"+type+"/"+id;
        StringEntity entity = null;
        try {
            entity = new StringEntity(json);


            ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,entity),ElasticsearchResult.class);

            if (esr.getResult().equals("updated")) elasticSearchId = esr.get_id();
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"Generic build is not serializable into JSON. Build: "
                    +genericBuild.getId()+" project:"+genericBuild.getName()+" will not be saved");
        }

        return elasticSearchId;
    }

    /**
     * Get Jenkins build history with a specific length.
     * @param index: Job name hash
     * @param type: builds
     * @param paginationSize: number of results to return
     * @param paginationStart: starts from where
     * @return: list of builds
     */
    protected List<GenericBuild> getPaginateBuildHistory(@Nonnull String index, @Nonnull String type,
                                                         @Nonnull Integer paginationSize, @Nullable String paginationStart) {

        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+index+"/"+type+"/_search";

        if(paginationStart == null)
            paginationStart = "0";

        StringEntity entity = null;
        try {
            entity = new StringEntity("{ \"query\" : {\n" +
                    "  \"match\" : {\n" +
                    "    \"jenkinsMasterName\" : \""+master+"\" \n" +
                    "  }\n" +
                    "},\n" +
                    " \"size\" : "+paginationSize+",\n" +
                    " \"from\" : "+paginationStart+",\n" +
                    " \"sort\" :  { \"_id\" : { \"order\" : \"desc\" } }\n" +
                    "}");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
            return null;
        }

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,entity);

        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        Integer max = total < paginationSize ? total : paginationSize;

        for(int i=0;i<max;i++) {

            GenericBuild genericBuild =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source").toString(),GenericBuild.class);
            listBuilds.add(genericBuild);
        }

        return listBuilds;
    }


    protected GenericBuild searchById(@Nonnull String index, @Nonnull String type,@Nonnull String id) {

        String uri = url+"/"+index+"/"+type+"/"+id+"/_source";
        return gson.fromJson(ElasticJenkinsUtil.elasticGet(uri),GenericBuild.class);
    }


    public List<ElasticMaster> getMasterByNameAndCluster(@Nonnull String masterName,
                                                         @Nonnull String clusterName) {
        List<ElasticMaster> listMasters = new ArrayList<>();
        String uri = url+"/"+jenkinsManageIndex+"/"+jenkinsManageType+"/_search";
        StringEntity entity = null;
        try {
            entity = new StringEntity("{ \"query\" : { \n" +
                    " \"bool\" : {\n" +
                    " \"should\" : [\n" +
                    "     { \"match\" : { \"jenkinsMasterName\" : \""+masterName+"\" }},\n" +
                    "     { \"match\" : { \"clusterName\": \""+clusterName+"\"}}\n" +
                    "     ]\n" +
                    "}\n" +
                    "}\n" +
                    "}");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
            return null;
        }
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,entity);

        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        for(int i=0;i<total;i++) {

            ElasticMaster elasticMaster =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source").toString(),ElasticMaster.class);
            listMasters.add(elasticMaster);
        }

        return listMasters;
    }

    public List<String> getMasterIdByNameAndCluster(@Nonnull String masterName,
                                                     @Nonnull String clusterName) {
        List<String> listIds = new ArrayList();
        String uri = url+"/"+jenkinsManageIndex+"/"+jenkinsManageType+"/_search";
        StringEntity entity = null;
        try {
            entity = new StringEntity("{ \"query\" : { \n" +
                    " \"bool\" : {\n" +
                    " \"should\" : [\n" +
                    "     { \"match\" : { \"jenkinsMasterName\" : \""+masterName+"\" }},\n" +
                    "     { \"match\" : { \"clusterName\": \""+clusterName+"\"}}\n" +
                    "     ]\n" +
                    "}\n" +
                    "}\n" +
                    "}");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
            return null;
        }
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,entity);

        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        for(int i=0;i<total;i++) {

            String id  =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._id").toString(),String.class);
            listIds.add(id);
        }

        return listIds;
    }
}
