package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.util.ArrayList;

/**
 * Manages a list of @QueryTarget
 * 
 * @author shk2
 * 
 */
public class QueryWrapper {

	private final ArrayList<QueryTarget> list = new ArrayList<QueryTarget>();
	private static QueryWrapper instance;

	private QueryWrapper() {
		list.clear();
	}

	/**
	 * returns the instance of @QueryLibary
	 * 
	 * @return
	 */
	public static QueryWrapper getInstance() {
		if (instance == null)
			instance = new QueryWrapper();
		return instance;
	}

	/**
	 * adds an given @QueryTarget to the managed List
	 * 
	 * @param target
	 */
	public void addToList(QueryTarget target) {
		if (target != null) {
			list.add(target);
		}
	}

	/**
	 * returns the actually given list of elements
	 * 
	 * @return
	 */
	public ArrayList<QueryTarget> getQueryTargetList() {
		return list;

	}

	/**
	 * Clears the list of Elements, should be used before a new query
	 */
	public void ClearElements() {
		list.clear();
	}

}
