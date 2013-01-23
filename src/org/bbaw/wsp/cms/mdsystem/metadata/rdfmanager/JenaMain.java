package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.query.* ;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This class is designed to execute general tasks 
 * and to activate and control @RdfHandler and @WspRdfStore
 *  
 * @author marco juergens
 * @author sascha feldmann
 * @author marco seidler
 *
 */
public class JenaMain {
    
	/**
	 * examples for test purposes 
	 * 
	 */
	static final String oreTestBriefe = "/home/juergens/WspEtc/rdfData/Briefe.rdf";
	static final String oreTestSaschas = "/home/juergens/WspEtc/rdfData/AvH-Briefwechsel-Ehrenberg-sascha.rdf";
	static final String oreBiblioNeu = "/home/juergens/WspEtc/rdfData/BiblioNeu.rdf";
	private static final String EDOC = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/ParserTest/XSLTTest/outputs/v2_29_11_2012/eDocs";
	private static final String EDOC_1 = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/ParserTest/XSLTTest/outputs/v2_29_11_2012/eDocs/30.rdf";
	private static final String MODS = "C:/Dokumente und Einstellungen/wsp-shk1/Eigene Dateien/Development/ParserTest/XSLTTest/outputs/v2_29_11_2012/mods";
	
    private Model model;
	protected RdfHandler rdfHandler;
	protected WspRdfStore wspStore;
	protected Dataset dataset; 
	
	/**
	 * needs to be instantiated from outside e.g. org.bbaw.wsp.cms.test.TestLocal
	 */
    public JenaMain(){
    	
    }
    
    public void initStore() throws ApplicationException{
    	wspStore = new WspRdfStore("/home/juergens/wspTripleStore");
    	wspStore.createStore();
    	wspStore.createModelFactory();
    	dataset = wspStore.getDataset();
    	rdfHandler = new RdfHandler();
    	
    	doYourWork();
    }
    
    /**
     * call methods from here
     */
    private void doYourWork(){
//    	createNamedModelsFromOreSets("/home/juergens/WspEtc/rdfData/ModsToRdfTest");
//    	createNewModelFromSingleOre(oreBiblioNeu);
//    	getAllNamedModelsInDataset();
//    	wspStore.openDataset();
//    	Model model = wspStore.getNamedModel("http://edoc.bbaw.de/volltexte/2010/1347/pdf/13_brown.pdf");
//    	rdfHandler.deleteByJenaApi(model, "http://wsp.bbaw.de/wspMetadata", "http://purl.org/dc/terms/abstract", "knowledge browsing");
//    	rdfHandler.updateByJenaApi(model, "http://wsp.bbaw.de/wspMetadata", "http://purl.org/dc/terms/abstract", "knowledge browsing");
//    	wspStore.closeDataset();
//    	listAllStatementsbyJena("http://edoc.bbaw.de/volltexte/2010/1347/pdf/13_brown.pdf");
    	
//    	createNamedModelsFromOreSets(EDOC);
//    	createNamedModelsFromOreSets(MODS);
//    	createNewModelFromSingleOre(EDOC_1);
//    	model.close();
//		dataset.close();
//    	printIndex();
    }
    
    private void printIndex() {
		wspStore.getIndexStore().readIndex("+Humboldt");
		
	}

	/**
     * creates single namedmodel from code
     * 
     */
    private void createNewModelFromSingleOre(String location){
    	wspStore.openDataset();
    	Model freshModel = wspStore.getFreshModel();
    	Model model = rdfHandler.fillModelFromFile(freshModel, location);
    	String rdfAbout = rdfHandler.scanID(location);
    	wspStore.addNamedModelToWspStore(rdfAbout, model); 
    	wspStore.closeDataset();
    }
    
    /**
     * creates a namedmodel from code
     * @param location
     */
    private void createNamedModelsFromOreSets(String location){
    	List<String> pathList = extractPathLocally(location);
		System.out.println("pathlist : "+pathList.size());
		
		final long start = System.currentTimeMillis();
		wspStore.openDataset();
		for (String string : pathList) {
			Model freshsModel = wspStore.getFreshModel();
	    	Model m = rdfHandler.fillModelFromFile(freshsModel, string);
	    	String modsRdfAbout = rdfHandler.scanID(string);
	    	if(modsRdfAbout != null ) {
	    	  wspStore.addNamedModelToWspStore(modsRdfAbout, m); 	
	    	}
		}
		System.out.println("set read in time elapsed : "+(System.currentTimeMillis()-start)/1000);
		wspStore.closeDataset();
		}
		
