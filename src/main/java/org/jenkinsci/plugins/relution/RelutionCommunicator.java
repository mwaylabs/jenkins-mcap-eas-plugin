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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.relution.entities.ApplicationInformation;
import org.jenkinsci.plugins.relution.json.APIObject;
import org.jenkinsci.plugins.relution.net.Request;
import org.jenkinsci.plugins.relution.net.RequestFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

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
	public RelutionCommunicator(final String apiEndpoint, final String apiUsername,
			final String apiPassword, final String apiOrganization, 
			final String proxyHost, final String apiReleaseStatus, final int proxyPort, final RequestFactory requestFactory) {
		requestFactory.setRelutionApiUrl(apiEndpoint);
		requestFactory.setRelutionUsername(apiUsername);
		requestFactory.setRelutionPassword(apiPassword);
		requestFactory.setRelutionOrganization(apiOrganization);
		requestFactory.setRelutionReleaseStatus(apiReleaseStatus);
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

		JsonParser parse = new JsonParser();
        JsonObject object = (JsonObject) parse.parse(response);
        JsonArray apps = object.getAsJsonArray("results");
        String uuid = "";
        for (int i = 0; i < apps.size(); i++) {
            JsonObject jsonObject = (JsonObject) apps.get(i);
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
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ParseException 
	 */
	public String analyzeUploadedApplication(final String uploadToken, final String UUID, final String appIcon, final String releaseStatus, final String appName, final String appReleaseNotes, final FileSet fileSet, final RelutionCommunicator communicator) throws URISyntaxException, ParseException,
			ClientProtocolException, IOException {

		final Request request = this.getRequestFactory().createAnalyzeUploadedApplication(uploadToken, UUID);
		final String response = this.getRequestFactory().send(request);

		System.out.println("[analyzeUploadedApplication] " + response);
		
		JsonParser jsonStatusParser = new JsonParser();
		JsonObject jsonStatusObject = (JsonObject) jsonStatusParser.parse(response);
		String status = jsonStatusObject.get("status").getAsString();
		if(status.equals("-1")) {
			return jsonStatusObject.get("message").getAsString();
		}
		else {
			String changedApp = null;
			if(appName != null && !appName.equals("")) {
				changedApp = changeApplicationName(response, appName);
			}
			JsonParser jsonReleaseStatusParser = new JsonParser();
			if(changedApp != null) {
				if(releaseStatus != null && !releaseStatus.equals("")) {
					changedApp = changeReleaseStatus(changedApp, releaseStatus);
				}
			}
			else {
				if(releaseStatus != null && !releaseStatus.equals("")) {
					changedApp = changeReleaseStatus(response, releaseStatus);
				}
			}
			if(appIcon != null && !appIcon.equals("") || appReleaseNotes != null && !appReleaseNotes.equals("")) {
				File applicationIconFile = null;
				File applicationReleaseNotes = null;
				DirectoryScanner scanner = new DirectoryScanner();
				scanner.setBasedir(fileSet.getDir());
				scanner.setCaseSensitive(false);
				scanner.scan();
				String[] includedFiles = scanner.getIncludedFiles();
				for(int i = 0; i<includedFiles.length; i++) {
					if(includedFiles[i].equals(appIcon)) {
						applicationIconFile = new File(scanner.getBasedir().getAbsolutePath() + File.separator + includedFiles[i]);
						final String uploadedIconUUID = communicator.uploadApplicationAsset("", applicationIconFile);
						Request iconRequest = communicator.getRequestFactory().createUploadRequest(uploadedIconUUID, applicationIconFile);
						final String iconResponse = communicator.getRequestFactory().send(iconRequest);
						changedApp = changeApplicationIcon(iconResponse, changedApp);
					}
					if(includedFiles[i].equals(appReleaseNotes)) {
						applicationReleaseNotes = new File(scanner.getBasedir().getAbsolutePath() + File.separator + includedFiles[i]);
						String log = download("file://" + applicationReleaseNotes.getAbsolutePath());
						if(log.length() > 256) {
							log = log.substring(0, 252) + "...";
				        }
						changedApp = changeReleaseNotes(changedApp, log);
					}
				}
			}
			JsonObject jsonReleaseStatusObject = (JsonObject) jsonReleaseStatusParser.parse(changedApp);
			JsonArray results = jsonReleaseStatusObject.getAsJsonArray("results");
			return results.get(0).toString();
		}
	}
	
	private static String download(String url) throws java.io.IOException {
	    java.io.InputStream s = null;
	    String content = null;
	    try {
	        s = (java.io.InputStream)new URL(url).getContent();
	        content = IOUtils.toString(s, "UTF-8");

	    }
	    finally {
	        if (s != null) s.close(); 
	    }
	    return content.toString();
	}
	
	private static String changeReleaseNotes(String app,  String appReleaseStatus) {
		Gson gson = new Gson();
		APIObject.Changelog changeLog = new APIObject.Changelog();
		changeLog.setDe_DE(appReleaseStatus);
		changeLog.setEn_US(appReleaseStatus);
		
		APIObject apiObject = gson.fromJson(app, APIObject.class);
		for(int index=0; index<apiObject.getResults().size(); index++) {
			APIObject.Results result = apiObject.getResults().get(index);
			for(int j=0; j<result.getVersions().size(); j++) {
				APIObject.Versions version = result.getVersions().get(j);
				version.setChangelog(changeLog);
			}
		}
		return gson.toJson(apiObject);
		
	}
	
	private static String changeApplicationName(String app, String appName) {
		APIObject.Name apiObjectName = new APIObject.Name();
		apiObjectName.setDe_DE(appName);
		apiObjectName.setEn_US(appName);
		
		Gson gson = new Gson();
		APIObject apiObject = gson.fromJson(app, APIObject.class);
		for(int index=0; index<apiObject.getResults().size(); index++) {
			APIObject.Results result = apiObject.getResults().get(index);
			for(int j=0; j<result.getVersions().size(); j++) {
				APIObject.Versions version = result.getVersions().get(j);
				version.setName(apiObjectName);
			}
		}
		return gson.toJson(apiObject);
	}
	
	private static String changeApplicationIcon(String iconResponse, String apiObjectResponse) {
		Gson gson = new Gson();
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject)jsonParser.parse(iconResponse);
		String jsonIcon = jsonObject.get("results").toString();
		Type collectionType = new TypeToken<Collection<APIObject.Icon>>(){}.getType();
        Collection<APIObject.Icon> uploadedIconCollection = gson.fromJson(jsonIcon, collectionType);
        APIObject.Icon icon = (APIObject.Icon)uploadedIconCollection.toArray()[0];
        APIObject apiObject = gson.fromJson(apiObjectResponse, APIObject.class);
        for(int i=0; i<apiObject.getResults().size(); i++) {
        	APIObject.Results results = apiObject.getResults().get(i);
        	for(int j=0; j<results.getVersions().size(); j++) {
        		APIObject.Versions versions = results.getVersions().get(j);
        		versions.setIcon(icon);
        	}
        }
		return gson.toJson(apiObject);
	}
	
	private static String changeReleaseStatus(String app, String status) {
		System.out.println("change status to: " + status);
		APIObject apiObject = new Gson().fromJson(app, APIObject.class);
        for(int index = 0; index < apiObject.getResults().size(); index++) {
        	APIObject.Results results = apiObject.getResults().get(index);
            for(int j = 0; j < results.getVersions().size(); j++) {
            	APIObject.Versions versions = results.getVersions().get(j);
            	versions.setReleaseStatus(status);
            }
        }
        return new Gson().toJson(apiObject);
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

		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(response);
		String message = jsonObject.get("message").getAsString();
		ApplicationInformation information = new ApplicationInformation();
		if(message.equals("Application was created successfully")) {
			information.setPublished(true);
		}
		else {
			information.setPublished(false);
		} 
		return information;
	}
}