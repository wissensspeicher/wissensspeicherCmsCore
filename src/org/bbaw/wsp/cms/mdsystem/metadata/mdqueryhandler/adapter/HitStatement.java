/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.URL;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * A HitStatement holds the LARQ search results of a hit statement within the RdfStore. It belongs to a {@link HitGraph}.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class HitStatement {

  private final RDFNode subject;
  private final RDFNode predicate;
  private final RDFNode literal;
  private final RDFNode subjParent;
  private final RDFNode predParent;
  private final double score;

  /**
   * Create a new HitStatement to represent results of the {@link SparqlAdapter}
   * 
   * @param subject
   *          an {@link RDFNode}
   * @param predicate
   *          an {@link RDFNode}
   * @param literal
   *          an {@link RDFNode}
   * @param score
   *          a {@link Double}
   * @param subjParent
   *          an {@link RDFNode}
   * @param predParent
   *          an {@link RDFNode}
   */
  public HitStatement(final RDFNode subject, final RDFNode predicate, final RDFNode literal, final double score, final RDFNode subjParent, final RDFNode predParent) {
    this.subject = subject;
    this.predicate = predicate;
    this.literal = literal;
    this.score = score;
    this.subjParent = subjParent;
    this.predParent = predParent;
  }

  /**
   * @return the subject
   */
  public RDFNode getSubject() {
    return subject;
  }

  /**
   * @return the predicate
   */
  public RDFNode getPredicate() {
    return predicate;
  }

  /**
   * @return the literal
   */
  public RDFNode getLiteral() {
    return literal;
  }

  /**
   * @return the score
   */
  public double getScore() {
    return score;
  }

  /**
   * @return the subject
   */
  public RDFNode getSubjParent() {
    return subjParent;
  }

  /**
   * @return the predicate
   */
  public RDFNode getPredParent() {
    return predParent;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "\t\tHitStatement [subject=" + subject + ", predicate=" + predicate + ", literal=" + literal + ", score=" + score + ", subjectParent=" + subjParent + ", predicateParent=" + predParent + "]\n";
  }

}
