package io.jenkins.plugins.elasticjenkins;

import java.io.IOException;
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


public class ElasticJenkinsWrapper extends SimpleBuildWrapper {

	private static final Logger LOGGER = Logger.getLogger(ElasticJenkins.class.getName());
	
	@DataBoundConstructor
	public ElasticJenkinsWrapper(){}

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

	public static final class DisposerImpl extends Disposer {
		public String id;

	    public DisposerImpl(Run<?,?> build) {
	        //We save the build here when it starts
			LOGGER.log(Level.INFO,"Elasticsearch plugin build starts disposerImpl");
			ElasticManager em = new ElasticManager();
			String index = ElasticJenkinsUtil.getHash(build.getUrl());
			LOGGER.log(Level.INFO,"Job hash: "+index);
			id = em.addBuild(index,"builds",build);

        }


        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            if(build instanceof Run){
                //Update the status of the build and add the log output
				LOGGER.log(Level.INFO,"Elasticsearch plugin build tearDown disposerImpl");
				LOGGER.log(Level.INFO,"Id:"+id);
				ElasticManager em = new ElasticManager();
				String index = ElasticJenkinsUtil.getHash(build.getUrl());
				id = em.updateBuild(index,"builds",build,id);
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
		// Save the build here?
        //Add the disposer. When the build is completed it will call back the tearDown method
		LOGGER.log(Level.INFO,"Elasticsearch plugin build setUp");


        context.setDisposer(new DisposerImpl(build));
		
	}

}
