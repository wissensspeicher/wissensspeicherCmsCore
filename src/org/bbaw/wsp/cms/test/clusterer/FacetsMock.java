/**
 * 
 */
package org.bbaw.wsp.cms.test.clusterer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.lucene.facet.search.results.FacetResult;
import org.bbaw.wsp.cms.document.Facet;
import org.bbaw.wsp.cms.document.FacetValue;
import org.bbaw.wsp.cms.document.Facets;
import org.bbaw.wsp.cms.document.clustering.YearMapper;

/**
 * This is a mock of the {@link Facets} class so functionalites can get tested.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.11.2013
 *
 */
public class FacetsMock extends Facets {

	public FacetsMock(List<FacetResult> facetResults) {
		super(facetResults);
		// TODO Auto-generated constructor stub			
	}
	
	
	/**
	 * Fill the facets with the given test data.
	 * @param years a {@link List} of String. Each entry is a year.
	 */
	public void fillWithYears(List<String> years) {
		facets = new Hashtable<String, Facet>();
		
		ArrayList<FacetValue> facetValues = new ArrayList<FacetValue>();
		
		for (String year : years) {
			FacetValue facetValue = new FacetValue();
			facetValue.setCount(1);
			facetValue.setName(YearMapper.FACET_DATE);
			facetValue.setValue(year);
			
			facetValues.add(facetValue);
		}
		
		Facet facet = new Facet(YearMapper.FACET_DATE, facetValues );
		facets.put(YearMapper.FACET_DATE, facet);		
	}	
}
