/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.05.2013
 * 
 */
public class RelatedHitStatement extends HitStatement {
  protected int distance;

  /**
   * @return the distance (to a the the input Node).
   */
  public final int getDistance() {
    return distance;
  }

  public RelatedHitStatement(final RDFNode subject, final RDFNode predicate, final RDFNode object, final double score, final RDFNode subjParent, final RDFNode predParent, final int distance) {
    super(subject, predicate, object, score, subjParent, predParent);
    this.distance = distance;
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return super.toString() + " RelatedHitStatement [distance=" + distance + "]";
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
    return true;
  }

}
