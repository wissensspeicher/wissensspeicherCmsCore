/**
 * This package contains the transformation classes to transform to our destination OAI/ORE rdf file.
 */
package org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.bbaw.wsp.cms.dochandler.parser.text.reader.ResourceReaderImpl;
import org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer.integrator.ANormdataIntegration;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * This abstract class is the central api for all transformations to RDF.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 08.10.12
 * 
 */
public abstract class ToRdfTransformer {
  /**
   * Use this option to do a direct conversion (e.g.: from eDoc to RDF).
   */
  public static final String MODE_DIRECT = "hard-coded conversion";

  /**
   * Use this option to do an XSLT transformation
   */
  public static final String MODE_XSLT = "XSLT transformation";
  /**
   * The name of the creator. It will be set as dc:creator/foaf:name in the ouptput .rdf file.
   */
  public static final String TRANSFORMER_CREATOR_NAME = "Wissensspeicher";
  /**
   * The webpage of the creator. It will be set as dc:creator/foaf:page in the ouptput .rdf file.
   */
  public static final String TRANSFORMER_CREATOR_PAGE_URL = "http://wsp.bbaw.de";
  /**
   * The URL of the RDFNode within the normdata.rdf file. If the value is set, none of the the options above will be applied. It will be set as dc:creator rdf:resource="{VALUE}" in the ouptput .rdf file.
   */
  public static final String TRANSFORMER_CREATOR_NORMDATA_URI = "http://wsp.normdata.rdf/Wissensspeicher";
  /**
   * Path to the normdata file. It will be included in the depending xslt stylesheet file.
   */
  // public static final String NORMDATA_FILE_PATH = Constants.getInstance().getMdsystemNormdataFile();
  public static final String NORMDATA_FILE_PATH = "../mdsystem/wsp.normdata.rdf";

  /**
   * Parameter name in all XSLT stylesheets for the aggregation id,
   */
  private static final String STYLESHEET_PARAM_ID = "aggrId";
  /**
   * Parameter name in all XSLT stylesheets for the creator of the resulting OAI/ORE (Wissensspeicher),
   */
  private static final String STYLESHEET_PARAM_CREATOR_NAME = "resourceCreatorName";
  /**
   * Parameter name in all XSLT stylesheets for the creator's page (URL) of the resulting OAI/ORE (Wissensspeicher),
   */
  private static final String STYLESHEET_PARAM_CREATOR_PAGE = "resourceCreatorPage";
  /**
   * Parameter name in all XSLT stylesheets for the RDFNode's URL within the normdata.rdf file.
   */
  private static final String STYLESHEET_PARAM_CREATOR_NORMDATA_URI = "resourceCreatorNormdata";
  /**
   * Parameter name in all XSLT stylesheets for the path to the normdata file *
   */
  private static final String STYLESHEET_PARAM_NORMDATA_FILE = "normdataFile";

  private String xslInput;

  private final String transformMode;

  protected ResourceReaderImpl resourceReader;

  protected ResourceWriter resourceWriter;

  protected ANormdataIntegration normdataIntegrator;

  public Integer aggrId;

  /**
   * Create a new instance.
   * 
   * @param transformMode
   *          - the mode of the transformer (XSLT transformation or hard-coded conversion).
   * @throws ApplicationException
   *           if the entered mode isn't valid.
   */
  public ToRdfTransformer(final String transformMode) throws ApplicationException {
    if (!transformMode.equals(MODE_DIRECT) && !transformMode.equals(MODE_XSLT)) {
      throw new ApplicationException("The mode isn't available in ToRdfTransformer.");
    }
    this.transformMode = transformMode;
    resourceReader = new ResourceReaderImpl();
    resourceWriter = new ResourceWriter();
  }

