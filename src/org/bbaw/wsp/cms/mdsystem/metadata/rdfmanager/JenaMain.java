package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemQueryHandler;
import org.bbaw.wsp.cms.mdsystem.util.MdSystemConfigReader;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class is designed to execute general tasks and to activate and control @RdfHandler and @WspRdfStore
 * 
 * @author marco juergens
 * @author sascha feldmann
 * @author marco seidler
 * 
 */
public class JenaMain {

  private static final String datasetPath = MdSystemConfigReader.getInstance().getRdfMdDir();

  protected RdfHandler rdfHandler;
  protected WspRdfStore wspStore;
  protected Dataset dataset;
  Logger logger;

  public Dataset getDataset() {
    return dataset;
  }

  /**
   * needs to be instantiated from outside e.g. org.bbaw.wsp.cms.test.TestLocal
   */
  public JenaMain() {

  }

  public void initStore() throws ApplicationException {
    wspStore = WspRdfStore.getInstance();
    logger = Logger.getLogger(JenaMain.class);
    logger.info("datasetPath : " + datasetPath);
    wspStore.createStore(datasetPath);
    wspStore.createModelFactory();
    dataset = wspStore.getDataset();
    rdfHandler = new RdfHandler();
     doYourWork();
  }

  /**
   * call methods from here
   */
  private void doYourWork() {
    wspStore.setForce(true);
    createNamedModelsFromOreSets("/home/juergens/wspEtc/rdfData/wspNormdata/");
    // createNewModelFromSingleOre(oreBiblioNeu);
    // getAllNamedModelsInDataset();
    // wspStore.openDataset();
    // wspStore.getNamedModel("http://edoc.bbaw.de/volltexte/2010/1347/pdf/13_brown.pdf");
    // rdfHandler.deleteByJenaApi(model, "http://wsp.bbaw.de/wspMetadata",
    // "http://purl.org/dc/terms/abstract", "knowledge browsing");
    // rdfHandler.updateByJenaApi(model, "http://wsp.bbaw.de/wspMetadata",
    // "http://purl.org/dc/terms/abstract", "knowledge browsing");
    // wspStore.closeDataset();
    // listAllStatementsbyJena("http://edoc.bbaw.de/volltexte/2010/1347/pdf/13_brown.pdf");
    // createNamedModelsFromOreSets(EDOC);
    // createNamedModelsFromOreSets(MODS);
    // createNewModelFromSingleOre(EDOC_1);
    // model.close();
    // dataset.close();
    // printIndex();
  }

  /**
   * creates a namedmodel from code this should later be done by fuseki over http
   * 
   * @param location
   */
  private void createNamedModelsFromOreSets(final String location) {
    // read all rdf
    final List<String> pathList = extractPathLocally(location);
    logger.info("pathlist : " + pathList.size());

    final long start = System.currentTimeMillis();
    wspStore.openDataset();
    for (final String string : pathList) {
      final Model freshsModel = wspStore.getFreshModel();
      final Model m = rdfHandler.fillModelFromFile(freshsModel, string);
      logger.info("File path: " + string);
      final String rdfAbout = rdfHandler.scanID(string);
      if (rdfAbout != null) {
        try {
          wspStore.addNamedModelToWspStore(rdfAbout, m);
          logger.info(rdfAbout + " successfully added.");
        } catch (final ApplicationException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    logger.info("set read in time elapsed : " + (System.currentTimeMillis() - start) / 1000);
    wspStore.closeDataset();
  }

  /**
   * simply prints all named Models in a dataset
   */
  protected void getAllNamedModelsInDataset() {
    wspStore.openDataset();
    final Iterator<String> ite = dataset.listNames();
    while (ite.hasNext()) {
      final String s = ite.next();
      logger.info("name : " + s);
    }
    wspStore.closeDataset();
  }

  /**
   * helper method to get all paths from a set of rdf files
   * 
   * @param startUrl
   * @return
   */
  public List<String> extractPathLocally(final String startUrl) {
    final List<String> pathList = new ArrayList<String>();
    // home verzeichnis pfad Ã¼ber system variable
    // String loc = System.getenv("HOME")+"/wsp/configs";
    // out.println("hom variable + conf datei : "+loc);
    final File f = new File(startUrl);
    // out.println("readable : "+Boolean.toString(f.canRead()));
    // out.println("readable : "+f.isDirectory());
    if (f.isDirectory()) {
      final File[] filelist = f.listFiles();
      for (final File file : filelist) {
        if (file.getName().toLowerCase().contains("rdf")) {
          if (!startUrl.endsWith("/")) {
            pathList.add(startUrl + "/" + file.getName());
          } else {
            pathList.add(startUrl + file.getName());
          }
        }
      }
    }
    return pathList;
  }

  /**
   * Perform a select query.
   * 
   * @param queryString
   * @return the {@link QueryExecution} instance.
   */
  public QueryExecution performQuery(final String queryString) {
    final Query query = QueryFactory.create(queryString);
    wspStore.openDataset();
    final QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
    ResultSetFormatter.out(System.out, qExec.execSelect(), query);
    wspStore.closeDataset();
    return qExec;
  }

  /**
   * Perform a select query.
   * 
   * @param queryString
   * @param m
   *          the {@link Model} which will be queried
   * @return the {@link QueryExecution} instance.
   */
  public QueryExecution performQuery(final String queryString, final Model m) {
    final Query query = QueryFactory.create(queryString);

    wspStore.openDataset();
    final QueryExecution qExec = QueryExecutionFactory.create(query, m);
    ResultSetFormatter.out(System.out, qExec.execSelect(), query);
    wspStore.closeDataset();
    return qExec;
  }

  @Deprecated
  /**forbidden!!!**/
  public void makeDefaultGraphUnion() {
    final Model unionModel = returnUnionModel();
    wspStore.openDataset();
    dataset.setDefaultModel(unionModel);
    wspStore.closeDataset();
  }

  public Model returnUnionModel() {
    wspStore.openDataset();
    final Model m = dataset.getNamedModel("urn:x-arq:UnionGraph");
    wspStore.closeDataset();
    return m;
  }

  public IndexStore getIndexStore() {
    return wspStore.getIndexStore();
  }

  /**
   * @return the {@link RdfHandler}
   */
  public RdfHandler getManager() {
    return rdfHandler;
  }

  /**
   * Return the number of triples of all named graphs (the union of all graphes in the dataset)
   * 
   * @return
   */
  public long getNumberOfTriples() {
    return returnUnionModel().size();
  }

}
