/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.URL;

/**
 * A HitStatement holds the LARQ search results of a hit statement within the
 * RdfStore. It belongs to a {@link HitGraph}.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class HitStatement {

  private final String subject;
  private final URL predicate;
  private final String literal;
  private final double score;

  /**
   * 
   * @param subject
   *          a {@link URL}
   * @param predicate
   *          a {@link URL}
   * @param literal
   *          a {@link String}
   * @param score
   *          a {@link Double}
   */
  public HitStatement(final String subject, final URL predicate, final String literal, final double score) {
    this.subject = subject;
    this.predicate = predicate;
    this.literal = literal;
    this.score = score;
  }

  /**
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @return the predicate
   */
  public URL getPredicate() {
    return predicate;
  }

  /**
   * @return the literal
   */
  public String getLiteral() {
    return literal;
  }

  /**
   * @return the score
   */
  public double getScore() {
    return score;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "\t\tHitStatement [subject=" + subject + ", predicate=" + predicate + ", literal=" + literal + ", score=" + score + "]\n";
  }

}