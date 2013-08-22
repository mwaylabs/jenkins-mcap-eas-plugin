
package org.jenkinsci.plugins.relution.entities;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.relution.Messages;
import org.jenkinsci.plugins.relution.GlobalConfigurationImpl;
import org.jenkinsci.plugins.relution.net.Request;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletException;


public class ApiEndpoint extends AbstractDescribableImpl<ApiEndpoint> {

    public final static String  KEY_URL            = "url";
    public final static String  KEY_ORGANIZATION   = "organization";

    public final static String  KEY_USERNAME       = "username";
    public final static String  KEY_PASSWORD       = "password";

    private static final String LOGIN_API_V1       = "/gofer/security-login";
    private static final String USERNAME_PARAM     = "j_username";
    private static final String ORGANIZATION_PARAM = "j_organization";
    private static final String PASSWORD_PARAM     = "j_password";
    private static final String LOGINREGEX         = "^http\\://[a-zA-Z0-9\\-\\.]*";

    public final static String  KEY_RELEASE_STATUS = "releaseStatus";

    private String              mUrl;

    private String              mOrganization;

    private String              mUsername;
    private String              mPassword;

    private String              mReleaseStatus;

    public static ApiEndpoint fromJson(final String jsonString) {

        final JSONObject json = JSONObject.fromObject(jsonString);
        return new ApiEndpoint(json);
    }

    @DataBoundConstructor
    public ApiEndpoint(final String url, final String organization, final String username, final String password, final String releaseStatus) {

        this.setUrl(url);
        this.setOrganization(organization);

        this.setUsername(username);
        this.setPassword(password);

        this.setReleaseStatus(releaseStatus);
    }

    public ApiEndpoint(final JSONObject json) {

        this.setUrl(json.getString(KEY_URL));
        this.setOrganization(json.getString(KEY_ORGANIZATION));

        this.setUsername(json.getString(KEY_USERNAME));
        this.setPassword(json.getString(KEY_PASSWORD));

        this.setReleaseStatus(json.getString(KEY_RELEASE_STATUS));
    }

    public String getUrl() {
        return this.mUrl;
    }

    public void setUrl(final String url) {
        this.mUrl = url;
    }

    public String getOrganization() {
        return this.mOrganization;
    }

    public void setOrganization(final String organization) {
        this.mOrganization = organization;
    }

    public String getUsername() {
        return this.mUsername;
    }

    public void setUsername(final String username) {
        this.mUsername = username;
    }

    public String getPassword() {
        return this.mPassword;
    }

    public void setPassword(final String password) {
        this.mPassword = password;
    }

    public String getReleaseStatus() {
        return this.mReleaseStatus;
    }

    public void setReleaseStatus(final String releaseStatus) {
        this.mReleaseStatus = releaseStatus;
    }

    public String getAuthorizationToken() {

        final String authorization = this.mUsername + ":" + this.mOrganization + ":" + this.mPassword;
        return Base64.encodeBase64String(authorization.getBytes());
    }

    public JSONObject toJson() {

        final JSONObject json = new JSONObject();

        json.put(KEY_URL, this.mUrl);
        json.put(KEY_ORGANIZATION, this.mOrganization);

        json.put(KEY_USERNAME, this.mUsername);
        json.put(KEY_PASSWORD, this.mPassword);

        json.put(KEY_RELEASE_STATUS, this.mReleaseStatus);

        return json;
    }

    @Override
    public int hashCode() {

        final int a = (this.mUrl != null) ? this.mUrl.hashCode() : 0;
        final int b = (this.mOrganization != null) ? this.mOrganization.hashCode() : 0;
        final int c = (this.mUsername != null) ? this.mUsername.hashCode() : 0;

        return a ^ b ^ c;
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof ApiEndpoint) {
            final ApiEndpoint other = (ApiEndpoint) obj;
            return StringUtils.equals(this.mUrl, other.mUrl)
                    && StringUtils.equals(this.mOrganization, other.mOrganization)
                    && StringUtils.equals(this.mUsername, other.mUsername);
        }

        return false;
    }

    @Override
    public String toString() {

        String hostName = this.mUrl;

        try {
            final URI uri = new URI(this.mUrl);
            hostName = uri.getHost();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }

        return String.format(
                Locale.ENGLISH,
                "%s - %s@%s",
                hostName,
                this.mUsername,
                this.mOrganization);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ApiEndpoint> {

        /**
         * Necessary Object to read the values entered in the GlobalConfigurationScreen.
         */
        @Inject
        GlobalConfigurationImpl globalConfiguration;

        @Override
        public String getDisplayName() {
            return "API endpoint";
        }

        /**
         * List of the Statuses to an App.
         * @return List with the statuses an app could have.
         */
        public ListBoxModel doFillReleaseStatusItems() {
            final ListBoxModel items = new ListBoxModel();
            ReleaseStatus.fill(items);
            return items;
        }

        /**
         * Validates the given Credentials are right
         * @param url URL to connect to
         * @param username User to connect to the specified URL
         * @param organization Organization for the given User
         * @param password Password for the given User
         * @return Prints an dedicated statement to the User
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doTestConnection(
                @QueryParameter(ApiEndpoint.KEY_URL) final String url,
                @QueryParameter(ApiEndpoint.KEY_USERNAME) final String username,
                @QueryParameter(ApiEndpoint.KEY_ORGANIZATION) final String organization,
                @QueryParameter(ApiEndpoint.KEY_PASSWORD) final String password)
                throws IOException, ServletException {

            final Pattern p = Pattern.compile(LOGINREGEX);
            final Matcher m = p.matcher(url);
            String loginURL = "";

            while (m.find()) {
                loginURL = m.group();
            }
            loginURL += LOGIN_API_V1;
            loginURL += "?" + USERNAME_PARAM + "=" + username;
            loginURL += "&" + PASSWORD_PARAM + "=" + password;
            loginURL += "&" + ORGANIZATION_PARAM + "=" + organization;

            final Request request = new Request(Request.Method.GET, loginURL);
            final DefaultHttpClient client = new DefaultHttpClient();
            String response = "";

            try {
                final HttpRequestBase httpRequest = request.createHttpRequest();
                response = EntityUtils.toString(client.execute(httpRequest).getEntity(), Charset.forName("UTF-8"));
            } catch (final URISyntaxException e) {
                return FormValidation.error(Messages.Relution_unsucessfullLogin() + url + " Errormessage: " + response);
            }
            if (response.equals("success")) {
                return FormValidation.ok(Messages.Relution_successfulLogin() + url);
            }
            else {
                return FormValidation.error(Messages.Relution_unsucessfullLogin() + url + " Errormessage: : " + response);
            }
        }
    }
}
