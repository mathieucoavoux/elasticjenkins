package io.jenkins.plugins.elasticjenkins.entity;

/**
 * This entity is used to parse Eleasticsearch result
 * We have preferred to declare an entity and use the REST API of Elasticsearch here as
 *      - the Elasticsearch Java API is banned from Jenkins
 *      - it avoids to load too much the classloaders with additional libraries
 *      - the Elasticsearch transport Java API is different between versions
 *
 */
public class ElasticsearchResult {
    protected String _index;

    protected String _type;

    protected String _id;

    protected String _version;

    protected String result;

    protected String _seq_no;

    protected String _primary_term;

    public Object get_source() {
        return _source;
    }

    public void set_source(Object _source) {
        this._source = _source;
    }

    protected Object _source;

    public String get_index() {
        return _index;
    }

    public void set_index(String _index) {
        this._index = _index;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_version() {
        return _version;
    }

    public void set_version(String _version) {
        this._version = _version;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String get_seq_no() {
        return _seq_no;
    }

    public void set_seq_no(String _seq_no) {
        this._seq_no = _seq_no;
    }

    public String get_primary_term() {
        return _primary_term;
    }

    public void set_primary_term(String _primary_term) {
        this._primary_term = _primary_term;
    }






}
