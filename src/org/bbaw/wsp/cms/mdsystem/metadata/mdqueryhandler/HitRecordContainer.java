/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.SparqlAdapter;

/**
 * This class holds all {@link IHitRecord} instances which result from a single
 * (literal) query with the {@link SparqlAdapter}.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 04.02.2013
 * 
 */
public class HitRecordContainer {
  private HashMap<URL, IHitRecord> recordMap;
  private Date resultTime;

  /**
   * 
   * @return the {@link Date} / time stamp
   */
  public Date getResultTime() {
    return resultTime;
  }

  public HitRecordContainer(Date resultTime) {
    this.resultTime = resultTime;
  }

  public IHitRecord getHitRecord(URL namedGraphUrl) {
    return this.recordMap.get(namedGraphUrl);
  }

  public Collection<IHitRecord> getAllHits() {
    return this.recordMap.values();
  }
}
