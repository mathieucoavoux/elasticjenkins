package io.jenkins.plugins.elasticjenkins.util;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import jenkins.model.Jenkins;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
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

    protected static File propertiesFile = new File(Jenkins.getInstance().getRootDir()+"/elasticjenkins.properties");

    private static String jenkinsManageIndexCluster = ElasticJenkinsUtil.getProperty("jenkinsManageClusterIndex");
    private static String jenkinsManageIndexMapping = ElasticJenkinsUtil.getProperty("jenkinsManageMappingIndex");
    private static String jenkinsBuildsIndex = ElasticJenkinsUtil.getProperty("jenkinsBuildsIndex");
    private static String jenkinsQueuesIndex = ElasticJenkinsUtil.getProperty("jenkinsQueuesIndex");
    private static String jenkinsManageClusters = "clusters";
    private static String jenkinsManageMapping = "mapping";

    protected static String charset = ElasticJenkinsUtil.getProperty("elasticCharset");
    protected static String masterName = ElasticJenkinsUtil.getProperty("masterName");

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

    public static String getJenkinsManageClusters() {
        return jenkinsManageClusters;
    }

    public static String getJenkinsManageMapping() {
        return jenkinsManageMapping;
    }

    public static String getClusterName() {
        LOGGER.log(Level.FINEST,"ClusterName:"+clusterName);
        return clusterName;
    }

    public static String getUrl() {
        return url;
    }

    protected static String clusterName = ElasticJenkinsUtil.getProperty("clusterName");
    protected static String url = ElasticJenkinsUtil.getProperty("persistenceStore");

    public static boolean isInitialized = ElasticJenkinsUtil.isInitialized();
    public static boolean isEmpty = ElasticJenkinsUtil.isEmpty();

    /**
     * Get the job url to take the project name as the project display name can contain
     * quotes because of the folder plugin.
     * Examples:
     * 		Freestyle job: job/Job1/94/
     * 		Folder job: job/Demo/job/Test1/22/
     * @param url: Jenkins job url
     * @return: job name
     */
    public static String convertUrlToFullName(String url) {
        String[] extractString = url.split("/");
        int max = extractString.length - 2;	//We always ignore the latest entry as this is for the ID
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
     * @return: hash of the job
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

    public static void setMasterName(String newMasterName) { masterName = newMasterName; }

    public static void setClusterName(String newClusterName){ clusterName = newClusterName;}

    public static void setUrl(String newUrl){ url = newUrl;}

    public static void setJenkinsManageIndexCluster(String newJenkinsManageIndexCluster) {
        jenkinsManageIndexCluster = newJenkinsManageIndexCluster;
    }

    public static void setJenkinsManageIndexMapping(String newJenkinsManageIndexMapping){
        jenkinsManageIndexMapping = newJenkinsManageIndexMapping;
    }

    public static void setJenkinsBuildsIndex(String newJenkinsBuildsIndex){
        jenkinsBuildsIndex = newJenkinsBuildsIndex;
    }

    public static void setJenkinsQueuesIndex(String newJenkinsQueuesIndex) {
        jenkinsQueuesIndex = newJenkinsQueuesIndex;
    }

    public static void setIsInitialized(boolean newIsInitialized){ isInitialized = newIsInitialized;}

    public static void setIsEmtpy(boolean newIsEmpty){isEmpty = newIsEmpty;}

    /**
     * Get a property from the elasticjenkins file properties
     * @param property: property to retrieve
     * @return value of the property
     */
    public static String getProperty(String property) {
        Properties prop = new Properties();
        InputStream input = null;
        if(!propertiesFile.exists()) {
            LOGGER.log(Level.SEVERE, "Properties files doesn't exist:" + propertiesFile.getPath());
            return null;
        }
        try {
            input = new FileInputStream(propertiesFile);
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
     * @return: boolean true if saved successfully
     */
    public static synchronized boolean writeProperties(@Nonnull String masterName,
                                                       @Nonnull String clusterName,
                                                       @Nonnull String persistenceStore, @Nonnull String charset,
                                                       @Nonnull String logIndex, @Nonnull String buildsIndex,
                                                       @Nonnull String queuesIndex,
                                                       @Nonnull String clusterIndex, @Nonnull String mappingIndex) {
        OutputStream out = null;

        setCharset(charset);
        setMasterName(masterName);
        setClusterName(clusterName);
        setUrl(persistenceStore);
        setJenkinsBuildsIndex(buildsIndex);
        setJenkinsQueuesIndex(queuesIndex);
        setJenkinsManageIndexCluster(clusterIndex);
        setJenkinsManageIndexMapping(mappingIndex);
        setIsInitialized(true);
        createManageIndex();
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
        try {
            out = new FileOutputStream(propertiesFile);
            BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile.getPath(), false));
            props.store(writer, "Persist properties");
        }catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Properties file not found. This may occur in testing mode. We try to create parent directory");
            Jenkins.getInstance().getRootDir().mkdirs();
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

    public static String getElasticsearchStatus(@Nonnull String url) {

        String uri = url+"/_cluster/health";
        String result = JsonPath.parse(elasticGet(uri)).read("$.status").toString();

        return result;
    }

    public static String getIdByMaster(@Nonnull String master) {
        String uri = getProperty("persistenceStore")+"/"+ jenkinsManageIndexCluster +"/clusters/_search";
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
                "\t\t\t\"number_of_replicas\" : 2 \n" +
                "\t\t}\n" +
                "\t},\n" +
                "\n" +
                "\t\n" +
                "  \"mappings\": {\n" +
                "    \"builds\": {\n" +
                "      \"properties\": {\n" +
                "\n" +
                "            \"startDate\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"yyyy-MM-dd\"\n" +
                "\n" +
                "        },\n" +
                "        \"endDate\": {\n" +
                "              \"type\": \"date\",\n" +
                "              \"format\" : \"yyyy-MM-dd\"\n" +
                "\n" +
                "        },\n" +
                "        \"id\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        },\n" +
                "        \"name\": {\n" +
                "          \"type\": \"keyword\"\n" +
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
                "          \"type\" : \"keyword\"\n" +
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
                "\n" +
                "}";
        ElasticJenkinsUtil.elasticPut(uri,json);
        ElasticJenkinsUtil.elasticPut(uri2,json);
        ElasticJenkinsUtil.elasticPut(uriBuilds,mappingBuilds);
        ElasticJenkinsUtil.elasticPut(uriQueues,json);
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
     * @return: JSON result
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
     * @return: JSON result
     */
    public static String elasticGet(@Nonnull String uri) {
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

    public static String elasticPut(@Nonnull String uri,@Nonnull String json) {
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

    public static boolean elasticDelete(String uri) {
        boolean result = false;
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept","application/json");
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpDelete);
            result = response.getStatusLine().getStatusCode() == 200 ? true : false;
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
        String master = null;
        try {
            master = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            //if the local host name could not be resolved into an address.
            master = "jenkins";
        }

        return master;

    }

    protected static String getCurentMasterId() {
        String uri = url+"/"+jenkinsManageIndexCluster+"/"+jenkinsManageClusters+"/_search";
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

    public static Integer getCountCurrentBuilds(String masters) {
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

    public static Integer getCountCurrentItem(String masters) {
        String uri = url+"/"+jenkinsQueuesIndex+"/queues/_count";
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
