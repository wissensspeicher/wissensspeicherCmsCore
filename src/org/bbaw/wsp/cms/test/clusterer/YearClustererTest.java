package org.bbaw.wsp.cms.test.clusterer;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.facet.search.results.FacetResult;
import org.bbaw.wsp.cms.document.FacetValue;
import org.bbaw.wsp.cms.document.clustering.YearClusterer;
import org.bbaw.wsp.cms.document.clustering.YearMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test of the {@link YearClusterer}
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.11.2013
 *
 */
public class YearClustererTest {
	protected FacetsMock facetsMock;
	protected YearClusterer yearClusterer;
	
	/*
	 * #############################
	 * #
	 * # test preparation
	 * #
	 * #############################
	 */
	@Before
	public void setUp() throws Exception {
		List<FacetResult> emptyList = new ArrayList<FacetResult>();
		this.facetsMock = new FacetsMock(emptyList );
		
		// fill mock with test data
		this.facetsMock.fillWithYears(this.getTestData());
		
		this.yearClusterer = new YearClusterer();
	}

	@After
	public void tearDown() throws Exception {
		this.facetsMock = null;
	}

	/**
	 * Get the test data to be used as FacetValues.
	 * @return
	 */
	protected Map<String, Integer> getTestData() {
		Map<String, Integer> testMap = new HashMap<String, Integer>();
		
		// prepend invalid values
		testMap.put("19. Jahrhundert", 1);
		testMap.put("20th century", 1);
		
		testMap.put("2010", 406);
		testMap.put("2011", 334);
		testMap.put("2007", 173);
		testMap.put("2006", 107);
		testMap.put("unbekannt", 102);
		testMap.put("2009", 83);
		testMap.put("2012", 31);
		testMap.put("2008", 24);
		testMap.put("1920", 2);
		testMap.put("1940", 3);
		testMap.put("1893", 7);
		testMap.put("1894", 5);
		
		// append invalid values
		testMap.put("20. Jahrhundert", 1);
		
		return testMap;
	}
	
	/**
	 * Get the expected result list of FacetValue.
	 * @return
	 */
	protected List<FacetValue> getExpectedResultData() {
		List<FacetValue> expectedList = new ArrayList<FacetValue>();
		FacetValue expectedValue = new FacetValue();
		
		expectedValue = new FacetValue();
		expectedValue.setCount(17);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("1893 - 1940");
		expectedList.add(expectedValue );
		
		expectedValue = new FacetValue();
		expectedValue.setCount(1158);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("2006 - 2012");
		expectedList.add(expectedValue );				
		
		expectedValue = new FacetValue();
		expectedValue.setCount(102);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("unbekannt");
		expectedList.add(expectedValue );
		
		expectedValue = new FacetValue();
		expectedValue.setCount(1);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("19. Jahrhundert");
		expectedList.add(expectedValue );
		
		expectedValue = new FacetValue();
		expectedValue.setCount(1);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("20. Jahrhundert");
		expectedList.add(expectedValue );
		
		expectedValue = new FacetValue();
		expectedValue.setCount(1);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("20th century");
		expectedList.add(expectedValue );		
		
		return expectedList;
	}
		
	/*
	 * #############################
	 * #
	 * # test implementation
	 * #
	 * #############################
	 */

	@Test
	public void testYearClusteringList() {
		long beginningTime = new Date().getTime();
		System.out.println("Input Facets: " + this.facetsMock);
		List<FacetValue> resultingValues = this.yearClusterer.clusterYears(this.facetsMock, 10);
		System.out.println("resulting clusters (List of FacetValue): " + resultingValues);
		
		long endingTime = new Date().getTime();
		int i = 0;
		for (FacetValue expectedValue : this.getExpectedResultData()) {
			FacetValue resultingValue = resultingValues.get(i);
			
			assertEquals(expectedValue.getValue(), resultingValue.getValue());
			i++;
		}		
		
		// assert execution time of less than 1 second
		long elapsedTime = endingTime - beginningTime;
		assertTrue( elapsedTime < 1000);
		
	}
}
