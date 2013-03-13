package org.bbaw.wsp.cms.test;

import java.util.Date;

import org.bbaw.wsp.cms.collections.CollectionManager;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemQueryHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.dict.db.LexHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.MorphologyCache;

public class TestLocal {
  private IndexHandler indexer;

  public static void main(final String[] args) throws ApplicationException {
    try {

      final JenaMain main = new JenaMain();
      main.initStore();

      // MdSystemQueryHandler msqh = new MdSystemQueryHandler();

      final TestLocal test = new TestLocal();
      test.init();
      test.createAllDocuments();

      test.end();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private void init() throws ApplicationException {
    indexer = IndexHandler.getInstance();
  }

  private void end() throws ApplicationException {
    indexer.end();
  }

  private void createAllDocuments() throws ApplicationException {
    final CollectionManager collectionManager = CollectionManager.getInstance();
    // collectionManager.updateCollections();
    // collectionManager.updateCollection("test", true);
    collectionManager.updateCollection("AvH", true);
    // collectionManager.updateCollection("registres", true);
    // collectionManager.updateCollection("mes", true);
    // collectionManager.updateCollection("MEGA", true);
    // collectionManager.updateCollection("IG", true);
  }

  private void testCalls() throws ApplicationException {
    final Date before = new Date();
    System.out.println("Indexing start: " + before.getTime());

    final String docIdAvhNew = "/test/Dok280E18xml.xml";
    String docSrcUrlStr = "http://telota.bbaw.de:8085/exist/rest/db/AvHBriefedition/Briefe/Dok280E18xml.xml";
    CmsDocOperation docOperation = new CmsDocOperation("create", docSrcUrlStr, null, docIdAvhNew);
    docOperation.setMainLanguage("deu");
    final String[] elemNames = { "p", "s", "head" };
    docOperation.setElementNames(elemNames);
    // docHandler.doOperation(docOperation);

    final String docIdMega = "/mega/docs/MEGA_A2_B001-01_ETX.xml";
    docSrcUrlStr = "http://telota.bbaw.de:8085/exist/rest/db/mega/docs/MEGA_A2_B001-01_ETX.xml";
    docOperation = new CmsDocOperation("create", docSrcUrlStr, null, docIdMega);
    docOperation.setMainLanguage("deu");
    docOperation.setElementNames(elemNames);
    // docHandler.doOperation(docOperation);

    MorphologyCache.getInstance().end();
    LexHandler.getInstance().end();
  }

}