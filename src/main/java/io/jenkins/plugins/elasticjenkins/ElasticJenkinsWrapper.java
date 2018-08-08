package io.jenkins.plugins.elasticjenkins;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
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
	private static String charset = ElasticJenkinsUtil.getProperty("elasticCharset");
	
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
			ElasticManager em = new ElasticManager();
			//The hash of the project name is used for the index
			String index = ElasticJenkinsUtil.getHash(build.getUrl().split(build.getId())[0]);
			try {
				em.addProjectMapping(index,URLEncoder.encode(build.getUrl().split(build.getId())[0],charset));
			} catch (UnsupportedEncodingException e) {
				LOGGER.log(Level.SEVERE,"Charset not supported:"+charset);
			}
			LOGGER.log(Level.FINEST,"Job hash: "+index);
			id = em.addBuild(index,"builds",build);

        }


        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            if(build instanceof Run){
                //Update the status of the build and add the log output
				LOGGER.log(Level.INFO,"Elasticsearch plugin build tearDown disposerImpl");
				LOGGER.log(Level.INFO,"Id:"+id);
				ElasticManager em = new ElasticManager();
				String index = ElasticJenkinsUtil.getHash(build.getUrl().split(build.getId())[0]);
				int maxLines = (int) Files.lines(build.getLogFile().toPath()).count();
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
				id = em.updateBuild(index,"builds",build,id,status,build.getLog(maxLines));
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
