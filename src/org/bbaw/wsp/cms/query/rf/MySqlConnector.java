package org.bbaw.wsp.cms.query.rf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Connector for JDBC, also the main SQL requests are created here.
 * 
 * @author shk2
 * 
 */
public class MySqlConnector extends Tablenames {
    private static Connection connect = null;

    private static String server;
    private static String port;
    private static String database;
    private static MySqlConnector con;
    private static Boolean hasInfos = false;

    /**
     * Methode sets constant values for the Database connection.
     * 
     * @param server
     * @param port
     * @param database
     */
    @SuppressWarnings("static-access")
    public void setValues(final String server, final String port,
	    final String database) {
	this.server = server;
	this.port = port;
	this.database = database;
    }

    /**
     * Singelton Pattern, returns the instance of {@link MySqlConnector} or
     * creates a new and returns it.
     * 
     * @return
     * @throws Exception
     */
    public static MySqlConnector getInstance() throws Exception {
	if (con == null) {
	    con = new MySqlConnector();

	}
	return con;
    }

    /**
     * Methode connects actually to the Database, throwns Exception if values
     * are not set yet or wrong
     * 
     * @throws Exception
     */
    public static void readDataBase() throws Exception {

	try {
	    // This will load the MySQL driver, each DB has its own driver
	    Class.forName("com.mysql.jdbc.Driver");
	    // Setup the connection with the DB
	    connect = getConnection();

	    hasInfos = true;
	    // Statements allow to issue SQL queries to the database

	    // Result set get the result of the SQL query

	} catch (Exception e) {
	    throw e;
	}

    }

    /**
     * Returns bool if connection was successfully created
     * 
     * @return
     */
    public Boolean hasConnectionInfos() {
	return hasInfos;
    }

    /**
     * Calls the JDBC driver and sets the user rights to the database
     * 
     * @return
     * @throws SQLException
     */
    private static Connection getConnection() throws SQLException {

	Connection conn = null;
	Properties connectionProps = new Properties();
	connectionProps.put("user", "root");
	connectionProps.put("password", "toor");

	conn = DriverManager.getConnection("jdbc:" + "mysql" + "://" + server
		+ ":" + port + "/" + database, connectionProps);

	return conn;
    }

    /**
     * Methode is used to add a single value to the given Table and colum
     * 
     * @param table
     * @param element
     *            (s)
     * @param value
     *            (s)
     * @throws SQLException
     */
    public void inserSingelElementToTable(String table, String element,
	    String value) throws SQLException {

	value = createValidSQLString(value);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;
	PreparedStatement preparedStatement = null;

	if (table == Tablenames.QUERIES) {
	    resultSet = statement
		    .executeQuery("select requests from Queries where query="
			    + value);

	    int temp = 0;
	    while (resultSet.next()) {
		temp = Integer.parseInt(resultSet.getString(1));
	    }

	    if (temp > 0) {
		updateSingelValueInTable(table, "requests", element, ""
			+ ++temp, value);
		resultSet.close();
		return;
	    } else {
		resultSet.close();
		preparedStatement = connect.prepareStatement("insert into "
			+ table + " (" + element + ",requests) " + " values "
			+ "(" + value + ",1);");
		preparedStatement.executeUpdate();

	    }

	}

	else if (table == Tablenames.QUERY_WORDS) {

	    resultSet = statement.executeQuery("select id from "
		    + Tablenames.QUERY_WORDS + " where queryWord=" + value);

	    int temp = 0;
	    while (resultSet.next()) {
		temp = Integer.parseInt(resultSet.getString(1));
	    }

	    if (!(temp > 0)) {

		preparedStatement = connect.prepareStatement("insert into "
			+ table + " (" + element + ") values (" + value + ");");
		preparedStatement.executeUpdate();
		return;
	    }
	}

	else if (table == Tablenames.RELEVANT_DOCS) {

	    resultSet = statement.executeQuery("select requests from "
		    + RELEVANT_DOCS + " where " + RELEVANT_DOCS_COL + " = "
		    + value);

	    int temp = 0;
	    while (resultSet.next()) {
		temp = Integer.parseInt(resultSet.getString(1));
	    }

	    if (temp > 0) {
		updateSingelValueInTable(table, "requests", element, ""
			+ ++temp, value);
		resultSet.close();
		return;
	    } else {
		resultSet.close();

		preparedStatement = connect.prepareStatement("insert into "
			+ table + " (" + element + ",requests) values ("
			+ value + ",1);");
		preparedStatement.executeUpdate();
		return;
	    }
	}

    }

