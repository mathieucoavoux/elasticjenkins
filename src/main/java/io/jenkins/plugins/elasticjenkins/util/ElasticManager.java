package io.jenkins.plugins.elasticjenkins.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.jayway.jsonpath.JsonPath;
import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.entity.Parameters;


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
    private static String jenkinsManageIndexCluster = "jenkins_manage_clusters";
    private static String jenkinsManageIndexMapping = "jenkins_manage_mapping";
    private static String jenkinsManageClusters = "clusters";
    private static String jenkinsManageMapping = "mapping";


    protected String url = ElasticJenkinsUtil.getProperty("persistenceStore");
    protected String master = ElasticJenkinsUtil.getProperty("masterName");
    protected String charset = ElasticJenkinsUtil.getProperty("elasticCharset");

    protected Gson gson = new GsonBuilder().create();

    public String addBuild(@Nonnull String index, @Nonnull String type,
                           @Nonnull Run<?,?> build) {
        Gson gson = new GsonBuilder().create();

        //We set build values in a generic build to avoid to serialize unnecessary values
        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setName(ElasticJenkinsUtil.convertUrlToFullName(build.getUrl()));
        genericBuild.setId(build.getId());
        //genericBuild.setStartDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(build.getStartTimeInMillis())));

        genericBuild.setStartDate(System.currentTimeMillis());

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
        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),ElasticsearchResult.class);
        if (esr.getResult().equals("created") || esr.getResult().equals("updated")) elasticSearchId = esr.get_id();

        return elasticSearchId;
    }

    public String updateBuild(@Nonnull String index, @Nonnull String type,
                              @Nonnull Run<?,?> build, @Nonnull String id,
                              @Nonnull String status,
                              @Nullable List<String> logs) {
        String elasticSearchId = "";
        String indexLogs = ElasticJenkinsUtil.getProperty("jenkins_logs");

        List<String> update = new ArrayList<>();
        try {

            List<String> list = Files.readAllLines(build.getLogFile().toPath());

            for(String oneLine : logs) {
                //update.add(URLEncoder.encode(oneLine,charset));
                update.add(oneLine);
                LOGGER.log(Level.INFO,"Line:"+oneLine);
            }
            //genericBuild.setLogId(update);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
        }

        String suffix = "";
        if (logs != null ) {
            String typeLog = new SimpleDateFormat("yyyy_MM").format(new Date());
            String logId = null;
            String uriLogs = url + "/" + indexLogs + "/" + typeLog+"/";
            String json = "{ \"logs\" : \n" +
                    gson.toJson(update,List.class) +
                    "}";
            LOGGER.log(Level.FINEST,"URI: {0}, json: {1}", new Object[]{uriLogs,json});
            ElasticsearchResult esr2 = gson.fromJson(ElasticJenkinsUtil.elasticPost(uriLogs,json),ElasticsearchResult.class);
            if (esr2.getResult().equals("created")) logId = esr2.get_id();
            suffix = typeLog.concat("/"+logId);

        }
        String uri = url+"/"+index+"/"+type+"/"+id+"/_update";
        String json = null;
        try {
            json = "{\n" +
                    "  \"doc\": {\n" +
                    "    \"status\" : \""+status+"\",\n" +
                    "    \"logId\" : " + "\""+URLEncoder.encode(suffix,charset)+"\",\n" +
                    "   \"endDate\" : \""+System.currentTimeMillis()+"\"" +
                    "  }\n" +
                    "}";
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"Unable to encode URL");
        }

        LOGGER.log(Level.FINEST,"Update uri: {0}, json: {1}", new Object[]{uri,json});
        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),ElasticsearchResult.class);

        if (esr.getResult().equals("updated")) elasticSearchId = esr.get_id();

        return elasticSearchId;
    }

    /**
     * Get Jenkins build history with a specific length.
     * @param index : Job name hash
     * @param type : builds
     * @param masters
     * @param paginationSize : number of results to return
     * @param paginationStart : starts from where
     * @return: list of builds
     */
    public List<GenericBuild> getPaginateBuildHistory(@Nonnull String index, @Nonnull String type,
                                                      @Nonnull String masters,
                                                      @Nonnull Integer paginationSize, @Nullable String paginationStart) {

        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+index+"/"+type+"/_search";

        if(paginationStart == null)
            paginationStart = "0";

        String json = "{ \"query\" : {\n" +
                "  \"match\" : {\n" +
                "    \"jenkinsMasterName\" : \""+masters+"\" \n" +
                "  }\n" +
                "},\n" +
                " \"size\" : "+paginationSize+",\n" +
                " \"from\" : "+paginationStart+",\n" +
                " \"sort\" :  { \"_id\" : { \"order\" : \"desc\" } }\n" +
                "}";


        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);

        LOGGER.log(Level.FINEST,"jsonResponse:"+jsonResponse);
        //LOGGER.log(Level.INFO,"jsin hits: "+JsonPath.parse(jsonResponse).read("$.hits.hits").toString());
        //List<GenericBuild> listBuilds = gson.fromJson(JsonPath.parse(jsonResponse).read("$.hits.hits").toString(),List.class);

