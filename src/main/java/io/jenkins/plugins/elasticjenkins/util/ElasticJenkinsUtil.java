package io.jenkins.plugins.elasticjenkins.util;



import com.google.gson.*;
import com.jayway.jsonpath.JsonPath;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import jenkins.model.Jenkins;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsUtil {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsUtil.class.getName());

    //TODO: change the value to comply to Jenkins naming convention
    private static String elasticSearchConfFileName = "elasticjenkins.properties";

    public static String logStorageConfFileName = "io.jenkins.plugins.elasticjenkins.logStorage.properties";

    private static File propertiesFile = new File(Jenkins.getInstanceOrNull().getRootDir()+"/"+elasticSearchConfFileName);

    private static String jenkinsManageIndexCluster = ElasticJenkinsUtil.getESConfProperty("jenkinsManageClusterIndex");
    private static String jenkinsManageIndexMapping = ElasticJenkinsUtil.getESConfProperty("jenkinsManageMappingIndex");
    private static String jenkinsBuildsIndex = ElasticJenkinsUtil.getESConfProperty("jenkinsBuildsIndex");
    private static String jenkinsQueuesIndex = ElasticJenkinsUtil.getESConfProperty("jenkinsQueuesIndex");
    private static String jenkinsLogsIndex = ElasticJenkinsUtil.getESConfProperty("jenkins_logs");

    protected static String charset = ElasticJenkinsUtil.getESConfProperty("elasticCharset");
    private static String masterName = ElasticJenkinsUtil.getESConfProperty("masterName");
    protected static String health = ElasticJenkinsUtil.getESConfProperty("jenkinsMappingHealth");

    private static String logStorageType =  getStorageProperty("logStorageType");
    private static String configurationStorageType = getStorageProperty("configurationStorageType");

    public static String getConfigurationStorageType() {
        return configurationStorageType;
    }

    public static void setConfigurationStorageType(String configurationStorageType) {
        ElasticJenkinsUtil.configurationStorageType = configurationStorageType;
    }


    public static String getLogStorageType() {
        return logStorageType;
    }

    public static void setLogStorageType(String logStorageType) {
        ElasticJenkinsUtil.logStorageType = logStorageType;
    }



    private static boolean jenkinsHealthCheckEnable = true;

    static Long getStartupTime() {
        return startupTime;
    }

    public static boolean getJenkinsHealthCheckEnable() { return jenkinsHealthCheckEnable ;}

    public static void setJenkinsHealthCheckEnable(boolean newJenkinsHealthCheckEnable) {
        jenkinsHealthCheckEnable = newJenkinsHealthCheckEnable;
    }

    public static void setStartupTime(Long startupTime) {
        ElasticJenkinsUtil.startupTime = startupTime;
    }

    private static Long startupTime;

    public static String getStartupTimeId() {
        return startupTimeId;
    }

    public static void setStartupTimeId(String startupTimeId) {
        ElasticJenkinsUtil.startupTimeId = startupTimeId;
    }

    private static String startupTimeId;

    public static String getJenkinsManageIndexCluster() {
        return jenkinsManageIndexCluster;
    }

    public static String getJenkinsManageIndexMapping() {
        return jenkinsManageIndexMapping;
    }

    public static String getJenkinsBuildsIndex() {
        return jenkinsBuildsIndex;
    }

    public static String getJenkinsQueuesIndex() {
        return jenkinsQueuesIndex;
    }

    public static String getJenkinsLogsIndex() { return jenkinsLogsIndex; }

    public static String getClusterName() {
        LOGGER.log(Level.FINEST,"ClusterName:"+clusterName);
        return clusterName;
    }

    public static String getUrl() {
        return url;
    }

    public static String getJenkinsHealth() { return health;}

    protected static String clusterName = ElasticJenkinsUtil.getESConfProperty("clusterName");
    protected static String url = ElasticJenkinsUtil.getESConfProperty("persistenceStore");

    public static boolean isInitialized = ElasticJenkinsUtil.isInitialized();
    public static boolean isEmpty = ElasticJenkinsUtil.isEmpty();

    /**
     * Get the job url to take the project name as the project display name can contain
     * quotes because of the folder plugin.
     * Examples:
     * 		Freestyle job: job/Job1/
     * 		Folder job: job/Demo/job/Test1/
     * @param url: Jenkins job url
     * @return job name
     */
    static String convertUrlToFullName(String url) {
        String[] extractString = url.split("/");
        int max = extractString.length - 1;	//We always ignore the latest entry as this is for the ID
        String title = extractString[1];	//We always ignore the first entry as this is job
        for(int ind=3;ind<=max;ind+=2) {
            title = title.concat("/"+extractString[ind]);
        }
        return title;
    }

    /**
     * Compute the hash of the job as the job name can contain dirty parameters
     * since we have no control on the job name
     * @param url: Jenkins job url
     * @return hash of the job
     */
    public static String getHash(@Nonnull  String url) {
        LOGGER.log(Level.FINEST,"URL: "+url);
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(url.getBytes());
            byte[] digest = md.digest();
            hash = DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE,"I do NOT know the algorithm for the hashing");
        }
        LOGGER.log(Level.FINEST,"Hash:"+hash);
        return hash;
    }

    public static String getCharset() {
        return charset;
    }

    public static void setCharset(String newCharset) {
        charset = newCharset;
    }

    public static String getMasterName() {
        LOGGER.log(Level.FINEST,"masterName:"+masterName);
        return masterName;}

    private static void setMasterName(String newMasterName) { masterName = newMasterName; }

    public static void setClusterName(String newClusterName){ clusterName = newClusterName;}

    public static void setUrl(String newUrl){ url = newUrl;}

    private static void setJenkinsManageIndexCluster(String newJenkinsManageIndexCluster) {
        jenkinsManageIndexCluster = newJenkinsManageIndexCluster;
    }

    private static void setJenkinsManageIndexMapping(String newJenkinsManageIndexMapping){
        jenkinsManageIndexMapping = newJenkinsManageIndexMapping;
    }

    private static void setJenkinsBuildsIndex(String newJenkinsBuildsIndex){
        jenkinsBuildsIndex = newJenkinsBuildsIndex;
    }

    private static void setJenkinsQueuesIndex(String newJenkinsQueuesIndex) {
        jenkinsQueuesIndex = newJenkinsQueuesIndex;
    }

    private static void setJenkinsLogsIndex(String newJenkinsLogIndex) {
        jenkinsLogsIndex = newJenkinsLogIndex;
    }

    public static void setIsInitialized(boolean newIsInitialized){ isInitialized = newIsInitialized;}

    public static void setIsEmpty(boolean newIsEmpty){isEmpty = newIsEmpty;}

    private static void setJenkinsHealth(String newJenkinsIndexHealth) {
        health = newJenkinsIndexHealth;
    }

    /**
     * Get a property from the elasticjenkins file properties
     * @param property: property to retrieve
     * @return value of the property
     */
    public static String getESConfProperty(String property) {
        return getProperty(propertiesFile,property);
    }

    public static String getStorageProperty(String property) {
        return getProperty(new File(Jenkins.getInstanceOrNull().getRootDir(),"/"+logStorageConfFileName),property);
    }

    private static String getProperty(File fileProperties, String property) {
        Properties prop = new Properties();
        InputStream input = null;
        if(!fileProperties.exists()) {
            LOGGER.log(Level.SEVERE, "Properties files doesn't exist:" + fileProperties.getPath());
            return null;
        }
        try {
            input = new FileInputStream(fileProperties);
            prop.load(input);
            return prop.getProperty(property);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can not get property scriptPath", e);
        }finally {
            if(input != null) {
                try {
                    input.close();
                }catch(Exception e) {
                    LOGGER.log(Level.SEVERE, "Can not close file", e);
                }
            }
        }
        return null;
    }

    /**
     * Write properties used by ElasticJenkins plugin to the properties file
     * @param masterName : name of the Jenkins master
     * @param clusterName: name of the Jenkins cluster
     * @param persistenceStore : URL of the persistence store
     * @param charset : charset used in the log output encoding
     * @param logIndex: The index use to store the log output
     * @param buildsIndex: The index use to store the builds
     * @param queuesIndex: The index use to store the queued items
     * @param clusterIndex: The index of the cluster configuration
     * @param mappingIndex: The index use for the project mapping.
     * @param mappingHealth: The index use for the health
     * @return boolean true if saved successfully
     */
    public static synchronized boolean writeProperties(@Nonnull String masterName,
                                                       @Nonnull String clusterName,
                                                       @Nonnull String persistenceStore, @Nonnull String charset,
                                                       @Nonnull String logIndex, @Nonnull String buildsIndex,
                                                       @Nonnull String queuesIndex,
                                                       @Nonnull String clusterIndex, @Nonnull String mappingIndex, @Nonnull String mappingHealth) {
        OutputStream out = null;

        setCharset(charset);
        setMasterName(masterName);
        setClusterName(clusterName);
        setUrl(persistenceStore);
        setJenkinsLogsIndex(logIndex);
        setJenkinsBuildsIndex(buildsIndex);
        setJenkinsQueuesIndex(queuesIndex);
        setJenkinsManageIndexCluster(clusterIndex);
        setJenkinsManageIndexMapping(mappingIndex);
        setIsInitialized(true);
        setJenkinsHealth(mappingHealth);
        createManageIndex();
        createHealthIndex();
        Properties props = new Properties();
        props.setProperty("masterName", masterName);
        props.setProperty("clusterName",clusterName);
        props.setProperty("persistenceStore", persistenceStore);
        props.setProperty("elasticCharset", charset);
        props.setProperty("jenkins_logs",logIndex);
        props.setProperty("jenkinsBuildsIndex",buildsIndex);
        props.setProperty("jenkinsQueuesIndex",queuesIndex);
        props.setProperty("jenkinsManageClusterIndex",clusterIndex);
        props.setProperty("jenkinsManageMappingIndex",mappingIndex);
        props.setProperty("jenkinsMappingHealth",mappingHealth);
        try {
            out = new FileOutputStream(propertiesFile);
            BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile.getPath(), false));
            props.store(writer, "Persist properties");
        }catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Properties file not found. This may occur in testing mode. We try to create parent directory");
            //Since the RootDir can be different during the unit test we take the base directory of the properties
            new File(propertiesFile.getParent()).mkdirs();
            try {
                out = new FileOutputStream(propertiesFile);
                BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile.getPath(), false));
                props.store(writer, "Persist properties");
            }catch(Exception e1) {
                LOGGER.log(Level.SEVERE, "Can not save properties", e1);
                return false;
            }
        }catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can not save properties", e);
            return false;
        }finally {
            if(out != null) {
                try {
                    out.close();
                }catch(Exception e) {
                    LOGGER.log(Level.SEVERE, "Can not close file", e);
                }
            }
        }
        return true;
    }

    public static String getElasticSearchStatus(@Nonnull String url) {

        String uri = url+"/_cluster/health";

        return JsonPath.parse(elasticGet(uri)).read("$.status").toString();
    }

    public static String getIdByMaster(@Nonnull String master) {
        String uri = url+"/"+ jenkinsManageIndexCluster +"/clusters/_search";
        String json = "{ \"query\" : {\n" +
                "  \"match\" : {\n" +
                "    \"jenkinsMasterName\" : \""+master+"\" \n" +
                "  }\n" +
                "}}";
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,json);
        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        if (total != 1)
            return null;
        return JsonPath.parse(jsonResponse).read("$.hits.hits[0]._id").toString();
    }

    public static void createManageIndex() {
        String uri = url+"/"+jenkinsManageIndexCluster;
        String uri2 = url+"/"+jenkinsManageIndexMapping;
        String uriBuilds = url+"/"+jenkinsBuildsIndex;
        String uriQueues = url+"/"+jenkinsQueuesIndex;
        String json = "{\n" +
                "    \"settings\" : {\n" +
                "        \"index\" : {\n" +
                "            \"number_of_shards\" : 3, \n" +
                "            \"number_of_replicas\" : 2 \n" +
                "        }\n" +
                "    }\n" +
                "}";
        String mappingBuilds = "{\n" +
                "\t\"settings\" : {\n" +
                "\t\t\"index\" : {\n" +
                "\t\t\t\"number_of_shards\" : 3, \n" +
                "\t\t\t\"number_of_replicas\" : 0 \n" +
                "\t\t}\n" +
                "\t},\n" +
                "  \"mappings\": {\n" +
                "    \"builds\": {\n" +
                "      \"properties\": {\n" +
                "            \"startDate\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "        },\n" +
                "        \"endDate\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "        },\n" +
                "            \"startupTime\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "            },\n" +
                "        \"id\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"name\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        },\n" +
                "       \"url\" : {\n" +
                "           \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"queuedSince\" : {\n" +
                "          \"type\" : \"long\"\n" +
                "        },\n" +
                "        \"logId\" : {\n" +
                "          \"type\" : \"text\"\n" +
                "        },\n" +
                "        \"status\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"jenkinsMasterName\" : {\n" +
                "          \"type\" : \"text\",\n" +
                "           \"fields\" : {\n" +
                "               \"keyword\" : {\n" +
                "                   \"type\" : \"keyword\",\n"+
                "                   \"ignore_above\" : 256\n"+
                "               }\n" +
                "           }\n" +
                "        },\n" +
                "        \"projectId\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"launchedByName\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        },\n" +
                "        \"parameters\" : {\n" +
                "          \"properties\" : {\n" +
                "            \"name\" : {\n" +
                "              \"type\" : \"keyword\" \n" +
                "            },\n" +
                "            \"value\" : {\n" +
                "              \"type\" : \"keyword\" \n" +
                "            },\n" +
                "            \"description\" : {\n" +
                "              \"type\" : \"keyword\" \n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        ElasticJenkinsUtil.elasticPut(uri2,json);
        ElasticJenkinsUtil.elasticPut(uriBuilds,mappingBuilds);
        ElasticJenkinsUtil.elasticPut(uriBuilds,uri);
        ElasticJenkinsUtil.elasticPut(uriQueues,json);
    }

    public static void createHealthIndex() {
        String uri = url+"/"+health;
        String json = "{\n" +
        "\t\"settings\" : {\n" +
                "\t\t\"index\" : {\n" +
                "\t\t\t\"number_of_shards\" : 3, \n" +
                "\t\t\t\"number_of_replicas\" : 0 \n" +
                "\t\t}\n" +
                "\t},\n" +
                "  \"mappings\": {\n" +
                "    \"health\": {\n" +
                "      \"properties\": {\n" +
                "            \"startupTime\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "            },\n" +
                "             \"jenkinsMasterName\" : {\n" +
                "                   \"type\" : \"keyword\"\n" +
                "            },\n" +
                "             \"lastFlag\" : {\n" +
                "               \"type\": \"date\",\n" +
                "              \"format\" : \"epoch_millis\"\n" +
                "            },\n" +
                "             \"recoverBy\" : {\n" +
                "                   \"type\" : \"keyword\"\n" +
                "            },\n" +
                "             \"recoverStatus\" : {\n" +
                "                   \"type\" : \"keyword\"\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }";
        ElasticJenkinsUtil.elasticPut(uri,json);
    }

    public static boolean isInitialized() {
        if(url == null || jenkinsBuildsIndex == null || jenkinsQueuesIndex == null
              || jenkinsManageIndexCluster == null || jenkinsManageIndexMapping == null
                || clusterName == null || masterName == null || charset == null)
            return false;

        List<String> list = new ArrayList<>();
        list.add(jenkinsBuildsIndex);
        list.add(jenkinsQueuesIndex);
        list.add(jenkinsManageIndexCluster);
        list.add(jenkinsManageIndexMapping);
        for(String index : list) {
            if(elasticHead(url+"/"+index) != 200)
                return false;
        }
        return true;
    }

    public static boolean isEmpty() {
        if(url == null)
            return true;
        List<String> list = new ArrayList<>();
        list.add(jenkinsBuildsIndex+"/_mapping/builds");
        list.add(jenkinsQueuesIndex+"/_mapping/queues");
        list.add(jenkinsManageIndexCluster+"/_mapping/clusters");
        list.add(jenkinsManageIndexMapping+"/_mapping/mapping");
        for(String type : list) {
            if(elasticHead(url+"/"+type) != 200)
                return true;
        }
        return false;
    }

    /**
     * Send POST resquest to Elasticsearch
     * @param uri: URI
     * @param json: JSON sent in the POST
     * @return JSON result
     */
    public static String elasticPost(@Nonnull String uri,@Nonnull String json) {
        String result = null;
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Accept","application/json");
        httpPost.setHeader("Content-type","application/json");
        LOGGER.log(Level.FINEST,"Charset : "+charset);
        StringEntity entity = new StringEntity(json,charset);
        httpPost.setEntity(entity);
        LOGGER.log(Level.FINEST,"Uri:"+uri);
        LOGGER.log(Level.FINEST,"JSON:"+json);
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            result = EntityUtils.toString(response.getEntity());
            LOGGER.log(Level.FINEST,"Response:"+response.getStatusLine().getStatusCode()+",Message:"+response.getStatusLine().getReasonPhrase());
            if(response.getStatusLine().getStatusCode() > 399)
                LOGGER.log(Level.WARNING,"Code:{0}, Message: {1}", new Object[]{response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase()});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,"Cannot close the connection");
            }
        }

        return result;
    }

    /**
     * Send a GET request to Elasticsearch and return the value
     * @param uri: URI
     * @return JSON result
     */
    static String elasticGet(@Nonnull String uri) {
        String result = null;
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            result = EntityUtils.toString(response.getEntity());

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,"Cannot close the connection");
            }
        }

        return result;
    }

    private static String elasticPut(@Nonnull String uri, @Nonnull String json) {
        String result = null;
        HttpPut httpPut = new HttpPut(uri);
        httpPut.setHeader("Accept","application/json");
        httpPut.setHeader("Content-type","application/json");
        StringEntity entity = null;
        try {
            entity = new StringEntity(json);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
            return null;
        }
        httpPut.setEntity(entity);

        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpPut);
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,"Cannot close the connection");
            }
        }

        return result;
    }

    static boolean elasticDelete(String uri) {
        boolean result = false;
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpDelete);
            result = response.getStatusLine().getStatusCode() == 200;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"An unexpected response was received:",e);
        }finally {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,"Cannot close the connection");
            }
        }

        return result;
    }

    public static Integer elasticHead(String uri) {
        Integer code = 666;
        HttpHead httpHead = new HttpHead(uri);
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpHead);
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return code;
    }

    public static String getHostname() {
        String master;
        try {
            master = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            //if the local host name could not be resolved into an address.
            master = "jenkins";
        }

        return master;

    }

    static String getCurrentMasterId() {
        String jenkinsManageClusters = "clusters";
        String uri = url+"/"+jenkinsManageIndexCluster+"/"+ jenkinsManageClusters +"/_search";
        //First we check if the hash has been already saved
        String jsonReq = "{ \"query\" : { \n" +
                " \"bool\" : {\n" +
                " \"must\" : [\n" +
                "     { \"match\" : { \"jenkinsMasterName\" : \""+masterName+"\" }},\n" +
                "     { \"match\" : { \"clusterName\": \""+clusterName+"\"}}\n" +
                "     ]\n" +
                "}\n" +
                "}\n" +
                "}";

        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,jsonReq);
        Integer max = JsonPath.parse(jsonResponse).read("$.hits.hits.length()");
        if(max != 1) {
            LOGGER.log(Level.SEVERE, "The number of master ({0}) found: {1} ",new Object[]{masterName,max});
            return null;
        }
        Gson gson = new GsonBuilder().create();
        return  gson.fromJson(JsonPath.parse(jsonResponse).read(
                "$.hits.hits[0]._id").toString(),String.class);
    }

    static JsonDeserializer<ParameterValue> getPVDeserializer() {
        return new JsonDeserializer<ParameterValue>() {
            @Override
            public ParameterValue deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                return new StringParameterValue(
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("value").getAsString()
                );
            }
        };
    }
}
