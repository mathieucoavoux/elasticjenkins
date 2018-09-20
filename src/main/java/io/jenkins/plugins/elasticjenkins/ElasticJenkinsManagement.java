package io.jenkins.plugins.elasticjenkins;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hudson.Extension;
import hudson.model.ManagementLink;

import io.jenkins.plugins.elasticjenkins.entity.ElasticMaster;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchArrayResult;
import io.jenkins.plugins.elasticjenkins.entity.ElasticsearchResult;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;

import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;


import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsManagement extends ManagementLink {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsManagement.class.getName());

    protected String title = "Elasticjenkins management";


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
    public String getPersistenceStore() { return ElasticJenkinsUtil.getUrl();}

    /**
     * Return the Jenkins master name. If the name has never been set it returns hostname and the pid concatenated
     * This value will be used in the Elasticsearch ID and to identify which server has launch a job
     * Hence this name must be unique otherwise builds result can be overwrote by another server
     * @return: Jenkins master name
     */
    public String getJenkinsMaster() {
        String master = ElasticJenkinsUtil.getMasterName();
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
        String charset = ElasticJenkinsUtil.getCharset();
        return charset == null ? "UTF-8" : charset;
    }

    /**
     * Get the index name use to store the log output
     * @return: the index use for the log
     */
    public String getLogIndex() {
        String logIndex = ElasticJenkinsUtil.getProperty("logIndex");
        return logIndex == null ? "jenkins_logs" : logIndex;
    }

    public String getJenkinsBuilds() {
        String jenkinsBuildsIndex = ElasticJenkinsUtil.getJenkinsBuildsIndex();
        return jenkinsBuildsIndex == null ? "jenkins_builds" : jenkinsBuildsIndex;
    }

    public String getJenkinsQueues() {
        String jenkinsManageMappingIndex = ElasticJenkinsUtil.getJenkinsQueuesIndex();
        return jenkinsManageMappingIndex == null ? "jenkins_queues" : jenkinsManageMappingIndex;
    }

    public String getJenkinsClusterIndex() {
        String jenkinsClusterIndex= ElasticJenkinsUtil.getJenkinsManageIndexCluster();
        return jenkinsClusterIndex == null ? "jenkins_manage_clusters" : jenkinsClusterIndex;
    }

    public String getJenkinsManageIndex() {
        String jenkinsManageMappingIndex = ElasticJenkinsUtil.getJenkinsManageIndexMapping();
        return jenkinsManageMappingIndex == null ? "jenkins_manage_mapping" : jenkinsManageMappingIndex;
    }
    public String getClusterName() {
        return ElasticJenkinsUtil.getClusterName();
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
     * @param jenkinsLogs: The index use to store the log output
     * @param jenkinsBuilds: The index use to store the builds
     * @param jenkinsQueues: The index use to store the queued items
     * @param forceCreation: If set to true we force to save the new node configuration in Elasticsearch even if it exists
     *                     otherwise it will fail.
     * @param jenkinsClusterIndex: The index of the cluster configuration
     * @param jenkinsManageIndex: The index use for the project mapping.
     * @return: response if the configuration has been saved successfully or not
     */
    public HttpResponse doConfigure(@QueryParameter String masterName, @QueryParameter String persistenceStore,
                                    @QueryParameter String charset, @QueryParameter String clusterName,
                                    @QueryParameter String jenkinsLogs, @QueryParameter String jenkinsBuilds,
                                    @QueryParameter String jenkinsQueues, @QueryParameter boolean forceCreation,
                                    @QueryParameter String jenkinsClusterIndex,@QueryParameter String jenkinsManageIndex) {
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



        LOGGER.log(Level.FINEST,"masterName:{0},persistenceStore:{1},charset:{2},clusterName:{3},jenkinsLogs:{4}", new Object[]{masterName,persistenceStore,charset,clusterName,jenkinsLogs});
        //Save the properties


        if(!ElasticJenkinsUtil.writeProperties(masterName,clusterName , persistenceStore,charset,jenkinsLogs,jenkinsBuilds,
                jenkinsQueues,jenkinsClusterIndex,jenkinsManageIndex ))
            return HttpResponses.redirectTo(".?error");

        //Check if the master exist already
        String masterId = null;
        if(ElasticJenkinsUtil.isInitialized() == true &&
                ElasticJenkinsUtil.elasticHead(persistenceStore+"/"+jenkinsClusterIndex+"/_mapping/clusters") == 200)
            masterId = ElasticJenkinsUtil.getIdByMaster(masterName);
        if(masterId != null && ! forceCreation)
            return HttpResponses.redirectTo(".?exists");


        //Save the new master in the manage index
        if(addJenkinsMaster(masterName,clusterName,ElasticJenkinsUtil.getHostname(),masterId,jenkinsClusterIndex))
            return HttpResponses.redirectTo(".?success");


        return HttpResponses.redirectTo(".?error");
    }

    public boolean addJenkinsMaster(@Nonnull String jenkinsMaster, @Nonnull String clusterName,
                                           @Nonnull String hostname, @Nullable String masterId, @Nonnull String indexJenkinsCluster) {
        //We let Elasticsearch manage the ID
        String url = ElasticJenkinsUtil.getUrl();
        String uri = url+"/"+indexJenkinsCluster+"/clusters/";
        if(masterId != null)
            uri = uri.concat(masterId);

        ElasticMaster em = new ElasticMaster();
        em.setJenkinsMasterName(jenkinsMaster);
        em.setClusterName(clusterName);
        em.setHostname(hostname);
        Gson gson = new Gson();
        String json = gson.toJson(em);
        Type elasticsearchResulType = new TypeToken<ElasticsearchResult<GenericBuild>>(){}.getType();
        ElasticsearchResult esr = gson.fromJson(ElasticJenkinsUtil.elasticPost(uri,json),elasticsearchResulType);
        return esr.get_id() != null ? true : false;
    }

}
