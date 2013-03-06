package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.Serializable;

public class StorePathHandler implements Serializable {

	private final String system = System.getProperty("os.name");
	private String path = "";

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Boolean checkForNewRunningSystem(String sys) {

		if (sys.equals(system)) {
			return false;
		} else
			return true;

	}

}
