package org.bbaw.wsp.cms.document;

import java.util.ArrayList;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.json.simple.JSONObject;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class Person {
  public static int AUTHOR = 0; 
  public static int EDITOR = 1; 
  public static int TECHNICAL_WRITER = 2; 
  public static int ACTOR = 3; 
  private int role = AUTHOR;  // default
  private String name;  // whole name: built from: surname, forename if they exists
  private String surname;
  private String forename;
  private String gndLink;
  private String aboutLink;
  private String language;

  public Person() {
  }
  
  public Person(String personString) {
    personString = personString.trim();
    name = personString;
    surname = personString;
    String otherNames = null;
    if (personString.contains(",")) {
      int index = personString.indexOf(",");
      surname = personString.substring(0, index);
      otherNames = personString.substring(index + 1);
      forename = otherNames.trim();
    }
  }
  
  public static ArrayList<Person> fromXmlStr(XQueryEvaluator xQueryEvaluator, String xmlStr) throws ApplicationException {
    ArrayList<Person> retPersons = null;
    XdmValue xmdValuePersons = xQueryEvaluator.evaluate(xmlStr, "//person");
    if (xmdValuePersons != null && xmdValuePersons.size() > 0) {
      XdmSequenceIterator xmdValuePersonsIterator = xmdValuePersons.iterator();
      retPersons = new ArrayList<Person>();
      while (xmdValuePersonsIterator.hasNext()) {
        XdmItem xdmItemPerson = xmdValuePersonsIterator.next();
        String xdmItemPersonStr = xdmItemPerson.toString();
        Person person = new Person();
        String role = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/role)");
        if (role != null && role.equals("author"))
          person.setRole(AUTHOR);
        else if (role != null && role.equals("editor"))
          person.setRole(EDITOR);
        else if (role != null && role.equals("technical_writer"))
          person.setRole(TECHNICAL_WRITER);
        else if (role != null && role.equals("actor"))
          person.setRole(ACTOR);
        String name = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/name)");
        if (name != null)
          person.setName(name);
        String forename = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/forename)");
        if (forename != null)
          person.setForename(forename);
        String surname = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/surname)");
        if (surname != null)
          person.setSurname(surname);
        String gndLink = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/gndLink)");
        if (gndLink != null)
          person.setGndLink(gndLink);
        retPersons.add(person);
      }
    }
    return retPersons;
  }
  
  public int getRole() {
    return role;
  }

  public void setRole(int role) {
    this.role = role;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSurname() {
    return surname;
  }
  
  public void setSurname(String surname) {
    this.surname = surname;
  }
  
  public String getForename() {
    return forename;
  }
  
  public void setForename(String forename) {
    this.forename = forename;
  }

  public String getGndLink() {
    return gndLink;
  }

  public void setGndLink(String gndLink) {
    this.gndLink = gndLink;
  }

  public String getAboutLink() {
    return aboutLink;
  }

  public void setAboutLink(String aboutLink) {
    this.aboutLink = aboutLink;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String toXmlStr() {
    if (name == null)
      return null;
    String roleStr = null;
    if (role == AUTHOR)
      roleStr = "author";
    else if (role == EDITOR)
      roleStr = "editor";
    else if (role == TECHNICAL_WRITER)
      roleStr = "technical_writer";
    else if (role == ACTOR)
      roleStr = "actor";
    String retStr = "<person>";
    if (roleStr != null)
      retStr = retStr + "\n<role>" + roleStr + "</role>";
    if (forename != null)
      retStr = retStr + "\n<forename>" + StringUtils.deresolveXmlEntities(forename) + "</forename>";
    if (surname != null)
      retStr = retStr + "\n<surname>" + StringUtils.deresolveXmlEntities(surname) + "</surname>";
    if (name != null)
      retStr = retStr + "\n<name>" + StringUtils.deresolveXmlEntities(name) + "</name>";  // redundant
    if (gndLink != null)
      retStr = retStr + "\n<gndLink>" + StringUtils.deresolveXmlEntities(gndLink) + "</gndLink>";
    retStr = retStr + "\n</person>";
    return retStr;
  }
  
  public String toHtmlStr() {
    if (name == null)
      return null;
    String retStr = "<span class=\"author\">";
    String gndLinkHtmlStr = "";
    String aboutLinkHtmlStr = "";
    if (aboutLink != null)
      aboutLinkHtmlStr = "<a href=\"" + StringUtils.deresolveXmlEntities(aboutLink) + "\"><img src=\"../images/persons.png\" alt=\"Person\" border=\"0\" height=\"12\" width=\"12\"></a>";
    if (gndLink != null)
      gndLinkHtmlStr = "<a href=\"" + StringUtils.deresolveXmlEntities(gndLink) + "\"><img src=\"../images/person.png\" alt=\"Person\" border=\"0\" height=\"12\" width=\"12\"></a>";
    if (name != null)
      retStr = retStr + "<span class=\"name\">" + StringUtils.deresolveXmlEntities(name) + " (" + aboutLinkHtmlStr + gndLinkHtmlStr + ")" + "</span>";
    retStr = retStr + "</span>";
    return retStr;
  }

  public JSONObject toJsonObject() throws ApplicationException {
    JSONObject retJsonObject = new JSONObject();
    if (name == null)
      return null;
    String roleStr = null;
    if (role == AUTHOR)
      roleStr = "author";
    else if (role == EDITOR)
      roleStr = "editor";
    else if (role == TECHNICAL_WRITER)
      roleStr = "technical_writer";
    else if (role == ACTOR)
      roleStr = "actor";
    if (roleStr != null)
      retJsonObject.put("role", roleStr);
    if (forename != null)
      retJsonObject.put("forename", forename);
    if (surname != null)
      retJsonObject.put("surname", surname);
    if (name != null)
      retJsonObject.put("name", name);
    if (gndLink != null) {
      try {
        String gndLinkEnc = URIUtil.encodeQuery(gndLink);
        retJsonObject.put("gndLink", gndLinkEnc);
      } catch (URIException e) {}
    }
    if (aboutLink != null) {
      try {
        String aboutLinkEnc = URIUtil.encodeQuery(aboutLink);
        retJsonObject.put("aboutLink", aboutLinkEnc);
      } catch (URIException e) {}
    }
    return retJsonObject;
  }
  
}
