package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.util.Iterator;

import org.apache.jena.larq.HitLARQ;
import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;
import org.apache.jena.larq.LARQ;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.util.MdSystemConfigReader;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This class encapsulates the larq index to make it persistent.
 * 
 * @ Project : Untitled @ File Name : StoreIndex.java @ Date : 10.01.2013 @ Author : Sascha Feldmann
 */

public class IndexStore {
  /**
   * Define the Index Save Location here. Should be defined relative to the project.
   */
  private static final String LARQ_DIR = MdSystemConfigReader.getInstance().getRdfLarqDir();

  public IndexBuilderString larqBuilder;
  Logger logger;

  /**
   * 
   * @return the {@link IndexBuilderString}
   */
  public IndexBuilderString getLarqBuilder() {
    return larqBuilder;
  }

  /**
   * initialize the {@link IndexBuilderString}
   */
  public void initializeIndexBuilder() {
    if (larqBuilder != null) {
      commitIndex();
      closeIndex();
    }
    getLogger().info("Initializing LARQ index store in dir " + LARQ_DIR);
    larqBuilder = new IndexBuilderString(LARQ_DIR);
    commitIndex(); // commit the index immediately
  }

  private Logger getLogger() {
    if (logger == null) {
      final Logger logger = Logger.getLogger(getClass());
      this.logger = logger;
    }
    return logger;
  }

  /**
   * Add a model to the (persistent) index.
   * 
   * @param m
   */
  public void addModelToIndex(final Model m) {
    /*
     * This shouldn't be done. It doesn't really work.
     */
    // try {
    larqBuilder.indexStatements(m.listStatements());
    // } catch (final Exception e) {
    // // don't do anything...
    // }
    // while (m.listStatements().hasNext()) {
    // final Statement s = m.listStatements().next();
    // if (null != s) {
    // try {
    // larqBuilder.indexStatement(s);
    // } catch (final Exception e) {
    // // logger.error("StoreIndex: Couldn't index model " + m);
    // }
    //
    // }
    // }
    commitIndex();
  }

  /**
   * 
   * @return the latest {@link IndexLARQ} or null if the {@link IndexStore} wasn't initialized.
   */
  public IndexLARQ getCurrentIndex() {
    if (larqBuilder != null) {
      return larqBuilder.getIndex();
    }
    return null;
  }

  /**
   * Close the current index.
   */
  public void commitIndex() {
    if (larqBuilder != null) {
      larqBuilder.flushWriter();
      larqBuilder.setAvoidDuplicates(false);
      logger = Logger.getLogger(WspRdfStore.class);
      logger.info("setting larq index");
      LARQ.setDefaultIndex(getCurrentIndex());
    }
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

  /**
   * Close the index. Consider, that no more updates on the index will be possible.
   */
  public void closeIndex() {
    if (larqBuilder != null) {
      larqBuilder.closeWriter();
    }
  }
}
