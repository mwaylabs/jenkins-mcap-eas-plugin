
package org.jenkinsci.plugins.relution.json;

public class ApiConstraint {

    public final String uuid;

    public final String name;
    public final Object value;
    public final String type;

    protected ApiConstraint() {

        this.uuid = null;

        this.name = null;
        this.value = null;
        this.type = null;
    }
}
