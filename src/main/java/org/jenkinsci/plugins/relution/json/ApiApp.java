
package org.jenkinsci.plugins.relution.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ApiApp {

    public final String                    uuid;
    public final String                    type;
    public final String                    internalName;

    public final List<String>              platforms  = new ArrayList<String>();
    public final List<ApiCategory>         categories = new ArrayList<ApiCategory>();

    public final List<ApiVersion>          versions   = new ArrayList<ApiVersion>();

    public final Float                     rating;
    public final Integer                   ratingCount;
    public final Integer                   downloadCount;

    public final Map<String, List<String>> acl        = new HashMap<String, List<String>>();

    public final String                    createdBy;
    public final Long                      creationDate;
    public final String                    modifiedBy;
    public final Long                      modificationDate;

    private transient String               s;

    protected ApiApp() {

        this.uuid = null;
        this.type = null;
        this.internalName = null;

        this.rating = null;
        this.ratingCount = null;
        this.downloadCount = null;

        this.createdBy = null;
        this.creationDate = null;
        this.modifiedBy = null;
        this.modificationDate = null;
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
