package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.ConceptIdentifierPriority;

/**
 * Manages the Elements of a description. A Hashmap is used tag (Example "name" ) is used as key, cointains a Arraylist with one to many elements which got this tag
 * 
 * @author shk2
 * 
 */
public class ConceptQueryResult {

  private final HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
  private ConceptIdentifierPriority resultPriority;

  /**
   * adds itself to Managing list in @QueryLibary
   */
  public ConceptQueryResult() {
    ConceptQueryWrapper.getInstance().addToList(this);
    map.clear();
    resultPriority = ConceptIdentifierPriority.UNDEFINED;
  }

  /**
   * Adds a value to a given key, if key doesn't exist it will be created
   * 
   * @param key
   * @param value
   */
  public void addToMap(final String key, final String value) {

    if (key != null && value != null) {
      if (!map.containsKey(key)) {
        map.put(key, new ArrayList<String>());
      }

      map.get(key).add(value);

    }
  }

  /**
   * returns available keys of the hashmap
   * 
   * @return
   */
  public Set<String> getAllMDFields() {
    return map.keySet();
  }

  /**
   * returns the value to a given key
   * 
   * @param key
   * @return
   */
  public ArrayList<String> getValue(final String key) {
    if (key != null) {
      return map.get(key);
    }
    return null;
  }

  @Override
  public String toString() {
    String tmp = "";
    tmp += "resultPriority - " + resultPriority + "\n";
    for (final String s : map.keySet()) {
      tmp += s + " - " + map.get(s) + "\n";
    }
    return tmp;

  }

  /**
   * Set the priority according to this ConceptQueryResult so it can be compared to other results.
   * 
   * <p>
   * <strong>For example:</strong> if you enter Marx, the resulting concept can be a project or a historical person. The historical person has the highest / first priority.
   * </p>
   * 
   * @param priority
   *          , see the {@link ConceptIdentifierPriority}.
   */
  public void setPriority(final ConceptIdentifierPriority priority) {
    resultPriority = priority;
  }

  /**
   * Return the priority according to this query result.
   * 
   * <p>
   * <strong>For example:</strong> if you enter Marx, the resulting concept can be a project or a historical person. The historical person has the highest / first priority.
   * </p>
   * 
   * @return the resultPriority
   */
  public final ConceptIdentifierPriority getResultPriority() {
    return resultPriority;
  }
}
