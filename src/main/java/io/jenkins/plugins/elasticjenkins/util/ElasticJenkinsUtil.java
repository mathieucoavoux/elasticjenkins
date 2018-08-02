package io.jenkins.plugins.elasticjenkins.util;



import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import jenkins.model.Jenkins;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsUtil {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsUtil.class.getName());

    protected static File propertiesFile = new File(Jenkins.getInstance().getRootDir()+"/elasticjenkins.properties");

    private static String indexJenkinsMaster = "jenkins_manage";

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
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(url.getBytes());
            byte[] digest = md.digest();
            hash = DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE,"I do NOT know the algorithm for the hashing");
        }
        return hash;
    }

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
     * @param masterName: name of the Jenkins master
     * @param persistenceStore: URL of the persistence store
     * @param charset: charset used in the log output encoding
     * @return: boolean
     */
    public static synchronized boolean writeProperties(@Nonnull String masterName,
                                                       @Nonnull String persistenceStore,@Nonnull String charset,
                                                       @Nonnull String logIndex) {
        OutputStream out = null;
        Properties props = new Properties();
        props.setProperty("masterName", masterName);
        props.setProperty("persistenceStore", persistenceStore);
        props.setProperty("charset", charset);
        props.setProperty("jenkins_logs",logIndex);
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
        String uri = getProperty("persistenceStore")+"/"+indexJenkinsMaster+"/clusters/_search";
        StringEntity entity = null;
        try {
            entity = new StringEntity("{ \"query\" : {\n" +
                    "  \"match\" : {\n" +
                    "    \"jenkinsMasterName\" : \""+master+"\" \n" +
                    "  }\n" +
                    "}}");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"The filter is not in JSON format.");
            return null;
        }
        String jsonResponse = ElasticJenkinsUtil.elasticPost(uri,entity);
        Integer total = JsonPath.parse(jsonResponse).read("$.hits.total");
        if (total != 1)
            return null;
        return JsonPath.parse(jsonResponse).read("$.hits.hits[0]._id").toString();
    }


    /**
     * Send POST resquest to Elasticsearch
     * @param uri: URI
     * @param entity: JSON entity sent in the POST
     * @return: JSON result
     */
    public static String elasticPost(@Nonnull String uri,@Nonnull StringEntity entity) {
        String result = null;
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Accept","application/json");
        httpPost.setHeader("Content-type","application/json");
        httpPost.setEntity(entity);
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient client = builder.build();
        try {
            CloseableHttpResponse response = client.execute(httpPost);
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
}
