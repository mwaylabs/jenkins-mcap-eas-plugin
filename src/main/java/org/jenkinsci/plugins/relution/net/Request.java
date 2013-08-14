/**
 * Request.java
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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


public class Request {

    private final RequestQueryFields  mQueryFields = new RequestQueryFields();
    private final int                 mMethod;
    private final String              mUrl;
    private final Map<String, String> mHeaders     = new HashMap<String, String>();
    private HttpEntity                mHttpEntity;

    /**
     * Create an new Request Object
     * @param method 0: GET, 1: POST, 2: PUT, 3: DELETE
     * @param url specific url to which the request response
     */
    public Request(final int method, final String url) {
    	this.mMethod = method;
        this.mUrl = url;
    }

    private HttpRequestBase createHttpRequest(final int method, final HttpEntity entity) {

        switch (method) {
            default:
            case Method.GET:
                return new HttpGet();

            case Method.POST:
                final HttpPost post = new HttpPost();
                if (entity != null) {
                    post.setEntity(entity);
                }
                return post;

            case Method.PUT:
                final HttpPut put = new HttpPut();
                if (entity != null) {
                    put.setEntity(entity);
                }
                return put;

            case Method.DELETE:
                return new HttpDelete();
        }
    }

    /**
     * Add a key/value pair to the Requestheader.
     * @param name key of the Headerfield.
     * @param value value of the Headerfield.
     */
    public void addHeader(final String name, final String value) {
    	this.mHeaders.put(name, value);
    }
    
    /**
     * Add a key/value pair in an specific format to the Requestheader.
     * @param name key of the Headerfield.
     * @param format arguments will be concatenated @see {@link String#format(String, Object...)}
     * @param args values concatenated to a new value which will be added as a Headerfield.
     */
    public void addHeader(final String name, final String format, final Object... args) {
        final String value = String.format(format, args);
        this.mHeaders.put(name, value);
    }

    /**
     * @return all querys appended to an specific URL.
     */
    public RequestQueryFields queryFields() {
        return this.mQueryFields;
    }

    /**
     * @return Returns the HttpEntity.
     */
    public HttpEntity entity() {
        return this.mHttpEntity;
    }

    /**
     * @param entity HttpEntity to be set.
     */
    public void setEntity(final HttpEntity entity) {
        this.mHttpEntity = entity;
    }

    private String getUrl() {
        if (this.mQueryFields.size() == 0) {
            return this.mUrl;
        }
        return this.mUrl + this.mQueryFields.toString();
    }

    /**
     * creation of a new Request (HttpGet, HttpPost, HttpPut, HttpDelete) 
     * with a (possibly) number of header values. 
     * @return new Request (HttpGet, HttpPost, HttpPut, HttpDelete).
     * @throws URISyntaxException
     */
    public HttpRequestBase createHttpRequest() throws URISyntaxException {
    	final HttpRequestBase request = this.createHttpRequest(this.mMethod, this.mHttpEntity);
    	
    	for (final String name : this.mHeaders.keySet()) {
    	   request.addHeader(name, this.mHeaders.get(name));
    	}
    	final URI uri = new URI(this.getUrl());
    	request.setURI(uri);

    	return request;
    }

    /**
     * Supported request methods.
     */
    public interface Method {
        public final static int GET    = 0;
        public final static int POST   = 1;
        public final static int PUT    = 2;
        public final static int DELETE = 3;
    }
}