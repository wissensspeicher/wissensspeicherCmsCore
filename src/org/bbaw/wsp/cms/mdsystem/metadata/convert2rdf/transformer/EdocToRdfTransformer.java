package org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.transformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.bbaw.wsp.cms.dochandler.parser.document.PdfDocument;
import org.bbaw.wsp.cms.dochandler.parser.text.parser.EdocIndexMetadataFetcherTool;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.mdsystem.metadata.convert2rdf.util.TemplateMapper;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Instances of this (singleton) class transform eDoc metadata to the
 * destination OAI/ORE files.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 08.10.12
 * 
 */
public class EdocToRdfTransformer extends ToRdfTransformer {
  /**
   * Specify the RDF template here.
   */
  private static final String RDF_TEMPLATE_URL = "config/convert2rdf/eDocToRdfTemplate.xml";
  /**
   * The prefix of the aggregation name is it is stored in the quad.
   */
  public static final String AGGREGATION_NAME_PREFIX = "http://wsp.bbaw.de/edoc/";
  private static EdocToRdfTransformer instance;

  private EdocToRdfTransformer() throws ApplicationException {
    super(ToRdfTransformer.MODE_DIRECT);
  }

  /**
   * 
   * @return the only existing instance.
   * @throws ApplicationException
   *           if the mode wasn't specified correctly.
   */
  public static EdocToRdfTransformer getInstance() throws ApplicationException {
    if (instance == null) {
      return new EdocToRdfTransformer();
    }
    return instance;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * bbaw.wsp.parser.metadata.transformer.ToRdfTransformer#doTransformation(
   * java.lang.String, java.lang.String)
   */
  public void doTransformation(final String inputUrl, final String outputUrl) throws ApplicationException {
    super.doTransformation(inputUrl, outputUrl);
    MetadataRecord mdRecord = new MetadataRecord();
    EdocIndexMetadataFetcherTool.fetchHtmlDirectly(inputUrl, mdRecord);

    // map here
    System.out.println("Processing transformation from eDoc to RDF...");
    TemplateMapper mapper = new TemplateMapper(RDF_TEMPLATE_URL);
    HashMap<String, String> eDocMap = createMap(mdRecord);
    System.out.println("Mapping template...");
    mapper.mapPlaceholder(eDocMap);
   
   
    try {
      FileWriter writer = new FileWriter(new File(outputUrl));
      BufferedWriter buffer = new BufferedWriter(writer);
      buffer.write(mapper.getMappedTemplate());
      buffer.flush();
      buffer.close();
    } catch ( IOException e) {
      throw new ApplicationException("Couldn't generated a valid output file. "+e.getMessage());
    }
    
   // check validation    
    File f = new File(outputUrl);
    if(!this.checkValidation(f.getAbsolutePath())) {
      System.err.println("WARNING: the generated output file - "+f+" isn't XML valid. Please check the file!");
    }
    else
    {
      System.out.println("The generated output file - "+f+" is xml valid!");
    }
    
  }

  private HashMap<String, String> createMap(final MetadataRecord mdRecord) throws ApplicationException {
    HashMap<String, String> eDocPlaceholderMap = new HashMap<String, String>();

    eDocPlaceholderMap.put("%%aggregation_uri%%", AGGREGATION_NAME_PREFIX+EdocIndexMetadataFetcherTool.getDocYear(mdRecord.getRealDocUrl())+"/"+EdocIndexMetadataFetcherTool.getDocId(mdRecord.getRealDocUrl())+"/aggregation");
    eDocPlaceholderMap.put("%%creator_name%%", ToRdfTransformer.TRANSFORMER_CREATOR_NAME);
    eDocPlaceholderMap.put("%%creator_url%%", ToRdfTransformer.TRANSFORMER_CREATOR_URL);
    String uri = mdRecord.getRealDocUrl();
    if (uri == null) {
      uri = "";
    }
    eDocPlaceholderMap.put("%%resource_identifier%%", uri);
    String urn = mdRecord.getUrn();
    if (urn == null) {
      urn = "";
    }
    eDocPlaceholderMap.put("%%resource_urn_identifier%%", urn);
    
    String title = mdRecord.getTitle();
    if (title == null) {
      title = "";
    }
    eDocPlaceholderMap.put("%%dc_title%%", title);

    Date actual = new Date();
    String actualDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.CANADA_FRENCH).format(actual);
    eDocPlaceholderMap.put("%%actual_date%%", actualDate);

    Date dateCreated = mdRecord.getCreationDate();
    String dateCreatedString = "";
    if (dateCreated == null) {
      dateCreatedString = "";
    } else {
      // KOBF format yyyy -> format to W3CDTF
      dateCreatedString = new SimpleDateFormat("yyyy").format(dateCreated);
    }
    eDocPlaceholderMap.put("%%date_created%%", dateCreatedString);

