package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools;

import java.util.Map;
import java.util.Set;

import org.openjena.riot.Lang;
import org.openjena.riot.RiotLoader;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;

public class RdfManager {

	private int anz;

	public RdfManager(int anz) {
		this.anz = anz;
	}

	/**
	 * 
	 * @param store
	 * @return
	 */
	public String getAllTriples(Model model) {
		StringBuffer dump = new StringBuffer();

		StmtIterator statements = model.listStatements();
		while (statements.hasNext()) {
			Statement statement = statements.nextStatement();
			RDFNode object = statement.getObject();
			dump.append(statement.getSubject().getLocalName())
					.append(' ')
					.append(statement.getPredicate().getLocalName())
					.append(' ')
					.append((object instanceof Resource
							|| object instanceof Literal ? object.toString()
							: '"' + object.toString() + "\"")).append('\n');
		}
		return dump.toString();
	}

	/**
	 * 
	 * @param store
	 * @return
	 */
	public String getAllTriplesAsDot(Model model) {
		StringBuffer dump = new StringBuffer();
		String temp = "";

		StmtIterator statements = model.listStatements();
		dump.append("digraph G { ");
		dump.append('\n');
		//dump.append("edge [len=2];");
		dump.append("ratio=\"0.2\"");
		dump.append('\n');
		
		dump.append("ranksep=\"1.0 equally\"");
		dump.append('\n');
		while (statements.hasNext()) {

			Statement statement = statements.nextStatement();
			// System.out.println("statements.nextStatement() "+statement);
			RDFNode object = statement.getObject();

			dump.append("\"").append(statement.getSubject().getLocalName())
					.append("\" ").append("-> \"");
			if (object.toString().length() < anz) {
				String[] array = object.toString().split("[/]+");
				for (int i = array.length - 1; i > 0; --i) {
					if (temp.length() <= anz)
						temp = array[i] + "/" + temp;
				}
			

			}
			dump.append(
					(object instanceof Resource || object instanceof Literal ? temp : '"' + temp + "\""))
					.append("\"").append(" [label=\"")
					.append(statement.getPredicate().getLocalName())
					.append("\"]").append(";").append('\n');

		}
		dump.append('}');
		return dump.toString();
	}

	/**
	 * INSERT
	 * 
	 * @param store
	 * @param queryString
	 */
	public void performUpdate(Dataset dataset, String queryString) {
		GraphStore graphStore = GraphStoreFactory.create(dataset);

		UpdateRequest request = UpdateFactory.create(queryString);
		UpdateProcessor proc = UpdateExecutionFactory.create(request,
				graphStore);
		proc.execute();

		// System.out.println("model : "+model);
		// System.out.println("query : "+queryString);

	}

	public void count(Dataset dataset) {

		// A SPARQL query will see the new statement added.
		QueryExecution qExec = QueryExecutionFactory.create(
				"SELECT (count(*) AS ?count) { ?s ?p ?o} LIMIT 10", dataset);
		ResultSet rs = qExec.execSelect();
		System.out.println("_____________check_reults_______________");
		try {
			ResultSetFormatter.out(rs);
		} finally {
			qExec.close();
		}

	}

	public QueryExecution selectSomething(String sparqlSelect, Dataset dataset) {
		QueryExecution queExec = QueryExecutionFactory.create(sparqlSelect,
				dataset);
		return queExec;
	}

	/**
	 * INSERT Graph
	 * 
	 * @param store
	 */
	public Model readFile(Model model, String loc) {
		Model moodel = FileManager.get().readModel(model, loc);
		//
		// StmtIterator statements = moodel.listStatements();
		// while (statements.hasNext()) {
		// Statement graphName = statements.next();
		// System.out.println("statements : "+graphName);
		// }

		return moodel;
	}

	public void readQuadsViaRiot(String filename, DatasetGraph dataset,
			Lang lang, String baseURI) {
		RiotLoader.read(filename, dataset, lang, baseURI);
	}

	public void readFileViaRiot(String filename, DatasetGraph dataset,
			Lang lang, String baseURI) {
		RiotLoader.read(filename, dataset, lang, baseURI);
	}

	/**
	 * DELETE
	 * 
	 * @param store
	 * @param queryString
	 */
	public void deleteRecord(Model model, String queryString) {

		@SuppressWarnings("unused")
		GraphStore graphStore = GraphStoreFactory.create(model);
		queryString = "DELETE WHERE { <http://avh.bbaw.de/biblio/Aggr> ?p ?o }";

		/**
		 * PREFIX dc: <http://purl.org/dc/elements/1.1/>
		 * 
		 * DELETE DATA FROM <http://example/bookStore> { <http://example/book3>
		 * dc:title "Fundamentals of Compiler Desing" }
		 */

	}

