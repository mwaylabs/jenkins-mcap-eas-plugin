/**
 * GlobalConfigurationImpl.java
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
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.validator.routines.UrlValidator;
import org.jenkinsci.plugins.relution.entities.ApiEndpoint;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;


@Extension
public class GlobalConfigurationImpl extends GlobalConfiguration {

    public final static String  KEY_ENDPOINT   = "endpoints";
    public final static String  KEY_PROXY_URL  = "proxyHost";
    public final static String  KEY_PROXY_PORT = "proxyPort";

    private static final String UUIDPattern    = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private List<ApiEndpoint>   endpoints      = new ArrayList<ApiEndpoint>();

    private String              proxyHost;
    private int                 proxyPort;

    /**
     * Initializes a new instance of the {@link GlobalConfigurationImpl} class.
     */
    public GlobalConfigurationImpl() {
        this.load();
    }

    /**
     * Performs on-the-fly validation of the form field 'name'.
     * @param value This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the browser.
     */
    public FormValidation doCheckApiEndpoint(@QueryParameter final String value) throws IOException, ServletException {

        if (value.length() == 0) {
            return FormValidation.error(Messages.Relution_apiEndpointUrlIsRequired());
        }

        final String[] schemes = {"http", "https"};
        final UrlValidator validator = new UrlValidator(schemes);

        if (!validator.isValid(value)) {
            return FormValidation.error(Messages.Relution_apiEndpointUrlIsInvalid());
        }

        return FormValidation.ok();
    }

    /**
     * Performs on-the-fly validation of the form field 'applicationFile'.
     * @param value This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the browser.
     */
    @SuppressWarnings("rawtypes")
    public FormValidation doCheckApplicationFile(@AncestorInPath final AbstractProject project, @QueryParameter final String value)
            throws IOException, ServletException {
        return FilePath.validateFileMask(project.getSomeWorkspace(), value);
    }

    /**
     * Performs on-the-fly validation of the form field 'applicationUUID'.
     * @param value This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the browser.
     */
    public FormValidation doCheckApplicationUUID(@QueryParameter final String value) {
        // Valid UUID: ac8a68b2-e2d1-459a-a6ff-3504a6f9c7ac
        if (!value.matches(UUIDPattern)) {
            return FormValidation.error(Messages.Relution_invalidApplicationUUID());
        }

        return FormValidation.ok();
    }

    /**
     * Returns true if this GlobalConfiguration type is applicable to the given project.
     */
    public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
        return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
        return Messages.Relution_displayName();
    }

    /**
     * Gets the list of configured {@link ApiEndpoint}s. 
     */
    public List<ApiEndpoint> getEndpoints() {
        return this.endpoints;
    }

    public void setEndpoints(final List<ApiEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Adds an endpoint to the configuration.
     * @param endpoint The {@link ApiEndpoint} to add.
     */
    public void add(final ApiEndpoint endpoint) {
        this.endpoints.add(endpoint);
    }

    /**
     * Removes an endpoint from the configuration.
     * @param endpoint The {@link ApiEndpoint} to remove.
     */
    public void remove(final ApiEndpoint endpoint) {
        this.endpoints.remove(endpoint);
    }

    /**
     * These method is responsible for the save the entered values in the GlobalConfigurationFields. 
     * @param req Request that should be send.
     * @param json Represents all fields parsed into an JSONObject.
     * @return return value of the extended GlobalConfiguration Class.
     */
    @Override
    public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
        System.out.println(json.toString());

        this.endpoints.clear();
        final Object value = json.get(KEY_ENDPOINT);

        if (value instanceof JSONArray) {
            final JSONArray endpoints = (JSONArray) value;

            for (int n = 0; n < endpoints.size(); n++) {
                final JSONObject endpoint = endpoints.getJSONObject(n);
                this.addEndpoint(endpoint);
            }

        } else if (value instanceof JSONObject) {
            final JSONObject endpoint = (JSONObject) value;
            this.addEndpoint(endpoint);
        }

        this.proxyHost = json.getString(KEY_PROXY_URL);
        this.proxyPort = json.optInt(KEY_PROXY_PORT, 0);

        this.save();
        return false;
    }

    private void addEndpoint(final JSONObject json) {
        final ApiEndpoint endpoint = new ApiEndpoint(json);
        this.endpoints.add(endpoint);
    }

    /**
     * @return ProxyHost necessary for remote debugging
     */
    public String getProxyHost() {
        return this.proxyHost;
    }

    /**
     * @param proxyHost Sets entry of  the textfield proxyHost.
     */
    public void setProxyHost(final String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * @return ProxyPort necessary for remote debugging.
     */
    public int getProxyPort() {
        return this.proxyPort;
    }

    /**
     * @param proxyPort Sets entry of  the textfield proxyPort.
     */
    public void setProxyPort(final int proxyPort) {
        this.proxyPort = proxyPort;
    }
}
