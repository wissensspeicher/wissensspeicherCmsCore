package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.Serializable;
import java.util.ArrayList;

public class StorePathHandler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4303415179300522376L;
	private String system = System.getProperty("os.name");
	private String path = "";
	private ArrayList<String> modelList = new ArrayList<String>();

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void refreshSystemInfo() {
		system = System.getProperty("os.name");
	}

	public Boolean checkForNewRunningSystem(String sys) {

		if (sys.equals(system)) {
			return false;
		} else
			return true;

	}

	public ArrayList<String> getModelList() {
		return modelList;
	}

	public void addToModelList(String model) {
		modelList.add(model);
	}

	public void removeModel(String model) {
		modelList.remove(model);
	}

	public void setModelList(ArrayList<String> array) {
		modelList = array;
	}

}