    /**
     * simply prints all named Models in a dataset
     */
    protected void getAllNamedModelsInDataset(){
		wspStore.openDataset();
		Iterator<String> ite = dataset.listNames();
		while(ite.hasNext()){
			String s = ite.next();
			System.out.println("name : "+s);
		}
		wspStore.closeDataset();
    }

    /**
     * list all statements of the default model
     * same as queryAllBySelect() but without Sparql
     */
	private void listAllStatementsbyJena(){
        try{
			wspStore.openDataset();
	        Model defModel = dataset.getDefaultModel();
	        StmtIterator erator = defModel.listStatements();
	        while(erator.hasNext()){
	        	Statement st = erator.next();
	        	System.out.println("st : "+st);
	        }
	        wspStore.closeDataset();
        
		} catch (Exception e){
			e.printStackTrace();
		}
	}
    
    /**
     * list all statements of a named model
     * same as queryAllBySelect() but without Sparql
     * @param model
     */
	private void listAllStatementsbyJena(String model){
		try{
	        wspStore.openDataset();
	        Model briefe = dataset.getNamedModel(model);
	        StmtIterator erator = briefe.listStatements();
	        while(erator.hasNext()){
	        	Statement st = erator.next();
	        	System.out.println("st : "+st);
	        }
	        wspStore.closeDataset();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
    
    /**
     * TODO
     */
    private void createInferenceModel(){
		//create inference model
    	wspStore.createInfModel(dataset.getDefaultModel());
    	System.out.println("infmodel : "+wspStore.getRdfsModel());
//    	 list the nicknames
        StmtIterator iter = wspStore.getRdfsModel().listStatements();
        while (iter.hasNext()) {
            System.out.println("    " + iter.nextStatement());
        }
    }
    
    /**
     * list all statements of the amodel
     * same as queryAllBySelect() but without Sparql
     */
	private void queryAllStatementsFromJenaCode(){
		try{
			wspStore.openDataset();
	        Model briefe = dataset.getNamedModel("http://edoc.bbaw.de/volltexte/2010/1347/pdf/13_brown.pdf");
	        StmtIterator erator = briefe.listStatements();
	        while(erator.hasNext()){
	        	Statement st = erator.next();
	        	System.out.println("st : "+st);
	        }
	        wspStore.closeDataset();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * helper method to get all paths from a set of rdf files
	 * @param startUrl
	 * @return
	 */
	public List<String> extractPathLocally(String startUrl) {
	    List<String> pathList = new ArrayList<String>();
	    // home verzeichnis pfad Ã¼ber system variable
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
	
	/**
	 * Perform a select query.
	 * @param queryString
	 * @return the {@link QueryExecution} instance.
	 */
	public QueryExecution performQuery(String queryString) {
		Query query = QueryFactory.create(queryString);
		wspStore.openDataset();
		QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
		ResultSetFormatter.out(System.out, qExec.execSelect(), query);
		wspStore.closeDataset();
		return qExec;
	}
	
	/**
	 * Perform a select query.
	 * @param queryString
	 * @param m the {@link Model} which will be queried
	 * @return the {@link QueryExecution} instance.
	 */
	public QueryExecution performQuery(String queryString, Model m) {
		Query query = QueryFactory.create(queryString);
		
		wspStore.openDataset();
		QueryExecution qExec = QueryExecutionFactory.create(query, m);
		ResultSetFormatter.out(System.out, qExec.execSelect(), query);
		wspStore.closeDataset();
		return qExec;
	}
	
	
	public void makeDefaultGraphUnion() {
		Model unionModel = returnUnionModel(); 
		dataset.setDefaultModel(unionModel);
	}
	
	public Model returnUnionModel() {
		wspStore.openDataset();
		Model m= dataset.getNamedModel("urn:x-arq:UnionGraph"); 
		wspStore.closeDataset();
		return m;
	}

	public StoreIndex getIndexStore() {
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
	 * @return
	 */
	public long getNumberOfTriples() {
		return returnUnionModel().size();
	}
	
}
