package org.bbaw.wsp.cms.collections;

import java.util.ArrayList;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class OaiPmhSetManager {

  CollectionReader cReader;
  String source = "/opt/apache-tomcat-7.0.29/webapps/oai/WEB-INF/repository_settings_and_data/ListSets-config.xml";
  private XQueryEvaluator xQueryEvaluator;
  private ArrayList<Collection> collections;

  public static void main(String[] args) throws ApplicationException {
    OaiPmhSetManager setManager = new OaiPmhSetManager();
    setManager.init();
    setManager.convert();
  }

  public OaiPmhSetManager() {
  }

  public void init(){
		try {
			this.cReader = CollectionReader.getInstance();
			this.xQueryEvaluator = new XQueryEvaluator();
			this.collections = cReader.getCollections();
		} catch (ApplicationException e) {
			e.printStackTrace();
		}
	}
  
  private void convert() throws ApplicationException{
    
    for (Collection c : collections) {
      Collection collection = cReader.getCollection(c.getName());
      System.out.println(collection.getId()); 
    }
    
    
  }
}