    /**
     * Creates new Connections in RelevantDocsConnection the given query
     * 
     * @param query
     * @param docUrl
     * @throws SQLException
     */
    @SuppressWarnings("null")
    public void updateDocs(String query, String docUrl) throws SQLException {

	String id_doc = createValidSQLString(getID(RELEVANT_DOCS, docUrl));
	String id_query = createValidSQLString(getID(QUERIES, query));

	if (id_doc == null || id_query == null)
	    return;

	query = createValidSQLString(query);
	docUrl = createValidSQLString(docUrl);

	ResultSet resultSet = null;
	Statement statement = connect.createStatement();
	PreparedStatement preparedStatement = null;
	int temp = 0;

	String request = "select id from " + RELEVANT_DOCS_CONNECTION
		+ " where " + RELEVANT_DOCS_CONNECTION_DOC_COL + " = " + id_doc
		+ " and " + RELEVANT_DOCS_CONNECTION_QUERY_COL + " = "
		+ id_query + ";";

	resultSet = statement.executeQuery(request);

	while (resultSet.next()) {

	    temp = Integer.parseInt(resultSet.getString(1));
	}

	resultSet.close();
	statement.close();

	if (temp == 0) {

	    request = "insert into " + RELEVANT_DOCS_CONNECTION + " ("
		    + RELEVANT_DOCS_CONNECTION_DOC_COL + ", "
		    + RELEVANT_DOCS_CONNECTION_QUERY_COL + ") values ("
		    + id_doc + ", " + id_query + ");";

	    preparedStatement = connect.prepareStatement(request);

	    preparedStatement.executeUpdate();

	}

    }

    /**
     * Returns the id in the given table, where the value has the value of the
     * given value, sounds may stupid but works xD
     * 
     * @param table
     * @param value
     * @return
     * @throws SQLException
     */
    private String getID(String table, String value) throws SQLException {

	value = createValidSQLString(value);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;

	String element = "";

	switch (table) {
	case QUERY_WORDS:
	    element = QUERY_WORDS_COL;
	    break;
	case QUERIES:
	    element = QUERIES_COL;
	    break;

	case RELEVANT_DOCS:
	    element = RELEVANT_DOCS_COL;
	    break;
	default:
	    return null;

	}

	String temp = null;
	resultSet = statement.executeQuery("select id from " + table
		+ " where " + element + " = " + value + ";");
	while (resultSet.next()) {
	    temp = resultSet.getString(1);
	}
	resultSet.close();
	return temp;

    }

    /**
     * Creates new Connections in QueryWordConnection the given query
     * 
     * @param value
     * @throws SQLException
     */
    public void updateQueries(String value) throws SQLException {

	String stat = "select " + QUERIES_COL + " from " + QUERIES + ";";

	Statement st = connect.createStatement();

	ResultSet resultSet = st.executeQuery(stat);

	String id_qword = getID(QUERY_WORDS, value);

	if (id_qword == null)
	    return;

	// int i = 0;
	resultSet.beforeFirst();
	while (resultSet.next()) {

	    String temp = resultSet.getString(1);

	    if (temp.contains(value)) {

		String id_q = getID(QUERIES, temp);

		inserQueryWordConnection(id_qword, id_q);

	    }

	}
	resultSet.close();

    }

    /**
     * Methode to inser a value used in updateDocs Methode
     * 
     * @param queryWord_id
     * @param query_id
     * @throws SQLException
     */
    private void inserQueryWordConnection(String queryWord_id, String query_id)
	    throws SQLException {

	queryWord_id = createValidSQLString(queryWord_id);
	query_id = createValidSQLString(query_id);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;
	PreparedStatement preparedStatement = null;

	resultSet = statement.executeQuery("select id from "
		+ QUERY_WORD_CONNECTION + " where "
		+ QUERY_WORD_CONNECTION_WORD_COL + " = " + queryWord_id
		+ " and " + QUERY_WORD_CONNECTION_QUERY_COL + " = " + query_id);

	int temp = 0;
	while (resultSet.next()) {
	    temp = Integer.parseInt(resultSet.getString(1));
	}

	if (!(temp > 0)) {

	    resultSet.close();
	    preparedStatement = connect.prepareStatement("insert into "
		    + QUERY_WORD_CONNECTION + " ("
		    + QUERY_WORD_CONNECTION_WORD_COL + ", "
		    + QUERY_WORD_CONNECTION_QUERY_COL + ") " + " values ("
		    + queryWord_id + ", " + query_id + ");");
	    preparedStatement.executeUpdate();

	}

    }

