package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.factory;

import java.util.HashMap;

import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.MetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.ModsMetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.RdfMetadataExtractor;
import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.WspNormdataExtractor;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class offers the {@link MetadataExtractor} instances on an easy way. You don't need to define the namespaces.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 25.10.2012
 * 
 */
public class MetadataExtractorFactory {

  /**
   * Create a new {@link RdfMetadataExtractor}
   * 
   * @return the {@link RdfMetadataExtractor} instance.
   * @throws ApplicationException
   *           if the resource to be parsed is not validated by Saxon.
   */
  public static RdfMetadataExtractor newRdfMetadataParser(final String uri) throws ApplicationException {
    final HashMap<String, String> namespaces = new HashMap<String, String>();
    namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    namespaces.put("", "http://wsp.normdata.rdf#");
    namespaces.put("foaf", "http://xmlns.com/foaf/0.1/");
    namespaces.put("owl", "http://www.w3.org/2002/07/owl#");
    namespaces.put("dc", "http://purl.org/elements/1.1/");
    namespaces.put("dcterms", "http://purl.org/dc/terms/");
    namespaces.put("gnd", "http://d-nb.info/standards/elementset/gnd#");
    namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    namespaces.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    return new RdfMetadataExtractor(uri, namespaces);
  }

  /**
   * Create a new {@link ModsMetadataExtractor}.
   * 
   * @param uri
   *          the xml resource to be parsed.
   * @return the {@link ModsMetadataExtractor} instance.
   * @throws ApplicationException
   *           if the resource to be parsed is not validated by Saxon.
   */
  public static ModsMetadataExtractor newModsMetadataParser(final String uri) throws ApplicationException {
    final HashMap<String, String> namespaces = new HashMap<String, String>();
    namespaces.put("mods", "http://www.loc.gov/mods/v3");
    return new ModsMetadataExtractor(uri, namespaces);
  }

  /**
   * Create a new {@link WspNormdataExtractor} instance.
   * 
   * @param normdataFile
   *          the path to the normdata file (*.rdf)
   * @return the {@link WspNormdataExtractor} instance.
   * @throws ApplicationException
   *           if Saxon raises errors.
   */
  public static WspNormdataExtractor newWspNormdataExtractor(final String normdataFile) throws ApplicationException {
    final HashMap<String, String> namespaces = new HashMap<String, String>();
    // namespaces from WSP file at 25.04.2013
    namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    namespaces.put("foaf", "http://xmlns.com/foaf/0.1/");
    namespaces.put("owl", "http://www.w3.org/2002/07/owl#");
    namespaces.put("dc", "http://purl.org/dc/elements/1.1/");
    namespaces.put("dcterms", "http://purl.org/dc/terms/");
    namespaces.put("gnd", "http://d-nb.info/standards/elementset/gnd#");
    namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    namespaces.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    namespaces.put("", "http://wsp.normdata.rdf/");
    return new WspNormdataExtractor(normdataFile, namespaces);
  }
}
