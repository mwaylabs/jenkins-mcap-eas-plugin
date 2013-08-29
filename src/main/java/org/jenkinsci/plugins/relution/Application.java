/**
 * Application.java
 * jenkins-mcap-meas-plugin
 * 
 * Created by Christian Steiger on 14.08.2013
 * Copyright (c)
 * 2013
 * M-Way Solutions GmbH. All rights reserved.
 * http://www.mwaysolutions.com
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.jenkinsci.plugins.relution;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import org.jenkinsci.plugins.relution.entities.ReleaseStatus;
import org.jenkinsci.plugins.relution.entities.ApiEndpoint;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

import javax.inject.Inject;


public class Application extends AbstractDescribableImpl<Application> {

    private String      applicationFile;
    private String      applicationIcon;
    private String      apiReleaseStatus;

    private String      apiEndpointJson;
    private ApiEndpoint apiEndpoint;

    private String      applicationName;
    private String      applicationReleaseNotes;
    private String      applicationDescription;

    /**
     * These constructor will be executed every time when the save/submit button will be triggered in the Jenkins. 
     * @param apiEndpointURL URL to which the app should communicate.
     * @param applicationFile String representation of the File which should be uploaded.
     * @param applicationIcon String Representation of the Icon which should be used for the uploaded app.
     * @param apiReleaseStatus String Representation of the ReleaseState to the app will be published.
     */
    @DataBoundConstructor
    public Application(final String apiEndpoint, final String applicationFile, final String applicationIcon, final String apiReleaseStatus,
            final String applicationName, final String applicationReleaseNotes, final String applicationDescription) {

        this.setApiEndpoint(apiEndpoint);
        this.setApplicationFile(applicationFile);
        this.setApplicationIcon(applicationIcon);
        this.setApiReleaseStatus(apiReleaseStatus);
        this.setApplicationName(applicationName);
        this.setApplicationReleaseNotes(applicationReleaseNotes);
        this.setApplicationDescription(applicationDescription);

        System.out.println("Initialized application " + this.toString());
    }

    /**
     * @return URL to which your app will connect.
     */
    public String getApiEndpoint() {
        return this.apiEndpointJson;
    }

    /**
     * @param apiEndpointURL Communication endpoint to set.
     */
    public void setApiEndpoint(final String jsonString) {
        this.apiEndpointJson = jsonString;
        this.apiEndpoint = ApiEndpoint.fromJson(jsonString);
    }

    public ApiEndpoint getEndpoint() {
        return this.apiEndpoint;
    }

    /**
     * @return File which will be uploaded to relution.
     */
    public String getApplicationFile() {
        return this.applicationFile;
    }

    /**
     * @param applicationFile File that should be uploaded to relution.
     */
    public void setApplicationFile(final String applicationFile) {
        this.applicationFile = applicationFile;
    }

    /**
     * @return Pictures that will be uploaded to relution and represents your app.
     */
    public String getApplicationIcon() {
        return this.applicationIcon;
    }

    /**
     * @param applicationIcon Picture that should represent your app in relution.
     */
    public void setApplicationIcon(final String applicationIcon) {
        this.applicationIcon = applicationIcon;
    }

    /**
     * @return Publsih your app with the current releasestate.
     */
    public String getApiReleaseStatus() {
        return this.apiReleaseStatus;
    }

    /**
     * @param apiReleaseStatus ReleaseState that your app will have after upload to relution.
     */
    public void setApiReleaseStatus(final String apiReleaseStatus) {
        this.apiReleaseStatus = apiReleaseStatus;
    }

    /**
     * The Name of these app corresponds to the file which will be uploaded.
     * If the textfield in the Job-Configuration is not null your app will be represented by these name
     * @return The Name of the app in relution store.
     */
    public String getApplicationName() {
        return this.applicationName;
    }

    /**
     * @param applicationName Name of the new uploaded app
     */
    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * @return Content of the Log-File which will be set in the TextField.
     */
    public String getApplicationReleaseNotes() {
        return this.applicationReleaseNotes;
    }

    /**
     * @param applicationReleaseNotes Content that should be set to the app.
     */
    public void setApplicationReleaseNotes(final String applicationReleaseNotes) {
        this.applicationReleaseNotes = applicationReleaseNotes;
    }

    /**
     * @return Content of the Descriptopm-File which will be set in the TextField.
     */
    public String getApplicationDescription() {
        return this.applicationDescription;
    }

    /**
     * @param applicationDescription Content that should be set to the app.
     */
    public void setApplicationDescription(final String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    @Override
    public String toString() {

        return String.format("%s -> %s",
                this.applicationFile,
                this.apiEndpoint.toString());
    }

    /**
     * Descriptor for {@link DescriptorImpl}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<Application> {

        /**
         * Necessary Object to read the values entered in the GlobalConfigurationScreen.
         */
        @Inject
        GlobalConfigurationImpl globalConfiguration;

        @Override
        public String getDisplayName() {
            return "";
        }

        public FormValidation doCheckApplicationFile(@QueryParameter final String value) {
            return FormValidation.error("This plugin is deprecated, please use the new version.");
        }

        /**
         * Fills the DropDownList on the configuration page of an job. The values are read out of the GlobalConfiguration.
         * @return List of URLs which are entered in the GlobalConfigurationScreen
         */
        public ListBoxModel doFillApiEndpointItems() {
            final List<ApiEndpoint> endpoints = this.globalConfiguration.getEndpoints();
            final ListBoxModel items = new ListBoxModel();

            try {
                for (final ApiEndpoint endpoint : endpoints) {
                    items.add(endpoint.toString(), endpoint.toJson().toString());
                }
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
            return items;
        }

        /**
         * @return List of ReleaseStatuses that the actual app could have.
         */
        public ListBoxModel doFillApiReleaseStatusItems() {
            final ListBoxModel items = new ListBoxModel();
            ReleaseStatus.fill(items);
            return items;
        }
    }
}
