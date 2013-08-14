package org.jenkinsci.plugins.relution.entities;

public class ShortApplicationInformation {

	private String uuid;

	/**
	 * Holds Information of the app with the specific params.
	 * @param uuid distinct representation of the app.
	 */
	public ShortApplicationInformation(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * @return String representation of an unique app-identifier.
	 */
	public String getUUID() {
		return this.uuid;
	}
}
