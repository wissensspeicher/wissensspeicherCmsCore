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
    case "dc:creator":
      final String creatorUri = normdataExtractor.getParentOfGivenFamily(predicate, object);
      return creatorUri;
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
    // replace creator(s) by URI
    integrationRdf = _fetchCreators(integrationRdf, integrationRdf);
    return integrationRdf;
  }

  /**
   * Fetch the creator from the normdata file.
   * 
   * Check if there is a person with both the given and family name.
   * 
   * @param preTransformedRdf
   *          the pretransformed rdf-String
   * @param integrationRdf
   *          the progressing rdf integration String
   * @return the replaced string
   */
  private String _fetchCreators(final String preTransformedRdf, String integrationRdf) {
    System.out.println("_fetchCreators, integrationRdf: " + preTransformedRdf);
    // (?s) enables the dot for multiline matching
    final Pattern p = Pattern.compile("(?s)(?i)<dc:creator rdf:parseType=\"Resource\">(.*?)</dc:creator>(?i)");
    for (final Matcher m = p.matcher(preTransformedRdf); m.find();) {
      final String creator = m.group(1);
      System.out.println("EdocNormdataIntegrator: fetched creator " + creator);
      final Pattern pGiven = Pattern.compile("(?i)<foaf:givenName>(.*)</foaf:givenName>(?i)");
      final Pattern pFamily = Pattern.compile("(?i)<foaf:familyName>(.*)</foaf:familyName>(?i)");
      final Matcher mGiven = pGiven.matcher(creator);
      final Matcher mFamily = pFamily.matcher(creator);
      if (mGiven.find() && mFamily.find()) {
        final String givenName = mGiven.group(1);
        final String familyName = mFamily.group(1);
        System.out.println("EdocNormdataIntegrator: matched familyname " + familyName);
        System.out.println("EdocNormdataIntegrator: matched givenname " + givenName);
        final String creatorUri = fetchObject("dc:creator", givenName, familyName);
        System.out.println("EdocNormdataIntegrator: creatorUri " + creatorUri);
        String matchedCreator = "";
        if (creatorUri != null && !creatorUri.isEmpty()) {
          // the institute is a resource from the normdata
          matchedCreator = "<dc:creator rdf:resource=\"" + creatorUri + "\"/>\n\t\t\t\t";
          System.out.println("EdocNormdataIntegrator: matched creator " + matchedCreator);
          final Pattern pToBeReplaced = Pattern.compile("(?m)(?i)<dc:creator rdf:parseType=\"Resource\">\n.*?<foaf:givenName>" + givenName + "</foaf:givenName>\n.*?<foaf:familyName>" + familyName + "</foaf:familyName>\n.*?</dc:creator>");
          integrationRdf = preTransformedRdf.replaceFirst(pToBeReplaced.pattern(), matchedCreator);
        }
      }
    }
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
