/**
 * 
 */
package org.bbaw.wsp.cms.document.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	/**
	 * int the maximum allowed distance (Complete Linkage) between years in a cluster.
	 */
	protected int clusterDistance;
	protected YearMapper yearMapper;
	/**
	 * int the number of iterations that shall be done while looking for fragments
	 */
	private int optimizationCount = 3;
	/**
	 * int the distance factor multiplied to the clusterDistance for the tolerance while searching for fragments
	 * e.g.: a factor of 3 will be
	 */
	private int fragmentDistanceFactor = 4;

	public YearClusterer() {
		this.yearMapper = new YearMapper();
	}

	/**
	 * <p>
	 * Get the optimization count.<br />
	 * The optimization counts is the number of iterations that are performed on <br />
	 * the cluster list to detect and move fragments.<br />
	 * Attention: high values cause performance weakness.<br />
	 * </p>
	 * @return
	 */
	public int getOptimizationCount() {
		return optimizationCount;
	}

	/**
	 * <p>
	 * Set the optimization count.<br />
	 * The optimization counts is the number of iterations that are performed on <br />
	 * the cluster list to detect and move fragments.<br />
	 * Attention: high values cause performance weakness.<br />
	 * </p>
	 * @param optimizationCount
	 */
	public void setOptimizationCount(int optimizationCount) {
		this.optimizationCount = optimizationCount;
	}

	/**
	 * <p>
	 * Get the fragment distance factor.<br />
	 * The distance factor is important for the detecting and moving of cluster fragments.<br />
	 * It is multiplied to the clusterDistance to define a tolerance area for the defragmentation of clusters.<br />
	 * So, unnatural clusters are avoided.
	 * </p>
	 * @return
	 */
	public int getFragmentDistanceFactor() {
		return fragmentDistanceFactor;
	}

	/**
	 * <p>
	 * Set the fragment distance factor.<br />
	 * The distance factor is important for the detecting and moving of cluster fragments.<br />
	 * It is multiplied to the clusterDistance to define a tolerance area for the defragmentation of clusters.<br />
	 * So, unnatural clusters are avoided.<br />
	 * </p>
	 * @param fragmentDistanceFactor
	 */
	public void setFragmentDistanceFactor(int fragmentDistanceFactor) {
		this.fragmentDistanceFactor = fragmentDistanceFactor;
	}

	/**
	 * Get the cluster distance.
	 * 
	 * @return
	 */
	protected int getClusterDistance() {
		return clusterDistance;
	}

	/**
	 * Set the cluster distance.
	 * 
	 * @see clusterYears for a detailed description.
	 * @param clusterDistance
	 */
	protected void setClusterDistance(int clusterDistance) {
		this.clusterDistance = clusterDistance;
	}

	/**
	 * Create a cluster for the facet containing the years. This algorithm uses
	 * reference values to determine if other elements belong to the same
	 * cluster. The distance function realizes a "Complete Linkage" condition.
	 * 
	 * @param facets
	 * @param clusterDistance
	 *            {@link Integer} the maximum (arithmetic) distance between each
	 *            element of a cluster. For example: a cluster distance of 10
	 *            would result in a valid cluster: (1910,1912,1915,1920)
	 * @return a list of {@link FacetValue}
	 */
	public List<FacetValue> clusterYears(Facets facets, int clusterDistance) {
		// set the cluster distance first
		this.setClusterDistance(clusterDistance);

		List<Facet> yearFacets = this.yearMapper.filterYears(facets);
		this.sortYearValues(yearFacets);

		List<List<FacetValue>> resultingClusters = performDistanceClustering(yearFacets
				.get(0));
		
		while ( this.optimizationCount > 0 ) {			
			List<List<FacetValue>> resultingClustersCopy = resultingClusters;
			this.removeFragments(resultingClusters, resultingClustersCopy);
		}
		

		List<FacetValue> mergedFacetValues = mergeClusters(resultingClusters,
				this.yearMapper.getExcludedFacetValues());

		return mergedFacetValues;
	}

	/**
	 * Remove existing fragments within the given clusters. Fragments are
	 * clusters whose size is less than the average cluster size. Fragmented
	 * clusters shall get moved to another cluster.
	 * 
	 * @param resultingClusters
	 * @param resultingClustersCopy
	 */
	private void removeFragments(List<List<FacetValue>> resultingClusters,
			List<List<FacetValue>> resultingClustersCopy) {
		int avgCount = this.getAvgCount(resultingClustersCopy);
		
		int i = 0;
		for (List<FacetValue> yearValue : resultingClustersCopy) {
			int clusterCount = this.getClusterCountSum(yearValue);
			
			if ( i == resultingClusters.size() - 1 ) {
				this.optimizationCount--;
			}
			
			if (clusterCount < avgCount) {
				// move the year value to another cluster
				if ( this.moveFragmentToFittingCluster(resultingClusters, resultingClustersCopy, yearValue, avgCount, i) ) {
					return;
				}
			}
			
			i++;
		}
	}

	private boolean moveFragmentToFittingCluster(
			List<List<FacetValue>> resultingClusters, List<List<FacetValue>> resultingClustersCopy, List<FacetValue> yearValue, int avgCount, int yearValueIndex) {
		int i = 0;
		for (List<FacetValue> otherCluster: resultingClustersCopy) {			
			if ( !(otherCluster.equals(yearValue))) {
				int otherClusterCount = this.getClusterCountSum(otherCluster);
				if ( otherClusterCount < avgCount && (Math.abs(this.getAvgYear(otherCluster) - this.getAvgYear(yearValue)) < (this.getClusterDistance() * this.fragmentDistanceFactor))) {
					List<FacetValue> cluster = resultingClusters.get(i);
					cluster.addAll(yearValue);
					
					resultingClusters.remove(yearValueIndex);
					return true;
				}
			}
			
			i++;
		}
		
		return false;
	}

	/**
	 * Get the average year for a given cluster.
	 * @param otherCluster
	 * @return
	 */
	private int getAvgYear(List<FacetValue> otherCluster) {
		int yearSum = 0;
		
		for(FacetValue yearVal : otherCluster) {
			yearSum += Integer.parseInt(yearVal.getValue());
		}
		
		return yearSum / otherCluster.size();
	}

	/**
	 * Get the average (arithmetic) count.
	 * 
	 * @param resultingClusters
	 * @return
	 */
	private int getAvgCount(List<List<FacetValue>> resultingClusters) {
		int sum = 0;
		for (List<FacetValue> yearValue : resultingClusters) {
			for ( FacetValue value: yearValue) {
				sum += value.getCount();
			}			
		}
		return sum / (resultingClusters.size());
	}

	/**
	 * Sort the filtered years ascending.
	 * 
	 * @param yearFacets
	 */
	protected void sortYearValues(List<Facet> yearFacets) {
		List<FacetValue> yearValues = yearFacets.get(0).getValues();
		Comparator<FacetValue> yearComparator = new Comparator<FacetValue>() {
			@Override
			public int compare(FacetValue lValue, FacetValue rValue) {
				// convert for integer comparison
				int iLValue = Integer.parseInt(lValue.getValue());
				int iRValue = Integer.parseInt(rValue.getValue());

				if (iLValue < iRValue) {
					return -1;
				}
				if (iLValue > iRValue) {
					return 1;
				}
				return 0; // iLValue == iRvalue
			}
		};
		Collections.sort(yearValues, yearComparator);
	}

	/**
	 * Merge the resulting clusters.
	 * 
	 * @param resultingClusters
	 *            the {@link List} in which the elements are {@link List} of
	 *            {@link FacetValue}
	 * @param excludedValues
	 *            a {@link List} of {@link FacetValue} which where excluded from
	 *            the clustering.
	 * @return
	 */
	private List<FacetValue> mergeClusters(
			List<List<FacetValue>> resultingClusters,
			List<FacetValue> excludedValues) {
		List<FacetValue> mergedFacetValues = new ArrayList<FacetValue>();

		for (List<FacetValue> cluster : resultingClusters) {
			if (cluster.size() > 1) { // only merge if cluster consists of
										// several FacetValues
				FacetValue newFacetValue = new FacetValue();
				newFacetValue.setName(cluster.get(0).getName());
				newFacetValue.setCount(getClusterCountSum(cluster));
				newFacetValue.setValue(getClusterConcatValue(cluster));
				mergedFacetValues.add(newFacetValue);
			} else {
				mergedFacetValues.add(cluster.get(0));
			}
		}
		// append excluded facet values
		mergedFacetValues.addAll(excludedValues);

		return mergedFacetValues;
	}

	/**
	 * Concatenante the clusters' values. The concatenated String will be:
	 * "minimumYear - maximumYear" (within the builded cluster)
	 * 
	 * @param cluster
	 * @return
	 */
	private String getClusterConcatValue(List<FacetValue> cluster) {
		int minVal = Integer.parseInt(cluster.get(0).getValue());
		int maxVal = Integer.parseInt(cluster.get(0).getValue());
		for (int i = 0; i < cluster.size(); i++) {
			int intVal = Integer.parseInt(cluster.get(i).getValue());
			if (intVal > maxVal) {
				maxVal = intVal;
			} else if (intVal < minVal) {
				minVal = intVal;
			}
		}

		String newConcatValue = minVal + " - " + maxVal;

		return newConcatValue;
	}

	/**
	 * Calculate the new sum for the clusters' count values.
	 * 
	 * @param cluster
	 * @return
	 */
	private Integer getClusterCountSum(List<FacetValue> cluster) {
		int sum = 0;

		for (FacetValue facetValue : cluster) {
			sum += facetValue.getCount();
		}

		return sum;
	}

	/**
	 * Perform the clustering process for a given {@link Facet}.
	 * 
	 * @param facet
	 * @return
	 */
	private List<List<FacetValue>> performDistanceClustering(Facet facet) {
		List<List<FacetValue>> clusterList = new ArrayList<List<FacetValue>>();
		List<FacetValue> facetValuesCopy = new ArrayList<>(facet.getValues());

		for (FacetValue referenceValue : facet.getValues()) {
			this.buildCluster(referenceValue, facetValuesCopy, clusterList);
		}

		return clusterList;
	}

	/**
	 * Build a cluster for all {@link FacetValue}. It's neccessary to use a copy
	 * of the given {@link List} for the comparison of the values.
	 * 
	 * @param referenceValue
	 * @param facetValuesCopy
	 * @param clusterList
	 */
	private void buildCluster(FacetValue referenceValue,
			List<FacetValue> facetValuesCopy, List<List<FacetValue>> clusterList) {
		if (isInCluster(referenceValue, clusterList)) {
			return;
		}

		List<FacetValue> facetValueInCluster = new ArrayList<FacetValue>();
		clusterList.add(facetValueInCluster);
		facetValueInCluster.add(referenceValue);

		for (FacetValue facetValueCopy : facetValuesCopy) {
			if (!facetValueCopy.equals(referenceValue)) {
				// check if the other facet value is already in a cluster
				if (!isInCluster(facetValueCopy, clusterList)) {
					if (hasCloseDistance(referenceValue, facetValueCopy)) {
						facetValueInCluster.add(facetValueCopy);
					}
				}
			}
		}
	}

	/**
	 * Distance function which compares two {@link FacetValue}s and decides if
	 * they belong to the same cluster.
	 * 
	 * @param referenceValue
	 * @param facetValueCopy
	 * @return
	 */
	private boolean hasCloseDistance(FacetValue referenceValue,
			FacetValue facetValueCopy) {
		int referenceValueInt = Integer.parseInt(referenceValue.getValue());
		int facetValueCopyInt = Integer.parseInt(facetValueCopy.getValue());
		if (Math.abs(referenceValueInt - facetValueCopyInt) < this
				.getClusterDistance()) {
			return true;
		}
		return false;
	}

	/**
	 * Check if a given {@link FacetValue} is already contained in a cluster.
	 * This is checked to avoid overlapping.
	 * 
	 * @param facetValueCopy
	 * @param clusterList
	 * @return
	 */
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
