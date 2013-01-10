package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;
import org.apache.jena.larq.LARQ;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

@SuppressWarnings("serial")
public class WspRdfStore implements Serializable {

	private static final String INDEX_LOCATION = "save/rdfmanager/storeindex.ser";
	protected String directory = "";
	private transient Dataset dataset;
	private transient Model defaultModel;
	private InfModel rdfsModel;
	private transient ModelMaker modelmaker;
	private List<String> modelList;
	private DatasetGraphTDB dsdt;
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
	public WspRdfStore(String pathtoSave) {
		directory = pathtoSave;
	}

	public void createStore() throws ApplicationException {
		System.out.println("create Store");
		// Location loc = new Location(directory);
		// StoreConnection sc = StoreConnection.make(loc);
		// GraphTDB graphTdb = sc.begin(ReadWrite.WRITE).getDefaultGraphTDB();
		// dataset = sc.begin(ReadWrite.WRITE).toDataset();
		// dsdt = sc.getBaseDataset();
		// dataset = dsdt.toDataset();
		dataset = TDBFactory.createDataset(directory);

		defaultModel = dataset.getDefaultModel();
		// defaultModel.removeAll();
		modelList = new ArrayList<String>();
		TDB.getContext().set(TDB.symUnionDefaultGraph, true);
		// System.out.println("namedModel : ");
		// StmtIterator iter = namedModel.listStatements();
		// while (iter.hasNext()) {
		// System.out.println("    " + iter.nextStatement());
		// }
		// model.removeAll();
//		loadIndexStore();
		indexStore = new StoreIndex();
	}

	@Deprecated
	/**
	 * Load the index store from INDEX_LOCATION if available.
	 * 
	 * @throws ApplicationException
	 */
	private void loadIndexStore() throws ApplicationException {
		final File storeFile = new File(INDEX_LOCATION);

		if (storeFile.exists()) { // load from serialization
			try {
				ObjectInputStream o = new ObjectInputStream(new FileInputStream(storeFile));
				Object readOb = o.readObject();
				if (readOb instanceof StoreIndex) {
					indexStore = (StoreIndex) readOb;
					o.close();
				} else {
					o.close();
					throw new ApplicationException(
							"The serialized object is not of the type StoreIndex. Please check the file "
									+ storeFile
									+ " and either repair or remove it.");
				}				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {  // create new StoreIndex
			indexStore = new StoreIndex();
		}

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
		Model model = modelmaker.createFreshModel();
		// Let the LARQ Index Builder react on changes of the fresh model
		model.register(indexStore.getLarqBuilder());
		return model;
	}

	public void addNamedModelToWspStore(String name, Model model) {
		if (!this.dataset.containsNamedModel(name)) {
			if (model != null) {
				this.dataset.addNamedModel(name, model);
				modelList.add(name);
			}
		}
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

	@Deprecated
	/**
	 * Persist the index store.
	 */
	private void persistIndexStore() {
		final File storeFile = new File(INDEX_LOCATION);
		
		if(!storeFile.exists()) {
			try {
				storeFile.createNewFile();
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
		
		try {
			ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(storeFile));
			o.writeObject(indexStore);
			o.flush();
			o.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	public Model getMergedModelofAllGraphs() {
		return dataset.getNamedModel("urn:x-arq:UnionGraph");
	}

	public void createInfModel(Model model) {
		System.out.println("create Infmodel");
		if (model != null)
			rdfsModel = ModelFactory.createRDFSModel(model);
	}

	public Model getRdfsModel() {
		return this.rdfsModel;
	}

	public Dataset getDataset() {
		return this.dataset;
	}

	public String getTripleCountOfDefaultModel() {
		return "Default Graph contains: " + this.defaultModel.size()
				+ " triples";
	}

	/**
	 * Create a lucene index for a given model. The index will be activated
	 * automaticly.
	 * 
	 * @param model
	 *            - the {@link Model} for which the index is created.
	 */
	public void createIndexFromModel(Model model) {
		indexStore.addModelToIndex(model);		
	}
}
