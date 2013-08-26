
package org.jenkinsci.plugins.relution.json;

import java.util.HashMap;
import java.util.Map;


public class ApiCategory {

    public final String              uuid;

    public final Map<String, String> name        = new HashMap<String, String>();
    public final Map<String, String> description = new HashMap<String, String>();

    protected ApiCategory() {

        this.uuid = null;
    }
}
