package io.jenkins.plugins.elasticjenkins.util;

import hudson.model.Queue;
import hudson.model.Run;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public interface ConfigurationStorageInterface extends StorageInterface {

    /**
     * Save the build when starting.
     * @param projectId The id of the project.
     *                  This id is reused to list the history of the builds based on the project Id
     * @param build The build launched
     * @return The entry Id where the build has been saved
     */
    String addBuild(@Nonnull String projectId,
                    @Nonnull Run<?, ?> build);

    /**
     * TODO: Return a boolean to check if the entry has been updated correctly
     * Update the build information when complete
     * @param id The entry Id where the build has been saved
     * @param status The status of the build.
     * @param file The log file where the output is stored
     * @param fileCharset
     * @return The entry Id that have been updated.
     */
    String updateBuild(@Nonnull String id,
                       @Nonnull String status,
                       @Nullable File file, Charset fileCharset);

    /**
     * Get the list of the build for a specific project.
     * The list has a limited size and a number of where to start the cursor
     * @param projectName The name of the project that we request the builds list
     * @param viewType The view type.
     *                 If we use the cluster view, the list contains all the builds of all the nodes in the cluster.
     *                 Otherwise we take only the list of the current node
     * @param paginationSize Size of the list to return
     * @param paginationStart Where to start the list
     * @return The list of builds
     */
    List<GenericBuild> getPaginateBuildHistory(@Nonnull String projectName,
                                               @Nonnull String viewType,
                                               @Nonnull Integer paginationSize, @Nullable String paginationStart);

    /**
     * Get the newest build saved since the last fetch
     * @param projectHash : Hash of the project
     * @param lastFetch : Timestamp of the last fetch
     * @param viewType : View can be either cluster or server. If the cluster is selected we display all builds of the servers in the cluster
     * @return list of builds
     */
    List<GenericBuild> getNewResults(@Nonnull String projectHash,
                                     @Nonnull String lastFetch, String viewType);

    /**
     * Get the Elasticsearch id of a Jenkins master based on its name and its cluster name
     * @param masterName: Name of the jenkins master
     * @param clusterName: Name of the cluster
     * @return the list of match
     */
    List<String> getMasterIdByNameAndCluster(@Nonnull String masterName,
                                             @Nonnull String clusterName);

    /**
     * Create a mapping with the Elasticsearch id of the project and its hashed name and its encoded name
     * @param projectHash : the URL of the project hashed
     * @param projectEncodedName : the URL of the project encoded
     * @return the Elasticsearch id
     */
    String addProjectMapping(@Nonnull String projectHash, @Nonnull String projectEncodedName);

    /**
     * Find builds by its parameters. Depending of the type of the view we search in all nodes of the cluster or a specific server
     * @param hash: the project hashed name
     * @param viewType: the view Type (cluster/server)
     * @param parameter: the parameter value
     * @return The list of builds
     */
    List<GenericBuild> findByParameter(@Nonnull String hash,
                                       @Nonnull String viewType, @Nonnull String parameter);
    /**
     * Add an item from the queue to Elasticsearch. This prevent to lose the queued items if a crash occurs
     * @param waitingItem: the item
     * @param projectId: the project id
     * @return the elasticsearch id
     */
    String addQueueItem(Queue.WaitingItem waitingItem, String projectId);

    /**
     * TODO: return boolean value
     * Update the status of the item
     * @param eId : the elasticsearch id
     */
    void updateQueueItem(String eId);

    /**
     * Get the last 3 current builds, to display to the panel
     * @return the list of the current builds
     */
    List<GenericBuild> getLastCurrentBuilds();

    /**
     * Get the last 3 current items to display it in the panel
     * @return list of the item
     */
    List<GenericBuild> getLastCurrentItems();

    /**
     * Count the number of the current builds and display it to the panel
     * @return the number of the current build
     */
    Integer getCountCurrentBuilds();

    /**
     * Get the count of the current item and display it to the panel
     * @return The number of current item
     */
    Integer getCountCurrentItem();
}
