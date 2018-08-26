package io.jenkins.plugins.elasticjenkins.util;

import hudson.model.*;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ElasticLogHandlerTest {
    public static String url = "http://localhost:9200";
    public static String master = "MASTERNAME";
    public static String clusterName = "myCluster";
    public static String hostname = "MyMaster2";
    private static String indexLog = "jenkins_logs";
    public String buildsIndex = "jenkins_builds";
    public String queueIndex = "jenkins_queues";
    public String clusterIndex = "jenkins_manage_clusters";
    public String mappingIndex = "jenkins_manage_mapping";

    private static String testFolder = "elasticjenkins";

    public static String root = (System.getProperty("java.io.tmpdir"))+testFolder;
    public static File testFile = new File(root);

    public String title = "MyJob";

    public Computer computer;
    public Node node;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    public Run<?,?> generateBuild(String id) throws IOException {
        computer = j.jenkins.createComputer();
        node = computer.getNode();
        j.jenkins.setNumExecutors(2);

        Long queueId = new Long(1);
        String url = "/"+title+"/"+id;

        Run build = Mockito.mock(Run.class);

        StringParameterValue p1 = new StringParameterValue("parameter1","éeù");
        StringParameterValue p2 = new StringParameterValue("parameter2","value2");
        ParametersAction pa =  new ParametersAction(p1,p2);


        List<ParametersAction> list = new ArrayList<ParametersAction>();
        list.add(pa);
        //Create Mock Job
        Job job = Mockito.mock(Job.class);
        job.setDisplayName(title);

        Mockito.when(build.getUrl()).thenReturn(url);
        Mockito.when(build.getId()).thenReturn(id);
        Mockito.when(build.getActions(ParametersAction.class)).thenReturn(list);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(build.getQueueId()).thenReturn(queueId);
        Mockito.when(build.getDisplayName()).thenReturn(title);
        Mockito.when(build.getRootDir()).thenReturn(new File(root));

        return build;
    }

    @BeforeClass
    public static void initialize() throws IOException {
        if(! testFile.exists()) {
            if(!testFile.mkdirs())
                throw new IOException("Can not execute since the directory is not writtable: "+root);
        }
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        //ElasticJenkinsUtil.writeProperties(master,url,"UTF-16",logIndex);
        ElasticJenkinsUtil.writeProperties(master,clusterName , url,"UTF-8",indexLog,buildsIndex,queueIndex,clusterIndex,mappingIndex );
    }

    @Ignore
    @Test
    public void testGetLog() throws IOException {
        Run<?,?> build = generateBuild("76");
        //ElasticLogHandler elasticLogHandler = new ElasticLogHandler(build);
        //File result = elasticLogHandler.generateLogFile("MyMaster2","76",URLDecoder.decode("2018_08%2F2Dj4OGUB66rIEJg-Zbgg","UTF-8"));
        //System.out.println("Result:"+result);
    }
}
