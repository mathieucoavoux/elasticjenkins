package io.jenkins.plugins.elasticjenkins;

import com.google.gson.Gson;
import hudson.Extension;
import hudson.model.ManagementLink;

import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;

import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;


import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsManagement extends ManagementLink {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsManagement.class.getName());

    protected String title = "Elasticjenkins management";

    private static String indexJenkinsCluster = "jenkins_manage_clusters";

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return title;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "elastic_mgmt";
    }

    /**
     * Get the persistence store. We expect to have an ElasticSearch url pointing to the REST API
     * For example: http://localhost:9200
     * @return: return the URL
     */
    public String getPersistenceStore() { return ElasticJenkinsUtil.getProperty("persistenceStore");}

    /**
     * Return the Jenkins master name. If the name has never been set it returns hostname and the pid concatenated
     * This value will be used in the Elasticsearch ID and to identify which server has launch a job
     * Hence this name must be unique otherwise builds result can be overwrote by another server
     * @return: Jenkins master name
     */
    public String getJenkinsMaster() {
        String master = ElasticJenkinsUtil.getProperty("masterName");
        if(master == null) {
                master = ElasticJenkinsUtil.getHostname()+System.currentTimeMillis();
        }
        return master;
    }

    /**
     * Get the charset used in the log output
     * @return: charset
     */
    public String getJenkinsCharset() {
        String charset = ElasticJenkinsUtil.getProperty("elasticCharset");
        return charset == null ? "UTF-8" : charset;
    }

    /**
     * Get the index name use to store the log output
     * @return
     */
    public String getLogIndex() {
        String charset = ElasticJenkinsUtil.getProperty("logIndex");
        return charset == null ? "jenkins_logs" : charset;
    }

    public String getClusterName() {
        return ElasticJenkinsUtil.getProperty("clusterName");
    }

    /**
     * Save the form into elasticjenkins file properties.
     * If the fields are empty the method returns badParams.
     * If the Elasticsearch status is different from "green" or "yellow" the method returns badStatus.
     * If the properties file cannot be written the method returns error otherwise saved
     * @param masterName: Jenkins master name
     * @param persistenceStore: Url of the persistence store
     * @param charset: charset used for the log output
     * @param clusterName: The name of the cluster which this server will join. This will allow to see builds from other members
     * @return: response
     */
    public HttpResponse doConfigure(@QueryParameter String masterName, @QueryParameter String persistenceStore,
                                    @QueryParameter String charset, @QueryParameter String clusterName,
                                    @QueryParameter String jenkinsLogs,
                                    @QueryParameter boolean forceCreation) {
        //Check if the form is filled correctly
        if(masterName == null || masterName.isEmpty() || persistenceStore == null || masterName.isEmpty()
                || charset == null || charset.isEmpty() ||
                clusterName == null || clusterName.isEmpty()) {
            return HttpResponses.redirectTo(".?badParams");
        }

        //Check if the url is available
        String status = ElasticJenkinsUtil.getElasticsearchStatus(persistenceStore);
        if(! status.equals("green") && ! status.equals("yellow")) {
            LOGGER.log(Level.WARNING,"Elasticsearch cluster status is:"+status);
            return HttpResponses.redirectTo(".?badStatus");
        }
        //Create indexes if they are not existing yet
        ElasticManager elasticManager = new ElasticManager();
        elasticManager.createManageIndex();
        LOGGER.log(Level.FINEST,"masterName:{0},persistenceStore:{1},charset:{2},clusterName:{3},jenkinsLogs:{4}", new Object[]{masterName,persistenceStore,charset,clusterName,jenkinsLogs});
        //Save the properties
        ElasticJenkinsUtil elasticJenkinsUtil = new ElasticJenkinsUtil();
        elasticJenkinsUtil.setCharset(charset);
        elasticJenkinsUtil.setMasterName(masterName);
        elasticJenkinsUtil.setClusterName(clusterName);
        elasticJenkinsUtil.setUrl(persistenceStore);
        if(!ElasticJenkinsUtil.writeProperties(masterName,clusterName , persistenceStore,charset,jenkinsLogs ))
            return HttpResponses.redirectTo(".?error");

        //Check if the master exist already
        String masterId = ElasticJenkinsUtil.getIdByMaster(masterName);
        if(masterId != null && ! forceCreation)
            return HttpResponses.redirectTo(".?exists");

        //Save the new master in the manage index
        if(addJenkinsMaster(masterName,clusterName,ElasticJenkinsUtil.getHostname(),masterId))
            return HttpResponses.redirectTo(".?success");


        return HttpResponses.redirectTo(".?error");
    }

    public boolean addJenkinsMaster(@Nonnull String jenkinsMaster, @Nonnull String clusterName,
                                           @Nonnull String hostname, @Nullable String masterId) {
        //We let Elasticsearch manage the ID
        String url = ElasticJenkinsUtil.getProperty("persistenceStore");
        String uri = url+"/"+indexJenkinsCluster+"/clusters/";
        if(masterId != null)
            uri = uri.concat(masterId);

        ElasticMaster em = new ElasticMaster();
        em.setJenkinsMasterName(jenkinsMaster);
        em.setClusterName(clusterName);
        em.setHostname(hostname);
        Gson gson = new Gson();
        String json = gson.toJson(em);
        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),ElasticsearchResult.class);
        return esr.get_id() != null ? true : false;
    }

}
