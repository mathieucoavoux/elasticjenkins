package io.jenkins.plugins.elasticjenkins.util;

import com.jcraft.jzlib.GZIPInputStream;
import com.jcraft.jzlib.GZIPOutputStream;
import com.trilead.ssh2.crypto.Base64;
import hudson.console.AnnotatedLargeText;
import hudson.console.ConsoleAnnotationOutputStream;
import hudson.console.ConsoleAnnotator;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.remoting.ObjectInputStreamEx;
import hudson.triggers.SCMTrigger;
import jenkins.model.Jenkins;
import jenkins.security.CryptoConfidentialKey;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;



public class ElasticLogHandler {

    private AbstractProject<?,?>  project;

    public ElasticLogHandler(AbstractProject<?,?> project) {
        this.project = project;
    }
    /*
    private ConsoleAnnotator createAnnotator(StaplerRequest req) throws IOException {
        try {
            String base64 = req!=null ? req.getHeader("X-ConsoleAnnotator") : null;
            if (base64!=null) {
                Cipher sym = PASSING_ANNOTATOR.decrypt();

                ObjectInputStream ois = new ObjectInputStreamEx(new GZIPInputStream(
                        new CipherInputStream(new ByteArrayInputStream(Base64.decode(base64.toCharArray())),sym)),
                        Jenkins.getInstance().pluginManager.uberClassLoader);
                try {
                    long timestamp = ois.readLong();
                    if (TimeUnit.HOURS.toMillis(1) > abs(System.currentTimeMillis()-timestamp))
                        // don't deserialize something too old to prevent a replay attack
                        return (ConsoleAnnotator)ois.readObject();
                } finally {
                    ois.close();
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        // start from scratch
        return ConsoleAnnotator.initial(context==null ? null : context.getClass());
    }

    protected long writeLogTo(long start, OutputStream out,String suffix) throws IOException {
        CountingOutputStream os = new CountingOutputStream(out);
        ElasticManager elasticManager = new ElasticManager();
        List<String> list = elasticManager.getLogOutput(suffix);
        for(String output : list) {
            os.write(output.getBytes());
        }
        return 0L;
    }

    public long writeHtmlTo(long start, Writer w,String suffix) throws IOException {
        ConsoleAnnotationOutputStream caw = new ConsoleAnnotationOutputStream(
                w, createAnnotator(Stapler.getCurrentRequest()), context, charset);
        long r = writeLogTo(0,caw,suffix);
        org.apache.commons.io.output.ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Cipher sym = PASSING_ANNOTATOR.encrypt();

        return r;
    }
    */

    public File generateLogFile(String master,String id,String suffix) throws IOException {
        File file = new File(project.getRootDir(),master+"_"+id+".log");
        FileWriter writer = new FileWriter(file);
        ElasticManager elasticManager = new ElasticManager();
        List<String> list = elasticManager.getLogOutput(suffix);
        for(String row : list) {
            writer.write(row+"\n");
        }
        writer.close();
        return file;
    }


}
