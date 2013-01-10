package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.Serializable;

import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;
import org.apache.jena.larq.LARQ;

import com.hp.hpl.jena.rdf.model.Model;

/**

  This class encapsulates the larq index to make it persistent.

  @ Project : Untitled
  @ File Name : StoreIndex.java
  @ Date : 10.01.2013
  @ Author : Sascha Feldmann

*/


public class StoreIndex implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String LARQ_DIR = "save/larqindex";
	public IndexBuilderString larqBuilder;
	
	/**
	 * 
	 * @return the {@link IndexBuilderString}
	 */
	public IndexBuilderString getLarqBuilder() {
		return larqBuilder;
	}

	/**
	 * Create a new (persistent) StoreIndex
	 */
	public StoreIndex() {
		larqBuilder = new IndexBuilderString(LARQ_DIR);
		commitIndex();
	}
	
	/**
	 * Add a model to the (persistent) index.
	 * @param m
	 */
	public void addModelToIndex(Model m) {
		larqBuilder.indexStatements(m.listStatements());
		commitIndex();		
	}
	
	/**
	 * 
	 * @return the latest {@link IndexLARQ}
	 */
	public IndexLARQ getCurrentIndex() {
		return larqBuilder.getIndex();
	}
	
	/**
	 * Close the current index.
	 */
	public void commitIndex() {
		larqBuilder.closeWriter();		
		LARQ.setDefaultIndex(getCurrentIndex());
	}
}
