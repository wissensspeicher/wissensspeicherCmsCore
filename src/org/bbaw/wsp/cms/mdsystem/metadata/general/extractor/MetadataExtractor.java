package org.bbaw.wsp.cms.mdsystem.metadata.general.extractor;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.xml.xpath.XPath;

import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.general.MetadataParserHelper;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This is the API class for all metadata parsers.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 25.10.2012
 * 
 */
public abstract class MetadataExtractor {
  /**
   * The uri to be read by the saxon compiler.
   */
  protected String uri;
  protected XPathCompiler xPathCompiler;
  protected XdmNode contextItem;

  /**
   * Create a new {@link MetadataExtractor} instance.
   * 
   * @param uri
   *          - the URI of the xml document to be parsed.
   * @throws ApplicationException
   *           if the ressource cannot be validated by Saxon.
   * @throws IllegalArgumentException
   *           if the uri is null, empty or doesn't refer to an existing file.
   */
  public MetadataExtractor(final String uri, final HashMap<String, String> namespaces) throws ApplicationException {
    if (uri == null || uri.isEmpty()) {
      throw new IllegalArgumentException("The value for the parameter uri in the constructor of ModsMetadataParser mustn't be empty.");
    }
    this.uri = uri;

    // define the Saxon processor
    final Processor processor = new Processor(false);
    xPathCompiler = processor.newXPathCompiler();
    // declare each namespace
    for (final String namespace : namespaces.keySet()) {
      xPathCompiler.declareNamespace(namespace, namespaces.get(namespace));
    }
    final DocumentBuilder builder = processor.newDocumentBuilder();
    try {
      final Logger logger = Logger.getLogger(MetadataExtractor.class);
      logger.info("MetadataExtractor parses: " + uri);
      contextItem = builder.build(new File(uri));
    } catch (final SaxonApiException e) {
      throw new ApplicationException("Error while trying to access file using Saxon: " + uri + "stacktrace: " + e);
    }
  }

  /**
   * Compile and execute and XPath query.
   * 
   * @param query
   *          - the {@link XPath} expression.
   * @param moreNodes
   *          - set true, if you want to fetch more nodes in a {@link List} of {@link String}.
   * @return an {@link Object} (String if moreNodes is false, String[] if moreNodes is set true)
   */
  protected Object buildXPath(final String query, final boolean moreNodes) {
    try {
      final XPathExecutable x = xPathCompiler.compile(query);

      final XPathSelector selector = x.load();
      selector.setContextItem(contextItem);
      final XdmValue value = selector.evaluate();

      if (moreNodes) {
        final String[] list = new String[value.size()];
        int i = 0;
        for (final XdmItem xdmItem : value) {
          list[i] = xdmItem.toString();
          ++i;
        }
        // Replace attribute chars
        return MetadataParserHelper.removeAttributeChars(list);
      }

      final ValueRepresentation rep = value.getUnderlyingValue();
      // Replace attribute chars
      return MetadataParserHelper.removeAttributeChars(rep.getStringValue());
    } catch (final SaxonApiException e) {
      e.printStackTrace();
      return null;
    } catch (final XPathException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Executes a XPath query and returns a single String if there is only one tag, otherweise an Array of Strings has to be casted for using
   * 
   * @param query
   * @return Object
   */
  protected Object buildNormdataXPath(final String query) {
    try {
      final XPathExecutable x = xPathCompiler.compile(query);

      final XPathSelector selector = x.load();
      selector.setContextItem(contextItem);
      final XdmValue value = selector.evaluate();

      if (value.size() > 1) {
        final String[] list = new String[value.size()];
        int i = 0;
        for (final XdmItem xdmItem : value) {
          list[i] = xdmItem.getStringValue();
          ++i;
        }
        // Replace attribute chars
        return MetadataParserHelper.removeAttributeChars(list);
      }

      final ValueRepresentation rep = value.getUnderlyingValue();
      // Replace attribute chars
      return MetadataParserHelper.removeAttributeChars(rep.getStringValue());
    } catch (final SaxonApiException e) {
      e.printStackTrace();
      return null;
    } catch (final XPathException e) {
      e.printStackTrace();
      return null;
    }
  }
}
