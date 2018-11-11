package io.jenkins.plugins.elasticjenkins;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.logging.Level;

import io.jenkins.plugins.elasticjenkins.util.ElasticJenkinsUtil;
import io.jenkins.plugins.elasticjenkins.util.ElasticManager;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;

import javax.annotation.Nullable;


public class ElasticJenkinsWrapper extends SimpleBuildWrapper implements Serializable {

	private static final Logger LOGGER = Logger.getLogger(ElasticJenkins.class.getName());

	public boolean pipeline;

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public String stepName;

	@DataBoundConstructor
	public ElasticJenkinsWrapper(@Nullable String stepName){
		if(stepName != null) {
			this.pipeline = true;
		}else{
			this.pipeline = false;
		}
	}

	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			// TODO Auto-generated method stub
			return true;
		}
		
		public String getDisplayName() {
			return "ElasticBuild";
		}
		
		public DescriptorImpl() {
			load();
		}
		
	}

	public class DisposerImpl extends Disposer {
		public String id;
		public String projectId;

	    public DisposerImpl(Run<?,?> build) {
	        //We save the build here when it starts
			ElasticManager em = new ElasticManager();
			//The hash of the project name is used for the index
            String buildUrl = build.getUrl().split("/"+build.getId()+"/$")[0];
			String index = ElasticJenkinsUtil.getHash(buildUrl);
			try {
				this.projectId = em.addProjectMapping(index,URLEncoder.encode(buildUrl,ElasticJenkinsUtil.getProperty("elasticCharset")));
				if( ElasticJenkinsUtil.isEmpty) {
					ElasticJenkinsUtil elasticJenkinsUtil = new ElasticJenkinsUtil();
					elasticJenkinsUtil.setIsEmpty(false);
				}

			} catch (UnsupportedEncodingException e) {
				LOGGER.log(Level.SEVERE,"Charset not supported:"+ElasticJenkinsUtil.getProperty("elasticCharset"));
			}
			LOGGER.log(Level.FINEST,"Job hash: "+index);
			id = em.addBuild(projectId,build);

        }


        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            if(build instanceof Run && pipeline == false){
                //Update the status of the build and add the log output

				ElasticManager em = new ElasticManager();
				//Handle weird characters
				//int maxLines = (int) Files.lines(build.getLogFile().toPath(), StandardCharsets.UTF_16).count();
				String status = "";
				if (build instanceof Run) {
					if(build.getResult() != null) {
						LOGGER.log(Level.FINEST, "Is the build completed : {0} ",new Object[] {build.getResult().toString()});
						status = build.getResult().toString();
					}else {
						LOGGER.log(Level.FINEST,"No result set, build looks like : {0}",new Object[] {build.toString()});
						status = "SUCCESS";
					}

				}

				id = em.updateBuild(id,status,build.getLogFile());
            }
        }
    }

    /**
     * This method is called during the build execution
     * @param context: this is used to add the plugin into the wrapper
     * @param build: project which is built
     * @param workspace: workspace of the project
     * @param launcher: resource which launch the build
     * @param listener: use for the logger
     * @param initialEnvironment: environment variables set when the project has been built
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
	@Override
	public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			EnvVars initialEnvironment) throws IOException, InterruptedException {
        //Add the disposer. When the build is completed it will call back the tearDown method
        context.setDisposer(new DisposerImpl(build));
		
	}

}
