
package org.jenkinsci.plugins.relution.entities;

import java.util.ArrayList;
import java.util.List;


public class ApplicationList {

	
    private List<ShortApplicationInformation> results;
	
    /**
     * Class holds informations about the released applications in the store
     * @param list list of all Released Applications.
     */
	public ApplicationList(ArrayList<ShortApplicationInformation> results) {
		this.results = results;
	}

	/**
	 * get an list of all released apps in the store.
	 * @return list of all released apps
	 */
	public List<ShortApplicationInformation> getResults() {
		return results;
	}

	/**
	 * set a new released app list
	 * @param results 
	 */
	public void setResults(List<ShortApplicationInformation> results) {
		this.results = results;
	}
}
