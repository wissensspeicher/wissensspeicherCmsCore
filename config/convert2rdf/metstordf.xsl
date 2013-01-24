<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:dc="http://purl.org/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/" xmlns:foaf="http://xmlns.com/foaf/0.1/"
	xmlns:edm="http://www.europeana.eu/schemas/edm/" xmlns:xi="http://www.w3.org/2001/XInclude"
	xmlns:mets="http://www.loc.gov/METS/" xmlns:dv="http://dfg-viewer.de/"
	xmlns:xlink="http://www.w3.org/1999/xlink">	
	<xsl:output method="xml" version="1.0" encoding="UTF-8"
		indent="yes" />
	<!-- parameters -->
	<xsl:param name="aggrId" select="--undefined--" />
	<!-- global variables -->
	<xsl:variable name="aggregationUri" as="xsd:anyURI"
		select="concat('http://wsp.bbaw.de/mets/',$aggrId,'/aggregation') cast as xsd:anyURI" />
	<xsl:variable name="docIdentifier" select="//dv:links/dv:presentation/text()" />
	<xsl:variable name="newline">
		<xsl:text>
	</xsl:text>
	</xsl:variable>


	<xsl:template match="/">
		<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
			xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:ore="http://www.openarchives.org/ore/terms/"
			xmlns:dc="http://purl.org/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/"
			xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:edm="http://www.europeana.eu/schemas/edm/"
			xmlns:xi="http://www.w3.org/2001/XInclude">
			<rdf:Description xml:base="{$docIdentifier}"
				rdf:about="{$docIdentifier}">
				<ore:describes rdf:resource="{$aggregationUri}" />
				<rdf:type rdf:resource="http://www.openarchives.org/ore/terms/ResourceMap" />
				<dc:creator rdf:parseType="Resource">
					<foaf:name>Wissensspeicher</foaf:name>
					<foaf:page rdf:resource="http://wsp.bbaw.de" />
				</dc:creator>
				<xsl:apply-templates select="/mets:mets/mets:metsHdr" />
				<xsl:variable name="todaysDate" select="fn:current-date()" />
				<dcterms:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
					<xsl:value-of select="format-date($todaysDate,'[Y0001]-[M01]-[D01]')" />
				</dcterms:created>
				<dcterms:modified />
				<dc:rights />
				<dcterms:rights rdf:resource="http://creativecommons.org/licenses/by-nc/2.5/" />
			</rdf:Description>
			<rdf:Description rdf:about="{$aggregationUri}">
				<ore:describedBy rdf:resource="{$docIdentifier}" />
				<rdf:type rdf:resource="http://www.openarchives.org/ore/terms/Aggregation" />
				<xsl:apply-templates select="/mets:mets/mets:dmdSec" />
				
			</rdf:Description>
			
			<xsl:apply-templates select="/mets:mets/mets:fileSec/mets:fileGrp" />
		</rdf:RDF>
	</xsl:template>

	<!-- Fetch the mets header -->
	<xsl:template match="*:metsHdr">
		<xsl:variable name="metsName" select="mets:agent/mets:name/text()" />
		<xsl:variable name="prename" select="substring-before($metsName, ' ')" />
		<xsl:variable name="family" select="substring-after($metsName, ' ')" />
		<dc:creator rdf:parseType="resource">
			<foaf:givenName>
				<xsl:value-of select="$prename" />
			</foaf:givenName>
			<foaf:familyName>
				<xsl:value-of select="$family" />
			</foaf:familyName>
			<dc:description>Digital Adaptation</dc:description>
		</dc:creator>
	</xsl:template>

	<xsl:template match="*:dmdSec">
		<xsl:variable name="modsTitle"
			select="mets:mdWrap/mets:xmlData/mods:mods/mods:titleInfo/mods:title/text()" />
		<dc:title>
			<xsl:value-of select="$modsTitle" />
		</dc:title>
		<xsl:variable name="modsName"
			select="mets:mdWrap/mets:xmlData/mods:mods/mods:name[@type='personal']/mods:displayForm/text()" />
		<xsl:variable name="family" select="substring-before($modsName, ',')" />
		<xsl:variable name="prename" select="substring-after($modsName, ', ')" />
		<dc:creator rdf:parseType="Resource">
			<foaf:givenName>
				<xsl:value-of select="$prename" />
			</foaf:givenName>
			<foaf:familyName>
				<xsl:value-of select="$family" />
			</foaf:familyName>
		</dc:creator>
		<xsl:variable name="modsTitle" select="//mods:url/text()" />
		<dc:identifier rdf:resource="{$docIdentifier}" />
		<dcterms:created>
			<dcterms:W3CDTF>
				<rdf:value></rdf:value>
			</dcterms:W3CDTF>
		</dcterms:created>
		<dcterms:issued>
			<dcterms:W3CDTF>
				<xsl:variable name="modsDateIssued"
					select="mets:xmlData/mods:mods/mods:originInfo/mods:dateIssued/text()" />
				<rdf:value>
					<xsl:value-of select="$modsDateIssued" />
				</rdf:value>
			</dcterms:W3CDTF>
		</dcterms:issued>
	</xsl:template>
	
	<!--  iterate over default scaled images -->
	<xsl:template match="*:fileGrp[@USE='DEFAULT']">
		<xsl:apply-templates select="mets:file" />
	</xsl:template>
	
	<xsl:template match="*:file">
		<xsl:variable name="fileUri"
			select="mets:FLocat/@xlink:href" />
		<rdf:Description rdf:about="{$fileUri}">
				<ore:describedBy rdf:resource="{$aggregationUri}" />
			<dc:identifier rdf:resource="{$fileUri}" />
			<dc:format>			
					<xsl:variable name="mimeType" select="@MIMETYPE" />		
					<dcterms:IMT rdf:value="{$mimeType}"/>					
			</dc:format>
		</rdf:Description>
	</xsl:template>
	

</xsl:stylesheet>