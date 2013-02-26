package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class JenaMainForAI {

	/**
	 * examples for test purposes
	 */
	static final String oreTestBriefe = "/home/juergens/WspEtc/rdfData/Briefe.rdf";
	static final String oreTestSaschas = "/home/juergens/WspEtc/rdfData/AvH-Briefwechsel-Ehrenberg-sascha.rdf";
	// static final String oreBiblio =
	// "/home/juergens/WspEtc/rdfData/AvHBiblio.rdf";
	static final String oreBiblioNeu = "/home/juergens/WspEtc/rdfData/BiblioNeu.rdf";
	private static final String EDOC = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/ParserTest/XSLTTest/outputs/v2_29_11_2012/eDocs";
	private static final String MODS = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/ParserTest/XSLTTest/outputs/v2_29_11_2012/mods";

	private Model model;
	private RdfHandler rdfHandler;
	private final WspRdfStore wspStore = WspRdfStore.getInstance();
	private Dataset dataset;
	private String source;
	private String destination;

	private StorePathHandler pathHandler = new StorePathHandler();

	/**
	 * needs to be instantiated from outside e.g.
	 * org.bbaw.wsp.cms.test.TestLocal
	 * 
	 */
	public JenaMainForAI() {

	}

	/**
	 * Contains all the main functionality features
	 * 
	 * @param newSet
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void initStore(final Boolean newSet) throws ClassNotFoundException,
			IOException {

		// inital Thread creates Store/Dataset/RDFManager
		final Thread initial = new Thread() {
			@Override
			public void run() {
				if (newSet) {
					String temp = "";
					if (System.getProperty("os.name").startsWith("Windows")) {

						String[] array = destination.split("[\\]+");
						for (int i = 0; i < array.length - 1; ++i) {
							temp += "\\" + array[i];
						}
						temp = temp.substring(1);

					} else {
						String[] array = destination.split("[/]+");

						for (int i = 0; i < array.length - 1; ++i) {
							temp += "/" + array[i];

						}
						temp = temp.substring(1);
					}

					try {
						pathHandler.setPath(temp);
						savePathFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {

					try {
						loadPathFile();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				try {

					wspStore.createStore(pathHandler.getPath());

				} catch (Exception e) {
					System.out.println("Store already in use");
					System.out.println(e.getMessage());
				}

				wspStore.createModelFactory();
				dataset = wspStore.getDataset();
				rdfHandler = new RdfHandler();

			}

		};

		initial.run();

		// Thread add one ore more files to a Dataset
		final Thread editStore = new Thread() {

			@Override
			public void run() {
				try {
					initial.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				wspStore.setForce(true);
				if (source != null) {
					if (new File(source).isDirectory()) {
						createNamedModelsFromOreSets(source);
					} else
						createNewModelFromSingleOre(source);
				}
				try {
					savePathFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		};

		editStore.run();

	}

	public void saveModel(final String name, final String fileName) {
		Thread saveAction = new Thread() {

			@Override
			public void run() {

				try {

					wspStore.createStore(pathHandler.getPath());
					wspStore.createModelFactory();
					wspStore.openDataset();
					dataset = wspStore.getDataset();
					Model model = dataset.getNamedModel(name);

					// removes old file
					File file = new File(fileName);
					if (file.exists()) {
						file.delete();
						file = new File(fileName);
					}

					model.write(new FileOutputStream(file), "RDF/XML");

					wspStore.closeDataset();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		saveAction.run();
	}

	/**
	 * Exports all models of a given Dataset to the given Directory
	 * 
	 * @param directory
	 */
	public void saveAllModel(final String directory) {
		try {
			wspStore.createStore(pathHandler.getPath());
			wspStore.createModelFactory();
			wspStore.openDataset();
			dataset = wspStore.getDataset();
			Iterator<String> it = dataset.listNames();
			while (it.hasNext()) {
				String temp = it.next();
				Model model = dataset.getNamedModel(temp);
				try {

					// removes old file
					String name = buildValidXMLName(temp, directory);
					File file = new File(name);
					if (file.exists()) {
						file.delete();

						file = new File(name);
					}

					model.write(new FileOutputStream(name), "RDF/XML");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		} catch (ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * builds the filename and returns it
	 * 
	 * @param model
	 * @param directory
	 * @return
	 */
	private String buildValidXMLName(String model, String directory) {
		String[] temp = model.split("[://.]+");
		return directory + "/" + temp[temp.length - 2] + temp[temp.length - 1]
				+ ".xml";

	}

	/**
	 * Gets all named Graphs of the Dataset an returns them as arraylist
	 * 
	 * @return
	 */
	public ArrayList<String> getModels() {
		final ArrayList<String> liste = new ArrayList<String>();

		Thread mainAction = new Thread() {
			@Override
			public void run() {

				try {
					// closeAll.join();
					// System.out.println(pathHandler.getPath());
					if (pathHandler.getPath().equals(""))
						loadPathFile();
					wspStore.createStore(pathHandler.getPath());
					wspStore.createModelFactory();
					dataset = wspStore.getDataset();

					wspStore.openDataset();
					Iterator<String> it = dataset.listNames();

					while (it.hasNext()) {
						liste.add(it.next());

					}
					wspStore.closeDataset();

				} catch (ApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};
		mainAction.run();

		return liste;
	}

	/**
	 * Removes the given named model from the dataset of the given location
	 * 
	 * @param name
	 */
	public void removeModel(String name) {

		try {
			loadPathFile();
			wspStore.createStore(pathHandler.getPath());
			wspStore.createModelFactory();
			dataset = wspStore.getDataset();
			wspStore.openDataset();
			dataset.removeNamedModel(name);
			wspStore.closeDataset();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Loads a .store data uses chosen folder as Triplestore
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void loadPathFile() throws IOException, ClassNotFoundException {

		FileInputStream input = new FileInputStream(destination);
		ObjectInputStream ois = new ObjectInputStream(input);
		pathHandler = (StorePathHandler) ois.readObject();

		ois.close();

	}

	/**
	 * saves WspStore to give File
	 * 
	 * @throws IOException
	 */
	private void savePathFile() throws IOException {
		OutputStream fos = null;

		try {
			fos = new FileOutputStream(destination);
			@SuppressWarnings("resource")
			ObjectOutputStream o = new ObjectOutputStream(fos);
			o.writeObject(pathHandler);
			o.flush();
		} catch (IOException e) {
			System.err.println(e);
		} finally {

			fos.close();

		}
	}

	/**
	 * call methods from here
	 * 
	 * @throws IOException
	 */
	private void doYourWork() throws IOException {
		// createNamedModelsFromOreSets("/home/juergens/WspEtc/rdfData/ModsToRdfTest");
		if (new File(source).isDirectory()) {
			createDatasetFromSet(source);
		} else
			createNewModelFromSingleOre(source);
		// getAllNamedModelsInDataset();

		// queryPerSparqlSelect();
		// createNamedModelsFromOreSets(MODS);
		// model.close();
		// dataset.close();
		savePathFile();
		// dataset.close();
		// wspStore.closeDataset();
		wspStore.closeDataset();

	}

	/**
	 * creates single namedmodel from code this should later be done by fuseki
	 * over http
	 * 
	 */
	private void createNewModelFromSingleOre(String file) {

		wspStore.openDataset();
		Model freshModel = wspStore.getFreshModel();
		Model model = rdfHandler.fillModelFromFile(freshModel, file);
		// function is used in initstore() - result = modelName
		String rdfAbout = checkifValid(rdfHandler.scanID(file), file);

		try {
			wspStore.addNamedModelToWspStore(rdfAbout, model);
		} catch (ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			wspStore.closeDataset();
		}

	}

	/**
	 * Checks if titel is null it generates one by document name else it returns
	 * given string;
	 * 
	 * @param titel
	 * @return
	 */
	private String checkifValid(String result, String filename) {
		if (result == null) {

			File file = new File(filename);
			String temp = "http://" + file.getName();
			System.out.println("new Name: " + temp);
			return temp;

		}

		return result;
	}

	/**
	 * Gets a folder destination, checks all Files in there and adds them to a
	 * Dataset
	 * 
	 * @param location
	 */
	private void createDatasetFromSet(final String location) {
		ArrayList<String> data = new ArrayList<String>();
		for (File file : new File(location).listFiles()) {
			if (validForModel(file))
				data.add(file.getAbsolutePath());
		}

		wspStore.openDataset();
		for (String string : data) {
			Model freshsModel = wspStore.getFreshModel();
			Model m = rdfHandler.fillModelFromFile(freshsModel, string);
			String modsRdfAbout = checkifValid(rdfHandler.scanID(string),
					string);
			try {
				wspStore.addNamedModelToWspStore(modsRdfAbout, m);
			} catch (ApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		wspStore.closeDataset();

	}

	/**
	 * Checks if filename is valid for adding into the model
	 * 
	 * @param name
	 * @return true if valid
	 */
	public boolean validForModel(File name) {

		if (name.getName().toLowerCase().endsWith(".rdf")
				|| name.getName().toLowerCase().endsWith(".nt")
				|| name.getName().toLowerCase().endsWith(".ttl"))
			return true;

		return false;
	}

	/**
	 * creates a namedmodel from code this should later be done by fuseki over
	 * http
	 * 
	 * @param location
	 */
	private void createNamedModelsFromOreSets(String location) {
		// read all mods rdf
		final List<String> pathList = extractPathLocally(location);
		System.out.println("pathlist : " + pathList.size());

		final long start = System.currentTimeMillis();
		wspStore.openDataset();
		for (final String string : pathList) {
			final Model freshsModel = wspStore.getFreshModel();
			final Model m = rdfHandler.fillModelFromFile(freshsModel, string);
			System.out.println("File path: " + string);
			final String modsRdfAbout = rdfHandler.scanID(string);
			if (modsRdfAbout != null) {
				try {
					wspStore.addNamedModelToWspStore(modsRdfAbout, m);
					System.out.println(modsRdfAbout + " successfully added.");
				} catch (final ApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		System.out.println("set read in time elapsed : "
				+ (System.currentTimeMillis() - start) / 1000);
		wspStore.closeDataset();
	}

	/**
	 * simply prints all named Models in a dataset
	 */
	private void getAllNamedModelsInDataset() {
		wspStore.openDataset();
		Iterator<String> ite = dataset.listNames();
		while (ite.hasNext()) {
			String s = ite.next();
			System.out.println("name : " + s);
		}
		wspStore.closeDataset();
	}

	// wspStore.openDataset();
	//
	// dataset = wspStore.getDataset();
	// manager.count(dataset);
	// Model model = dataset.getDefaultModel();
	// wspStore.closeDataset();

	/**
	 * TODO
	 */
	private void createInferenceModel() {
		// create inference model
		wspStore.createInfModel(dataset.getDefaultModel());
		System.out.println("infmodel : " + wspStore.getRdfsModel());
		// list the nicknames
		StmtIterator iter = wspStore.getRdfsModel().listStatements();
		while (iter.hasNext()) {
			System.out.println("    " + iter.nextStatement());
		}
	}

	// dataset = wspStore.getDataset();
	// wspStore.openDataset();
	// manager.count(dataset);
	// wspStore.closeDataset();
	//
	/**
	 * try your sparql SELECT here this should later be done by fuseki over http
	 */
	private void queryPerSparqlSelect() {
		wspStore.openDataset();
		String sparqlSelect = "select ?s ?p ?o where {?s ?p ?o}";
		// String sparqlSelect =
		// "PREFIX foaf:<http://xmlns.com/foaf/0.1/> SELECT ?familyName FROM NAMED <http://wsp.bbaw.de/oreTestBriefe> WHERE {?x foaf:familyName  ?familyName}";
		QueryExecution quExec = rdfHandler.selectSomething(sparqlSelect,
				dataset);
		// QueryExecution quExec = QueryExecutionFactory.create(
		// "PREFIX foaf:<http://xmlns.com/foaf/0.1/> SELECT ?familyName FROM NAMED <http://wsp.bbaw.de/oreTestBriefe> WHERE {?x foaf:familyName  ?familyName}"
		// , dataset) ;
		ResultSet res = quExec.execSelect();
		System.out.println("");
		System.out.println("_____________check_reults_______________");
		try {
			ResultSetFormatter.out(res);
		} finally {
			quExec.close();
		}
		wspStore.closeDataset();
	}

	/**
	 * TODO still considering if we really need this
	 */
	private void reify() {
		// turn triples into quads
		wspStore.openDataset();
		Model modsModel = dataset
				.getNamedModel("http://wsp.bbaw.de/oreTestBriefe");
		StmtIterator stit = modsModel.listStatements();
		while (stit.hasNext()) {
			Statement state = stit.next();
			// System.out.println("statements : "+state);
			ReifiedStatement reifSt = state
					.createReifiedStatement("http://wsp.bbaw.de/oreTestBriefe");
			System.out.println("is reified : " + state.isReified());
			System.out.println("reified statement : " + reifSt.toString());
		}
		System.out.println(rdfHandler.getAllTriples(model));
	}

	/**
	 * updates a namedModel
	 */
	private void updateModelBySparqlInsert() {
		String updateIntoNamed = "PREFIX dc:  <http://purl.org/dc/elements/1.1/> "
				+ "INSERT DATA INTO <http://wsp.bbaw.de/oreTestBriefe>"
				+ " { <http://www.bbaw.de/posterDh> dc:title  \"Digital Knowledge Store\" } ";
		String updateToDefault = "PREFIX dc:  <http://purl.org/dc/elements/1.1/> "
				+ "INSERT DATA "
				+ " { <http://www.bbaw.de/posterDh> dc:title  \"Digital Knowledge Store\" } ";
		wspStore.openDataset();
		rdfHandler.performUpdate(dataset, updateToDefault);
		wspStore.closeDataset();
	}

	/**
	 * get all statements by sparql SELECT from a single named Graph this should
	 * later be done by fuseki over http
	 */
	private void queryAllBySelect() {
		wspStore.openDataset();
		// select all FROM
		QueryExecution quecExec = QueryExecutionFactory
				.create("SELECT * FROM NAMED <http://pom.bbaw.de:8085/exist/rest/db/wsp/avh/avhBib.xml> WHERE { GRAPH <http://pom.bbaw.de:8085/exist/rest/db/wsp/avh/avhBib.xml> { ?s ?p ?o } }",
						dataset);
		ResultSet resu = quecExec.execSelect();
		System.out.println("_____________check_reults_______________");
		try {
			ResultSetFormatter.out(resu);
		} finally {
			quecExec.close();
		}
		wspStore.closeDataset();
	}

	/**
	 * same as queryAllBySelect() but without Sparql
	 */
	private void queryAllStatementsFromJenaCode() {
		wspStore.openDataset();
		Model briefe = dataset
				.getNamedModel("http://wsp.bbaw.de/oreTestBriefe");
		StmtIterator erator = briefe.listStatements();
		while (erator.hasNext()) {
			Statement st = erator.next();
			System.out.println("st : " + st);
		}
		wspStore.closeDataset();
	}

	public void buildLuceneIndex() {

	}

	/**
	 * TODO writes statement to dot format for visualization in graphviz
	 * 
	 * @param res
	 */
	private void writeToDotLang(String res) {
		try {
			OutputStream outputStream = new FileOutputStream(new File(
					"oreTestBriefe.dot"));
			Writer writer = new OutputStreamWriter(outputStream);

			writer.write(res);

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * helper method to get all paths from a set of rdf files
	 * 
	 * @param startUrl
	 * @return
	 */
	public List<String> extractPathLocally(String startUrl) {
		List<String> pathList = new ArrayList<String>();
		// home verzeichnis pfad über system variable
		// String loc = System.getenv("HOME")+"/wsp/configs";
		// out.println("hom variable + conf datei : "+loc);
		File f = new File(startUrl);
		// out.println("readable : "+Boolean.toString(f.canRead()));
		// out.println("readable : "+f.isDirectory());
		if (f.isDirectory()) {
			File[] filelist = f.listFiles();
			for (File file : filelist) {
				if (file.getName().toLowerCase().contains("rdf")
						|| file.getName().toLowerCase().endsWith(".nt")) {
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

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;

	}

}
