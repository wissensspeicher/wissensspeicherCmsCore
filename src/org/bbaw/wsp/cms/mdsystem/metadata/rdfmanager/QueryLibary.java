package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;

public class QueryLibary {

	private final ArrayList<QueryTarget> liste = new ArrayList<QueryTarget>();
	private static QueryLibary instance;

	private QueryLibary() {
		liste.clear();
	}

	public static QueryLibary getInstance() {
		if (instance == null)
			instance = new QueryLibary();
		return instance;
	}

	public void addToList(QueryTarget target) {
		if (target != null) {
			liste.add(target);
		}
	}

	public ArrayList<QueryTarget> getAllElements() {
		return liste;

	}

	public void ClearElements() {
		liste.clear();
	}

}
