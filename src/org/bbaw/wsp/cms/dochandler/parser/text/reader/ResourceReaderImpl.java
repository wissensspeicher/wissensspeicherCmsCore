package org.bbaw.wsp.cms.dochandler.parser.text.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * The Adapter class which builds an InputStream depending on the kind of resource (e.g. HTTP or file system resource)
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 07.08.2012
 * 
 */
public class ResourceReaderImpl implements IResourceReader {

  /*
   * (non-Javadoc)
   * 
   * @see bbaw.wsp.parser.fulltext.IResourceReader#read(java.lang.String)
   */
  @Override
  public InputStream read(final String uri) throws ApplicationException {
    try {
      if (uri.startsWith("http://") || uri.startsWith("file:/")) {
        URL url;
        url = new URL(uri);
        final InputStream in = url.openStream();
        return in;
      } else {
        final InputStream in = new FileInputStream(new File(uri));
        return in;
      }
    } catch (final IOException e) {
      throw new ApplicationException("The type of resource for this URI " + uri + " isn't supported: " + e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see bbaw.wsp.parser.fulltext.readers.IResourceReader#getResourceType(java .lang.String)
   */
  @Override
  @SuppressWarnings("deprecation")
  public URL getURI(final String uri) throws ApplicationException {
    try {
      if (uri.contains("http://")) {
        URL url;
        url = new URL(uri);
        return url;
      } else {
        final File file = new File(uri);
        URL url;
        url = file.toURL();
        return url;
      }
    } catch (final IOException e) {
      throw new ApplicationException("The type of resource for this URI " + uri + " isn't supported: " + e.getMessage());
    }
  }
}
