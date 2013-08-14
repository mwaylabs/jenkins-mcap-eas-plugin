
package org.jenkinsci.plugins.relution.net;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class RequestQueryFields {

    private final static String DEFAULT_CHARSET      = "UTF-8";
    private final static String UNSUPPORTED_ENCODING = "The configured charset is unsupported on the current platform.";
    private String              mCharsetName         = DEFAULT_CHARSET;
    private final List<String>  mFields              = new ArrayList<String>();
    private String              mQuery;

    private String encode(final String value) {
        try {
            return URLEncoder.encode(value, this.mCharsetName);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(UNSUPPORTED_ENCODING, e);
        }
    }

    /**
     * Gets the character set used when encoding strings.
     */
    public String getCharset() {
        return this.mCharsetName;
    }

    /**
     * Specifies the character set to use when encoding strings.
     * @param charsetName The name of the character set to use.
     * @throws IllegalCharsetNameException if the specified name is illegal.
     * @throws UnsupportedEncodingException if the specified character set is unsupported.
     */
    public void setCharset(final String charsetName) throws UnsupportedEncodingException {

        if (!Charset.isSupported(charsetName)) {
            throw new UnsupportedEncodingException("The specified charset is unsupported: " + charsetName);
        }
        this.mCharsetName = charsetName;
    }

    /**
     * adding key/value pair to an list which will be switched to an Query which could be append to an URL.
     * @param name key of the parameter.
     * @param value value of the parameter.
     */
    public void add(final String name, final String value) {
        if (name == null) {
            throw new IllegalArgumentException("The specified argument cannot be null: name");
        }

        if (value == null || value.length() == 0) {
            this.mFields.remove(name);
            return;
        }

        this.mFields.add(name);
        this.mFields.add(value);
        this.mQuery = null;
    }

    /**
     * @param name value to be removed from the List that could switched to an Query.
     */
    public void remove(final String name) {
        final int index = this.mFields.indexOf(name);
        if (index % 2 == 0) {
            this.mFields.remove(index); // key
            this.mFields.remove(index); // value
        }
        this.mQuery = null;
    }

    /**
     * clear all instance variables.
     */
    public void clear() {
        this.mFields.clear();
        this.mQuery = null;
    }

    /**
     * @return size of the list divided by 2.
     */
    public int size() {
        return this.mFields.size() / 2;
    }

    /**
     * Switch the key/value Parameter of and list to an Query which could append to an URL.
     */
    @Override
    public String toString() {
        if (this.mQuery == null) {
            final StringBuilder sb = new StringBuilder();
            final int len = size();

            for (int n = 0; n < len; n++) {
                final String name = this.mFields.get(n * 2);
                final String value = this.mFields.get(n * 2 + 1);

                if (sb.length() == 0) {
                    sb.append('?');
                } else {
                    sb.append('&');
                }
                sb.append(this.encode(name));
                sb.append('=');
                sb.append(this.encode(value));
            }
            this.mQuery = sb.toString();
        }
        return this.mQuery;
    }
}
