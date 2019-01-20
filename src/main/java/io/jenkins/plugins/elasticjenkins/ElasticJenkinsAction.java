package io.jenkins.plugins.elasticjenkins;


import com.google.gson.Gson;

import hudson.console.AnnotatedLargeText;
import hudson.model.*;
import hudson.slaves.ComputerLauncher;

import io.jenkins.plugins.elasticjenkins.entity.GenericBuild;
import io.jenkins.plugins.elasticjenkins.util.*;

import jenkins.model.RunAction2;
import org.apache.commons.jelly.XMLOutput;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticJenkinsAction extends ComputerLauncher implements RunAction2 {

    private AbstractProject<?,?> project;

    private static final Logger LOGGER = Logger.getLogger(ElasticJenkinsAction.class.getName());

    protected AbstractBuild<?,?> build;

    ElasticJenkinsAction(AbstractProject<?,?> project) {
        this.project = project;
    }

    public String getIconFileName() {
        return "/plugin/elasticjenkins/24x24/elasticjenkins.png";
    }

    public String getDisplayName() {
        return "Elastic build history";
    }

    public String getUrlName() {
        return "ej1";
    }

    public AbstractProject<?,?> getProject() {
        return project;
    }


    @Override
    public void onAttached(Run<?,?> r) {
        build = (AbstractBuild<?,?>) r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        build = (AbstractBuild<?,?>) r;
    }


    @JavaScriptMethod
    public List<GenericBuild> getPaginatedHistory(@Nonnull String viewType,
                                                  @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        try {
            ConfigurationStorageInterface configurationStorage = (ConfigurationStorageInterface) StorageProxyFactory.newInstance(ConfigurationStorageInterface.class);
            String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);

            return configurationStorage.getPaginateBuildHistory(index,viewType,paginationSize,paginationStart);
        }catch (ClassNotFoundException e1){
            LOGGER.log(Level.SEVERE,"Proxy has NOT found the required interface. Cause:",e1.getCause());
        }catch (ConfigurationException e2){
            LOGGER.log(Level.SEVERE,"Configuration of the plugin has not been found. Please ensure you configure the plugin correctly");
        }
        return null;
    }


    @JavaScriptMethod
    public String getPagninatedHistoryJson(@Nonnull String viewType,
                                           @Nonnull Integer paginationSize,@Nonnull String paginationStart) {
        try {
            String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
            Gson gson = new Gson();
            ConfigurationStorageInterface configurationStorage = (ConfigurationStorageInterface) StorageProxyFactory.newInstance(ConfigurationStorageInterface.class);

            return gson.toJson(configurationStorage.getPaginateBuildHistory(index,viewType,paginationSize,paginationStart));
        }catch (ClassNotFoundException e1){
            LOGGER.log(Level.SEVERE,"Proxy has NOT found the required interface. Cause:",e1.getCause());
        }catch (ConfigurationException e2){
            LOGGER.log(Level.SEVERE,"Configuration of the plugin has not been found. Please ensure you configure the plugin correctly");
        }
        return null;
    }

    @JavaScriptMethod
    public String getNewResultsJson(@Nonnull String type,@Nonnull String viewType,@Nonnull String lastFetch) {
        try{
            String projectHash = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
            Gson gson = new Gson();
            ConfigurationStorageInterface configurationStorage = (ConfigurationStorageInterface) StorageProxyFactory.newInstance(ConfigurationStorageInterface.class);

            return gson.toJson(configurationStorage.getNewResults(projectHash, lastFetch,viewType ));
        }catch (ClassNotFoundException e1){
            LOGGER.log(Level.SEVERE,"Proxy has NOT found the required interface. Cause:",e1.getCause());
        }catch (ConfigurationException e2){
            LOGGER.log(Level.SEVERE,"Configuration of the plugin has not been found. Please ensure you configure the plugin correctly");
        }
        return null;
    }

    @JavaScriptMethod
    public String filterResult(@Nonnull String type,@Nonnull String viewType,@Nonnull String filterType, @Nonnull String filterValue) {
        if(filterType.equals("parameters")) {
            return getBuildByParameters(type,viewType,filterValue);
        }

        return null;
    }


    public String getBuildByParameters(@Nonnull String type,@Nonnull String viewType,@Nonnull String parameter) {
        try {
            String projectHash = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
            ConfigurationStorageInterface configurationStorage = (ConfigurationStorageInterface) StorageProxyFactory.newInstance(ConfigurationStorageInterface.class);
            Gson gson = new Gson();
            return gson.toJson(configurationStorage.findByParameter(projectHash, viewType,parameter));
        }catch (ClassNotFoundException e1){
            LOGGER.log(Level.SEVERE,"Proxy has NOT found the required interface. Cause:",e1.getCause());
        }catch (ConfigurationException e2){
            LOGGER.log(Level.SEVERE,"Configuration of the plugin has not been found. Please ensure you configure the plugin correctly");
        }
        return null;
    }

    @JavaScriptMethod
    public void writeOutput(XMLOutput out,String id) throws IOException, SAXException {
        try {
            //Get log id
            String index = ElasticJenkinsUtil.getHash(project.getUrl().split("/$")[0]);
            LogStorageInterface logStorage = (LogStorageInterface) StorageProxyFactory.newInstance(LogStorageInterface.class);
            String suffix = logStorage.getLogOutputId(id);
            File logOutput = logStorage.getLogOutput(URLDecoder.decode(suffix,"UTF-8"),id);
            LOGGER.log(Level.FINEST,"Log output:"+logOutput.getPath());
            new AnnotatedLargeText<GenericBuild>(logOutput,Charset.defaultCharset(),true,new GenericBuild()).writeHtmlTo(0,out.asWriter());

            logOutput.delete();
        }catch (ClassNotFoundException e1){
            LOGGER.log(Level.SEVERE,"Proxy has NOT found the required interface. Cause:",e1.getCause());
        }catch (ConfigurationException e2){
            LOGGER.log(Level.SEVERE,"Configuration of the plugin has not been found. Please ensure you configure the plugin correctly");
        }

    }

    public void doProgressiveHtml(StaplerRequest req, StaplerResponse rsp) throws IOException {

        ElasticManager elasticManager = new ElasticManager();


    }


    public HttpResponse doGetLog(StaplerRequest request) {
        return HttpResponses.forwardToView(this,"log_output.jelly");
    }
}
