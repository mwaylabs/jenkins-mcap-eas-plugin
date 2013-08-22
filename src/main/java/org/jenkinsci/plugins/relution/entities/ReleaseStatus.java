
package org.jenkinsci.plugins.relution.entities;

import hudson.util.ListBoxModel;


public class ReleaseStatus {

    public final static ReleaseStatus[] STATUSES;

    public final String                 key;
    public final String                 name;

    static {
        STATUSES = new ReleaseStatus[] {
            new ReleaseStatus("DEVELOPMENT", "Development"),
            new ReleaseStatus("REVIEW", "Review"),
            new ReleaseStatus("RELEASE", "Release")
        };
    }

    public ReleaseStatus(final String key, final String name) {
        this.key = key;
        this.name = name;
    }

    public static void fill(final ListBoxModel model) {

        for (final ReleaseStatus status : STATUSES) {
            model.add(status.name, status.key);
        }
    }
}
