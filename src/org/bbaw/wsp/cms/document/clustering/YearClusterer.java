/**
 * 
 */
package org.bbaw.wsp.cms.document.clustering;

import java.util.ArrayList;
import java.util.List;

import org.bbaw.wsp.cms.document.Facet;
import org.bbaw.wsp.cms.document.FacetValue;
import org.bbaw.wsp.cms.document.Facets;

/**
 * The YearClusterer is responsible for clustering incoming {@link Facet}.
 * 
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.11.2013
 */
public class YearClusterer {
	protected static final int DISTANCE = 10;
	protected YearMapper yearMapper;
	
	public YearClusterer() {
		this.yearMapper = new YearMapper();
	}
	
	/**
	 * Create a cluster for the facet containing the years.
	 * @param facets
	 * @return a list of {@link FacetValue}
	 */
	public List<FacetValue> clusterYears(Facets facets) {
		List<Facet> yearFacets = this.yearMapper.filterYears(facets);		
					
		List<List<FacetValue>> resultingClusters = performClustering(yearFacets.get(0));
		List<FacetValue> mergedFacetValues = mergeClusters(resultingClusters, this.yearMapper.getOuttakenFacetValues());
		System.out.println(resultingClusters);
		System.out.println(mergedFacetValues);
		
		
		return mergedFacetValues;		
	}

	/**
	 * Merge the resulting clusters.
	 * @param resultingClusters the {@link List} in which the elements are {@link List} of {@link FacetValue} 
	 * @param excludedValues a {@link List} of {@link FacetValue} which where excluded from the clustering.
	 * @return 
	 */
	private List<FacetValue> mergeClusters(
			List<List<FacetValue>> resultingClusters, List<FacetValue> excludedValues) {
		List<FacetValue> mergedFacetValues = new ArrayList<FacetValue>();
		
		for (List<FacetValue> cluster : resultingClusters) {
			if (cluster.size() > 1) { // only merge if cluster consists of several FacetValues
				FacetValue newFacetValue = new FacetValue();
				newFacetValue.setName(cluster.get(0).getName());
				newFacetValue.setCount(getClusterCountSum(cluster));
				newFacetValue.setValue(getClusterConcatValue(cluster));
				mergedFacetValues.add(newFacetValue);
			}
			else {
				mergedFacetValues.add(cluster.get(0));
			}
		}
		// append excluded facet values
		mergedFacetValues.addAll(excludedValues);
		
		return mergedFacetValues;
	}

	private String getClusterConcatValue(List<FacetValue> cluster) {		
		int minVal = Integer.parseInt(cluster.get(0).getValue());
		int maxVal = Integer.parseInt(cluster.get(0).getValue());
		for(int i=0; i < cluster.size(); i++) {
			int intVal = Integer.parseInt(cluster.get(i).getValue());
			if (intVal > maxVal) {
				maxVal = intVal;
			}
			else if (intVal < minVal) {
				minVal = intVal;
			}
		}
		
		String newConcatValue = minVal + " - " + maxVal;
		
		return newConcatValue;
	}

	private Integer getClusterCountSum(List<FacetValue> cluster) {
		int sum = 0;
		
		for (FacetValue facetValue : cluster) {
			sum += facetValue.getCount();
		}
		
		return sum;
	}

	private List<List<FacetValue>> performClustering(Facet facet) {
		List<List<FacetValue>> clusterList = new ArrayList<List<FacetValue>>();
		List<FacetValue> facetValuesCopy = new ArrayList<>(facet.getValues());
		
		for (FacetValue referenceValue : facet.getValues()) {
			this.buildCluster(referenceValue, facetValuesCopy, clusterList);
		}
		
		return clusterList;				
	}

	private void buildCluster(FacetValue referenceValue,
			List<FacetValue> facetValuesCopy, List<List<FacetValue>> clusterList) {
		if (isInCluster(referenceValue, clusterList)) {
			return;
		}
		
		List<FacetValue> facetValueInCluster = new ArrayList<FacetValue>();		
		clusterList.add(facetValueInCluster );
		facetValueInCluster.add(referenceValue);
		
		for (FacetValue facetValueCopy : facetValuesCopy) {
			if ( ! facetValueCopy.equals(referenceValue) ) {
				// check if the other facet value is already in a cluster
				if ( ! isInCluster(facetValueCopy, clusterList)) {
					if (hasCloseDistance(referenceValue, facetValueCopy)) {
						facetValueInCluster.add(facetValueCopy);
					}
				}
			}
		}
	}

	private boolean hasCloseDistance(FacetValue referenceValue,
			FacetValue facetValueCopy) {
		int referenceValueInt = Integer.parseInt(referenceValue.getValue());
		int facetValueCopyInt = Integer.parseInt(facetValueCopy.getValue());
		if ( Math.abs( referenceValueInt - facetValueCopyInt ) < DISTANCE) {
			return true;
		}
		return false;
	}

	private boolean isInCluster(FacetValue facetValueCopy,
			List<List<FacetValue>> clusterList) {
		for (List<FacetValue> list : clusterList) {
			if (list.contains(facetValueCopy)) {
				return true;
			}
		}
		return false;
	}

}
