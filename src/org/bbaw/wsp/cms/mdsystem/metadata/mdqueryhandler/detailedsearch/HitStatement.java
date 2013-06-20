/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

/**
 * A HitStatement holds the LARQ search results of a hit statement within the RdfStore. It belongs to a {@link HitGraph}.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class HitStatement {

  protected final RDFNode subject;
  protected final RDFNode predicate;
  protected final RDFNode object;
  protected final RDFNode subjParent;
  protected final RDFNode predParent;
  protected final double score;

  /**
   * Create a new HitStatement to represent results of the {@link SparqlAdapter}
   * 
   * @param subject
   *          an {@link RDFNode}
   * @param predicate
   *          an {@link RDFNode}
   * @param object
   *          an {@link RDFNode}
   * @param score
   *          a {@link Double}
   * @param subjParent
   *          an {@link RDFNode}
   * @param predParent
   *          an {@link RDFNode}
   */
  public HitStatement(final RDFNode subject, final RDFNode predicate, final RDFNode object, final double score, final RDFNode subjParent, final RDFNode predParent) {

    this.subject = removeSparqlBrackets(subject);
    this.predicate = removeSparqlBrackets(predicate);
    this.object = removeSparqlBrackets(object);
    this.score = score;
    this.subjParent = removeSparqlBrackets(subjParent);
    this.predParent = removeSparqlBrackets(predParent);
  }

  private RDFNode removeSparqlBrackets(final RDFNode rdfnode) {
    final RDFNode resultingNode;
    if (rdfnode != null && rdfnode.isResource()) {
      String uri = rdfnode.asResource().getURI();
      if (uri != null) {
        uri = uri.replaceFirst("\\<", "");
        uri = uri.replaceFirst("\\>", "");
        resultingNode = new ResourceImpl(uri);
        return resultingNode;
      }
    }
    return rdfnode;
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
   * @return the object
   */
  public RDFNode getObject() {
    return object;
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
    return "\t\tHitStatement [subject=" + subject + ", predicate=" + predicate + ", literal=" + object + ", score=" + score + ", subjectParent=" + subjParent + ", predicateParent=" + predParent + "]\n";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((object == null) ? 0 : object.hashCode());
    result = prime * result + ((predParent == null) ? 0 : predParent.hashCode());
    result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
    long temp;
    temp = Double.doubleToLongBits(score);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((subjParent == null) ? 0 : subjParent.hashCode());
    result = prime * result + ((subject == null) ? 0 : subject.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final HitStatement other = (HitStatement) obj;
    if (object == null) {
      if (other.object != null) {
        return false;
      }
    } else if (!object.equals(other.object)) {
      return false;
    }
    if (predParent == null) {
      if (other.predParent != null) {
        return false;
      }
    } else if (!predParent.equals(other.predParent)) {
      return false;
    }
    if (predicate == null) {
      if (other.predicate != null) {
        return false;
      }
    } else if (!predicate.equals(other.predicate)) {
      return false;
    }
    if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
      return false;
    }
    if (subjParent == null) {
      if (other.subjParent != null) {
        return false;
      }
    } else if (!subjParent.equals(other.subjParent)) {
      return false;
    }
    if (subject == null) {
      if (other.subject != null) {
        return false;
      }
    } else if (!subject.equals(other.subject)) {
      return false;
    }
    return true;
  }

}
