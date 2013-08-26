/**
 * RequestFactory.java
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

package org.jenkinsci.plugins.relution.net;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.relution.entities.ApiEndpoint;
import org.jenkinsci.plugins.relution.json.ApiApp;
import org.jenkinsci.plugins.relution.json.ApiVersion;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;


public class RequestFactory {

    private final static Charset CHARSET                   = Charset.forName("UTF-8");
    private final static String  URL_APP_STORE_ITEMS       = "apps";
    private final static String  URL_APP_STORE_APP_VERSION = "versions";
    private final static String  URL_TEMP_FILE             = "files";
    private final static String  URL_RESOURCE_ANALYZER     = "apps/fromFile";

    private BasicCookieStore     relutionCookieStore;
    private HttpHost             relutionProxyHost;
    private ApiEndpoint          endpoint;

    private PrintStream          logger;

    /**
     * send the request with org.apache.http
     * @param request request should be send
     * @return response after executing request
     * @throws URISyntaxException
     * @throws ParseException
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String send(final Request request) throws URISyntaxException, ParseException, ClientProtocolException, IOException {
        final DefaultHttpClient client = new DefaultHttpClient();

        try {
            if (this.relutionProxyHost != null) {
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, this.relutionProxyHost);
            }
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            client.setCookieStore(this.relutionCookieStore);

            final HttpRequestBase httpRequest = request.createHttpRequest();
            this.log("Request >>> %s", httpRequest.toString());

            final String response = EntityUtils.toString(client.execute(httpRequest).getEntity(), CHARSET);
            this.log("Response <<< %s", response);
            return response;

        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void setLogger(final PrintStream logger) {
        this.logger = logger;
    }

    private String getUrl(final String path, final String... subs) {

        final StringBuilder sb = new StringBuilder();
        final String url = this.endpoint.getUrl();
        sb.append(url);

        if (!url.endsWith("/")) {
            sb.append("/");
        }
        sb.append(path);

        for (final String sub : subs) {
            sb.append("/");
            sb.append(sub);
        }

        return sb.toString();
    }

    private Request getBaseRequest(final int method, final String path, final String... subs) {

        final String url = this.getUrl(path, subs);
        final Request request = new Request(method, url);
        request.addHeader("Accept", "application/json");
        request.addHeader("Authorization", "Basic " + this.endpoint.getAuthorizationToken());

        return request;
    }

    /**
     * With these created request you could retrieve the apps stored in the relution store.
     * @return json representation of the apps stored in the relution.
     */
    public Request createAppStoreItemsRequest() {
        final Request request = this.getBaseRequest(Request.Method.GET, URL_APP_STORE_ITEMS);
        request.queryFields().add("locale", "de");
        return request;
    }

    /**
     * Creates an request which could be send against the relution.
     * @param uploadToken	empty string
     * @param file			file which should be created
     * @return a new persisted APIObject File.
     */
    public Request createUploadRequest(final String uploadToken, final File file) {
        final Request request = this.getBaseRequest(Request.Method.POST, URL_TEMP_FILE, uploadToken);
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(file));
        request.setEntity(entity);

        this.log("createUploadRequest: uploadToken=%s, file=%s", uploadToken, file.getAbsolutePath());
        return request;
    }

    /**
     * Creates an request which could be send against the relution.
     * @param uploadToken empty String
     * @param UUID uuid of the Object which should be checked
     * @return APIObject App if the app not exists in the store or an error if the app already exists
     */
    public Request createAnalyzeUploadedApplication(final String uploadToken, final String UUID) {
        final Request request = this.getBaseRequest(Request.Method.POST, URL_RESOURCE_ANALYZER, uploadToken);

        this.log("createAnalyzeUploadedApplication: uploadToken=%s, UUID=%s", uploadToken, UUID);
        return request;
    }

    public Request createUploadedApplicationInformationRequest(final ApiApp app) {
        final Request request = this.getBaseRequest(Request.Method.POST, URL_APP_STORE_ITEMS);
        request.addHeader("Content-Type", "application/json");

        final StringEntity entity = new StringEntity(app.toString(), CHARSET);
        request.setEntity(entity);

        this.log("createUploadedApplicationInformationRequest: appObject=%s", app.toString());
        return request;
    }

    /**
     * Creates an request that could be send against the relution with an APIObject.
     * @param appObject The APIObject returned in an previous step 
     * @return request could be send aginst the relution to upload the appObject
     */
    public Request createUploadedVersionInformationRequest(final ApiVersion version) {
        final Request request = this.getBaseRequest(Request.Method.POST, URL_APP_STORE_ITEMS, version.appUuid, URL_APP_STORE_APP_VERSION);
        request.addHeader("Content-Type", "application/json");

        final StringEntity entity = new StringEntity(version.toString(), CHARSET);
        request.setEntity(entity);

        this.log("createUploadedApplicationInformationRequest: appObject=%s", version.toString());
        return request;
    }

    public ApiEndpoint getEndpoint() {
        return this.endpoint;
    }

    public void setEndpoint(final ApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return Password relates to the Username.
     */
    public String getRelutionPassword() {
        return this.endpoint.getPassword();
    }

    /**
     * @param relutionPassword Sets entry of  the textfield apiPassword.
     */
    public void setRelutionPassword(final String relutionPassword) {
        this.endpoint.setPassword(relutionPassword);
    }

    /**
     * @return actual entered ReleaseStatus.
     */
    public String getRelutionReleaseStatus() {
        return this.endpoint.getReleaseStatus();
    }

    /**
     * @param relutionReleaseStatus Sets entry of  the textfield apiReleaseStatus.
     */
    public void setRelutionReleaseStatus(final String relutionReleaseStatus) {
        this.endpoint.setReleaseStatus(relutionReleaseStatus);
    }

    /**
     * @return URL to which your app will connect.
     */
    public String getRelutionApiUrl() {
        return this.endpoint.getUrl();
    }

    /**
     * @param relutionApiUrl Communication endpoint to set.
     */
    public void setRelutionApiUrl(final String relutionApiUrl) {
        this.endpoint.setUrl(relutionApiUrl);
    }

    /**
     * @param relutionProxyHost HttpHost to be set.
     */
    public void setRelutionProxyHost(final HttpHost relutionProxyHost) {
        this.relutionProxyHost = relutionProxyHost;
    }

    /**
     * @param relutionCookieStore BasicCookieStore to set.
     */
    public void setRelutionCookieStore(final BasicCookieStore relutionCookieStore) {
        this.relutionCookieStore = relutionCookieStore;
    }

    /**
     * @return Username sed to login to relution.
     */
    public String getRelutionUsername() {
        return this.endpoint.getUsername();
    }

    /**
     * @param relutionUsername Sets entry of  the textfield apiUsername.
     */
    public void setRelutionUsername(final String relutionUsername) {
        this.endpoint.setUsername(relutionUsername);
    }

    /**
     * @return Organization relates to the Username.
     */
    public String getRelutionOrganization() {
        return this.endpoint.getOrganization();
    }

    /**
     * @param relutionOrganization Sets entry of  the textfield apiOrganization.
     */
    public void setRelutionOrganization(final String relutionOrganization) {
        this.endpoint.setOrganization(relutionOrganization);
    }

    private void log(final String format, final Object... args) {

        if (this.logger != null) {
            final String text = String.format(format, args);
            final String message = String.format("[Request Factory] %s", text);
            this.logger.println(message);
        }
    }
}
