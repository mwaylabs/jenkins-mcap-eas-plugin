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
import hudson.util.ListBoxModel;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.relution.net.Request;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;


@Extension
public class GlobalConfigurationImpl extends GlobalConfiguration {

    private List<GlobalConfigurationImpl> instances          = new ArrayList<GlobalConfigurationImpl>();
    private static final String           LOGIN_API_V1       = "/gofer/security-login";
    private static final String           USERNAME_PARAM     = "j_username";
    private static final String           ORGANIZATION_PARAM = "j_organization";
    private static final String           PASSWORD_PARAM     = "j_password";
    private final static String           LOGINREGEX         = "^http\\://[a-zA-Z0-9\\-\\.]*";
    private final static String           UUIDPattern        = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private final Map<String, String>     loginCredentials   = new HashMap<String, String>();
    private String                        apiEndpoint;
    private String                        apiUsername;
    private String                        apiPassword;
    private String                        proxyHost;
    private int                           proxyPort;
    private String                        apiOrganization;
    private String                        response;
    private String                        apiReleaseStatus;

    /**
     * executed during startup of the Plugin and instantiate different GlobalConfiguration-Objects.
     */
    public GlobalConfigurationImpl() {
        this.load();
    }

    /**
     * These constructor will be executed every time when the save/submit button will be triggered in the Jenkins. 
     * @param apiEndpoint URL to which the app should communicate
     * @param apiUsername User which should be logged in.
     * @param apiPassword Password for the given User.
     * @param apiOrganization Organization for the given User.
     * @param apiReleaseStatus ReleaseStatus to the given App
     * @param proxyHost 
     * @param proxyPort
     */
    @DataBoundConstructor
    public GlobalConfigurationImpl(final String apiEndpoint, final String apiUsername, final String apiPassword, final String apiOrganization,
            final String apiReleaseStatus, final String proxyHost, final int proxyPort) {
        super();
        this.load();

        this.setApiEndpoint(apiEndpoint);
        this.setApiUsername(apiUsername);
        this.setApiPassword(apiPassword);
        this.setApiOrganization(apiOrganization);
        this.setApiReleaseStatus(apiReleaseStatus);
        this.setProxyHost(proxyHost);
        this.setProxyPort(proxyPort);
    }

