package io.jenkins.plugins.elasticjenkins.util;

import java.util.List;
import java.util.Map;

public interface ClusterStorageInterface extends StorageInterface {

    void markCompletedRecover(String healthId,String status);

    boolean recoverBuilds(String id);

    Map<String,Boolean> markToRecover(List<String> healthIds);

    List<String> getUnavailableNode();

    void updateHealthFlag(String id);

    String addMasterStartup();


}
