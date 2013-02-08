/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapter;

/**
 * This class holds all {@link IHitRecord} instances which result from a single
 * (literal) query with the {@link SparqlAdapter}.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @since 04.02.2013
 * 
 */
public class HitRecordContainer {
  private HashMap<URL, IHitRecord> recordMap;
  private Date resultTime;

  public HitRecordContainer() {
    recordMap = new HashMap<URL, IHitRecord>();
  }

  /**
   * 
   * @return the {@link Date} / time stamp
   */
  public Date getResultTime() {
    return resultTime;
  }

  /**
   * Return the time point when the query was built.
   * 
   * @param resultTime
   *          a {@link Date}
   */
  public HitRecordContainer(final Date resultTime) {
    this.resultTime = resultTime;
  }

  /**
   * Return an {@link IHitRecord} which holds information on the results of a
   * query on one named graph.
   * 
   * @param namedGraphUrl
   * @return
   */
  public IHitRecord getHitRecord(final URL namedGraphUrl) {
    return recordMap.get(namedGraphUrl);
  }

  /**
   * Return a {@link Collection} of {@link IHitRecord} which holds all the query
   * data of the named graphes.
   * 
   * @return
   */
  public Collection<IHitRecord> getAllHits() {
    return recordMap.values();
  }

  /**
   * Add another {@link IHitRecord}. This should be done from the
   * {@link SparqlAdapter} after a query was performed.
   * 
   * @param graphUrl
   * @param graphRecord
   */
  public void addHitRecord(final URL graphUrl, final IHitRecord graphRecord) {
    recordMap.put(graphUrl, graphRecord);
  }
}
