/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben;

import java.util.ArrayList;
import java.util.List;

/**
 * Record class for a parsed Vorhaben.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 04.03.2013
 * 
 */
public class Vorhaben extends ARecordEntry {
  private List<ARecordEntry> projekte;

  public Vorhaben() {
    projekte = new ArrayList<ARecordEntry>();
  }

  /**
   * @return the projekte
   */
  public List<ARecordEntry> getProjekte() {
    return projekte;
  }

  /**
   * @param projekte
   *          the projekte to set
   */
  public void setProjekte(final List<ARecordEntry> projekte) {
    this.projekte = projekte;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return super.toString() + "\nVorhaben [projekte=" + projekte + "]";
  }

  public void addProjekt(final Projekt aktProjekt) {
    projekte.add(aktProjekt);
  }

}
