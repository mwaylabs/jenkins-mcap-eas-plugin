
package org.jenkinsci.plugins.relution.net;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
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


public class RequestFactory {

	private final static Charset CHARSET = Charset.forName("UTF-8");
    private final static String URL_APP_STORE_ITEMS = "apps";
    private final static String URL_TEMP_FILE = "files";
    private final static String URL_RESOURCE_ANALYZER = "apps/fromFile";

    private BasicCookieStore relutionCookieStore;
    private HttpHost relutionProxyHost;
    private String relutionApiUrl;
    private String relutionUsername;
    private String relutionPassword;
    private String relutionOrganization;
    private String relutionReleaseStatus;
    
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
            if (relutionProxyHost != null) {
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, relutionProxyHost);
            }
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            client.setCookieStore(relutionCookieStore);

            final HttpRequestBase httpRequest = request.createHttpRequest();
            System.out.println("Request > " + httpRequest.toString());

            final String response = EntityUtils.toString(client.execute(httpRequest).getEntity(), CHARSET);
            System.out.println("Response < " + response);
            return response;

        } finally {
            client.getConnectionManager().shutdown();
        }
    }
    
    private String getAuthorizationToken(final String username, final String organization, String password) {

        final String authorization = username + ":" + organization + ":" + password;
        return Base64.encodeBase64String(authorization.getBytes());
    }
    
    private String getUrl(final String path, final String... subs) {

        final StringBuilder sb = new StringBuilder();
        sb.append(relutionApiUrl);
        sb.append(path);

        for (final String sub : subs) {
            sb.append("/");
            sb.append(sub);
        }

        return sb.toString();
    }

    private Request getBaseRequest(final int method, final String path, final String... subs) {

        final String url = getUrl(path, subs);
        final Request request = new Request(method, url);
        request.addHeader("Accept", "application/json");
        request.addHeader("Authorization", "Basic " + getAuthorizationToken(relutionUsername, relutionOrganization, getRelutionPassword()));

        return request;
    }

    /**
     * With these created request you could retrieve the apps stored in the relution store.
     * @return json representation of the apps stored in the relution.
     */
    public Request createAppStoreItemsRequest() {
        final Request request = getBaseRequest(Request.Method.GET, URL_APP_STORE_ITEMS);
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
        final Request request = getBaseRequest(Request.Method.POST, URL_TEMP_FILE, uploadToken);
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(file));
        request.setEntity(entity);
        return request;
    }

    /**
     * Creates an request which could be send against the relution.
     * @param uploadToken empty String
     * @param UUID uuid of the Object which should be checked
     * @return APIObject App if the app not exists in the store or an error if the app already exists
     */
    public Request createAnalyzeUploadedApplication(final String uploadToken, final String UUID) {
        final Request request = getBaseRequest(Request.Method.POST, URL_RESOURCE_ANALYZER, uploadToken);
        return request;
    }

    /**
     * Creates an request that could be send against the relution with an APIObject.
     * @param appObject The APIObject returned in an previous step 
     * @return request could be send aginst the relution to upload the appObject
     */
    public Request createUploadedApplicationInformationRequest(String appObject) {
    	final Request request = getBaseRequest(Request.Method.POST, URL_APP_STORE_ITEMS);
        request.addHeader("Content-Type", "application/json");
        final StringEntity entity = new StringEntity(appObject, CHARSET);
        request.setEntity(entity);
        return request;
    }

    /**
     * @return Password relates to the Username.
     */
    public String getRelutionPassword() {
		return relutionPassword;
	}

    /**
     * @param relutionPassword Sets entry of  the textfield apiPassword.
     */
    public void setRelutionPassword(String relutionPassword) {
		this.relutionPassword = relutionPassword;
	}

    /**
     * @return actual entered ReleaseStatus.
     */
    public String getRelutionReleaseStatus() {
		return relutionReleaseStatus;
	}

    /**
     * @param relutionReleaseStatus Sets entry of  the textfield apiReleaseStatus.
     */
    public void setRelutionReleaseStatus(String relutionReleaseStatus) {
		this.relutionReleaseStatus = relutionReleaseStatus;
	}
	
    /**
     * @return URL to which your app will connect.
     */
    public String getRelutionApiUrl() {
		return relutionApiUrl;
	}

    /**
     * @param relutionApiUrl Communication endpoint to set.
     */
    public void setRelutionApiUrl(String relutionApiUrl) {
		this.relutionApiUrl = relutionApiUrl;
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
		return this.relutionUsername;
	}
	
    /**
     * @param relutionUsername Sets entry of  the textfield apiUsername.
     */
    public void setRelutionUsername(final String relutionUsername) {
		this.relutionUsername = relutionUsername;
	}

    /**
     * @return Organization relates to the Username.
     */
    public String getRelutionOrganization() {
		return this.relutionOrganization;
	}
	
    /**
     * @param relutionOrganization Sets entry of  the textfield apiOrganization.
     */
    public void setRelutionOrganization(final String relutionOrganization) {
		this.relutionOrganization = relutionOrganization;
	}
}
