package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch;

import java.util.ArrayList;

/**
 * Manages a list of @QueryTarget
 * 
 * @author shk2
 * 
 */
public class ConceptQueryWrapper {

	private final ArrayList<ConceptQueryResult> list = new ArrayList<ConceptQueryResult>();
	private static ConceptQueryWrapper instance;

	private ConceptQueryWrapper() {
		list.clear();
	}

	/**
	 * returns the instance of @QueryLibary
	 * 
	 * @return
	 */
	public static ConceptQueryWrapper getInstance() {
		if (instance == null)
			instance = new ConceptQueryWrapper();
		return instance;
	}

	/**
	 * adds an given @QueryTarget to the managed List
	 * 
	 * @param target
	 */
	public void addToList(ConceptQueryResult target) {
		if (target != null) {
			list.add(target);
		}
	}

	/**
	 * returns the actually given list of elements
	 * 
	 * @return
	 */
	public ArrayList<ConceptQueryResult> getQueryTargetList() {
		return list;

	}

	/**
	 * Clears the list of Elements, should be used before a new query
	 */
	public void ClearElements() {
		list.clear();
	}

}
