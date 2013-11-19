package org.bbaw.wsp.cms.test.clusterer;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	protected List<String> getTestData() {
		List<String> testList = new ArrayList<String>();
		
		testList.add("1933");
		testList.add("1905");
		testList.add("1914");
		testList.add("1921");
		testList.add("1928");
		
		return testList;
	}
	
	/**
	 * Get the expected result list of FacetValue.
	 * @return
	 */
	protected List<FacetValue> getExpectedResultData() {
		List<FacetValue> expectedList = new ArrayList<FacetValue>();
		FacetValue expectedValue = new FacetValue();
		expectedValue.setCount(2);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("1928 - 1933");
		expectedList.add(expectedValue );
		
		expectedValue = new FacetValue();
		expectedValue.setCount(2);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("1905 - 1914");
		expectedList.add(expectedValue );
		
		expectedValue = new FacetValue();
		expectedValue.setCount(1);
		expectedValue.setName(YearMapper.FACET_DATE);
		expectedValue.setValue("1921");
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
		List<FacetValue> resultingValues = this.yearClusterer.clusterYears(this.facetsMock);
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
