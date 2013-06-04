<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- This XSL stylesheet defines global variables and parameters for each 
	of our XSL stylesheets. It's used by the ToRdfTransformer class. Author: 
	Sascha Feldmann Date: 24.01.2013 Version: 1.0.0 ! -->
<xsl:stylesheet version="2.0"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:dc="http://purl.org/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/" xmlns:foaf="http://xmlns.com/foaf/0.1/"
	xmlns:edm="http://www.europeana.eu/schemas/edm/" xmlns:xi="http://www.w3.org/2001/XInclude"
	xmlns:mets="http://www.loc.gov/METS/" xmlns:dv="http://dfg-viewer.de/"
	xmlns:xlink="http://www.w3.org/1999/xlink">

	<!-- parameters -->
	<xsl:param name="aggrId" select="--undefined--" />
	<xsl:param name="resourceCreatorName" select="Wissensspeicher" />
	<xsl:param name="resourceCreatorPage" select="'http://wsp.bbaw.de' cast as xsd:anyURI" />
	<xsl:param name="normdataFile" />
	<xsl:param name="resourceCreatorNormdata" />

	<!-- global variables -->
	<xsl:variable name="varResourceCreatorPage"
		select="$resourceCreatorPage cast as xsd:anyURI" />
	<xsl:variable name="varResourceCreatorName" select="$resourceCreatorName" />
	<xsl:variable name="newline">
		<xsl:text>
	</xsl:text>
	</xsl:variable>

	<!-- integrate normdata -->
	<xsl:variable name="normdata" select="document($normdataFile)" />

	<!-- global templates -->
	<xsl:template match="*">
		<rdf:Description xml:base="{$docIdentifier}"
			rdf:about="{$docIdentifier}">
			<ore:describes rdf:resource="{$aggregationUri}" />
			<rdf:type rdf:resource="http://www.openarchives.org/ore/terms/ResourceMap" />
			<xsl:choose>
				<xsl:when test="$resourceCreatorNormdata != ''">
					<dc:creator rdf:resource="{$resourceCreatorNormdata}" />
				</xsl:when>
				<xsl:otherwise>
					<dc:creator rdf:parseType="Resource">
						<foaf:name>
							<xsl:value-of select="$varResourceCreatorName" />
						</foaf:name>
						<foaf:page rdf:resource="{$varResourceCreatorPage}" />
					</dc:creator>
				</xsl:otherwise>
			</xsl:choose>
			<!-- <xsl:apply-templates select="/mets:mets/mets:metsHdr" /> -->
			<xsl:variable name="todaysDate" select="fn:current-date()" />
			<dcterms:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
				<xsl:value-of select="format-date($todaysDate,'[Y0001]-[M01]-[D01]')" />
			</dcterms:created>
			<dcterms:rights rdf:resource="http://creativecommons.org/licenses/by-nc/2.5/" />
		</rdf:Description>
	</xsl:template>

</xsl:stylesheet>