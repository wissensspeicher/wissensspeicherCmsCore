package org.bbaw.wsp.cms.collections;

public class JdbcConnection {
  private String type;  // e.g. mySQL
  private String host;
  private String port;
  private String db;
  private String user;
  private String pw;

  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public String getHost() {
    return host;
  }
  public void setHost(String host) {
    this.host = host;
  }
  public String getPort() {
    return port;
  }
  public void setPort(String port) {
    this.port = port;
  }
  public String getDb() {
    return db;
  }
  public void setDb(String db) {
    this.db = db;
  }
  public String getUser() {
    return user;
  }
  public void setUser(String user) {
    this.user = user;
  }
  public String getPw() {
    return pw;
  }
  public void setPw(String pw) {
    this.pw = pw;
  }

}