	/**
	 * DELETE all
	 */
	public void deleteAll() {
		/**
		 * 
		 * DELETE { ?book ?p ?v } WHERE { ?book dc:date ?date . FILTER ( ?date <
		 * "2000-01-01T00:00:00"^^xsd:dateTime ) ?book ?p ?v }
		 * 
		 */
	}

	/**
	 * copy
	 */
	public void copyRecords() {
		/**
		 * von einem namedGraph zu einem anderen
		 * 
		 * PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd:
		 * <http://www.w3.org/2001/XMLSchema#>
		 * 
		 * INSERT INTO <http://example/bookStore2> { ?book ?p ?v } WHERE { GRAPH
		 * <http://example/bookStore> { ?book dc:date ?date . FILTER ( ?date <
		 * "2000-01-01T00:00:00"^^xsd:dateTime ) ?book ?p ?v } }
		 * 
		 */
	}

	public void moveRecords() {
		/**
		 * 
		 * PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd:
		 * <http://www.w3.org/2001/XMLSchema#>
		 * 
		 * INSERT INTO <http://example/bookStore2> { ?book ?p ?v } WHERE { GRAPH
		 * <http://example/bookStore> { ?book dc:date ?date . FILTER ( ?date <
		 * "2000-01-01T00:00:00"^^xsd:dateTime ) ?book ?p ?v } }
		 * 
		 * DELETE FROM <http://example/bookStore> { ?book ?p ?v } WHERE { GRAPH
		 * <http://example/bookStore> { ?book dc:date ?date . FILTER ( ?date <
		 * "2000-01-01T00:00:00"^^xsd:dateTime ) ?book ?p ?v } }
		 * 
		 */
	}

	/**
	 * Helper method that splits up a URI into a namespace and a local part. It
	 * uses the prefixMap to recognize namespaces, and replaces the namespace
	 * part by a prefix.
	 * 
	 * @param prefixMap
	 * @param resource
	 */
	public static String[] split(PrefixMapping prefixMap, Resource resource) {
		String uri = resource.getURI();
		if (uri == null) {
			return new String[] { null, null };
		}
		Map<String, String> prefixMapMap = prefixMap.getNsPrefixMap();
		Set<String> prefixes = prefixMapMap.keySet();
		String[] split = { null, null };
		for (String key : prefixes) {
			String ns = prefixMapMap.get(key);
			if (uri.startsWith(ns)) {
				split[0] = key;
				split[1] = uri.substring(ns.length());
				return split;
			}
		}
		split[1] = uri;
		return split;
	}

	// public void navigateTest(){
	// // create an empty model
	// Model model = ModelFactory.createDefaultModel();
	//
	// // use the FileManager to find the input file
	// InputStream in = FileManager.get().open(oreTest);
	// if (in == null) {
	// throw new IllegalArgumentException( "File: " + oreTest + " not found");
	// }
	//
	// // read the RDF/XML file
	// model.read(new InputStreamReader(in), "");
	//
	// // retrieve the Adam Smith vcard resource from the model
	// Resource descr = model.getResource("http://avh.bbaw.de/biblio/Aggr");
	//
	// Resource statem = (Resource
	// )descr.getRequiredProperty(DC.title).getObject();
	//
	// System.out.println("statem : "+statem);
	//
	// // retrieve the value of the N property
	// // Resource name = (Resource)
	// descr.getRequiredProperty(DCTerms.creator).getObject();
	//
	// Statement s = descr.getProperty(DCTerms.creator);
	// System.out.println("as triple  : "+s.asTriple());
	// System.out.println("creator s ? : "+s.getSubject());
	// System.out.println("creator p ? : "+s.getPredicate());
	// System.out.println("creator o ? : "+s);
	//
	// if (s instanceof Resource) {
	// System.out.println("creator o ? : "+s);
	// } else {
	// // object is a literal
	// System.out.println(" \"" + s.toString() + "\"");
	// }
	//
	// // retrieve the given name property
	// String rights = descr.getRequiredProperty(DC.rights).getString();
	//
	// // add two nick name properties to vcard
	// descr.addProperty(DCTerms.format, "PDF");
	//
	// // set up the output
	// System.out.println("The rights of \"" + rights + "\" are:");
	// // list the nicknames
	// StmtIterator iter = descr.listProperties(DCTerms.format);
	// while (iter.hasNext()) {
	// System.out.println("    " + iter.nextStatement().getObject().toString());
	// }
	// }

}
