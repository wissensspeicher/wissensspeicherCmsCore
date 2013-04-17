/**
 * 
 */
package org.bbaw.wsp.cms.mdsystem.metadata.general.vorhaben.csvreader;

import com.googlecode.jcsv.writer.CSVEntryConverter;

/**
 * @author <a href="mailto:wsp-shk1@bbaw.de">Sascha Feldmann</a>
 * @since 14.03.2013
 * 
 */
public class ParsedProjectEntryConverter implements CSVEntryConverter<ParsedProject> {

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.googlecode.jcsv.writer.CSVEntryConverter#convertEntry(java.lang.Object)
   */
  @Override
  public String[] convertEntry(final ParsedProject project) {
    final String[] columns = new String[31];
    columns[0] = project.getVorhaben();
    columns[1] = project.getProject();
    columns[2] = project.getPublisher();
    columns[3] = project.getDate();
    columns[4] = project.getCoverage();
    columns[5] = project.getSubject();
    columns[6] = project.getExtent();
    columns[7] = project.getLanguage();
    columns[8] = project.getFormat();
    columns[9] = project.getType();
    columns[10] = project.getContributor();
    columns[11] = project.getMethod();
    columns[12] = project.getAcronym();
    columns[13] = project.getState();
    columns[14] = project.getRelation_part_of();
    columns[15] = project.getLaufzeit();
    columns[16] = project.getSubjekt();
    columns[17] = project.getCooperation();
    columns[18] = project.getMitarbeiter();
    columns[19] = project.getVersion();
    columns[20] = project.getCoverage_geo();
    columns[21] = project.getIdentifier();
    columns[22] = project.getPfad();
    columns[23] = project.getKompatibel_mit();
    columns[24] = project.getRelation();
    columns[25] = project.getRelation_gefoerdet_von();
    columns[26] = project.getPerson_historisch();
    columns[27] = project.getGenre();
    columns[28] = project.getPersonen_gruender();
    columns[29] = project.getPfad_struktur();
    columns[30] = project.getStatus_anett();
    return columns;
  }
}