//        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
//        Integer max = total < paginationSize ? total : paginationSize;
          Integer max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");
        for(int i=0;i<max;i++) {
            LOGGER.log(Level.FINEST,"Index: {0}, content: {1}", new Object[]{i,JsonPath.parse(jsonResponse).read("$.hits.hits["+i+"]._source").toString()});
            GenericBuild genericBuild =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source").toString(),GenericBuild.class);
            listBuilds.add(genericBuild);
        }

        return listBuilds;
    }

    public List<GenericBuild> getNewResults(@Nonnull String index, @Nonnull String type,
                                            @Nonnull String lastFetch, String masters) {
        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+index+"/"+type+"/_search";
        String json = "{\n" +
                "   \"query\": {\n" +
                "       \"bool\": {\n" +
                "           \"must\": [\n" +
                "               { \"match\": { \"jenkinsMasterName\": \""+masters+"\" }},\n" +
                "               { \"range\": { \"startDate\": { \"gte\": "+lastFetch+" }}}\n" +
                "           ]\n" +
                "       }\n" +
                "   }\n" +
                "}";
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);
        Integer max = 0;
        try {
            max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");
        }catch(Exception e){
                LOGGER.log(Level.FINEST,"ERROR, jsonResponse:"+jsonResponse);
            }
        for(int i=0;i<max;i++) {
            LOGGER.log(Level.FINEST,"Index: {0}, content: {1}", new Object[]{i,JsonPath.parse(jsonResponse).read("$.hits.hits["+i+"]._source").toString()});
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

    public String getLogOutputId(@Nonnull String index, @Nonnull String type, @Nonnull String id) {
        String uri = url+"/"+index+"/"+type+"/"+id+"/_source";
        return gson.fromJson(JsonPath.parse(ElasticJenkinsUtil.elasticGet(uri)).read("$.logId").toString(),String.class);
    }

    public List<String> getLogOutput(@Nonnull String suffix) {
        String indexLogs = ElasticJenkinsUtil.getProperty("jenkins_logs");
        String uri = url+"/"+indexLogs+"/"+suffix+"/_source";
        return gson.fromJson(JsonPath.parse(ElasticJenkinsUtil.elasticGet(uri)).read("$.logs").toString(),List.class);
    }

    public List<ElasticMaster> getMasterByNameAndCluster(@Nonnull String masterName,
                                                         @Nonnull String clusterName) {
        List<ElasticMaster> listMasters = new ArrayList<>();
        String uri = url+"/"+jenkinsManageIndexCluster+"/"+ jenkinsManageClusters +"/_search";
        String json = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"should\" : [\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masterName+"\" }},\n" +
                "     { \"match\" : { \"clusterName\": \""+clusterName+"\"}}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);

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
        String uri = url+"/"+jenkinsManageIndexCluster+"/"+ jenkinsManageClusters +"/_search";
        String json = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"should\" : [\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masterName+"\" }},\n" +
                "     { \"match\" : { \"clusterName\": \""+clusterName+"\"}}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);

        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        for(int i=0;i<total;i++) {

            String id  =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._id").toString(),String.class);
            listIds.add(id);
        }

        return listIds;
    }

    public String getNodesByCluster(@Nonnull String clusterName) {
        String uri = url+"/"+jenkinsManageIndexCluster+"/"+ jenkinsManageClusters +"/_search";
        String json = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"should\" : [\n" +
                "     { \"match\" : { \"clusterName\": \""+clusterName+"\"}}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);
        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        String result = null;
        for(int i=0;i<total;i++) {
            String masterName  =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source.jenkinsMasterName").toString(),String.class);
            if(result == null)
                result = masterName;
            else
                result = result.concat(" "+masterName);
        }
        return result;
    }

    public void addProjectMapping(@Nonnull String projectHash, @Nonnull String projectEncodedName) {
        String uri = url+"/"+jenkinsManageIndexMapping+"/"+jenkinsManageMapping;
        //First we check if the hash has been already saved
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"should\" : [\n" +
                "     { \"match\" : { \"projectHash\" : \""+projectHash+"\" }},\n" +
                "     { \"match\" : { \"projectEncodedName\": \""+projectEncodedName+"\"}}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri+"/_search",jsonReq);
        LOGGER.log(Level.INFO,"Uri {0}, return response: {1}",new Object[]{uri,jsonResponse});
        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        if(total == 0) {
            String jsonUpdate = "{\n" +
                    "\t\"projectHash\": \""+projectHash+"\",\n" +
                    "\t\"projectEncodedName\": \""+projectEncodedName+"\"\n" +
                    "}";
            LOGGER.log(Level.FINEST,"Mapping uri: {0}, json : {1}",new Object[]{uri,jsonUpdate});
            ElasticJenkinsUtil.elasticPost(uri.concat("/"),jsonUpdate);
        }
    }


    public List<GenericBuild> findByParameter(@Nonnull String index, @Nonnull String type,
                                               @Nonnull String masters,@Nonnull String parameter ){
        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+index+"/"+type+"/_search";

        String json = "{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"must\": [\n" +
                "        { \"match\": { \"jenkinsMasterName\": \""+masters+"\" }},\n" +
                "        { \"match\": { \"parameters.value\": \""+parameter+"\" }}\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";


        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);

        LOGGER.log(Level.FINEST,"jsonResponse:"+jsonResponse);
        Integer max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");

        for(int i=0;i<max;i++) {
            LOGGER.log(Level.FINEST,"Index: {0}, content: {1}", new Object[]{i,JsonPath.parse(jsonResponse).read("$.hits.hits["+i+"]._source").toString()});
            GenericBuild genericBuild =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source").toString(),GenericBuild.class);
            listBuilds.add(genericBuild);
        }

        return listBuilds;
    }

    public void createManageIndex() {
        String uri = url+"/"+jenkinsManageIndexCluster;
        String uri2 = url+"/"+jenkinsManageIndexMapping;
        String json = "{\n" +
                "    \"settings\" : {\n" +
                "        \"index\" : {\n" +
                "            \"number_of_shards\" : 3, \n" +
                "            \"number_of_replicas\" : 2 \n" +
                "        }\n" +
                "    }\n" +
                "}";
        ElasticJenkinsUtil.elasticPut(uri,json);
        ElasticJenkinsUtil.elasticPut(uri2,json);
    }
}
