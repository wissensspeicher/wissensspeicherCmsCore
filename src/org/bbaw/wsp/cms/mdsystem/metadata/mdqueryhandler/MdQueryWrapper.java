package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.util.ArrayList;

/**
 * Manages a list of @QueryTarget
 * 
 * @author shk2
 * 
 */
public class MdQueryWrapper {

	private final ArrayList<MdQueryResult> list = new ArrayList<MdQueryResult>();
	private static MdQueryWrapper instance;

	private MdQueryWrapper() {
		list.clear();
	}

	/**
	 * returns the instance of @QueryLibary
	 * 
	 * @return
	 */
	public static MdQueryWrapper getInstance() {
		if (instance == null)
			instance = new MdQueryWrapper();
		return instance;
	}

	/**
	 * adds an given @QueryTarget to the managed List
	 * 
	 * @param target
	 */
	public void addToList(MdQueryResult target) {
		if (target != null) {
			list.add(target);
		}
	}

	/**
	 * returns the actually given list of elements
	 * 
	 * @return
	 */
	public ArrayList<MdQueryResult> getQueryTargetList() {
		return list;

	}

	/**
	 * Clears the list of Elements, should be used before a new query
	 */
	public void ClearElements() {
		list.clear();
	}

}
