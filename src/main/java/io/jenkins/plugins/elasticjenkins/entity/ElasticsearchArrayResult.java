package io.jenkins.plugins.elasticjenkins.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ElasticsearchArrayResult<T> {
    public String getTook() {
        return took;
    }

    public String getTimedOut() {
        return timedOut;
    }

    public Shards getShards() {
        return shards;
    }

    protected String took;

    @SerializedName("timed_out")
    protected String timedOut;

    @SerializedName("_shards")
    protected Shards shards;

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

        public String getSkipped() {
            return skipped;
        }

        public String getFailed() {
            return failed;
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

        public String getMaxScore() {
            return maxScore;
        }

        public List<ElasticsearchResult<T>> getHits() {
            return hits;
        }

        public void setHits(List<ElasticsearchResult<T>> hits) {
            this.hits = hits;
        }

        @SerializedName("max_score")
        protected String maxScore;

        protected  List<ElasticsearchResult<T>> hits;
    }
}
