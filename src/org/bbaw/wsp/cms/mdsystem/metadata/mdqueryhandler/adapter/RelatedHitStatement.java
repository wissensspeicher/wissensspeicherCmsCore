/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.05.2013
 * 
 */
public class RelatedHitStatement extends HitStatement {
  /**
   * The relationship of the related statements.
   * 
   * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
   * @since 23.05.2013
   * 
   */
  enum Relationship {
    /**
     * The related statement is a parent statement (the object of the related statement is subject of another statement which is related to the queried node via another object).
     * 
     * An example graph can be the following:
     * 
     * <http://wsp.bbaw.de/edoc/2006/86/aggregation> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.openarchives.org/ore/terms/Aggregation>.
     * 
     * <http://wsp.bbaw.de/edoc/2006/86/aggregation> <http://www.openarchives.org/ore/terms/describedBy> <queriedNode>
     */
    PARENT,
    /**
     * The related statement is a child statement (the subject of the related statement is object of another statement which is related to the queried node via another subject).
     * 
     * An example graph:
     * 
     * <queriedNode> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.openarchives.org/ore/terms/ResourceMap>
     * 
     * <http://www.openarchives.org/ore/terms/ResourceMap> <anotherPred> <anotherObj>
     * 
     * 
     */
    CHILD;
  }

  protected int distance;
  /**
   * The relationship of this related statement.
   */
  protected Relationship relationship;

  /**
   * @return the distance (to a the the input Node).
   */
  public final int getDistance() {
    return distance;
  }

  public RelatedHitStatement(final RDFNode subject, final RDFNode predicate, final RDFNode object, final double score, final RDFNode subjParent, final RDFNode predParent, final int distance, final Relationship relationship) {
    super(subject, predicate, object, score, subjParent, predParent);
    this.distance = distance;
    this.relationship = relationship;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return super.toString() + " RelatedHitStatement [distance=" + distance + ", relationship=" + relationship + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + distance;
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (!super.equals(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RelatedHitStatement other = (RelatedHitStatement) obj;
    if (distance != other.distance) {
      return false;
    }
    if (relationship != other.relationship) {
      return false;
    }
    return true;
  }

}
