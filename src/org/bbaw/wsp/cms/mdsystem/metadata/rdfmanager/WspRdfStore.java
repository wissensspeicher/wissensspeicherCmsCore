package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class represents the RDF-Store itself
 * 
 * @author marco juergens
 * @author sascha feldmann
 * @author marco seidler
 */
@SuppressWarnings("serial")
public class WspRdfStore implements Serializable {

  protected String directory = "";
  private transient Dataset dataset;
  private transient Model defaultModel;
  private InfModel rdfsModel;
  private transient ModelMaker modelmaker;
  private List<String> modelList;

  /**
   * The global (persistent) store index.
   */
  private StoreIndex indexStore;

  /**
   * 
   * @return {@link StoreIndex}
   */
  public StoreIndex getIndexStore() {
    return indexStore;
  }

  /**
   * Give here the path to the Store
   * 
   * @param pathtoSave
   */
  public WspRdfStore(final String pathtoSave) {
    directory = pathtoSave;
  }

  public void createStore() throws ApplicationException {
    System.out.println("create Store");
    dataset = TDBFactory.createDataset(directory);
    defaultModel = dataset.getDefaultModel();
    modelList = new ArrayList<String>();
    TDB.getContext().set(TDB.symUnionDefaultGraph, true);
    //		loadIndexStore();
    indexStore = new StoreIndex();
  }

  public void createModelFactory() {
    modelmaker = ModelFactory.createMemModelMaker();
  }

  /**
   * Create a fresh model and register it to the LARQ Index Builder.
   * 
   * @return
   */
  public Model getFreshModel() {
    final Model model = modelmaker.createFreshModel();
    // Let the LARQ Index Builder react on changes of the fresh model
    model.register(indexStore.getLarqBuilder());
    return model;
  }

  public void addNamedModelToWspStore(final String name, final Model model) {
    if (!dataset.containsNamedModel(name)) {
      if (model != null) {
        dataset.addNamedModel(name, model);
        modelList.add(name);
      }
    }
  }

  public Model getNamedModel(final String name){
    if (dataset.getNamedModel(name) == null) {
      try {
        throw new Exception("desired Model doesn't exist.");
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return dataset.getNamedModel(name);
  }

  public void openDataset() {
    dataset.begin(ReadWrite.WRITE);
  }

  /**
   * Close transaction and "commit" current LARQ index.
   */
  public void closeDataset() {
    indexStore.commitIndex();
    dataset.commit();
    dataset.end();
    //		persistIndexStore();
  }

  public Model getMergedModelofAllGraphs() {
    return dataset.getNamedModel("urn:x-arq:UnionGraph");
  }

  public void createInfModel(final Model model) {
    System.out.println("create Infmodel");
    if (model != null) {
      rdfsModel = ModelFactory.createRDFSModel(model);
    }
  }

  public Model getRdfsModel() {
    return rdfsModel;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public String getTripleCountOfDefaultModel() {
    return "Default Graph contains: " + defaultModel.size()
        + " triples";
  }

  /**
   * Create a lucene index for a given model. The index will be activated
   * automaticly.
   * 
   * @param model
   *            - the {@link Model} for which the index is created.
   */
  public void createIndexFromModel(final Model model) {
    indexStore.addModelToIndex(model);
  }
}
