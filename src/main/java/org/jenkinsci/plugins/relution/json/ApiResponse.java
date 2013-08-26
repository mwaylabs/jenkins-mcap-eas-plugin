
package org.jenkinsci.plugins.relution.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;


public class ApiResponse {

    public final static Gson  GSON    = new GsonBuilder().setPrettyPrinting().create();

    public final int          status;
    public final String       message;

    public final ApiError     errors;

    public final int          total;
    public final List<ApiApp> results = new ArrayList<ApiApp>();

    private transient String  s;

    public static <T extends ApiResponse> T fromJson(final String json, final Class<T> clazz) {
        return ApiResponse.GSON.fromJson(json, clazz);
    }

    public static ApiResponse fromJson(final String json) {
        return ApiResponse.fromJson(json, ApiResponse.class);
    }

    protected ApiResponse() {

        this.status = 0;
        this.message = null;

        this.errors = null;

        this.total = 0;
    }

    public String toJson() {
        return ApiResponse.GSON.toJson(this);
    }

    @Override
    public String toString() {

        if (this.s == null) {
            this.s = this.toJson();
        }
        return this.s;
    }
}
