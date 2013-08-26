
package org.jenkinsci.plugins.relution.json;

public class ApiFile {

    public final String  uuid;

    public final String  name;
    public final String  link;
    public final String  contentType;

    public final long    size;
    public final Long    modificationDate;

    public final Integer downloadCount;

    protected ApiFile() {

        this.uuid = null;

        this.name = null;
        this.link = null;
        this.contentType = null;

        this.size = 0;
        this.modificationDate = null;

        this.downloadCount = null;
    }
}
