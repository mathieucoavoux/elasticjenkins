package io.jenkins.plugins.elasticjenkins;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class ElasticJenkinsIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    TestsUtil testsUtil = new TestsUtil(story, temporaryFolder);


    @Test
    public void testStartJenkins() throws IOException, InterruptedException {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                testsUtil.reset();
                ElasticManager elasticManager = new ElasticManager();
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class,"p");
                story.j.createSlave();
                p.setDefinition(new CpsFlowDefinition(testsUtil.loadResource("oneStepPipeline.groovy"),true));
                WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
                assertNotNull(b1);
                story.j.waitForCompletion(b1);

                story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b1));
                Thread.sleep(5000);
                String hash = ElasticJenkinsUtil.getHash(p.getUrl().split("/$")[0]);
                //Check if there is one build in EXECUTING status
                List<GenericBuild> list = elasticManager.getPaginateBuildHistory(hash, "clusters" , 5, "0");
                int maxRetry = 10;
                int retry = 0;
                while(list.size() == 0 && retry < maxRetry) {
                    Thread.sleep(1000);
                    list = elasticManager.getPaginateBuildHistory(hash, "clusters" , 5, "0");
                    retry = retry + 1;
                }
                assertEquals(1,list.size());
                assertEquals("EXECUTING",list.get(0).getStatus());
            }
        });

    }

    @Test
    public void testCompletedPipeline() throws IOException, InterruptedException {
        story.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                testsUtil.reset();
                ElasticManager elasticManager = new ElasticManager();
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class,"p2");
                story.j.createSlave();
                p.setDefinition(new CpsFlowDefinition(testsUtil.loadResource("completedPipeline.groovy"),true));
                WorkflowRun b1 = p.scheduleBuild2(0).waitForStart();
                assertNotNull(b1);
                story.j.waitForCompletion(b1);

                story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b1));

                String projectName = p.getUrl().split("/$")[0];
                String hash = ElasticJenkinsUtil.getHash(projectName);
                //Check if there is one build in SUCCESS status
                List<GenericBuild> list = elasticManager.getPaginateBuildHistory(hash, "clusters" , 5, "0");
                String result = list.size() == 1 ? list.get(0).getStatus() : null;
                int maxRetry = 40;
                int retry = 0;
                while((list.size() == 0 ||  result != "SUCCESS") && retry < maxRetry ) {
                    Thread.sleep(1000);
                    list = elasticManager.getPaginateBuildHistory(hash, "clusters" , 5, "0");
                    result = list.size() == 1 ? list.get(0).getStatus() : null;
                    retry = retry + 1;
                }
                assertEquals(1,list.size());
                assertEquals("SUCCESS",list.get(0).getStatus());
            }
        });

    }
}
