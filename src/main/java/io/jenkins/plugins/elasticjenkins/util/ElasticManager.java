package io.jenkins.plugins.elasticjenkins.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.jayway.jsonpath.JsonPath;
import hudson.model.*;
import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.entity.Parameters;
import org.apache.http.entity.StringEntity;


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
    private static String jenkinsManageClusters = "clusters";
    private static String jenkinsManageMapping = "mapping";


    protected String url = ElasticJenkinsUtil.getProperty("persistenceStore");
    protected String master = ElasticJenkinsUtil.getProperty("masterName");
    protected String charset = ElasticJenkinsUtil.getProperty("charset");

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
            if (esr.getResult().equals("created") || esr.getResult().equals("updated")) elasticSearchId = esr.get_id();
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"Generic build is not serializable into JSON. Build: "
                    +genericBuild.getId()+" project:"+genericBuild.getName()+" will not be saved");
        }
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
                update.add(URLEncoder.encode(oneLine,charset));
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
            String uriLogs = url + "/" + indexLogs + "/" + typeLog;
            StringEntity entityLog = null;
            try {
                entityLog = new StringEntity("{ \"logs\" : \n" +
                                 gson.toJson(update,List.class) +
                        "}");
                ElasticsearchResult esr2 = gson.fromJson(ElasticJenkinsUtil.elasticPost(uriLogs,entityLog),ElasticsearchResult.class);
                if (esr2.getResult().equals("created")) logId = esr2.get_id();
                suffix = typeLog.concat("/"+logId);
            } catch (UnsupportedEncodingException e) {
                LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
            }
        }
        String uri = url+"/"+index+"/"+type+"/"+id+"/_update";
        StringEntity entity = null;
        try {
            entity = new StringEntity("{\n" +
                    "  \"doc\": {\n" +
                    "    \"status\" : \""+status+"\",\n" +
                    "    \"logId\" : " + "\""+suffix+"\"" +
                    "  }\n" +
                    "}");


            ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,entity),ElasticsearchResult.class);

            if (esr.getResult().equals("updated")) elasticSearchId = esr.get_id();
        } catch (UnsupportedEncodingException e) {
           // LOGGER.log(Level.SEVERE,"Generic build is not serializable into JSON. Build: "
            //        +genericBuild.getId()+" project:"+genericBuild.getName()+" will not be saved");
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

    protected List<String> getLogOutput(@Nonnull String suffix) {
        String indexLogs = ElasticJenkinsUtil.getProperty("jenkins_logs");
        String uri = url+"/"+indexLogs+"/"+suffix+"/_source";
        return gson.fromJson(JsonPath.parse(ElasticJenkinsUtil.elasticGet(uri)).read("$.logs").toString(),List.class);
    }

    public List<ElasticMaster> getMasterByNameAndCluster(@Nonnull String masterName,
                                                         @Nonnull String clusterName) {
        List<ElasticMaster> listMasters = new ArrayList<>();
        String uri = url+"/"+jenkinsManageIndex+"/"+ jenkinsManageClusters +"/_search";
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
        String uri = url+"/"+jenkinsManageIndex+"/"+ jenkinsManageClusters +"/_search";
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

    public void addProjectMapping(@Nonnull String projectHash, @Nonnull String projectEncodedName) {
        String uri = url+"/"+jenkinsManageMapping;
        //First we check if the hash has been already saved
        StringEntity entity = null;
        try {
            entity = new StringEntity("{ \"query\" : { \n" +
                    " \"bool\" : {\n" +
                    " \"should\" : [\n" +
                    "     { \"match\" : { \"projectHash\" : \""+projectHash+"\" }},\n" +
                    "     { \"match\" : { \"projectEncodedName\": \""+projectEncodedName+"\"}}\n" +
                    "     ]\n" +
                    "}\n" +
                    "}\n" +
                    "}");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
            return ;
        }
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri+"/_search",entity);
        if((Integer) JsonPath.parse(jsonResponse).read("$.hits.total") != 0) {
            StringEntity entityAdd = null;
            try {
                entityAdd = new StringEntity("{\n" +
                        "\t\"projectHash\": \""+projectHash+"\",\n" +
                        "\t\"projectEncodedName\": \""+projectEncodedName+"\"\n" +
                        "}");
            } catch (UnsupportedEncodingException e) {
                LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
                return ;
            }
            ElasticJenkinsUtil.elasticPost(uri+"/",entity);
        }
    }
}
