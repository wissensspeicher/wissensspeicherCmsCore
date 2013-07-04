package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;

import com.hp.hpl.jena.graph.query.QueryHandler;

/**
 * 
 * This enum is used to exchange the scores of each {@link ConceptQueryResult}. *
 * 
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 02.07.2013
 * @see QueryHandler
 */
public enum ConceptIdentifierPriority {
  /**
   * The priorty wasn't specified.
   */
  UNDEFINED, START,
  /**
   * Highest priority, descending.
   */
  FIRST, SECOND, THIRD, FOURTH, FIVTH,
  /**
   * End of the road.
   */
  END;

  /**
   * Get the next priority, following the the given one.
   * 
   * @param prevPriority
   * @return the following priority
   * @throws IllegalArgumentException
   *           if you try to hand in a
   */
  public ConceptIdentifierPriority getNextPriority() {
    switch (this) {
    case START:
      return FIRST;
    case FIRST:
      return SECOND;
    case SECOND:
      return THIRD;
    case THIRD:
      return FOURTH;
    case FOURTH:
      return FIVTH;
    case FIVTH:
      return END;
    default:
      throw new IllegalArgumentException("Illegal value for the parameter prevPriority. Must be an enum value");
    }

  }
}