    /**
     * Performs on-the-fly validation of the form field 'name'.
     * @param value This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the browser.
     */
    public FormValidation doCheckApiEndpoint(@QueryParameter final String value)
            throws IOException, ServletException {
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
     * List of the Statuses to an App.
     * @return List with the statuses an app could have.
     */
    public ListBoxModel doFillApiReleaseStatusItems() {
        final ListBoxModel items = new ListBoxModel();
        items.add("DEVELOPMENT");
        items.add("RELEASE");
        items.add("REVIEW");
        return items;
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
     * @return List of all saved fields entered in the GlobalConfigurationscreen in Jenkins.
     */
    public List<GlobalConfigurationImpl> getInstances() {
        return this.instances;
    }

    /**
     * @param sets an new instances added in the GlobalConfigurationscreen.
     */
    public void setInstances(final List<GlobalConfigurationImpl> instances) {
        this.instances = instances;
    }

    /**
     * These method is responsible for the save the entered values in the GlobalConfigurationFields. 
     * @param req Request that should be send.
     * @param formData Represents all fields parsed into an JSONObject.
     * @return return value of the extended GlobalConfiguration Class.
     */
    @Override
    public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
        Object apiEndpoint;
        apiEndpoint = formData.get("apiEndpoint");
        if (apiEndpoint instanceof JSONArray) {
            this.instances.clear();
            this.loginCredentials.clear();
            final JSONArray jsonArray = formData.getJSONArray("apiEndpoint");
            for (int index = 0; index < jsonArray.size(); index++) {
                this.apiEndpoint = ((JSONObject) jsonArray.get(index)).get("apiEndpoint").toString();
                this.apiUsername = ((JSONObject) jsonArray.get(index)).get("apiUsername").toString();
                this.apiPassword = ((JSONObject) jsonArray.get(index)).get("apiPassword").toString();
                this.apiOrganization = ((JSONObject) jsonArray.get(index)).get("apiOrganization").toString();
                this.apiReleaseStatus = ((JSONObject) jsonArray.get(index)).get("apiReleaseStatus").toString();
                this.proxyHost = formData.getString("proxyHost");
                this.proxyPort = formData.getInt("proxyPort");
                this.instances.add(new GlobalConfigurationImpl(this.apiEndpoint, this.apiUsername, this.apiPassword, this.apiOrganization,
                        this.apiReleaseStatus,
                        this.proxyHost, this.proxyPort));
                this.loginCredentials.put(((JSONObject) jsonArray.get(index)).get("apiEndpoint").toString(), this.apiUsername + ":" + this.apiOrganization
                        + ":"
                        + this.apiPassword);
            }
        }
        else if (apiEndpoint instanceof JSONObject) {
            this.instances.clear();
            this.loginCredentials.clear();
            final JSONObject innerObject = (JSONObject) formData.get("apiEndpoint");
            this.apiEndpoint = innerObject.get("apiEndpoint").toString();
            this.apiUsername = innerObject.get("apiUsername").toString();
            this.apiPassword = innerObject.get("apiPassword").toString();
            this.apiOrganization = innerObject.get("apiOrganization").toString();
            this.apiReleaseStatus = innerObject.getString("apiReleaseStatus");
            this.proxyHost = formData.getString("proxyHost");
            this.proxyPort = formData.getInt("proxyPort");
            this.instances.add(new GlobalConfigurationImpl(this.apiEndpoint, this.apiUsername, this.apiPassword, this.apiOrganization, this.apiReleaseStatus,
                    this.proxyHost, this.proxyPort));
            this.loginCredentials.put(this.apiEndpoint, this.apiUsername + ":" + this.apiOrganization + ":" + this.apiPassword);
        }
        System.out.println(formData.toString());

        this.save();
        return super.configure(req, formData);
    }

    /**
     * Validates the given Credentials are right
     * @param endpoint URL to connect to
     * @param username User to connect to the specified URL
     * @param organization Organization for the given User
     * @param password Password for the given User
     * @return Prints an dedicated statement to the User
     * @throws IOException
     * @throws ServletException
     */
    public FormValidation doTestConnection(@QueryParameter("apiEndpoint") final String endpoint,
            @QueryParameter("apiUsername") final String username,
            @QueryParameter("apiOrganization") final String organization,
            @QueryParameter("apiPassword") final String password) throws IOException, ServletException {
        final Pattern p = Pattern.compile(LOGINREGEX);
        final Matcher m = p.matcher(endpoint);
        String loginURL = "";
        while (m.find()) {
            loginURL = m.group();
        }
        loginURL += LOGIN_API_V1 + "?" + USERNAME_PARAM + "=" + username + "&" + PASSWORD_PARAM + "=" + password + "&" + ORGANIZATION_PARAM + "="
                + organization;
        final Request request = new Request(Request.Method.GET, loginURL);
        final DefaultHttpClient client = new DefaultHttpClient();
        try {
            final HttpRequestBase httpRequest = request.createHttpRequest();
            this.response = EntityUtils.toString(client.execute(httpRequest).getEntity(), Charset.forName("UTF-8"));
        } catch (final URISyntaxException e) {
            return FormValidation.error(Messages.Relution_unsucessfullLogin() + endpoint +
                    " Errormessage: " + this.response);
        }
        if (this.response.equals("success")) {
            return FormValidation.ok(Messages.Relution_successfulLogin() + endpoint);
        }
        else {
            return FormValidation.error(Messages.Relution_unsucessfullLogin() + endpoint + " Errormessage: : " + this.response);
        }
    }

    /**
     * @return URL to which your app will connect.
     */
    public String getApiEndpoint() {
        return this.apiEndpoint;
    }

    /**
     * @param apiEndpointURL Communication endpoint to set.
     */
    public void setApiEndpoint(final String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    /**
     * @return Username used to login to relution.
     */
    public String getApiUsername() {
        return this.apiUsername;
    }

    /**
     * @param apiUsername Sets entry of  the textfield apiUsername.
     */
    public void setApiUsername(final String apiUsername) {
        this.apiUsername = apiUsername;
    }

    /**
     * @return Password relates to the Username.
     */
    public String getApiPassword() {
        return this.apiPassword;
    }

    /**
     * @param apiPassword Sets entry of  the textfield apiPassword.
     */
    public void setApiPassword(final String apiPassword) {
        this.apiPassword = apiPassword;
    }

    /**
     * @return Organization relates to the Username.
     */
    public String getApiOrganization() {
        return this.apiOrganization;
    }

    /**
     * @param apiOrganization Sets entry of  the textfield apiOrganization.
     */
    public void setApiOrganization(final String apiOrganization) {
        this.apiOrganization = apiOrganization;
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

    /**
     * @return Map with the entered login-Credentials.
     */
    public Map<String, String> getLoginCredentials() {
        return this.loginCredentials;
    }

    /**
     * @param apiReleaseStatus Sets entry of  the textfield apiReleaseStatus.
     */
    public void setApiReleaseStatus(final String apiReleaseStatus) {
        this.apiReleaseStatus = apiReleaseStatus;
    }

    /**
     * @return actual entered ReleaseStatus.
     */
    public String getApiReleaseStatus() {
        return this.apiReleaseStatus;
    }
}
