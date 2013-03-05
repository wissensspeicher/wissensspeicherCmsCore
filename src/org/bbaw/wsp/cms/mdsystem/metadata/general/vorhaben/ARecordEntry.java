/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 05.03.2013
 * 
 */
public abstract class ARecordEntry {
  protected String name;
  protected Map<String, String> fields;

  public ARecordEntry() {
    fields = new HashMap<String, String>();
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * @return the fields
   */
  public Map<String, String> getFields() {
    return fields;
  }

  public void addField(final String key, final String value) {
    fields.put(key, value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ARecordEntry [name=" + name + ", fields=" + fields + "]";
  }

}
