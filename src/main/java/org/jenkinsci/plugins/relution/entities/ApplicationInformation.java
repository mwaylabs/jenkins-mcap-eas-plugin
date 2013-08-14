
package org.jenkinsci.plugins.relution.entities;

import java.util.Map;


public class ApplicationInformation {

	private Boolean             published;
    
	/**
	 * holds information if the actual build application is published
	 * @return returns the actual published state of the application
	 */
    public boolean getPublished() {
    	return this.published;
    }
    
    /**
	 * sets published information to the actual build application
     */
    public void setPublished(boolean published) {
    	this.published = published;
    }
}
