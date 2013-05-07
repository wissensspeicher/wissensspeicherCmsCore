/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

/**
 * @author <a href="mailto:sascha.feldmann@gmx.de">Sascha Feldmann</a>
 * @since 04.02.2013
 * 
 */
public class HitGraph {

  private final URL namedGraphUrl;
  private final ArrayList<HitStatement> hitStatements;

  /**
   * Create a new {@link HitGraph}.
   * 
   * @param namedGraphUrl
   *          the name (an URL) of the named graph as stored in the {@link WspRdfStore}
   */
  public HitGraph(final URL namedGraphUrl) {
    this.namedGraphUrl = namedGraphUrl;
    hitStatements = new ArrayList<HitStatement>();
  }

  public URL getNamedGraphUrl() {
    return namedGraphUrl;
  }

  /**
   * Hier den Score berechnen.
   * 
   * z.B. als arithmetisches Mittel aller Scores der HitStatements...
   * 
   * @return
   */
  public double calcScore() {
    // calc arithmetisches Mittel
    double sum = 0;
    for (final HitStatement statement : hitStatements) {
      sum += statement.getScore();
    }
    double avg = 0;
    if (sum != 0) {
      avg = sum / hitStatements.size();
    }
    return avg;
  }

  /**
   * Add a {@link HitStatement}. Should be done by the {@link SparqlAdapter} only.
   * 
   * @param statement
   *          a {@link HitStatement}
   */
  public void addStatement(final HitStatement statement) {
    hitStatements.add(statement);
  }

  /**
   * Return the {@link HitStatement} of this {@link HitGraph}.
   * 
   * @return
   */
  public Collection<HitStatement> getAllHitStatements() {
    return hitStatements;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "\tHitGraph [namedGraphUrl=" + namedGraphUrl + ", avgScore= " + calcScore() + "hitStatements=" + hitStatements + "]\n";
  }

}
