package io.jenkins.plugins.elasticjenkins.entity;

import com.google.gson.annotations.SerializedName;

/**
 * This entity is used to parse Eleasticsearch result
 * We have preferred to declare an entity and use the REST API of Elasticsearch here as
 *      - the Elasticsearch Java API is banned from Jenkins
 *      - it avoids to load too much the classloaders with additional libraries
 *      - the Elasticsearch transport Java API is different between versions
 *
 */
public class ElasticsearchResult<T> {
    @SerializedName("_index")
    protected String index;

    @SerializedName("_type")
    protected String type;

    @SerializedName("_id")
    protected String id;

    @SerializedName("_version")
    protected String version;

    protected String result;

    @SerializedName("_seq_no")
    protected String seqNo;

    @SerializedName("_primary_term")
    protected String primaryTerm;


    public T getSource() {
        return source;
    }

    @SerializedName("_source")
    protected T source;


    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSeqNo() {
        return seqNo;
    }

    public String getPrimaryTerm() {
        return primaryTerm;
    }

}