  /**
   * Do an transformation job. The kind of job (XSLT transformation or direct parsing) depends on the subclass.
   * 
   * @param inputUrl
   *          - the url of the resource to be transformed.
   * @throws ApplicationException
   */
  public void doTransformation(final String inputUrl, final String outputUrl) throws ApplicationException {
    if (inputUrl == null || inputUrl.isEmpty()) {
      throw new ApplicationException("The value for the parameter inputUrl in ToRdfTransformer.doTransformation() mustn't be null or empty!");
    }

    final InputStream inputFileStream = resourceReader.read(inputUrl);

    if (transformMode.equals(MODE_XSLT)) {
      if (getXslInput() == null) {
        throw new ApplicationException("You must specify an XSLT stylesheet before transforming in XSLT mode!");
      }
      final InputStream xslInput = resourceReader.read(getXslInput());

      final OutputStream xmlOutput = resourceWriter.write(outputUrl);
      transform(inputFileStream, xslInput, xmlOutput);
    } else if (transformMode.equals(MODE_DIRECT)) {
      // Do a direct transformation - this is specified by the subclass
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see bbaw.wsp.parser.metadata.transformer.IXsltTransformable#transform(java. io.InputStream, java.io.InputStream, java.io.InputStream)
   */
  public void transform(final InputStream xmlInput, final InputStream xslStylesheet, final OutputStream xmlOutput) throws ApplicationException {
    if (xmlInput == null || xslStylesheet == null || xmlOutput == null) {
      throw new ApplicationException("The values for the parameters in ToRdfTransformer.transform() mustn't be null.");
    }

    try {
      final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      // Xslt file
      final Source xsltSource = new SAXSource(xmlReader, new InputSource(xslStylesheet));

      final Processor processor = new Processor(false);
      final XsltCompiler compiler = processor.newXsltCompiler();
      compiler.setErrorListener(SaxonCompilerErrorListener.getInstance());

      final XsltExecutable executable = compiler.compile(xsltSource);

      // Input file
      final StreamSource inputSource = new StreamSource(xmlInput);

      // Serializer for output file
      final Serializer serializer = new Serializer();
      serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
      serializer.setOutputStream(xmlOutput);

      // Do transformation
      final XsltTransformer transformer = executable.load();
      // use an id for the aggregation if it was specified
      if (aggrId != null) {
        final XdmValue item = new XdmAtomicValue(aggrId + "");

        transformer.setParameter(new QName(STYLESHEET_PARAM_ID), item);
      }
      // export creator name
      transformer.setParameter(new QName(STYLESHEET_PARAM_CREATOR_NAME), new XdmAtomicValue(TRANSFORMER_CREATOR_NAME));
      // export creator page
      transformer.setParameter(new QName(STYLESHEET_PARAM_CREATOR_PAGE), new XdmAtomicValue(TRANSFORMER_CREATOR_PAGE_URL));
      // export URI of the WSP's rdf node within the normdata.rdf file
      transformer.setParameter(new QName(STYLESHEET_PARAM_CREATOR_NORMDATA_URI), new XdmAtomicValue(TRANSFORMER_CREATOR_NORMDATA_URI));
      // export path to the normdata.rdf file
      System.out.println("normdata path: " + NORMDATA_FILE_PATH);
      transformer.setParameter(new QName(STYLESHEET_PARAM_NORMDATA_FILE), new XdmAtomicValue(NORMDATA_FILE_PATH));

      transformer.setSource(inputSource);
      transformer.setDestination(serializer);
      transformer.transform();
    } catch (final SaxonApiException e) {
      // throw new ApplicationException("Problem while transforming -- " +
      // e.getMessage());
      e.printStackTrace();
    } catch (final SAXException e) {
      // throw new ApplicationException("Problem while transforming -- " +
      // e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Check if an file is XML valid.
   * 
   * @param xmlOutput
   *          - the url as String.
   * @return
   * @throws ApplicationException
   *           if the source isn't available.
   */
  public boolean checkValidation(final String xmlOutput) throws ApplicationException {
    return XmlValidator.isValid(xmlOutput);
  }

  public String getXslInput() {
    return xslInput;
  }

  public void setXslInput(final String xslInput) {
    this.xslInput = xslInput;
  }
}
