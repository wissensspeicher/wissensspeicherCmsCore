package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryTarget {

	private final HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

	public QueryTarget() {
		QueryLibary.getInstance().addToList(this);
		map.clear();
	}

	public void addToMap(String key, String value) {

		if (key != null && value != null) {
			if (!map.containsKey(key)) {
				map.put(key, new ArrayList<String>());
			}

			map.get(key).add(value);

		}

	}

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
