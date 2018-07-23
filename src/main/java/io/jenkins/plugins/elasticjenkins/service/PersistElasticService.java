package io.jenkins.plugins.elasticjenkins.service;


/*
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
*/
import java.util.Map;

//import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class PersistElasticService {

    //protected Client client;

    /**
     * Elasticsearch type
     */
    private static String buildTypeName = "builds";

    /*
        TODO:
            Since we store the log output as a String and persist it in the Elasticsearch in a key/value,
            we limit the log output length to the HEAP size.
            We shall split large logs output in different indexes and update the result with this list of indexes
            Then merge this output in a smart way for the rendering.
     */
    /**
     * Insert build result with its parameters in ElasticSearch
     * @param index : Elasticsearch index name, we use the Jenkins master name (lower case)
     * @param build : Jenkins job properties: job name, job id, start date, end date, status and parameters
     * @return Return the id generated
     */
    /*
    public String insertBuildResult(@NotNull String index,
                                                     @NotNull Map<String, String> build) {
        /*
        return client.prepareIndex(index,buildTypeName)
                .setSource(build
                ).get().getId();

        */
    //}

}
