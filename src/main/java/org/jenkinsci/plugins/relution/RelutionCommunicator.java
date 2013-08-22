/**
 * RelutionCommunicator.java
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.relution.entities.ApiEndpoint;
import org.jenkinsci.plugins.relution.entities.ApplicationInformation;
import org.jenkinsci.plugins.relution.json.APIObject;
import org.jenkinsci.plugins.relution.net.Request;
import org.jenkinsci.plugins.relution.net.RequestFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;


public class RelutionCommunicator {

    private RequestFactory requestFactory;

    /**
     * setting an Factory for an specific communicator
     * @param requestFactory
     */
    public void setRequestFactory(final RequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    /**
     * getting the Factory an specific communicator
     * @return getting the Factory an specific communicator
     */
    public RequestFactory getRequestFactory() {
        return this.requestFactory;
    }

    /**
     * Constructor to create a Communicator for Relution.
     * @param apiEndpoint URL to which the communcator connects
     * @param apiUsername login with the specified User 
     * @param apiPassword login with the specified Password
     * @param apiOrganization login with the specified Organization
     * @param apiReleaseStatus 
     * @param proxyHost necessary Host for remote debugging
     * @param proxyPort necessary Port for remote debugging
     * @param requestFactory handles the creation of the requests
     */
    public RelutionCommunicator(final ApiEndpoint endpoint, final String proxyHost, final int proxyPort, final RequestFactory requestFactory) {
        requestFactory.setEndpoint(endpoint);
        requestFactory.setRelutionCookieStore(new BasicCookieStore());

        if (proxyHost != null && !StringUtils.isEmpty(proxyHost)
                && proxyPort > 0) {
            requestFactory.setRelutionProxyHost(new HttpHost(proxyHost, proxyPort));
        }
        this.setRequestFactory(requestFactory);
    }

    /**
     * Upload the application file and return the token for analyzing
     * 
     * @param uploadToken
     * @param file
     * @return String
     * @throws URISyntaxException
     * @throws ParseException
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String uploadApplicationAsset(final String uploadToken,
            final File file) throws URISyntaxException, ParseException,
            ClientProtocolException, IOException {

        final Request request = this.getRequestFactory().createUploadRequest(uploadToken, file);
        final String response = this.getRequestFactory().send(request);

        System.out.println("[uploadApplicationAsset] " + response);

        final JsonParser parse = new JsonParser();
        final JsonObject object = (JsonObject) parse.parse(response);
        final JsonArray apps = object.getAsJsonArray("results");
        String uuid = "";
        for (int i = 0; i < apps.size(); i++) {
            final JsonObject jsonObject = (JsonObject) apps.get(i);
            uuid = jsonObject.get("uuid").getAsString();
        }

        return uuid.length() > 0 ? uuid : null;
    }

    /**
     * Analyze the uploaded application
     * 
     * @param uploadToken
     * @param UUID unique ID of the App that should be uploaded.
     * @return Json Object that could be uploaded to the mcap. 
     * @throws URISyntaxException
     * @throws ParseException 
     * @throws ClientProtocolException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException 
     */
    public String analyzeUploadedApplication(final String uploadToken,
            final String UUID,
            final String appIcon,
            final String releaseStatus,
            final String appName,
            final String appReleaseNotes,
            final String appDescription,
            final FileSet fileSet,
            final RelutionCommunicator communicator) throws URISyntaxException, ParseException,
            ClientProtocolException, IOException, IllegalArgumentException, IllegalAccessException {

        final Request request = this.getRequestFactory().createAnalyzeUploadedApplication(uploadToken, UUID);
        final String response = this.getRequestFactory().send(request);

        System.out.println("[analyzeUploadedApplication] " + response);

        final JsonParser jsonStatusParser = new JsonParser();
        final JsonObject jsonStatusObject = (JsonObject) jsonStatusParser.parse(response);
        final String status = jsonStatusObject.get("status").getAsString();
        if (status.equals("-1")) {
            return jsonStatusObject.get("message").getAsString();
        }
        else {
            String changedApp = null;
            if (appName != null && !appName.equals("")) {
                final APIObject.Name apiObjectName = new APIObject.Name();
                apiObjectName.setDe_DE(appName);
                apiObjectName.setEn_US(appName);
                changedApp = changeAPIObject(response, apiObjectName, null, null);
            }
            final JsonParser jsonReleaseStatusParser = new JsonParser();
            if (changedApp != null) {
                if (releaseStatus != null && !releaseStatus.equals("")) {
                    changedApp = changeAPIObject(changedApp, new APIObject.Versions(), "releaseStatus", releaseStatus);
                }
            }
            else {
                if (releaseStatus != null && !releaseStatus.equals("")) {
                    changedApp = changeAPIObject(response, new APIObject.Versions(), "releaseStatus", releaseStatus);
                }
            }
            if (appIcon != null && !appIcon.equals("") ||
                    appReleaseNotes != null && !appReleaseNotes.equals("") ||
                    appDescription != null && !appDescription.equals("")) {
                File applicationIconFile = null;
                File applicationReleaseNotes = null;
                File applicationDescription = null;
                final DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir(fileSet.getDir());
                scanner.setCaseSensitive(false);
                scanner.scan();
                final String[] includedFiles = scanner.getIncludedFiles();
                for (int i = 0; i < includedFiles.length; i++) {
                    if (includedFiles[i].equals(appIcon)) {
                        applicationIconFile = new File(scanner.getBasedir().getAbsolutePath() + File.separator + includedFiles[i]);
                        final String uploadedIconUUID = communicator.uploadApplicationAsset("", applicationIconFile);
                        final Request iconRequest = communicator.getRequestFactory().createUploadRequest(uploadedIconUUID, applicationIconFile);
                        final String iconResponse = communicator.getRequestFactory().send(iconRequest);

                        //parse the icon out of the json returned by the communicator
                        final Type collectionType = new TypeToken<Collection<APIObject.Icon>>() {}.getType();
                        final Collection<APIObject.Icon> uploadedIconCollection = new Gson().fromJson(
                                ((JsonObject) new JsonParser().parse(iconResponse)).get("results").toString(), collectionType);
                        final APIObject.Icon changeIcon = (APIObject.Icon) uploadedIconCollection.toArray()[0];
                        changedApp = changeAPIObject(changedApp, changeIcon, null, null);
                    }
                    if (includedFiles[i].equals(appReleaseNotes)) {
                        applicationReleaseNotes = new File(scanner.getBasedir().getAbsolutePath() + File.separator + includedFiles[i]);
                        String log = download("file://" + applicationReleaseNotes.getAbsolutePath());
                        if (log.length() > 256) {
                            log = log.substring(0, 252) + "...";
                        }
                        final APIObject.Changelog changeLog = new APIObject.Changelog();
                        changeLog.setDe_DE(log);
                        changeLog.setEn_US(log);
                        changedApp = changeAPIObject(changedApp, changeLog, null, null);
                    }
                    if (includedFiles[i].equals(appDescription)) {
                        applicationDescription = new File(scanner.getBasedir().getAbsolutePath() + File.separator + includedFiles[i]);
                        String description = download("file://" + applicationDescription.getAbsolutePath());
                        if (description.length() > 256) {
                            description = description.substring(0, 252) + "...";
                        }
                        final APIObject.Description changeDescription = new APIObject.Description();
                        changeDescription.setDe_DE(description);
                        changeDescription.setEn_US(description);
                        changedApp = changeAPIObject(changedApp, changeDescription, null, null);
                    }
                }
            }
            final JsonObject jsonReleaseStatusObject = (JsonObject) jsonReleaseStatusParser.parse(changedApp);
            final JsonArray results = jsonReleaseStatusObject.getAsJsonArray("results");
            return results.get(0).toString();
        }
    }

    private static String changeAPIObject(final String app, final Object object, final String fieldName, final String newFieldValue)
            throws IllegalArgumentException,
            IllegalAccessException {
        final Gson gson = new Gson();
        final APIObject apiObject = gson.fromJson(app, APIObject.class);
        for (int index = 0; index < apiObject.getResults().size(); index++) {
            final APIObject.Results result = apiObject.getResults().get(index);
            if (object instanceof APIObject.Versions) {
                final APIObject.Versions objectVersion = result.getVersions().get(index);
                for (final Field field : objectVersion.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    final String name = field.getName();
                    if (name.equals(fieldName)) {
                        field.set(objectVersion, newFieldValue);
                    }
                    else {
                        final Object value = field.get(objectVersion);
                        field.set(objectVersion, value);
                    }
                }
            }
            for (int j = 0; j < result.getVersions().size(); j++) {
                final APIObject.Versions version = result.getVersions().get(j);
                if (object instanceof APIObject.Description) {
                    version.setDescription((APIObject.Description) object);
                }
                if (object instanceof APIObject.Changelog) {
                    version.setChangelog((APIObject.Changelog) object);
                }
                if (object instanceof APIObject.Name) {
                    version.setName((APIObject.Name) object);
                }
                if (object instanceof APIObject.Icon) {
                    version.setIcon((APIObject.Icon) object);
                }
            }
        }
        return gson.toJson(apiObject);
    }

    private static String download(final String url) throws java.io.IOException {
        java.io.InputStream s = null;
        String content = null;
        try {
            s = (java.io.InputStream) new URL(url).getContent();
            content = IOUtils.toString(s, "UTF-8");

        } finally {
            if (s != null) {
                s.close();
            }
        }
        return content.toString();
    }

    /**
     * Obtain information stored in the app store for the given UUID
     * 
     * @param UUID
     * @return ApplicationInformation
     * @throws URISyntaxException
     * @throws ParseException
     * @throws ClientProtocolException
     * @throws IOException
     */
    public ApplicationInformation saveApplicationInformation(final String appObject) throws URISyntaxException, ParseException,
            ClientProtocolException, IOException {

        final Request request = this.getRequestFactory().createUploadedApplicationInformationRequest(appObject);
        final String response = this.getRequestFactory().send(request);

        System.out.println("[uploadedApplicationInformation] " + response);

        final JsonParser jsonParser = new JsonParser();
        final JsonObject jsonObject = (JsonObject) jsonParser.parse(response);
        final String message = jsonObject.get("message").getAsString();
        final ApplicationInformation information = new ApplicationInformation();
        if (message.equals("Application was created successfully")) {
            information.setPublished(true);
        }
        else {
            information.setPublished(false);
        }
        return information;
    }

    public boolean hasEndpoint(final ApiEndpoint endpoint) {
        return this.requestFactory.getEndpoint().equals(endpoint);
    }

    @Override
    public String toString() {

        String hostName = this.requestFactory.getRelutionApiUrl();

        try {
            final URI uri = new URI(this.requestFactory.getRelutionApiUrl());
            hostName = uri.getHost();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }

        return String.format(
                Locale.ENGLISH,
                "%s — %s@%s",
                hostName,
                this.requestFactory.getRelutionUsername(),
                this.requestFactory.getRelutionOrganization());
    }
}