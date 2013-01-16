package org.bbaw.wsp.cms.document;

public class Person {
  private String surname;
  private String forename;

  public Person(String personString) {
    personString = personString.trim();
    surname = personString;
    String otherNames = null;
    if (personString.contains(",")) {
      int index = personString.indexOf(",");
      surname = personString.substring(0, index);
      otherNames = personString.substring(index + 1);
      forename = otherNames.trim();
    }
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
  
}
