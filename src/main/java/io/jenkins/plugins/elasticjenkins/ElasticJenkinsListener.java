package io.jenkins.plugins.elasticjenkins;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ElasticJenkinsListener extends QueueListener implements ExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsListener.class.getName());

    public String id;
    public String projectId;

    @Override
    public void onEnterWaiting(Queue.WaitingItem waitingItem) {
        ElasticManager elasticManager = new ElasticManager();
        String buildUrl = waitingItem.task.getUrl().split("/"+waitingItem.getId()+"/$")[0];
        String index = ElasticJenkinsUtil.getHash(buildUrl);
        try {
            this.projectId = elasticManager.addProjectMapping(index,URLEncoder.encode(buildUrl,ElasticJenkinsUtil.getProperty("elasticCharset")));
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE,"Charset not supported:"+ElasticJenkinsUtil.getProperty("elasticCharset"));
        }
        this.id = elasticManager.addQueueItem(waitingItem,this.projectId);
    }

    @Override
    public void onLeft(Queue.LeftItem leftItem) {
        LOGGER.log(Level.FINEST,"Id:"+leftItem.outcome.item.getId());
        ElasticManager elasticManager = new ElasticManager();
        elasticManager.updateQueueItem(leftItem,id);
    }

    public static ExtensionList<QueueListener> all() {
        return ExtensionList.lookup(QueueListener.class);
    }
}
