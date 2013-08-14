package org.jenkinsci.plugins.relution;

import hudson.FilePath.FileCallable;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.apache.http.ParseException;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.relution.entities.ApplicationInformation;
import org.jenkinsci.plugins.relution.entities.ShortApplicationInformation;
import org.jenkinsci.plugins.relution.json.APIObject;
import org.jenkinsci.plugins.relution.net.RequestFactory;

@SuppressWarnings("serial")
public class ApplicationFileCallable implements FileCallable<Boolean> {

	@SuppressWarnings("rawtypes")
	private final AbstractBuild build;
	private final BuildListener listener;
	private final Application application;
	private final List<RelutionCommunicator> communicators;
	
	/**
	 * Constructor.
	 * @param build Actual build number.
	 * @param listener Receives events that happen during a build.
	 * @param communicators List of all global specified communicators.
	 * @param application Job that will be build.
	 */
	@SuppressWarnings("rawtypes")
	public ApplicationFileCallable(final AbstractBuild build,
			final BuildListener listener, final List<RelutionCommunicator> communicators,
			final Application application) {
		this.build = build;
		this.listener = listener;
		this.application = application;
		this.communicators = communicators;
	}
	
	/**
	 * These method is responsible to get the file to upload against the relution store during the build process.
	 * 
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
	public Boolean invoke(final File f, final VirtualChannel channel) throws IOException, InterruptedException {
		ShortApplicationInformation app = null;
		final FileSet fileSet = Util.createFileSet(f, this.application.getApplicationFile());
		
		if (fileSet.getDirectoryScanner().getIncludedFilesCount() < 1) {
			this.listener.getLogger().println("[Relution Publisher]: Error, no files to deploy");
			this.build.setResult(Result.UNSTABLE);

			return false;
		}

		final File applicationFile = new File(fileSet.getDirectoryScanner().getBasedir().getAbsolutePath() + File.separator + fileSet.getDirectoryScanner().getIncludedFiles()[0]);
		try {
			// -----------------------------------------------------------------
			// if(this.application.getApplicationUUID() == null) {
			// this.application.setApplicationUUID(UUID.randomUUID().toString());
			// }
			app = new ShortApplicationInformation(UUID.randomUUID().toString());
			if (app == null || app.getUUID() == null) {
				this.listener.getLogger().println("[Relution Publisher]: Could not obtain application uuid");
				return false;
			}
			this.listener.getLogger().println("[Relution Publisher]: Obtained application UUID: " + app.getUUID());
			for(int index = 0; index < communicators.size(); index ++) {
				RelutionCommunicator communicator = communicators.get(index);
				if(communicator.getRequestFactory().getRelutionApiUrl().equals(this.application.getApiEndpointURL())) {
					// app = communicator.getShortApplicationInformation(this.application.getApplicationUUID());
					// -----------------------------------------------------------------
					final String uploadedAssetToken = communicator.uploadApplicationAsset("", applicationFile);
					// this.listener.getLogger().println("[Relution Publisher]: Uploaded application asset (token " + uploadedAssetToken + ")");
					// -----------------------------------------------------------------
					final String appObject = communicator.analyzeUploadedApplication(uploadedAssetToken, app.getUUID(), this.application.getApplicationIcon(), this.application.getApiReleaseStatus(), this.application.getApplicationName(), this.application.getApplicationReleaseNotes(), fileSet, communicator);
					if(appObject.equals("Version already exists. Please delete the old one to upload the same version again.")) {
						this.listener.getLogger().println("[Relution Publisher]: Version already exists. Please delete the old one to upload the same version again.");
						return false;
					}
					// -----------------------------------------------------------------
					final ApplicationInformation information = communicator.saveApplicationInformation(appObject);
					// this.listener.getLogger().println("[Relution Publisher]: Obtained information for uploaded application asset ( " + information + " )");
					this.listener.getLogger().println("[Relution Publisher]: Upload app with name " + applicationFile.getName());
				}
			}
			return true;
		} catch (final ParseException e) {
			e.printStackTrace();
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}

		return false;
	}

}
