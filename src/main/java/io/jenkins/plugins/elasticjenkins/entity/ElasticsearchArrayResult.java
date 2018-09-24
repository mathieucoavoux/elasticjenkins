package io.jenkins.plugins.elasticjenkins.entity;

import java.util.List;

public class ElasticsearchArrayResult<T> {
    public String getTook() {
        return took;
    }

    public void setTook(String took) {
        this.took = took;
    }

    public String getTimed_out() {
        return timed_out;
    }

    public void setTimed_out(String timed_out) {
        this.timed_out = timed_out;
    }

    public Shards get_shards() {
        return _shards;
    }

    public void set_shards(Shards _shards) {
        this._shards = _shards;
    }

    protected String took;
    protected String timed_out;
    protected Shards _shards;

    public Hits<T> getHits() {
        return hits;
    }

    public void setHits(Hits<T> hits) {
        this.hits = hits;
    }

    protected Hits<T> hits;

    public static class Shards {
        public String getTotal() {
            return total;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        public String getSuccessful() {
            return successful;
        }

        public void setSuccessful(String successful) {
            this.successful = successful;
        }

        public String getSkipped() {
            return skipped;
        }

        public void setSkipped(String skipped) {
            this.skipped = skipped;
        }

        public String getFailed() {
            return failed;
        }

        public void setFailed(String failed) {
            this.failed = failed;
        }

        public String total;
        public String successful;
        public String skipped;
        public String failed;
    }

    public static class Hits<T> {
        protected String total;

        public String getTotal() {
            return total;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        public String getMax_score() {
            return max_score;
        }

        public void setMax_score(String max_score) {
            this.max_score = max_score;
        }

        public List<ElasticsearchResult<T>> getHits() {
            return hits;
        }

        public void setHits(List<ElasticsearchResult<T>> hits) {
            this.hits = hits;
        }

        protected String max_score;

        protected  List<ElasticsearchResult<T>> hits;
    }
}
