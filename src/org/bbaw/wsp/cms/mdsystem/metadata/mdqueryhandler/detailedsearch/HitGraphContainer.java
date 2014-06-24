/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

/**
 * This class holds all {@link IHitRecord} instances which result from a single (literal) query with the {@link SparqlAdapter}.
 * 
 * @author marco juergens
 * @author Sascha Feldmann
 * @since 04.02.2013
 * 
 */
public class HitGraphContainer extends HashMap<URL, HitGraph> {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private Date resultTime;

  public HitGraphContainer() {

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
  public HitGraphContainer(final Date resultTime) {
    this.resultTime = resultTime;
  }

  /**
   * Return an {@link IHitRecord} which holds information on the results of a query on one named graph.
   * 
   * @param namedGraphUrl
   * @return
   */
  public HitGraph getHitGraph(final URL namedGraphUrl) {
    return get(namedGraphUrl);
  }

  /**
   * Return a {@link Collection} of {@link IHitRecord} which holds all the query data of the named graphes.
   * 
   * @return
   */
  public Collection<HitGraph> getAllHits() {
    return values();
  }

  /**
   * Add another {@link IHitRecord}. This should be done from the {@link SparqlAdapter} after a query was performed.
   * 
   * @param graphUrl
   * @param graphRecord
   */
  public void addHitRecord(final URL graphUrl, final HitGraph graphRecord) {
    put(graphUrl, graphRecord);
  }

  public boolean contains(final URL graphUrl) {
    return containsKey(graphUrl);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return super.toString() + "\n " + "[resultTime=" + resultTime + "]";
  }
}
