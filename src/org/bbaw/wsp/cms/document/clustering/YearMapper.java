package org.bbaw.wsp.cms.document.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbaw.wsp.cms.document.Facet;
import org.bbaw.wsp.cms.document.FacetValue;
import org.bbaw.wsp.cms.document.Facets;

/**
 * The YearMapper is responsible for filtering and correcting the incoming Year
 * facets.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.11.2013
 */
public class YearMapper {
	/**
	 * Key / identifier for date facets.
	 */
	public static final String FACET_DATE = "date";
	private List<FacetValue> excludedFacetValues;

	/**
	 * Map the given year value if it doesn't match our expected year value.
	 * e.g.: replace '19th century' by '1800'.
	 * 
	 * @param yearFacet
	 * @return the corrected year facet
	 */
	public FacetValue mapYear(FacetValue yearFacet) {
		return yearFacet;
	}

	/**
	 * Check the format of the (mapped) {@link FacetValue}. It should be a
	 * 4-digit String which can be casted to an integer.
	 * 
	 * @param yearFacet
	 * @return {@link Boolean} true if the format is ok
	 * @throws IllegalArgumentException
	 *             if the value for year cannot be used for clustering
	 */
	protected boolean checkYearFormat(FacetValue yearFacet) {
		Pattern pYear = Pattern.compile("[0-9]{4}");
		Matcher m = pYear.matcher(yearFacet.getValue());

		if (!m.matches()) { // the facet value is not a 4-digit integer
			throw new IllegalArgumentException(
					getClass()
							+ ":mapYear: the given FacetValue doesn't contain a correct year (4-digit integer).");
		}

		// try conversion to int value
		try {
			Integer.parseInt(yearFacet.getValue());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					getClass()
							+ ":mapYear: the given FacetValue's value couldn't be casted to integer");
		}
		return true;
	}

	/**
	 * Filter the years within the incoming facets.
	 * 
	 * @param facets
	 * @return the filtered facet in a {@link List}
	 */
	public List<Facet> filterYears(Facets facets) {
		List<Facet> filteredFacets = new ArrayList<Facet>();
		List<FacetValue> excludedFacetValues = new ArrayList<FacetValue>();

		for (Facet facet : facets) {
			if (facet.getId().equals(FACET_DATE)) {
				filteredFacets.add(facet);

				// map year value if neccessary
				ArrayList<FacetValue> mappedValues = new ArrayList<FacetValue>();

				for (FacetValue facetValue : facet.getValues()) {
					try { // mapYear will throw an exception for invalid year
							// values (such as "Unbekannt")
						FacetValue yearMapped = this.mapYear(facetValue);
						if (this.checkYearFormat(yearMapped)) {
							mappedValues.add(yearMapped);
						}
					} catch (IllegalArgumentException e) { // invalid value will be excluded
						excludedFacetValues.add(facetValue);
					}
				}

				facet.setValues(mappedValues);
			}
		}
		this.setExcludedFacetValues(excludedFacetValues);

		return filteredFacets;
	}

	private void setExcludedFacetValues(List<FacetValue> outtakenFacetValues) {
		this.excludedFacetValues = outtakenFacetValues;
	}

	/**
	 * Get the excluded facet values (the years that were illegal and cannot be
	 * used for clustering, e.g. "Unbekannt").
	 * 
	 * @return {@link List} of {@link FacetValue}
	 */
	public List<FacetValue> getExcludedFacetValues() {
		return excludedFacetValues;
	}
}
