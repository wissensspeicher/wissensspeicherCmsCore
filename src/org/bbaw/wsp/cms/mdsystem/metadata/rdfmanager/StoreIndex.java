package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.File;
import java.util.Iterator;

import org.apache.jena.larq.HitLARQ;
import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;
import org.apache.jena.larq.LARQ;
import org.bbaw.wsp.cms.mdsystem.util.MdystemConfigReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * This class encapsulates the larq index to make it persistent.
 * 
 * @ Project : Untitled @ File Name : StoreIndex.java @ Date : 10.01.2013 @
 * Author : Sascha Feldmann
 */

public class StoreIndex {
  /**
   * Define the Index Save Location here. Should be defined relative to the
   * project.
   */
  private static final String LARQ_DIR = MdystemConfigReader.getInstance().getConfig().getLarqIndexPath();

  public IndexBuilderString larqBuilder;

  /**
   * 
   * @return the {@link IndexBuilderString}
   */
  public IndexBuilderString getLarqBuilder() {
    return larqBuilder;
  }

  /**
   * Create a new (persistent) StoreIndex within the default directory (mdsystem
   * config)
   */
  public StoreIndex() {
    larqBuilder = new IndexBuilderString(LARQ_DIR);
    commitIndex();
  }

  /**
   * Create a new (persistent) StoreIndex relative to the {@link WspRdfStore}.
   * 
   * @param directory
   */
  public StoreIndex(final String directory) {
    final File subDir = new File(new File(directory), "larqindex");
    if (!subDir.exists()) {
      subDir.mkdir();
    }
    larqBuilder = new IndexBuilderString(subDir);
    commitIndex();
  }

  /**
   * Add a model to the (persistent) index.
   * 
   * @param m
   */
  public void addModelToIndex(final Model m) {
    // larqBuilder.indexStatements(m.listStatements());
    while (m.listStatements().hasNext()) {
      final Statement s = m.listStatements().next();
      larqBuilder.indexStatement(s);
    }
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
    System.out.println("StoreIndex: setting current index");
    LARQ.setDefaultIndex(getCurrentIndex());
  }

  /**
   * Perform a direct query on the FulltextIndex.
   * 
   * @param queryString
   */
  public void readIndex(final String queryString) {
    System.out.println("LARQ query: " + queryString);
    final Iterator<HitLARQ> results = getCurrentIndex().search(queryString);
    System.out.println("results : " + results.hasNext());
    while (results.hasNext()) {
      final HitLARQ doc = results.next();
      System.out.println("-----------");
      System.out.println("DocId: " + doc.getLuceneDocId());
      System.out.println("Node: " + doc.getNode().toString());
      System.out.println("Score: " + doc.getScore());
      System.out.println("-----------");
    }
    System.out.println("finished lucene search");
  }
}
