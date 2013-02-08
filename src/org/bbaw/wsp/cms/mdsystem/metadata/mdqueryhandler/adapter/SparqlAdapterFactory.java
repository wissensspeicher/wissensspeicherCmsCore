/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter;

import java.net.URL;

import com.hp.hpl.jena.query.ResultSet;

/**
 * This factory offers {@link SparqlAdapter}
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 08.02.2013
 * 
 */
public class SparqlAdapterFactory {

  /**
   * Return an {@link ISparqlAdapter} by defining an URL to a fuseki dataset
   * (e.g. http://fuseki/[yourdataset]) WITHOUT '/' at the end
   * 
   * @param fusekiDatasetUrl
   *          an {@link URL}
   * @return an {@link ISparqlAdapter}
   */
  public static ISparqlAdapter getDefaultAdapter(final URL fusekiDatasetUrl) {
    final IQueryStrategy<ResultSet> strategy = new QueryStrategyFuseki(fusekiDatasetUrl);
    final ISparqlAdapter adapter = new SparqlAdapter<ResultSet>(strategy);
    return adapter;
  }
}
