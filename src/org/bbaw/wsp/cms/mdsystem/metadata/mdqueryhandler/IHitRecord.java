/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.URL;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

/**
 * A HitRecord holds the results of a general search on our {@link WspRdfStore}.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 04.02.2013
 * 
 */
public interface IHitRecord {

  /**
   * Return the {@link URL} of the belonging named graph.
   * 
   * @return
   */
  URL getNamedGraphUrl();

  /**
   * Berechne den Score f�r die pf:textMatch-Ergebnisse ALLER matchenden Tripels
   * f�r diesen Named Graphen.
   * 
   * M�glichkeiten der Berechnung:
   * 
   * 1. Das arithmetische Mittel aller einzelnen Scores (die zu einem Tripel der
   * Form ?o pf:textMatch '+Humboldt' geh�ren.
   * 
   * Nachteil: Ausrei�er ver�ndern den Score deutlich.
   * 
   * @return
   */
  double calcScore();
}
