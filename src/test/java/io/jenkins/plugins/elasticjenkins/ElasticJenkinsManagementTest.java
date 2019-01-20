package io.jenkins.plugins.elasticjenkins;

import hudson.model.Hudson;
import io.jenkins.plugins.elasticjenkins.util.ConfigurationStorageInterface;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import io.jenkins.plugins.elasticjenkins.util.StorageProxyFactory;
import jenkins.model.Jenkins;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.reactor.ReactorException;
import org.jvnet.hudson.test.JenkinsRecipe;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/*
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class,ElasticJenkinsManagement.class})
@PowerMockIgnore({"javax.crypto.*"})
*/
public class ElasticJenkinsManagementTest {




    @Rule public JenkinsRule j = new JenkinsRule(){
        @Override
        public void after() throws Exception {
            super.after();
            if(TestEnvironment.get() != null) {
                try {
                    TestEnvironment.get().dispose();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    TestsUtil testsUtil = new TestsUtil(j, temporaryFolder);

    private static String master = "MASTERNAME3";
    private static String clusterName = "CLUSTER_NAME3";


    private static String testFolder = "elasticjenkins";

    public static String root = (System.getProperty("java.io.tmpdir"))+testFolder;
    public static File testFile = new File(root);

    @BeforeClass
    public static void initialize() throws IOException {
        if(! testFile.exists()) {
            if(!testFile.mkdirs())
                throw new IOException("Can not execute since the directory is not writtable: "+root);
        }

    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        testsUtil.reset();
    }


    @Test
    public void testDoConfigure() throws IllegalAccessException, NoSuchFieldException, IOException, InterruptedException, ReactorException, ConfigurationException, ClassNotFoundException {

        ElasticJenkinsUtil.setConfigurationStorageType("elasticsearch");

        ElasticJenkinsManagement elasticJenkinsManagement = new ElasticJenkinsManagement();

        HttpResponse responseOK = elasticJenkinsManagement.doConfigure(master,TestsUtil.url,"UTF-8",
                clusterName,TestsUtil.logIndex,TestsUtil.buildsIndex,TestsUtil.queueIndex,
                false,TestsUtil.clusterIndex,TestsUtil.mappingIndex, TestsUtil.mappingHealth);
        //Check response
        Field statusCodeField = responseOK.getClass().getDeclaredField("statusCode");
        statusCodeField.setAccessible(true);
        assertEquals(302,statusCodeField.getInt(responseOK));
        Field urlField = responseOK.getClass().getDeclaredField("url");
        urlField.setAccessible(true);
        assertEquals(".?success",urlField.get(responseOK));
        //Wait few seconds to let Elasticsearch save the master
        Thread.sleep(2000);
        //Get Id of the master to delete it
        ConfigurationStorageInterface configurationStorage = (ConfigurationStorageInterface) StorageProxyFactory.newInstance(ConfigurationStorageInterface.class);
        List<String> listIds = configurationStorage.getMasterIdByNameAndCluster(TestsUtil.master,TestsUtil.clusterName);
        for(String id : listIds) {
            //testsUtil.deleteTest(TestsUtil.clusterIndex,TestsUtil.clusterType,id);
        }

    }
}
