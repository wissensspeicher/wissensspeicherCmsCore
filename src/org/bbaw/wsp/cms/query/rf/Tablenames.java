package org.bbaw.wsp.cms.query.rf;

/**
 * Class contains static values of Tablenames to make an easy change of tables
 * or columns names possible
 * 
 * @author shk2
 * 
 */

public class Tablenames {

    // Tablenames
    public static final String QUERIES = "Queries";
    public static final String QUERY_WORDS = "QueryWords";

    public static final String RELEVANT_DOCS = "RelevantDocs";
    public static final String QUERY_WORD_CONNECTION = "QueryWordConnection";
    public static final String RELEVANT_DOCS_CONNECTION = "RelevantDocsConnection";

    // column names for matching
    public static final String QUERIES_COL = "query";
    public static final String QUERY_WORDS_COL = "queryword";

    public static final String RELEVANT_DOCS_COL = "url";
    public static final String QUERY_WORD_CONNECTION_WORD_COL = "id_QueryWord";
    public static final String QUERY_WORD_CONNECTION_QUERY_COL = "id_Query";
    public static final String RELEVANT_DOCS_CONNECTION_DOC_COL = "id_revdocs";
    public static final String RELEVANT_DOCS_CONNECTION_QUERY_COL = "id_Querie";

}
