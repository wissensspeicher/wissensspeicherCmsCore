package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.*;

public class JenaTester {

	//private Model model;
	//private Dataset dataset;
	private JenaWspStore wspStore;
	private RdfManager manager;
	private File file;
	private String des;
	private String dot;

	// static final String oreTestBriefe =
	// "/home/juergens/WspEtc/test/Briefe.rdf";
	// static final String oreTestSaschas =
	// "/home/juergens/WspEtc/test/AvH-Briefwechsel-Ehrenberg-sascha.rdf";
	// static final String inputFileName =
	// "/home/juergens/workspace/wspApp/wspCmsCore/src/org/bbaw/wsp/cms/metadata/rdfJenaTest.rdf";
	// static final String oreTest = "/home/juergens/WspEtc/test/AvHBiblio.rdf";

	public JenaTester() {

	}

	/**
	 * Creates the name of the dot file by changing the name from .rdf to .dot
	 * 
	 * @return
	 */
	public String getID() {
		String id = file.getName();
		dot = id.substring(0, id.length() - 4) + ".dot";

		return des + dot;

	}

	/**
	 * Creates the model, parses the model and save it as dot
	 * 
	 * @param dat
	 * @param d
	 * @param anz
	 * @return the dot file name
	 */
	public String testStore(File dat, String d, int anz) {
		des = d + "/";

		file = dat;
		// DatasetImpl dsImpl = new DatasetImpl(dataset);
		wspStore = new JenaWspStore();

		wspStore.createStore();
		wspStore.createModelFactory();
		// removeAll() performed?

		// dataset = wspStore.getDataset();
		manager = new RdfManager(anz);

		Model model = wspStore.getFreshModel();

		model = manager.readFile(model, file.getAbsolutePath());

		writeToDotLang(manager.getAllTriplesAsDot(model));
		return dot;

		// wspStore.openDataset();
		// Model freshModel = wspStore.getFreshModel();
		// Model moodel = manager.readFile(freshModel, dataset, oreTestSaschas);
		// wspStore.addNamedModelToWspStore("http://wsp.bbaw.de/oreTestSaschas",
		// moodel);
		// wspStore.closeDataset();
		//
		// manager.readFileViaRiot("/home/juergens/WspEtc/test/Briefe.rdf",
		// dataset.asDatasetGraph(), Lang.RDFXML,
		// "http://wsp.example.org/Briefe");
		// wspStore.addNamedModelToWspStore("http://wsp.example.org/Briefe",
		// dsGraph);

		// //read all mods rdf
		// List<String> pathList =
		// extractPathLocally("/home/juergens/WspEtc/test/ModsToRdfTest");
		// System.out.println("pathlist : "+pathList.size());
		//
		// for (String string : pathList) {
		// wspStore.openDataset();
		// Model freshsModel = wspStore.getFreshModel();
		// Model m = manager.readFile(freshsModel, dataset);
		// //woher name des model nehmen: klasse für id finden
		// wspStore.addNamedModelToWspStore(string, m);
		// wspStore.closeDataset();
		// }

		// wspStore.openDataset();
		//
		// dataset = wspStore.getDataset();
		// manager.count(dataset);
		// Model model = dataset.getDefaultModel();
		// wspStore.closeDataset();

		// wspStore.createInfModel();
		// System.out.println("infmodel : "+wspStore.getRdfsModel());
		// list the nicknames
		// StmtIterator iter = wspStore.getRdfsModel().listStatements();
		// while (iter.hasNext()) {
		// System.out.println("    " + iter.nextStatement());
		// }

		// dataset = wspStore.getDataset();
		// wspStore.openDataset();
		// manager.count(dataset);
		// wspStore.closeDataset();
		//
		// wspStore.openDataset();
		// String sparqlSelect =
		// "PREFIX foaf:<http://xmlns.com/foaf/0.1/> SELECT ?familyName FROM NAMED <http://wsp.bbaw.de/oreTestBriefe> WHERE {?x foaf:familyName  ?familyName}";
		// QueryExecution quExec = manager.selectSomething(sparqlSelect,
		// dataset);
		// // QueryExecution quExec = QueryExecutionFactory.create(
		// //
		// "PREFIX foaf:<http://xmlns.com/foaf/0.1/> SELECT ?familyName FROM NAMED <http://wsp.bbaw.de/oreTestBriefe> WHERE {?x foaf:familyName  ?familyName}"
		// // , dataset) ;
		// ResultSet res = quExec.execSelect() ;
		//
		// System.out.println("_____________check_reults_______________");
		// try{
		// ResultSetFormatter.out(res) ;
		// } finally{quExec.close(); }
		// wspStore.closeDataset();

		// wspStore.openDataset();
		// QueryExecution queExec =
		// manager.selectSomething("SELECT * FROM NAMED <http://wsp.bbaw.de/oreTestBriefe> WHERE { GRAPH <http://wsp.bbaw.de/oreTestBriefe> { ?s ?p ?o } }",
		// dataset);
		// ResultSet ress = queExec.execSelect() ;
		// // String trip =
		// manager.getAllTriplesAsDot(dataset.getNamedModel("http://wsp.bbaw.de/oreTestBriefe"));
		// // writeToDotLang(trip);
		// System.out.println("_____________check_reults_______________");
		// try{
		// ResultSetFormatter.out(ress) ;
		// } finally{queExec.close(); }
		// wspStore.closeDataset();

		// System.out.println("dsimpl : "+dsImpl.containsNamedModel("http://wsp.bbaw.de/oreTestBriefe"));

		// wspStore.openDataset();
		// Iterator<String> ite = dataset.listNames();
		// while(ite.hasNext()){
		// String s = ite.next();
		// System.out.println("name : "+s);
		// }
		// System.out.println("contains? "+dataset.containsNamedModel("/home/juergens/WspEtc/test/ModsToRdfTest/20091113-533850-WS-BBAW.xml.rdf"));
		// wspStore.closeDataset();

		// turn triples into quads
		// wspStore.openDataset();
		// Model modsModel =
		// dataset.getNamedModel("http://wsp.bbaw.de/oreTestBriefe");
		// StmtIterator stit = modsModel.listStatements();
		// while(stit.hasNext()){
		// Statement state = stit.next();
		// // System.out.println("statements : "+state);
		// ReifiedStatement reifSt =
		// state.createReifiedStatement("http://wsp.bbaw.de/oreTestBriefe");
		// System.out.println("is reified : "+state.isReified());
		// System.out.println("reified statement : "+reifSt.toString());
		// }
		//
		// System.out.println(manager.getAllTriples(model));

		// String updateIntoNamed =
		// "PREFIX dc:  <http://purl.org/dc/elements/1.1/> "
		// + "INSERT DATA INTO <http://wsp.bbaw.de/oreTestBriefe>"
		// +
		// " { <http://www.bbaw.de/posterDh> dc:title  \"Digital Knowledge Store\" } ";
		// String updateToDefault =
		// "PREFIX dc:  <http://purl.org/dc/elements/1.1/> "
		// + "INSERT DATA "
		// +
		// " { <http://www.bbaw.de/posterDh> dc:title  \"Digital Knowledge Store\" } ";
		// wspStore.openDataset();
		// manager.performUpdate(dataset, updateToDefault);
		//
		// wspStore.closeDataset();

		// wspStore.openDataset();
		// //select all FROM
		// QueryExecution quecExec = QueryExecutionFactory.create(
		// //
		// "SELECT * FROM NAMED <http://wsp.bbaw.de/oreTestBriefe> WHERE { GRAPH <http://wsp.bbaw.de/oreTestBriefe> { ?s ?p ?o } }"
		// "SELECT * where { ?s ?p ?o }"
		// , dataset) ;
		// ResultSet resu = quecExec.execSelect() ;
		// System.out.println("_____________check_reults_______________");
		// try {
		// ResultSetFormatter.out(resu) ;
		// } finally { quecExec.close() ; }
		// wspStore.closeDataset();
		//
		// StmtIterator rator = model.listStatements();
		// while(rator.hasNext()){
		// Statement st = rator.next();
		// System.out.println("st : "+st);
		// }
		//
		// wspStore.openDataset();
		// Model briefe =
		// dataset.getNamedModel("http://wsp.bbaw.de/oreTestBriefe");
		// StmtIterator erator = briefe.listStatements();
		// while(erator.hasNext()){
		// Statement st = erator.next();
		// System.out.println("st : "+st);
		// }
		// wspStore.closeDataset();
		// model.close();
		// dataset.close();
	}

	/**
	 * Writes the Dot file into filesystem
	 * 
	 * @param res
	 */
	private void writeToDotLang(String res) {
		try {

			File file = new File(getID());

			OutputStream outputStream = new FileOutputStream(file);

			Writer writer = new OutputStreamWriter(outputStream);

			writer.write(res);

			writer.close();

			// super.println(dot + " has been created.");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

}
