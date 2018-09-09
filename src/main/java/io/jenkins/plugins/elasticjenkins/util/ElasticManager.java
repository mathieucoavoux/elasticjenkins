package io.jenkins.plugins.elasticjenkins.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.jayway.jsonpath.JsonPath;

import hudson.model.*;

import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.entity.Parameters;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
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

    private static String jenkinsManageClusters = "clusters";
    private static String jenkinsManageMapping = "mapping";

    private static String jenkinsBuildsType = "builds";

    private static String jenkinsQueueType = "queues";


    protected String url = ElasticJenkinsUtil.getUrl();
    protected String master = ElasticJenkinsUtil.getMasterName();
    protected String clusterName = ElasticJenkinsUtil.getClusterName();
    protected String charset = ElasticJenkinsUtil.getCharset();
    protected String jenkinsBuildsIndex = ElasticJenkinsUtil.getJenkinsBuildsIndex();
    protected String jenkinsQueueIndex = ElasticJenkinsUtil.getJenkinsQueuesIndex();
    protected String jenkinsManageIndexCluster = ElasticJenkinsUtil.getJenkinsManageIndexCluster();
    protected String jenkinsManageIndexMapping = ElasticJenkinsUtil.getJenkinsManageIndexMapping();

    protected Gson gson = new GsonBuilder().create();

    /**
     * Add a build to Elasticsearch. The status, log id and completion time is update at the end of the build
     * We extract required information from the build and store them in a simple object.
     * This avoid unnecessary information to be stored in Elasticsearch as we serialize this object in JSON.
     * @param projectId : The Elasticsearch id of the project which is stored in {@link ElasticJenkinsUtil#getJenkinsManageIndexMapping()}
     * @param build : The build to save
     * @return: The Elasticsearch id
     */
    public String addBuild(@Nonnull String projectId,
                           @Nonnull Run<?, ?> build) {
        Gson gson = new GsonBuilder().create();

        //We set build values in a generic build to avoid to serialize unnecessary values
        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setName(ElasticJenkinsUtil.convertUrlToFullName(build.getUrl()));
        genericBuild.setId(build.getId());
        genericBuild.setStartDate(System.currentTimeMillis());

        List<ParametersAction> parametersActions = build.getActions(ParametersAction.class);
        if(parametersActions.size() > 0) {
            List<Parameters> listParameters = new ArrayList<>();
            for (ParametersAction parametersAction : parametersActions) {
                List<ParameterValue> parameterValues = parametersAction.getAllParameters();
                for (ParameterValue parameterValue : parameterValues) {
                    Parameters parameters = new Parameters();
                    parameters.setName(parameterValue.getName());
                    parameters.setValue(parameterValue.getValue());
                    parameters.setDescription(parameterValue.getDescription());
                    listParameters.add(parameters);
                }

            }
            genericBuild.setParameters(listParameters);
        }
        genericBuild.setLaunchedByName(User.current().getDisplayName());
        genericBuild.setLaunchedById(User.current().getId());
        genericBuild.setJenkinsMasterName(master);
        genericBuild.setJenkinsMasterId(ElasticJenkinsUtil.getCurentMasterId());
        genericBuild.setProjectId(projectId);
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
        String eId = build.getId()+"_"+projectId+"_"+ElasticJenkinsUtil.getCurentMasterId();
        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/"+eId;
        String elasticSearchId = null;
        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),ElasticsearchResult.class);
        if (esr.getResult().equals("created") || esr.getResult().equals("updated")) elasticSearchId = esr.get_id();

        //If we can save the build we can remove the dequeued item
        String queueUri = url+"/"+jenkinsQueueIndex+"/"+jenkinsQueueType+"/"+build.getQueueId()+"_"+projectId+"_"+ElasticJenkinsUtil.getCurentMasterId();
        if(elasticSearchId != null)
            if(isItemExists(queueUri) && ! ElasticJenkinsUtil.elasticDelete(queueUri))
                LOGGER.log(Level.SEVERE,"Cannot delete queued item:"+queueUri);
        return elasticSearchId;
    }

    /**
     * Update the build with the status, log output and completion date.
     * @param id: Elasticsearch id of the build
     * @param status: Status of the job
     * @param file: Log file where is store the output
     * @return: the Elasticsearch id
     */
    public String updateBuild(@Nonnull String id,
                              @Nonnull String status,
                              @Nullable File file) {
        String elasticSearchId = "";
        String indexLogs = ElasticJenkinsUtil.getProperty("jenkins_logs");

        long size = 0;
        if(Files.exists(file.toPath())) {

            try {
                size = Files.size(file.toPath());
            } catch (IOException e) {
                size = 0;
            }
        }
        String suffix = "";
        if (size > 0 ) {
            try {
                String typeLog = new SimpleDateFormat("yyyy_MM").format(new Date());
                String logId = null;
                String uriLogs = url + "/" + typeLog + "/" + indexLogs + "/";
                String json = "{ \"logs\" : \n" +
                        gson.toJson(Files.readAllLines(file.toPath()), List.class) +
                        "}";
                LOGGER.log(Level.FINEST, "URI: {0}, json: {1}", new Object[]{uriLogs, json});
                ElasticsearchResult esr2 = gson.fromJson(ElasticJenkinsUtil.elasticPost(uriLogs, json), ElasticsearchResult.class);
                if (esr2.getResult().equals("created")) logId = esr2.get_id();
                suffix = typeLog+"/"+indexLogs+"/"+logId;
            }catch (IOException e) {
                LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
            }
        }
        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/"+id+"/_update";
        String json = null;
        try {
            json = "{\n" +
                    "  \"doc\": {\n" +
                    "    \"status\" : \""+status+"\",\n" +
                    "    \"logId\" : " + "\""+URLEncoder.encode(suffix,charset)+"\",\n" +
                    "   \"endDate\" : "+System.currentTimeMillis() +
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
     * @param projectName : Job name hash
     * @param viewType : if we select the cluster view we receive all builds of the cluster
     * @param paginationSize : number of results to return
     * @param paginationStart : starts from where
     * @return: list of builds
     */
    public List<GenericBuild> getPaginateBuildHistory(@Nonnull String projectName,
                                                      @Nonnull String viewType,
                                                      @Nonnull Integer paginationSize, @Nullable String paginationStart) {

        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/_search";

        String masters = master;;
        if(viewType.equals("cluster"))
            masters = getNodesByCluster(clusterName);

        if(paginationStart == null)
            paginationStart = "0";

        String json = "{\n" +
                "   \"query\": {\n" +
                "       \"bool\": {\n" +
                "           \"must\": [\n" +
                "               { \"match\": { \"jenkinsMasterName\": \""+masters+"\" }},\n" +
                "               { \"match\": { \"projectId\": \""+getProjectId(projectName)+"\" }}\n" +
                "           ]\n" +
                "       }\n" +
                "   },\n" +
                " \"size\" : "+paginationSize+",\n" +
                " \"from\" : "+paginationStart+",\n" +
                " \"sort\" :  { \"_id\" : { \"order\" : \"desc\" } }\n" +
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

    /**
     * Get the newest build saved since the last fetch
     * @param projectHash : Hash of the project
     * @param lastFetch : Timestamp of the last fetch
     * @param viewType : View can be either cluster or server. If the cluster is selected we display all builds of the servers in the cluster
     * @return: list of builds
     */
    public List<GenericBuild> getNewResults(@Nonnull String projectHash,
                                            @Nonnull String lastFetch, String viewType) {
        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/_search";

        String masters = master;;
        if(viewType.equals("cluster"))
            masters = getNodesByCluster(clusterName);

        String json = "{\n" +
                "   \"query\": {\n" +
                "       \"bool\": {\n" +
                "           \"must\": [\n" +
                "               { \"match\": { \"jenkinsMasterName\": \""+masters+"\" }},\n" +
                "               { \"range\": { \"startDate\": { \"gte\": "+lastFetch+" }}},\n" +
                "               { \"match\": { \"projectId\": \""+getProjectId(projectHash)+"\" }}\n" +
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

    protected boolean isItemExists(String url) {
        Integer code = ElasticJenkinsUtil.elasticHead(url);

        if(code == 200)
            return true;
        return false;
    }

    /**
     * Search a build by its Elasticsearch id
     * @param id : Elasticsearch id
     * @return: the build
     */
    protected GenericBuild searchById(@Nonnull String id) {

        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/"+id+"/_source";
        return gson.fromJson(ElasticJenkinsUtil.elasticGet(uri),GenericBuild.class);
    }

    /**
     * Get Elasticsearch id of the log output
     * @param id :
     * @return
     */
    public String getLogOutputId(@Nonnull String id) {
        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/"+id+"/_source";
        return gson.fromJson(JsonPath.parse(ElasticJenkinsUtil.elasticGet(uri)).read("$.logId").toString(),String.class);
    }

    /**
     * Get the output from Elasticsearch and write it to a file. Then return the file
     * @param suffix: The Elasticsearch index/type/id of where the log is available
     * @param id: the elasticsearch id
     * @return: file where the log has been written
     */
    public File getLogOutput(@Nonnull String suffix,@Nonnull String id) {
        String indexLogs = ElasticJenkinsUtil.getProperty("jenkins_logs");
        String uri = url+"/"+suffix+"/_source";

        File file = new File(Jenkins.getInstance().getRootDir(),"/"+id);
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(file.getPath(),true));
            for(String row: (List<String>) gson.fromJson(JsonPath.parse(ElasticJenkinsUtil.elasticGet(uri)).read("$.logs").toString(),List.class)) {
                writer.append(row+"\n");
            }

            writer.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An error occured while trying to write file:"+file.getPath(),e);
        }
        return file;

    }

    /**
     * Get the Elasticsearch id of a Jenkins master based on its name and its cluster name
     * @param masterName: Name of the jenkins master
     * @param clusterName: Name of the cluster
     * @return: the list of match
     */
    @VisibleForTesting
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

    /**
     * Get jenkins masters for a specific cluster
     * @param clusterName: cluster name
     * @return: The list of masters separated by space
     */
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

    /**
     * Create a mapping with the Elasticsearch id of the project and its hashed name and its encoded name
     * @param projectHash: the URL of the project hashed
     * @param projectEncodedName: the URL of the project encoded
     * @return: the Elasticsearch id
     */
    public String addProjectMapping(@Nonnull String projectHash, @Nonnull String projectEncodedName) {
        String uri = url+"/"+jenkinsManageIndexMapping+"/"+jenkinsManageMapping;
        //First we check if the hash has been already saved
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"projectHash\" : \""+projectHash+"\" }},\n" +
                "     { \"match\" : { \"projectEncodedName\": \""+projectEncodedName+"\"}}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri+"/_search",jsonReq);
        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        String eid;
        if(total == 0) {
            String jsonUpdate = "{\n" +
                    "\t\"projectHash\": \""+projectHash+"\",\n" +
                    "\t\"projectEncodedName\": \""+projectEncodedName+"\"\n" +
                    "}";
            LOGGER.log(Level.FINEST,"Mapping uri: {0}, json : {1}",new Object[]{uri,jsonUpdate});
            jsonResponse = ElasticJenkinsUtil.elasticPost(uri.concat("/"),jsonUpdate);
            eid = JsonPath.parse(jsonResponse).read("$._id");
        }else{
            eid = JsonPath.parse(jsonResponse).read("$.hits.hits[0]._id");
        }
        return eid;
    }

    /**
     * Find builds by its parameters. Depending of the type of the view we search in all nodes of the cluster or a specific server
     * @param hash: the project hashed name
     * @param viewType: the view Type (cluster/server)
     * @param parameter: the parameter value
     * @return: The list of builds
     */
    public List<GenericBuild> findByParameter(@Nonnull String hash,
                                              @Nonnull String viewType, @Nonnull String parameter){
        List<GenericBuild> listBuilds = new ArrayList<>();
        String uri = url+"/"+jenkinsBuildsIndex+"/"+jenkinsBuildsType+"/_search";

        String masters = master;;
        if(viewType.equals("cluster"))
            masters = getNodesByCluster(clusterName);

        String json = "{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"must\": [\n" +
                "        { \"match\": { \"jenkinsMasterName\": \""+masters+"\" }},\n" +
                "        { \"match\": { \"parameters.value\": \""+parameter+"\" }},\n" +
                "        { \"match\": { \"projectId\": \""+getProjectId(hash)+"\" }}\n" +
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


    /**
     * Add an item from the queue to Elasticsearch. This prevent to lose the queued items if a crash occurs
     * @param waitingItem: the item
     * @param projectId: the project id
     * @return: the elasticsearch id
     */
    public String addQueueItem(Queue.WaitingItem waitingItem,String projectId) {
        GenericBuild genericBuild = new GenericBuild();
        genericBuild.setName(ElasticJenkinsUtil.convertUrlToFullName(waitingItem.task.getUrl()));
        genericBuild.setId(Long.toString(waitingItem.getId()));

        List<ParametersAction> parametersActions = waitingItem.getActions(ParametersAction.class);
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
        genericBuild.setQueuedSince(waitingItem.getInQueueSince());
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
        genericBuild.setStatus("ENQUEUED");
        //Convert the generic build to a Json string
        String json = gson.toJson(genericBuild);

        //Post the json to Elasticsearch
        String eId = waitingItem.getId()+"_"+projectId+"_"+ElasticJenkinsUtil.getCurentMasterId();;
        String uri = url+"/"+jenkinsQueueIndex+"/"+jenkinsQueueType+"/"+eId;
        String elasticSearchId = null;
        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),ElasticsearchResult.class);
        if (esr.getResult().equals("created") || esr.getResult().equals("updated")) elasticSearchId = esr.get_id();

        return elasticSearchId;
    }

    /**
     * Update the status of the item
     * @param leftItem: the item
     * @param eId: the elasticsearch id
     */
    public void updateQueueItem(Queue.LeftItem leftItem,String eId) {
        String uri = url+"/"+jenkinsQueueIndex+"/"+jenkinsQueueType+"/"+eId+"/_update";

        String json = null;
        json = "{\n" +
                "  \"doc\": {\n" +
                "    \"status\" : \"DEQUEUED\"" +
                "  }\n" +
                "}";


        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),ElasticsearchResult.class);


    }

    /**
     * Get the Elasticsearch id of the project
     * @param projectHash: hash of the project
     * @return: Elasticsearch id
     */
    public String getProjectId(String projectHash) {
        String uri = url+"/"+jenkinsManageIndexMapping+"/"+jenkinsManageMapping+"/_search";
        //First we check if the hash has been already saved
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"projectHash\" : \""+projectHash+"\" }}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,jsonReq);
        Integer max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");
        if(max != 1) {
            LOGGER.log(Level.SEVERE, "The number of project ({0}) found: {1}",new Object[]{projectHash,max});
            return null;
        }

        return  gson.fromJson(JsonPath.parse(jsonResponse).read(
                "$.hits.hits[0]._id").toString(),String.class);
    }

    /**
     * Get the last 3 current builds, to display to the panel
     * @return: the list of the current builds
     */
    public List<GenericBuild> getLastCurrentBuilds() {
        List<GenericBuild> listBuilds = new ArrayList<>();
        String masters = getNodesByCluster(clusterName);
        String uri = url+"/"+jenkinsBuildsIndex+"/builds/_search";
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"status\" : \"EXECUTING\" }},\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masters+"\" }}\n" +
                "     ]\n" +
                "}\n" +
                "},\n" +
                " \"size\" : 3,\n" +
                " \"from\" : 0,\n" +
                " \"sort\" :  { \"_id\" : { \"order\" : \"desc\" } }\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,jsonReq);
        Integer max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");

        for(int i=0;i<max;i++) {
            GenericBuild genericBuild =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source").toString(),GenericBuild.class);
            listBuilds.add(genericBuild);
        }

        return listBuilds;
    }

    /**
     * Get the last 3 current items to display it in the panel
     * @return: list of the item
     */
    public List<GenericBuild> getLastCurrentItems() {
        List<GenericBuild> listBuilds = new ArrayList<>();
        String masters = getNodesByCluster(clusterName);
        String uri = url+"/"+jenkinsQueueIndex+"/queues/_search";
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"status\" : \"ENQUEUED\" }},\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masters+"\" }}\n" +
                "     ]\n" +
                "}\n" +
                "},\n" +
                " \"size\" : 3,\n" +
                " \"from\" : 0,\n" +
                " \"sort\" :  { \"_id\" : { \"order\" : \"desc\" } }\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,jsonReq);
        Integer max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");

        for(int i=0;i<max;i++) {
            GenericBuild genericBuild =  gson.fromJson(JsonPath.parse(jsonResponse).read(
                    "$.hits.hits["+i+"]._source").toString(),GenericBuild.class);
            listBuilds.add(genericBuild);
        }

        return listBuilds;
    }

    /**
     * Count the number of the current builds and display it to the panel
     * @return: the number of the current build
     */
    public Integer getCountCurrentBuilds() {
        String masters = getNodesByCluster(clusterName);
        String uri = url+"/"+jenkinsBuildsIndex+"/builds/_count";
        //First we check if the hash has been already saved
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"status\" : \"EXECUTING\" }},\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masters+"\" }}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,jsonReq);
        return JsonPath.parse(jsonResponse).read("$.count");

    }

    /**
     * Get the count of the current item and display it to the panel
     * @return: The number of current item
     */
    public Integer getCountCurrentItem() {
        String masters = getNodesByCluster(clusterName);
        String uri = url+"/"+jenkinsQueueIndex+"/queues/_count";
        //First we check if the hash has been already saved
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"status\" : \"ENQUEUED\" }},\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masters+"\" }}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,jsonReq);
        return JsonPath.parse(jsonResponse).read("$.count");

    }


}
