package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;

/**
 * Manages a list of @QueryTarget
 * 
 * @author shk2
 * 
 */
public class QueryLibary {

	private final ArrayList<QueryTarget> liste = new ArrayList<QueryTarget>();
	private static QueryLibary instance;

	private QueryLibary() {
		liste.clear();
	}

	/**
	 * returns the instance of @QueryLibary
	 * 
	 * @return
	 */
	public static QueryLibary getInstance() {
		if (instance == null)
			instance = new QueryLibary();
		return instance;
	}

	/**
	 * adds an given @QueryTarget to the managed List
	 * 
	 * @param target
	 */
	public void addToList(QueryTarget target) {
		if (target != null) {
			liste.add(target);
		}
	}

	/**
	 * returns the actually given list of elements
	 * 
	 * @return
	 */
	public ArrayList<QueryTarget> getAllElements() {
		return liste;

	}

	/**
	 * Clears the list of Elements, should be used before a new query
	 */
	public void ClearElements() {
		liste.clear();
	}

}
