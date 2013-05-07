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
  private double avgScore;
  private double highestScore;

  /**
   * Create a new {@link HitGraph}.
   * 
   * @param namedGraphUrl
   *          the name (an URL) of the named graph as stored in the {@link WspRdfStore}
   */
  public HitGraph(final URL namedGraphUrl) {
    this.namedGraphUrl = namedGraphUrl;
    hitStatements = new ArrayList<HitStatement>();
    avgScore = 0;
    highestScore = 0;
  }

  public URL getNamedGraphUrl() {
    return namedGraphUrl;
  }

  /**
   * 
   * @return the average score of all {@link HitStatement}.
   */
  public double getAvgScore() {
    return avgScore;
  }

  /**
   * 
   * @return the maximum score of all {@link HitStatement}
   */
  public double getHighestScore() {
    return highestScore;
  }

  /**
   * Add a {@link HitStatement}. Should be done by the {@link SparqlAdapter} only.
   * 
   * @param statement
   *          a {@link HitStatement}
   */
  public void addStatement(final HitStatement statement) {
    hitStatements.add(statement);
    // calculate scores
    calcAvgScore(statement);
    calcHighestScore(statement);
  }

  private void calcHighestScore(final HitStatement statement) {
    if (highestScore == 0) {
      highestScore = statement.getScore();
    } else {
      if (statement.getScore() > highestScore) {
        highestScore = statement.getScore();
      }
    }
  }

  private void calcAvgScore(final HitStatement newStatement) {
    // calc arithmetisches Mittel
    if (avgScore == 0) {
      avgScore = newStatement.getScore();
    } else {
      final double sum = avgScore + newStatement.getScore();
      double avg = 0;
      if (sum != 0) {
        avg = sum / 2;
      }
      avgScore = avg;
    }
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
    return "HitGraph [namedGraphUrl=" + namedGraphUrl + ", hitStatements=" + hitStatements + ", avgScore=" + avgScore + ", highestScore=" + highestScore + "]";
  }

}
