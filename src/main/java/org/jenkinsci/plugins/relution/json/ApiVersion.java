
package org.jenkinsci.plugins.relution.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ApiVersion {

    public final String              uuid;
    public String                    appUuid;

    public final String              versionName;
    public final int                 versionCode;

    public String                    releaseStatus;

    public final Integer             downloadCount;
    public final Integer             installCount;

    public final String              link;

    public final ApiFile             file;
    public ApiFile                   icon;
    public final List<ApiFile>       screenshots = new ArrayList<ApiFile>();

    public final List<ApiConstraint> constraints = new ArrayList<ApiConstraint>();

    public final Map<String, String> name        = new HashMap<String, String>();
    public final Map<String, String> keywords    = new HashMap<String, String>();

    public final Map<String, String> description = new HashMap<String, String>();
    public final Map<String, String> changelog   = new HashMap<String, String>();

    public final String              copyright;
    public final String              developerName;
    public final String              developerWeb;
    public final String              developerEmail;

    public final String              createdBy;
    public final Long                creationDate;

    public final String              modifiedBy;
    public final Long                modificationDate;

    private transient String         s;

    protected ApiVersion() {
        this.uuid = null;
        this.appUuid = null;

        this.releaseStatus = null;
        this.versionName = null;
        this.versionCode = 0;

        this.downloadCount = null;
        this.installCount = null;

        this.link = null;

        this.file = null;
        this.icon = null;

        this.copyright = null;
        this.developerName = null;
        this.developerWeb = null;
        this.developerEmail = null;

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
