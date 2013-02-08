/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler;

import java.net.URL;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.WspRdfStore;

/**
 * @author <a href="mailto:sascha.feldmann@gmx.de">Sascha Feldmann</a>
 * @since 04.02.2013
 * 
 */
public class HitRecord implements IHitRecord {

  private final URL namedGraphUrl;

  /**
   * Create a new {@link HitRecord}.
   * 
   * @param namedGraphUrl
   *          the name (an URL) of the named graph as stored in the
   *          {@link WspRdfStore}
   */
  public HitRecord(final URL namedGraphUrl) {
    this.namedGraphUrl = namedGraphUrl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.IHitRecord#getNamedGraphUrl
   * ()
   */
  @Override
  public URL getNamedGraphUrl() {
    return namedGraphUrl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.IHitRecord#calcScore()
   */
  @Override
  public double calcScore() {
    // TODO Auto-generated method stub
    return 0;
  }

}
