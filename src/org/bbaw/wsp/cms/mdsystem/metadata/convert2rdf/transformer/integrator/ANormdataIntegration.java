/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer.integrator;

import java.io.File;

import org.bbaw.wsp.cms.general.Constants;

/**
 * This interface declares methods for concrete normdata integrator classes to integrate normdata ressources to their pretransformed rdf.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 23.05.2013
 * 
 */
public abstract class ANormdataIntegration {
  protected File rdfNormdataFile;

  /**
   * Create and initialize the NormdataIntegration class. Initialize the normdata file immediatly.
   */
  public ANormdataIntegration() {
    rdfNormdataFile = new File(Constants.getInstance().getMdsystemNormdataFile());
  }

  /**
   * Find an RDF object in the normdata for the given triple.
   * 
   * The triple consists of the given arguments.
   * 
   * @param subject
   *          the Subject
   * @param predicate
   *          the Predicate
   * @param object
   *          the pretransformed object
   * @return the normdata ressource (an URL as String) or null if no object could get matched
   */
  public abstract String fetchObject(String subject, String predicate, String object);

  /**
   * Integrate the normdata from the WSP rdf file for a whole pre-transformed rdf string.
   * 
   * @param preTransformedRdf
   *          the pretransformed rdf string
   * @return the rdf string with the integrated ressource from the normdata file
   */
  public abstract String integrateNormdata(String preTransformedRdf);
}