    /**
     * Methode returns an Arraylist of matching Documents to the given query,
     * returns empty list if none does.
     * 
     * @param query
     * @return
     * @throws SQLException
     */
    public ArrayList<String[]> getDocmuents(String query) throws SQLException {

	ArrayList<String[]> list = new ArrayList<String[]>();

	String id_query = createValidSQLString(getID(QUERIES, query));

	query = createValidSQLString(query);

	ResultSet resultSet = null;
	Statement statement = connect.createStatement();

	if (id_query == null)
	    return null;

	String request = "select " + RELEVANT_DOCS_CONNECTION_DOC_COL
		+ " from " + RELEVANT_DOCS_CONNECTION + " where "
		+ RELEVANT_DOCS_CONNECTION_QUERY_COL + " = " + id_query + ";";

	resultSet = statement.executeQuery(request);

	ArrayList<String> temp = new ArrayList<String>();

	while (resultSet.next()) {
	    temp.add(resultSet.getString(1));

	}

	if (temp.isEmpty()) {
	    return list;
	}

	request = "select " + RELEVANT_DOCS_COL + ",requests from "
		+ RELEVANT_DOCS + " where ";

	for (int i = 0; i < temp.size(); i++) {
	    if (i == temp.size() - 1) {
		request += " id = " + temp.get(i) + " ";
	    } else
		request += " id = " + temp.get(i) + " or ";

	}
	request += " order by requests ASC;";

	temp.clear();
	statement.close();

	Statement stat = connect.createStatement();
	ResultSet set = null;
	set = stat.executeQuery(request);

	while (set.next()) {
	    String[] array = new String[2];
	    array[0] = set.getString(1);
	    array[1] = set.getString(2);
	    list.add(array);
	}
	set.close();
	stat.close();

	return list;

    }

    /**
     * Updates a singel values given Table and cloumn, with given new value
     * under given condition with the conditionValue
     * 
     * @param table
     * @param columnToUpdate
     * @param columnConditionToUpdate
     * @param value
     * @param conditionValue
     * @throws SQLException
     */
    public void updateSingelValueInTable(String table, String columnToUpdate,
	    String columnConditionToUpdate, String value, String conditionValue)
	    throws SQLException {

	PreparedStatement preparedStatement = null;
	preparedStatement = connect.prepareStatement("update " + table
		+ " set " + columnToUpdate + " = " + value + " where "
		+ columnConditionToUpdate + " = " + conditionValue + ";");
	preparedStatement.executeUpdate();

	preparedStatement.close();

    }

    /**
     * Methode returns matching Arraylist of Queries to given QueryWord, if none
     * does empty List returns
     * 
     * @param value
     * @return
     * @throws SQLException
     */
    public ArrayList<String[]> getQueries(String value) throws SQLException {

	ArrayList<String[]> list = new ArrayList<String[]>();

	value = createValidSQLString(value);

	String request = "select " + QUERY_WORD_CONNECTION_QUERY_COL + " from "
		+ QUERY_WORD_CONNECTION + " where "
		+ QUERY_WORD_CONNECTION_WORD_COL + " = (select id from "
		+ QUERY_WORDS + " where " + QUERY_WORDS_COL + " = " + value
		+ ");";

	ArrayList<String> temp = new ArrayList<String>();

	value = createValidSQLString(value);

	Statement statement = connect.createStatement();
	ResultSet resultSet = null;

	resultSet = statement.executeQuery(request);

	while (resultSet.next()) {
	    temp.add(resultSet.getString(1));
	}
	resultSet.close();

	if (temp.isEmpty())
	    return list;

	request = "select " + QUERIES_COL + ",requests from " + QUERIES
		+ " where ";

	for (int i = 0; i < temp.size(); i++) {
	    if (i == temp.size() - 1) {
		request += " id = " + temp.get(i) + " ";
	    } else
		request += " id = " + temp.get(i) + " or ";

	}
	request += " order by requests ASC;";

	temp.clear();
	statement.close();

	Statement stat = connect.createStatement();
	ResultSet set = null;
	set = stat.executeQuery(request);

	while (set.next()) {
	    String[] array = new String[2];
	    array[0] = set.getString(1);
	    array[1] = set.getString(2);
	    list.add(array);
	}
	set.close();
	stat.close();

	return list;

    }

    /**
     * Methode sets a String in "" and returns it to match sql syntax
     * 
     * @param str
     * @return
     */
    private String createValidSQLString(String str) {
	return "\"" + str + "\"";
    }

    /**
     * JDBC Connection closes
     */
    public void closeConnection() {
	try {

	    if (connect != null) {
		connect.close();
	    }
	} catch (Exception e) {

	}
    }

}
