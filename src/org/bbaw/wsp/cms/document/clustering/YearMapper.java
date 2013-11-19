package org.bbaw.wsp.cms.document.clustering;

import java.util.ArrayList;
import java.util.List;

import org.bbaw.wsp.cms.document.Facet;
import org.bbaw.wsp.cms.document.FacetValue;
import org.bbaw.wsp.cms.document.Facets;

/**
 * The YearMapper is responsible for filtering and correcting the incoming Year facets.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.11.2013
 */
public class YearMapper {
	/**
	 * Key / identifier for date facets.
	 */
	public static final String FACET_DATE = "date";

	/**
	 * Map the given year value if it doesn't match our expected year value.
	 * e.g.: replace '19th century' by '1800'.
	 * @param yearFacet
	 * @return the corrected year facet
	 */
	public FacetValue mapYear(FacetValue yearFacet) {
		return yearFacet;		
	}
	
	/**
	 * Filter the years within the incoming facets.
	 * @param facets
	 * @return the filtered facet in a {@link List}
	 */
	public List<Facet> filterYears (Facets facets) {
		List<Facet> filteredFacets = new ArrayList<Facet>();
		for (Facet facet : facets) {
			if (facet.getId().equals(FACET_DATE)) {
				filteredFacets.add(facet);
				
				// map year value if neccessary
				ArrayList<FacetValue> mappedValues = new ArrayList<FacetValue>();
				
				for (FacetValue facetValue : facet.getValues()) {
					mappedValues.add(this.mapYear(facetValue));
				}
				
				facet.setValues(mappedValues);
			}
		}
		return filteredFacets;		
	}
}