    Date dateIssued = mdRecord.getPublishingDate();
    System.out.println("date published: " + dateIssued);
    String dateIssuedString = "";
    if (dateIssued == null) {
      dateIssuedString = "";
    } else {
      // KOBV format dd.mm.yyyy -> format to yyyy-dd-mm
      dateIssuedString = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.CANADA_FRENCH).format(dateIssued);
    }
    eDocPlaceholderMap.put("%%date_issued%%", dateIssuedString);
    String publisher = mdRecord.getPublisher();
    if (publisher == null) {
      publisher = "";
    }
    eDocPlaceholderMap.put("%%publisher%%", publisher);
    String language = mdRecord.getLanguage();
    language = this.convertToLanguageTerm(language);   
    eDocPlaceholderMap.put("%%language%%", language);
   
    eDocPlaceholderMap.put("%%mime_type%%", PdfDocument.MIME_TYPE);
    String description = mdRecord.getDescription();
    if (description == null) {
      description = "";
    }
    eDocPlaceholderMap.put("%%dc_description%%", description);

    // Map SWD subjects and other subjects
    String subject = mdRecord.getSwd();  // swd
    String subString = "";
    if (subject == null) {
      subject = "";
    } else {
      subString = "";
      String[] subjects = subject.split(",");
      for (String sub : subjects) {
        if (!sub.trim().isEmpty()) {
          subString += "<dc:subject>"+sub+"</dc:subject>\n\t\t\t\t";
        }
      }
    }
    String freeWords = mdRecord.getSubject(); // freie schlagwörter
    if (freeWords != null) {
      String[] swds = freeWords.split(",");
      for (String sub : swds) {
        if(!sub.trim().isEmpty()) {
          subString += "\t\t\t<dc:subject>"+sub+"</dc:subject>\n\t\t\t\t";
        }
      }
    }

    eDocPlaceholderMap.put("%%subjects%%", subString);
    
    String documentType = mdRecord.getDocumentType();
    if (documentType == null) {
      documentType = "";
    }
    eDocPlaceholderMap.put("%%document_type%%", documentType);
    
    String creator = mdRecord.getCreator();
    if (creator == null) {
      creator = "";
    }    
    try {     
      StringBuilder creatorsInTemplates = new StringBuilder();
      if(creator.contains(";")) // more than one creator
        {         
          String[] creators = creator.split(";");          
          for (String c : creators) {     
            // split to given and family name
            String creatorBlock = this.generatedDcCreatorBlock(c);
            creatorsInTemplates.append(creatorBlock);
          }         
        }
      else {
        String creatorBlock = this.generatedDcCreatorBlock(creator);
        creatorsInTemplates.append(creatorBlock);
      }
      creator = creatorsInTemplates.toString();
    }
     catch (StringIndexOutOfBoundsException e) {
      creator = "";
    }   
    eDocPlaceholderMap.put("%%creator%%", creator);
    
    String ddc = mdRecord.getDdc();
    if (ddc == null) {
      ddc = "";
    }
    eDocPlaceholderMap.put("%%ddc%%", ddc);
    
    String numberPages = mdRecord.getPageCount()+"";    
    eDocPlaceholderMap.put("%%number_pages%%", numberPages);
    
    String source = mdRecord.getInPublication();
    if (source == null) {
     source = "";
    }
    eDocPlaceholderMap.put("%%source%%", source);
    

    return eDocPlaceholderMap;
  }

  /**
   * Convert an input string to a language term. 
   * This language terms follows the ISO iso639-3 specification.
   * @param language
   * @return the iso639-3 value or the original language if not determined.
   */
  private String convertToLanguageTerm(String language) {
    if (language.equals("Deutsch")) {
      language = "deu";
    }
    else if (language.equals("Franz&ouml;sisch")) {
      language = "fra";
    }
    return language;
  }

  /**
   * Construct a dc:creator block with a foaf:givenName and foaf:familyName by giving a string from the {@link MetadataRecord}.
   * @param c the String containing one single creator.
   * @return the constructed string.
   */
  private String generatedDcCreatorBlock(String c) {
    StringBuilder creatorsInTemplates = new StringBuilder();
    creatorsInTemplates.append("\n\t\t\t\t<dc:creator rdf:parseType=\"Resource\">");
    String givenName = c.substring(c.indexOf(",")+1).trim();
    String familyName = c.substring(0, c.indexOf(",")).trim(); 
    creatorsInTemplates.append("\n\t\t\t\t\t<foaf:givenName>"+givenName+"</foaf:givenName>");
    creatorsInTemplates.append("\n\t\t\t\t\t<foaf:familyName>"+familyName+"</foaf:familyName>");
    creatorsInTemplates.append("\n\t\t\t\t</dc:creator>");
    return creatorsInTemplates.toString();
  }

}
