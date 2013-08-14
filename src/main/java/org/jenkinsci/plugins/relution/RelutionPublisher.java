
package org.jenkinsci.plugins.relution;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jenkinsci.plugins.relution.net.RequestFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@SuppressWarnings("rawtypes")
public class RelutionPublisher extends Recorder {

    private List<Application>   applications = Collections.emptyList();
    private final static String UUIDPattern  = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private final static String LOGIN_REGEX = "([^:]*)";
    
    /**
     * Constructor.
     *  
     * @param applicationFile
     * @param applicationUUID
     */
    @DataBoundConstructor
    public RelutionPublisher(final List<Application> applications) {
    	this.getDescriptor().setInstances(applications);
    	this.applications = applications;
    }

    /**
     * Returns the application file path mask
     * 
     * @return String
     */
    public List<Application> getApplications() {
        return this.applications;
    }

    /**
     * These method is the main entry point for the build-execution
     * If the build with the actual number is already build without success the build will skipped
     * After checking the build success retrieve the build-workspace and configure the Communicator
     * The Communicator contains all necessary informations for the build and where to publish the file
     * The last step is a check of the file field which must be present, if not the build will marked unsuccessful
     * 
     * @param build 		Actual Build-Number
     * @param launcher		Starts the process
     * @param listener		The Listener is resposible for the Output in the jenkins Console-log
     */
    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
    	List<RelutionCommunicator> communicators = new ArrayList<RelutionCommunicator>();
    	Map<String, String> loginCredentials = this.getDescriptor().getGlobalConfiguration().getLoginCredentials();
        for(Application instance: applications) {
        	for(Map.Entry<String, String> entry: loginCredentials.entrySet()) {
        		if(entry.getKey().equals(instance.getApiEndpointURL())) {
        			String[] credentials = entry.getValue().split(":");
        			RelutionCommunicator communicator = new RelutionCommunicator(entry.getKey(), credentials[0], credentials[2], credentials[1], this.getDescriptor().getGlobalConfiguration().getProxyHost(), instance.getApiReleaseStatus(), this.getDescriptor().getGlobalConfiguration().getProxyPort(), new RequestFactory());
        			communicators.add(communicator);
        		}
        	}
        }
        
        if (build.getResult() != Result.SUCCESS) {
            listener.getLogger().println("[Relution Publisher]: Skipping due to unsuccessful build");
            return true;
        }
        
        final FilePath workspace = build.getWorkspace();
        for (final Application application : this.applications) {

            // Check that a file name pattern has been given
            if (StringUtils.isEmpty(application.getApplicationFile())) {
                build.setResult(Result.UNSTABLE);
                listener.getLogger().println("[Relution Publisher]: ERROR - No application file mask defined");

                return false;
            }

            // Check that a valid application uuid has been provided
            if (StringUtils.isEmpty(application.getApplicationFile()) || application.getApplicationFile().matches(UUIDPattern)) {
                build.setResult(Result.UNSTABLE);
                listener.getLogger().println("[Relution Publisher]: ERROR - No application file mask defined");

                return false;
            }

            final ApplicationFileCallable file = new ApplicationFileCallable(build, listener, communicators, application);
            
            workspace.act(file);            
        }

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Get an Monitor that monitors the whole build process
     */
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    /**
     * Descriptor for {@link RelutionPublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    	private List<Application> instances = new ArrayList<Application>();
        
    	/**
    	 * Necessary Object to read the values entered in the GlobalConfigurationScreen.
    	 */
        @Inject
        GlobalConfigurationImpl globalConfiguration;
        
        /**
         * @return List of all saved fields entered in the JobConfigurationscreen in Jenkins.
         */
        public List<Application> getInstances() {
            return instances;
    	}

        /**
         * @param sets an new instances added in the JobConfigurationscreen.
         */
    	public void setInstances(List<Application> instances) {
    		this.instances = instances; 
    	}
    	
        /**
         * load instance variables during startup.
         */
    	public DescriptorImpl() {
            this.load();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.Relution_displayName();
        }

        /**
         * @return Actual GlobalConfiguration that contributes to the system configuration page.
         */
        public GlobalConfigurationImpl getGlobalConfiguration() {
            return this.globalConfiguration;
        }
    }
}
