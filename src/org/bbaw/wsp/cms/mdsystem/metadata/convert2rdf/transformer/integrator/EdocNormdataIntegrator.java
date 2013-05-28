/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer.integrator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer.EdocToRdfTransformer;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * The normdata integrator which will be used by the {@link EdocToRdfTransformer}
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 23.05.2013
 * 
 */
public class EdocNormdataIntegrator extends ANormdataIntegration {

  public EdocNormdataIntegrator() throws ApplicationException {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer.integrator.ANormdataIntegration#fetchObject(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public String fetchObject(final String subject, final String predicate, final String object) {
    switch (subject) {
    case "dc:publisher":
      final String vorhabenUri = normdataExtractor.getParentOfFoaf(object);
      return vorhabenUri;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer.integrator.ANormdataIntegration#integrateNormdata(java.lang.String)
   */
  @Override
  public String integrateNormdata(final String preTransformedRdf) {
    String integrationRdf = preTransformedRdf;

    // replace publisher by URI
    integrationRdf = _fetchPublisher(preTransformedRdf, integrationRdf);
    return integrationRdf;
  }

  /**
   * Fetch the publisher from the normdata file.
   * 
   * @param preTransformedRdf
   *          the pretransformed rdf-String
   * @param integrationRdf
   *          the progressing rdf integration String
   * @return
   */
  private String _fetchPublisher(final String preTransformedRdf, String integrationRdf) {
    final Pattern p = Pattern.compile("(?i)<dc:publisher rdf:value=\"(.*?)\"/>(?i)");
    final Matcher m = p.matcher(preTransformedRdf);
    if (m.find()) {
      System.out.println("EdocNormdataIntegrator: found publisher");
      final String replacementRessource = m.group(1);
      final String[] institutes = replacementRessource.split(";");
      String instituteTriples = "";
      for (final String institute : institutes) {
        final String vorhabenUri = fetchObject("dc:publisher", "rdf:resource", institute);
        if (vorhabenUri != null && !vorhabenUri.isEmpty()) {
          // the institute is a resource from the normdata
          instituteTriples += "<dc:publisher rdf:resource=\"" + vorhabenUri + "\"/>\n\t\t\t\t";
        }
        // if there's no replacement for an institute, force it to appear in the integration string
        else {
          // the insttitute is a value
          instituteTriples += "<dc:publisher rdf:value=\"" + institute + "\"/>\n\t\t\t\t";
        }
      }
      if (!instituteTriples.isEmpty()) {
        integrationRdf = preTransformedRdf.replaceFirst(p.pattern(), instituteTriples);
      }
    }
    return integrationRdf;
  }
}
