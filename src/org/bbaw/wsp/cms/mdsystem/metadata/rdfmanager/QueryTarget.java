package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages the Elements of an description. A Hashmap is used tag (Example "name"
 * ) is used as key, cointains a Arraylist with one to many elements which got
 * this tag
 * 
 * @author shk2
 * 
 */
public class QueryTarget {

	private final HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

	/**
	 * adds itself to Managing list in @QueryLibary
	 */
	public QueryTarget() {
		QueryLibary.getInstance().addToList(this);
		map.clear();
	}

	/**
	 * Adds a value to a given key, if key doesn't exist it will be created
	 * 
	 * @param key
	 * @param value
	 */
	public void addToMap(String key, String value) {

		if (key != null && value != null) {
			if (!map.containsKey(key)) {
				map.put(key, new ArrayList<String>());
			}

			map.get(key).add(value);

		}

	}

	/**
	 * returns the value to a given key
	 * 
	 * @param key
	 * @return
	 */
	public ArrayList<String> getValue(String key) {
		if (key != null) {
			return map.get(key);
		}
		return null;
	}

	@Override
	public String toString() {
		String tmp = "";
		for (String s : map.keySet()) {
			tmp += s + " - " + map.get(s) + "\n";
		}
		return tmp;

	}

}
