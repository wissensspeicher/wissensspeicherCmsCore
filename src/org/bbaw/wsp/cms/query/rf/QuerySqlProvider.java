package org.bbaw.wsp.cms.query.rf;

import java.sql.SQLException;
import java.util.ArrayList;

import org.bbaw.wsp.cms.mdsystem.util.WspJsonEncoder;
import org.json.simple.JSONObject;

/**
 * Class creates and manages the Database connection
 * 
 * @author shk2
 * 
 */
public class QuerySqlProvider extends Tablenames {

    private final MySqlConnector con;

    @SuppressWarnings("static-access")
    /**
     * Construktor of @QuerySqlProvider,
     * gets an instance of @MySqlConnector 
     * and sets the given connection values 
     * @param server - host of the database
     * @param port
     * @param databasename
     * @throws Exception
     */
    public QuerySqlProvider(String server, String port, String databasename)
	    throws Exception {
	super();
	con = MySqlConnector.getInstance();
	if (!con.hasConnectionInfos()) {
	    con.setValues(server, port, databasename);
	    con.readDataBase();
	}

	con.readDataBase();

    }

    /**
     * Closes the Database connection
     */
    public void closeConnection() {
	con.closeConnection();
    }

    /**
     * Methode uses the given query to create an new record in the database, if
     * or to increase the counter of the query. Futhermore it updates the
     * connection Database between Queries and QueryWords.
     * 
     * @param query
     * @throws SQLException
     */
    public void updateQueries(String query) throws SQLException {

	String queryWord = query.split("[ ]+")[0];
	System.out.println(queryWord);

	con.inserSingelElementToTable(QUERY_WORDS, QUERY_WORDS_COL, queryWord);

	con.inserSingelElementToTable(QUERIES, QUERIES_COL, query);

	con.updateQueries(queryWord);
    }

    /**
     * Methode uses the given docUrl to create an new record in the database, if
     * or to increase the counter of the docUrl. Futhermore it updates the
     * connection Database between Queries and RelevantDocs.
     * 
     * @param query
     * @param docUrl
     * @throws SQLException
     */
    public void updateDocs(String query, String docUrl) throws SQLException {

	con.inserSingelElementToTable(RELEVANT_DOCS, RELEVANT_DOCS_COL, docUrl);

	con.updateDocs(query, docUrl);
    }

    /**
     * Methode is used to add a single value to the given Table and colum
     * 
     * @param table
     * @param col
     * @param query
     * @throws SQLException
     */
    @SuppressWarnings("unused")
    private void inserSingelElementToTable(String table, String col,
	    String query) throws SQLException {
	con.inserSingelElementToTable(table, col, query);
    }

    @SuppressWarnings({ "static-access", "unused" })
    /**
     * Methode accutally creates the Database connection with the already 
     * setted values.
     * @throws Exception
     */
    private void readDataBase() throws Exception {
	con.readDataBase();
    }

    /**
     * Methode searches for Queries in the database which has a connection to
     * the given QueryWord. Futhermore it creates a JSON Object with the results
     * and returns the Object.
     * 
     * @param queryWord
     * @return
     * @throws SQLException
     */
    public JSONObject getQueries(String queryWord) throws SQLException {
	ArrayList<String> temp = con.getQueries(queryWord);

	WspJsonEncoder coder = new WspJsonEncoder();

	int i = 0;
	for (String string : temp) {
	    coder.putStrings("" + ++i, string);
	}

	return coder.getJsonObject();

    }

    /**
     * Methode searches for Documents in the database which has a connection to
     * the given Query. Futhermore it creates a JSON Object with the results and
     * returns the Object.
     * 
     * @param query
     * @return
     * @throws SQLException
     */
    public JSONObject getDocuments(String query) throws SQLException {
	ArrayList<String> temp = con.getDocmuents(query);

	WspJsonEncoder coder = new WspJsonEncoder();

	int i = 0;
	for (String string : temp) {
	    coder.putStrings("" + ++i, string);
	}

	return coder.getJsonObject();

    }

}
