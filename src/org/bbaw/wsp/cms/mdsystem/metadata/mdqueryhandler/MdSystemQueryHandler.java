package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.bbaw.wsp.cms.mdsystem.metadata.general.extractor.ConceptIdentifierPriority;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentfierSearchMode;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptIdentifier;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

/**
 * handles the JSON-query sent from the GUI and delegates to @SparqlAdapter and @ConceptIdentifier
 * 
 * @author marco juergens
 * 
 */

public class MdSystemQueryHandler {

  private ConceptIdentifier identifier;
  private ISparqlAdapter sparqlAdapter;
  private static MdSystemQueryHandler mdSystemQueryHandler;

  private MdSystemQueryHandler() {
  }

  public static MdSystemQueryHandler getInstance() {
    if (mdSystemQueryHandler == null) {
      mdSystemQueryHandler = new MdSystemQueryHandler();
    }
    return mdSystemQueryHandler;
  }

  public void init() {
    final WspRdfStore store = WspRdfStore.getInstance();
    URL datasetUrl;
    try {
      datasetUrl = new URL("http://localhost:3030/ds");
      sparqlAdapter = SparqlAdapterFactory.getDefaultAdapter(datasetUrl);
      identifier = new ConceptIdentifier();
    } catch (final MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // sparqlAdapter = new SparqlAdapter(store.getDataset());
  }

  /**
   * use locally
   * 
   * @param query
   * @return
   */
  public ArrayList<ConceptQueryResult> getConcept(final String query) {
    identifier.initIdentifying(query, ConceptIdentfierSearchMode.METHODE_OR);
    final ArrayList<ConceptQueryResult> results = identifier.getResultList();
    return results;
  }

}
