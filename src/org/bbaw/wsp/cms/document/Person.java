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
  public static int MENTIONED = 4;  // person which is marked in a document (e.g. persName without parent editor) 
  private int role = AUTHOR;  // default
  private String name;  // whole name: built from: surname, forename if they exists
  private String surname;
  private String forename;
  private String ref;
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
        else if (role != null && role.equals("mentioned"))
          person.setRole(MENTIONED);
        String name = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/name)");
        if (name != null && ! name.isEmpty()) {
          name = name.trim();
          name = name.replaceAll("-\\s([^u]?)|^\\.|^-", "$1");
          person.setName(name);
        }
        String forename = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/forename)");
        if (forename != null & ! forename.isEmpty())
          person.setForename(forename);
        String surname = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/surname)");
        if (surname != null && ! surname.isEmpty())
          person.setSurname(surname);
        String ref = xQueryEvaluator.evaluateAsString(xdmItemPersonStr, "string(/person/ref)");
        if (ref != null && ! ref.isEmpty())
          person.setRef(ref);
        if ((name != null && ! name.isEmpty()) || (forename != null && ! forename.isEmpty()) || (surname != null && ! surname.isEmpty()))
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

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
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
    else if (role == MENTIONED)
      roleStr = "mentioned";
    String retStr = "<person>";
    if (roleStr != null)
      retStr = retStr + "\n<role>" + roleStr + "</role>";
    if (forename != null)
      retStr = retStr + "\n<forename>" + StringUtils.deresolveXmlEntities(forename) + "</forename>";
    if (surname != null)
      retStr = retStr + "\n<surname>" + StringUtils.deresolveXmlEntities(surname) + "</surname>";
    if (name != null)
      retStr = retStr + "\n<name>" + StringUtils.deresolveXmlEntities(name) + "</name>";  // redundant
    if (ref != null)
      retStr = retStr + "\n<ref>" + StringUtils.deresolveXmlEntities(ref) + "</ref>";
    if (aboutLink != null)
      retStr = retStr + "\n<referenceAbout>" + StringUtils.deresolveXmlEntities(aboutLink) + "</referenceAbout>";
    retStr = retStr + "\n</person>";
    return retStr;
  }
  
  public String toHtmlStr() {
    if (name == null)
      return null;
    String roleStr = "author";
    String roleAbrStr = "author";
    if (role == AUTHOR) {
      roleStr = "author";
      roleAbrStr = "author";
    } else if (role == EDITOR) {
      roleStr = "editor";
      roleAbrStr = "eds.";
    } else if (role == TECHNICAL_WRITER) {
      roleStr = "technical_writer";
      roleAbrStr = "techn.";
    } else if (role == ACTOR) {
      roleStr = "actor";
      roleAbrStr = "actor";
    } else if (role == MENTIONED) {
      roleStr = "mentioned";
      roleAbrStr = null;
    }
    String retStr = "<span class=\"" + roleStr + "\">";
    String refHtmlStr = "";
    String aboutLinkHtmlStr = "";
    String roleAbrStrHtml = "";
    if (roleAbrStr != null)
      roleAbrStrHtml = roleAbrStr + ", ";
    if (aboutLink != null)
      aboutLinkHtmlStr = "<a href=\"" + StringUtils.deresolveXmlEntities(aboutLink) + "\"><img src=\"../images/persons.png\" alt=\"Person\" border=\"0\" height=\"12\" width=\"12\"></a>";
    if (ref != null)
      refHtmlStr = "<a href=\"" + StringUtils.deresolveXmlEntities(ref) + "\"><img src=\"../images/person.png\" alt=\"Person\" border=\"0\" height=\"12\" width=\"12\"></a>";
    if (name != null)
      retStr = retStr + "<span class=\"name\">" + StringUtils.deresolveXmlEntities(name) + " [" + roleAbrStrHtml + aboutLinkHtmlStr + refHtmlStr + "]" + "</span>";
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
    else if (role == MENTIONED)
      roleStr = "mentioned";
    if (roleStr != null)
      retJsonObject.put("role", roleStr);
    if (forename != null)
      retJsonObject.put("forename", forename);
    if (surname != null)
      retJsonObject.put("surname", surname);
    if (name != null)
      retJsonObject.put("name", name);
    if (ref != null) {
      try {
        String refEnc = URIUtil.encodeQuery(ref);
        retJsonObject.put("reference", refEnc);
      } catch (URIException e) {}
    }
    if (aboutLink != null) {
      try {
        String aboutLinkEnc = URIUtil.encodeQuery(aboutLink);
        retJsonObject.put("referenceAbout", aboutLinkEnc);
      } catch (URIException e) {}
    }
    return retJsonObject;
  }
  
}
