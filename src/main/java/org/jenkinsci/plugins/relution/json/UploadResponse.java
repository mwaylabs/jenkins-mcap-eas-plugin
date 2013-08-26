
package org.jenkinsci.plugins.relution.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;


public class UploadResponse {

    public final static Gson   GSON    = new GsonBuilder().setPrettyPrinting().create();

    public final int           status;
    public final String        message;

    public final ApiError      errors;

    public final int           total;
    public final List<ApiFile> results = new ArrayList<ApiFile>();

    private transient String   s;

    public static <T extends UploadResponse> T fromJson(final String json, final Class<T> clazz) {
        return UploadResponse.GSON.fromJson(json, clazz);
    }

    public static UploadResponse fromJson(final String json) {
        return UploadResponse.fromJson(json, UploadResponse.class);
    }

    protected UploadResponse() {

        this.status = 0;
        this.message = null;

        this.errors = null;

        this.total = 0;
    }

    public String toJson() {
        return UploadResponse.GSON.toJson(this);
    }

    @Override
    public String toString() {

        if (this.s == null) {
            this.s = this.toJson();
        }
        return this.s;
    }
}
