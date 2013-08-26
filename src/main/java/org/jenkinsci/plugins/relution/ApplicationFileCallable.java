/**
 * ApplicationFileCallable.java
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

import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;

import org.apache.commons.lang.StringUtils;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.relution.entities.ApplicationInformation;
import org.jenkinsci.plugins.relution.entities.ShortApplicationInformation;
import org.jenkinsci.plugins.relution.json.ApiApp;
import org.jenkinsci.plugins.relution.json.ApiFile;
import org.jenkinsci.plugins.relution.json.ApiResponse;
import org.jenkinsci.plugins.relution.json.ApiVersion;
import org.jenkinsci.plugins.relution.json.UploadResponse;
import org.jenkinsci.plugins.relution.net.Request;
import org.jenkinsci.plugins.relution.net.RequestFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;


@SuppressWarnings("serial")
public class ApplicationFileCallable implements FileCallable<Boolean> {

    @SuppressWarnings("rawtypes")
    private final AbstractBuild              build;
    private final BuildListener              listener;
    private final Application                application;
    private final List<RelutionCommunicator> communicators;

    /**
     * Constructor.
     * @param build Actual build number.
     * @param listener Receives events that happen during a build.
     * @param communicators List of all global specified communicators.
     * @param application Job that will be build.
     */
    @SuppressWarnings("rawtypes")
    public ApplicationFileCallable(final AbstractBuild build, final BuildListener listener, final List<RelutionCommunicator> communicators,
            final Application application) {

        this.build = build;
        this.listener = listener;
        this.application = application;
        this.communicators = communicators;
    }

    /**
     * Uploads the artifact (e.g. binary file) produced by a build process to the Relution store
     * after the build process completes. If the specified {@link File} does not exist, the build
     * is considered as <code>not built</code>.
     * <p/>
     * After existence of the file has been verified, a {@link ShortApplicationInformation} object
     * will be retrieved. 
     * <p>
     * If the file could not be found the build will marked as failed!
     * After the file is found retrieving short Information of the file 
     * In the last steps different request's will be created to
     * - after creating the first request an APIObject File Object will be returned in the reponse
     * - in the second step an request will created to get an APIObject App
     *   - Returns the existing APIObject App if resource with package name (internalName) already exists.
     *   - Returns an error if APIObject Version with versionCode already exists.
     * - the last step will upload the APIObject App created in step 2
     * @param f Holds information about path the project relates to.
     * @param channel Represents a communication channel to the remote/local peer.
     * @return true if build executes fine, false else
     */
    @Override
    public Boolean invoke(final File f, final VirtualChannel channel) throws IOException, InterruptedException {

        try {
            final FileSet fileSet = Util.createFileSet(f, this.application.getApplicationFile());

            // If the file does not exist, consider the build as "not built" 
            if (fileSet.getDirectoryScanner().getIncludedFilesCount() < 1) {
                this.log("The configured application file does not exist, no files to deploy");
                this.build.setResult(Result.NOT_BUILT);
                return false;
            }

            final File applicationFile = this.getApplicationFile(fileSet);

            this.log("Retrieving communicator for endpoint '%s'...", this.application.getEndpoint().toString());
            final RelutionCommunicator communicator = this.getCommunicator(this.application);

            if (communicator == null) {
                this.log("Failed to retrieve communicator.");
                this.build.setResult(Result.FAILURE);
                return false;
            }
            final RequestFactory requestFactory = communicator.getRequestFactory();
            requestFactory.setLogger(this.listener.getLogger());

            this.log("Communicator retrieved, will upload to %s", requestFactory.getRelutionApiUrl());

            this.log("Preparing to deploy '%s', retrieving application UUID...", applicationFile.getName());
            final ShortApplicationInformation info = new ShortApplicationInformation(UUID.randomUUID().toString());

            if (info == null || info.getUUID() == null) {
                this.log("Failed to obtain application UUID");
                this.build.setResult(Result.FAILURE);
                return false;
            }
            this.log("Obtained application UUID {%s}", info.getUUID());

            this.log("Upload application asset and retrieve token...");
            final ApiFile file = this.uploadApplicationAsset(requestFactory, "", applicationFile);

            if (file == null || StringUtils.isBlank(file.uuid)) {
                this.log("Upload failed, returned token is empty");
                this.build.setResult(Result.FAILURE);
                return false;
            }
            this.log("Token {%s} for application asset acquired", file.uuid);

            this.log("Retrieving application object...");
            final Request request = requestFactory.createAnalyzeUploadedApplication(file.uuid, info.getUUID());

            final String json = requestFactory.send(request);
            final ApiResponse response = ApiResponse.fromJson(json);

            if (response.status != 0) {
                this.log("Error retrieving application object: %s", response.message);
                this.build.setResult(Result.FAILURE);
                return false;
            }

            final ApiApp app = this.getApp(response, file.uuid);

            if (app == null) {
                this.log("Failed to retrieve application object.");
                this.build.setResult(Result.FAILURE);
                return false;
            }

            final ApiVersion version = this.getVersion(app, file.uuid);

            if (version == null) {
                this.log("Failed to retrieve version object.");
                this.build.setResult(Result.FAILURE);
                return false;
            }

            if (!StringUtils.isBlank(this.application.getApplicationName())) {
                for (final String key : version.name.keySet()) {
                    version.name.put(key, this.application.getApplicationName());
                }
            }

            if (!StringUtils.isBlank(this.application.getApiReleaseStatus())) {
                version.releaseStatus = this.application.getApiReleaseStatus();
            }

            if (!StringUtils.isBlank(this.application.getApplicationIcon())) {
                final ApiFile icon = this.upload(requestFactory, f, this.application.getApplicationIcon());
                version.icon = icon;
            }

            this.log("Application object retrieved:\n%s", version.toString());

            this.log("Saving application information...");
            ApplicationInformation information;

            if (app.uuid == null) {
                information = this.saveApplicationInformation(requestFactory, app);
            } else {
                version.appUuid = app.uuid;
                information = this.saveVersionInformation(requestFactory, version);
            }

            if (information == null || !information.getPublished()) {
                this.log("Failed to save application information.");
                this.build.setResult(Result.FAILURE);
                return false;
            }

            this.log("Application information saved");
            this.build.setResult(Result.SUCCESS);
            return true;

        } catch (final Exception e) {
            this.build.setResult(Result.FAILURE);
            this.log(e.toString());
            e.printStackTrace();
        }
        return false;
    }

    private File getApplicationFile(final FileSet fileSet) {

        final String dir = fileSet.getDirectoryScanner().getBasedir().getAbsolutePath();
        final String name = fileSet.getDirectoryScanner().getIncludedFiles()[0];
        final String path = String.format("%s%s%s", dir, File.separator, name);

        return new File(path);
    }

    private RelutionCommunicator getCommunicator(final Application application) {

        for (int index = 0; index < this.communicators.size(); index++) {
            final RelutionCommunicator communicator = this.communicators.get(index);

            if (communicator.hasEndpoint(application.getEndpoint())) {
                return communicator;
            }
        }
        return null;
    }

    private ApiApp getApp(final ApiResponse response, final String token) {

        for (final ApiApp app : response.results) {
            for (final ApiVersion version : app.versions) {
                if (version.uuid == null && version.file != null && token.equals(version.file.uuid)) {
                    return app;
                }
            }
        }
        return null;
    }

    private ApiVersion getVersion(final ApiApp app, final String token) {

        for (final ApiVersion version : app.versions) {
            if (version.uuid == null && version.file != null && token.equals(version.file.uuid)) {
                return version;
            }
        }
        return null;
    }

    private ApiFile upload(final RequestFactory requestFactory, final File baseDir, final String path)
            throws ParseException, ClientProtocolException, URISyntaxException, IOException {

        final FileSet fileSet = Util.createFileSet(baseDir, path);

        // If the file does not exist, consider the build as "not built" 
        if (fileSet.getDirectoryScanner().getIncludedFilesCount() < 1) {
            this.log("The configured application file does not exist, no files to deploy");
            this.build.setResult(Result.NOT_BUILT);
            return null;
        }

        final File file = this.getApplicationFile(fileSet);
        return this.uploadApplicationAsset(requestFactory, "", file);
    }

    private ApiFile uploadApplicationAsset(final RequestFactory requestFactory, final String uploadToken, final File file)
            throws URISyntaxException, ParseException, ClientProtocolException, IOException {

        final Request request = requestFactory.createUploadRequest(uploadToken, file);
        final String json = requestFactory.send(request);
        final UploadResponse response = UploadResponse.fromJson(json);

        if (response.status != 0) {
            return null;
        }

        return response.results.get(0);
    }

    private ApplicationInformation saveApplicationInformation(final RequestFactory requestFactory, final ApiApp app)
            throws URISyntaxException, ParseException, ClientProtocolException, IOException {

        final Request request = requestFactory.createUploadedApplicationInformationRequest(app);
        final String json = requestFactory.send(request);
        final ApiResponse response = ApiResponse.fromJson(json);

        System.out.println("[uploadedApplicationInformation] " + response.toString());

        final ApplicationInformation information = new ApplicationInformation();
        information.setPublished(response.status == 0);
        return information;
    }

    private ApplicationInformation saveVersionInformation(final RequestFactory requestFactory, final ApiVersion version)
            throws URISyntaxException, ParseException, ClientProtocolException, IOException {

        final Request request = requestFactory.createUploadedVersionInformationRequest(version);
        final String json = requestFactory.send(request);
        final ApiResponse response = ApiResponse.fromJson(json);

        System.out.println("[uploadedApplicationInformation] " + response.toString());

        final ApplicationInformation information = new ApplicationInformation();
        information.setPublished(response.status == 0);
        return information;
    }

    private void log(final String format, final Object... args) {

        final String text = String.format(format, args);
        final String message = String.format("[Relution Publisher] %s", text);
        this.listener.getLogger().println(message);
    }
}
