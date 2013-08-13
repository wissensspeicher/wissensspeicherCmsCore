<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dcterms="http://purl.org/dc/terms/" xmlns:foaf="http://xmlns.com/foaf/0.1/"
	xmlns:edm="http://www.europeana.eu/schemas/edm/" xmlns:xi="http://www.w3.org/2001/XInclude">
	<!-- include default stylesheet with general parameters and variables for 
		all our transformations -->
	<xsl:include href="config/convert2rdf/default.xsl" />

	<xsl:output method="xml" version="1.0" encoding="UTF-8"
		indent="yes" />

	<!-- global variables -->
	<xsl:variable name="aggregationUri" as="xsd:anyURI"
		select="concat('http://wsp.bbaw.de/mods/',$aggrId,'/aggregation') cast as xsd:anyURI" />
	<xsl:variable name="docIdentifier" select="//mods:url/text()" />

	<xsl:template match="/">
		<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
			xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:ore="http://www.openarchives.org/ore/terms/"
			xmlns:dc="http://purl.org/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/"
			xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:edm="http://www.europeana.eu/schemas/edm/"
			xmlns:xi="http://www.w3.org/2001/XInclude">
			<!-- integrate standard BBAW header -->
			<xsl:apply-templates select="*" />

			<rdf:Description rdf:about="{$aggregationUri}">
				<ore:describedBy rdf:resource="{$docIdentifier}" />
				<rdf:type rdf:resource="http://www.openarchives.org/ore/terms/Aggregation" />

				<xsl:apply-templates select="//mods:titleInfo//mods:title" />
				<xsl:apply-templates select="*/mods:name[@type='personal']" />
				<!-- Aggregated Resources -->
				<!-- hier kommen die aggregations hin -->
				<!-- Administrative Elemente -->
				<dc:identifier rdf:resource="{$docIdentifier}" />
				<dcterms:issued>
					<dcterms:W3CDTF>
						<xsl:apply-templates select="//mods:originInfo/mods:dateIssued" />
					</dcterms:W3CDTF>
				</dcterms:issued>
				<xsl:variable name="allPublisher" select="//mods:originInfo/mods:publisher" />
				<xsl:variable name="modsPublisher" select="$allPublisher[1]" />
				<xsl:apply-templates select="$modsPublisher" />
				<dc:language>
					<dcterms:ISO639-3>
						<xsl:variable name="allLang" select="//mods:language" />
						<xsl:variable name="lang" select="$allLang[1]/mods:languageTerm" />
						<xsl:apply-templates select="$lang" />
					</dcterms:ISO639-3>
				</dc:language>
				<dc:format>
					<xsl:variable name="allType"
						select="//mods:physicalDescription/mods:internetMediaType" />
					<xsl:variable name="mimeType" select="$allType[1]" />
					<xsl:apply-templates select="$mimeType" />
				</dc:format>
				<xsl:variable name="modsAbstract" select="//mods:abstract/text()" />
				<xsl:apply-templates select="//mods:abstract" />
				<!-- vorerst SWD -->
				<xsl:variable name="allSwd" select="//mods:subject[@authority='SWD']" />
				<xsl:apply-templates select="$allSwd[1]/mods:topic" />
				<!-- mods:geographic -->
				<xsl:variable name="allGeo"
					select="//mods:subject/mods:geographic[not(. = preceding::mods:geographic)]" />
				<xsl:apply-templates select="$allGeo" />
				<!-- mods:temporal -->

				<xsl:variable name="allTemp" select="//mods:subject/mods:temporal" />
				<xsl:apply-templates select="$allTemp[2]" />
				<!-- DDC -->
				<xsl:variable name="allDdc"
					select="//mods:classification[@authority='DDC']" />
				<xsl:variable name="modsDdc" select="$allDdc[1]/text()" />
				<dcterms:DDC rdf:value="{$modsDdc}" />
				<!-- mods:personal f�r digitale adaption -->
				<xsl:apply-templates select="//mods:subject/mods:name[@type='personal']" />
			</rdf:Description>
		</rdf:RDF>
	</xsl:template>

	<!-- Match the mods:abstract Transform it to an dc:abstract. -->
	<xsl:template match="mods:abstract">
		<dc:abstract>
			<xsl:value-of select="current()/text()" />
		</dc:abstract>
	</xsl:template>

	<!-- Match the mods:internetMediaType. Transform it to an dcterms:IMT value. 
		See http://www.iana.org/assignments/media-types for allowed media types. -->
	<xsl:template match="mods:internetMediaType">
		<dcterms:IMT rdf:value="{current()/text()}" />
	</xsl:template>

	<!-- Match the mods:languageTerm. Transform it to an rdf:value. -->
	<xsl:template match="mods:languageTerm">
		<rdf:value>
			<xsl:value-of select="current()" />
		</rdf:value>
	</xsl:template>

	<!-- Match the mods publisher. Transform it to a dc:publisher. Fetch the 
		publisher URI from the normdata if existing. -->
	<xsl:template match="mods:publisher">
		<xsl:variable name="publisher" select="current()" />
		<xsl:variable name="normdataPublisher"
			select="$normdata//foaf:name[text()=$publisher]/../@rdf:about" />
		<xsl:choose>
			<xsl:when test="$normdataPublisher != ''">
				<!-- normdata entry exists -->
				<dc:publisher rdf:resource="{$normdataPublisher}" />
			</xsl:when>
			<xsl:otherwise>
				<!-- just transform into rdf literal -->
				<dc:publisher rdf:value="{$publisher}" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Match the mods title. Transform it to a dc:title -->
	<xsl:template match="mods:title">
		<xsl:choose>
			<!-- only transform the title when NO content type attribute exists in 
				the current node (exclude nodes with attribute contentType == 'XPath' -->
			<xsl:when test="current()/@contentType">
			</xsl:when>
			<xsl:otherwise>
				<dc:title>
					<xsl:variable name="modsTitle" select="text()" />
					<xsl:value-of select="$modsTitle" />
				</dc:title>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Match the mods dateIssued Transform it to a dc:title -->
	<xsl:template match="mods:dateIssued">
		<xsl:choose>
			<!-- only transform the date when NO content type attribute exists in 
				the current node (exclude nodes with attribute contentType == 'XPath' -->
			<xsl:when test="current()/@contentType">
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="modsDateIssued" select="current()" />
				<rdf:value>
					<xsl:value-of select="$modsDateIssued" />
				</rdf:value>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- This template rule matches the mods:name tags. It will check the normdata.rdf 
		for matching URIs for a person. If a person with the same given and family 
		name exists, the output will be <dc:creator rdf:resource="{NORMDATA_URI}" 
		/> -->
	<xsl:template match="mods:name">
		<!-- family name from input mods file -->
		<xsl:variable name="modsFamily" select="mods:namePart[@type='family']" />
		<!-- given name from input mods file -->
		<xsl:variable name="modsPrename" select="mods:namePart[@type='given']" />
		<!-- role from input mods file -->
		<xsl:variable name="modsRole" select="mods:role/mods:roleTerm/text()" />
		<!-- list of person URIs for persons with the same family name within the 
			normdata file -->
		<xsl:variable name="normdataFamilyname"
			select="$normdata//foaf:familyName[text()=$modsFamily]/../@rdf:about" />
		<!-- list of person URIs for persons with the same given name within the 
			normdata file -->
		<xsl:variable name="normdataPrename"
			select="$normdata//foaf:givenName[text()=$modsPrename]/../@rdf:about" />

		<xsl:if test="$modsPrename != '' or $modsFamily != ''">
			<xsl:choose>
				<!-- .:: integrate person node if existing ::.. -->
				<xsl:when test="$normdataFamilyname != '' and $normdataPrename != ''">
					<!-- this condition will be true only for a single family name AND given -->
					<!-- name -->
					<xsl:for-each select="$normdataFamilyname">
						<xsl:variable name="outerArray" select="." />
						<xsl:for-each select="$normdataPrename">
							<xsl:variable name="innerArray" select="." />
							<xsl:if test="$innerArray = $outerArray">
								<dc:creator rdf:resource="{$innerArray}" />
							</xsl:if>
						</xsl:for-each>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise> <!-- just copy the person data -->
					<dc:creator rdf:parseType="Resource">
						<foaf:givenName>
							<xsl:value-of select="$modsPrename" />
						</foaf:givenName>
						<foaf:familyName>
							<xsl:value-of select="$modsFamily" />
						</foaf:familyName>
						<!-- @TODO concept for taking PNDS -->
						<xsl:if test="$modsRole != ''">
							<dc:description>
								<xsl:value-of select="$modsRole" />
							</dc:description>
						</xsl:if>
					</dc:creator>
				</xsl:otherwise>
				<!-- ..::::::::::::::::::::::::::::::::::::::::.. -->
			</xsl:choose>
		</xsl:if>
	</xsl:template>


	<!-- Match the mods:topic tags Transform it to a dc:subject tag. -->
	<xsl:template match="mods:topic">
		<xsl:variable name="swdTopic" select="current()" />
		<dc:subject>
			<xsl:value-of select="$swdTopic" />
		</dc:subject>
	</xsl:template>

	<!-- Match the mods:geographic tags Transform it to a dc:coverage tag. -->
	<xsl:template match="mods:geographic">
		<xsl:variable name="geo" select="current()" />
		<dc:coverage>
			<xsl:value-of select="$geo" />
		</dc:coverage>
	</xsl:template>

	<!-- Match the mods:temporal tags Transform it to a valid dcterms:temporal 
		tag in the form name={NAME_TEMPORAL};start={STARTYEAR};end={ENDYEAR};scheme={W3CDTF} 
		See http://web.resource.org/rss/1.0/modules/dcterms/#temporal for the syntax 
		and check http://www.w3.org/TR/NOTE-datetime for the date formats. -->
	<xsl:template match="mods:temporal">
		<xsl:if test="@point = 'end'">
			<xsl:variable name="tempStart"
				select="./parent::*/mods:temporal[@point = 'start']" />
			<xsl:variable name="tempEnd" select="current()" />
			<dcterms:temporal>
				<xsl:value-of
					select="concat('name= ;', $newline, 'start=',$tempStart, ';', $newline, 'end=',$tempEnd, ';', $newline, 'scheme=W3CDTF')" />
			</dcterms:temporal>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>