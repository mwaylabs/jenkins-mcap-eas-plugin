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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;

import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.relution.entities.ApplicationInformation;
import org.jenkinsci.plugins.relution.entities.ShortApplicationInformation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;


@SuppressWarnings("serial")
public class ApplicationFileCallable implements FileCallable<Boolean> {

    private static final String              ERROR_VERSION_ALREADY_EXISTS = "Version already exists. Please delete the old one to upload the same version again.";

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
            this.log("Communicator retrieved, will upload to %s", communicator.getRequestFactory().getRelutionApiUrl());

            this.log("Preparing to deploy '%s', retrieving application UUID...", applicationFile.getName());
            final ShortApplicationInformation app = new ShortApplicationInformation(UUID.randomUUID().toString());

            if (app == null || app.getUUID() == null) {
                this.log("Failed to obtain application UUID");
                this.build.setResult(Result.FAILURE);
                return false;
            }
            this.log("Obtained application UUID {%s}", app.getUUID());

            this.log("Upload application asset and retrieve token...");
            final String uploadedAssetToken = communicator.uploadApplicationAsset("", applicationFile);

            if (uploadedAssetToken == null || uploadedAssetToken.length() == 0) {
                this.log("Upload failed, returned token is empty");
                this.build.setResult(Result.FAILURE);
                return false;
            }
            this.log("Token {%s} for application asset acquired", uploadedAssetToken);

            this.log("Retrieving application object...");
            final String appObject = communicator.analyzeUploadedApplication(
                    uploadedAssetToken,
                    app.getUUID(),
                    this.application.getApplicationIcon(),
                    this.application.getApiReleaseStatus(),
                    this.application.getApplicationName(),
                    this.application.getApplicationReleaseNotes(),
                    this.application.getApplicationDescription(),
                    fileSet,
                    communicator);

            if (appObject == null || appObject.length() == 0) {
                this.log("Failed to retrieve application object.");
                this.build.setResult(Result.FAILURE);
                return false;
            }

            if (appObject.equals(ERROR_VERSION_ALREADY_EXISTS)) {
                this.log("A file with the same version already exists. You must remove this version before you can upload it again.");
                this.build.setResult(Result.UNSTABLE);
                return false;
            }

            final JsonParser parser = new JsonParser();
            final JsonElement element = parser.parse(appObject);
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            this.log("Application object retrieved:\n%s", gson.toJson(element));

            this.log("Saving application information...");
            final ApplicationInformation information = communicator.saveApplicationInformation(appObject);

            if (information == null) {
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

    private void log(final String format, final Object... args) {

        final String text = String.format(format, args);
        final String message = String.format("[Relution Publisher] %s", text);
        this.listener.getLogger().println(message);
    }
}
