<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:mods="http://www.loc.gov/mods/v3" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:ore="http://www.openarchives.org/ore/terms/" xmlns:dc="http://purl.org/elements/1.1/"
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
				<!-- ..:: small test ::.. -->
				<xsl:variable name="normdataTest" select="$normdata//rdf:Description[1]/@rdf:about" />
				<test><xsl:value-of select="$normdataTest" /></test>
				
				<!-- ..:: test end ::.. -->
				<ore:describedBy rdf:resource="{$docIdentifier}" />
				<rdf:type rdf:resource="http://www.openarchives.org/ore/terms/Aggregation" />
				<xsl:variable name="modsTitle" select="//mods:title/text()" />
				<dc:title>
					<xsl:value-of select="$modsTitle" />
				</dc:title>
				<!--mods:personal f�r quelltext -->
				<xsl:apply-templates select="*/mods:name[@type='personal']" />
				<!-- Aggregated Resources -->
				<!-- hier kommen die aggregations hin -->
				<!-- Administrative Elemente -->
				<xsl:variable name="modsTitle" select="//mods:url/text()" />
				<dc:title>
					<xsl:value-of select="$modsTitle" />
				</dc:title>
				<dc:identifier rdf:resource="{$docIdentifier}" />
				<dcterms:created>
					<dcterms:W3CDTF>
						<rdf:value></rdf:value>
					</dcterms:W3CDTF>
				</dcterms:created>
				<dcterms:issued>
					<dcterms:W3CDTF>
						<xsl:variable name="modsDateIssued"
							select="//mods:originInfo/mods:dateIssued/text()" />
						<rdf:value>
							<xsl:value-of select="$modsDateIssued" />
						</rdf:value>
					</dcterms:W3CDTF>
				</dcterms:issued>
				<xsl:variable name="allPublisher" select="//mods:originInfo/mods:publisher" />
				<xsl:variable name="modsPublisher" select="$allPublisher[1]/text()" />
				<dc:publisher rdf:resource="{$modsPublisher}" />
				<dc:language>
					<dcterms:ISO639-3>
						<xsl:variable name="allLang" select="//mods:language" />
						<xsl:variable name="lang"
							select="$allLang[1]/mods:languageTerm/text()" />
						<rdf:value>
							<xsl:value-of select="$lang" />
						</rdf:value>
					</dcterms:ISO639-3>
				</dc:language>
				<!-- dcmiType -->
				<dc:type><!-- nicht ausgezeichnet -->
				</dc:type>
				<edm:hasType />
				<dc:format>
					<xsl:variable name="allType"
						select="//mods:physicalDescription/mods:internetMediaType" />
					<xsl:variable name="mimeType" select="$allType[1]/text()" />
					<dcterms:IMT rdf:value="{$mimeType}" />
					<!--<dcterms:DCMIType rdf:value="InteractiveResource"/> -->
				</dc:format>
				<dc:modified>
					<dcterms:W3CDTF>
						<rdf:value><!-- nicht ausgezeichnet -->
						</rdf:value>
					</dcterms:W3CDTF>
				</dc:modified>
				<dc:extent><!-- nicht ausgezeichnet z.B.: ca. 7345 Titeleintr�ge -->
				</dc:extent>
				<xsl:variable name="modsAbstract" select="//mods:abstract/text()" />
				<dc:abstract>
					<xsl:value-of select="$modsAbstract" />
				</dc:abstract>
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
			<rdf:Description rdf:about="">
				<!-- hier kommen die allgemeinen Vorhabensdaten hin -->
				<!-- wo bekommt man diese her? -> Vorhabens-Daten-Parser?! -->
			</rdf:Description>
		</rdf:RDF>
	</xsl:template>

	<xsl:template match="mods:name">
		<!-- decide, if the personal is a creator -->
		<xsl:if test="mods:role/mods:roleTerm/text() = 'Original Content'">			
				<!--<foaf:name><xsl:value-of select="concat($modsPrename, ' ', $modsFamily)"/></foaf:name> -->
				<xsl:choose>
					<xsl:when test="mods:namePart[@type] != ''">
						<xsl:variable name="modsFamily" select="mods:namePart[@type='family']" />
						<xsl:variable name="modsPrename" select="mods:namePart[@type='given']" />
						<xsl:variable name="normdataFamilyname" select="$normdata//foaf:familyName[text()=$modsFamily]/../@rdf:about" />
						<xsl:variable name="normdataPrename" select="$normdata//foaf:givenName[text()=$modsPrename]/../@rdf:about" />
						<xsl:choose>	
							<!--  ..:: integrate person node if existing ::.. -->													
							<xsl:when test="$normdataFamilyname != '' and $normdataPrename != ''">
								<!--  this condition will be true only for a single family name AND given name -->
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
							<xsl:otherwise>												
								<dc:creator rdf:parseType="Resource">									
									<foaf:givenName>
										<xsl:value-of select="$modsPrename" />
									</foaf:givenName>
									<foaf:familyName>
										<xsl:value-of select="$modsFamily" />
									</foaf:familyName>
									<!--hier muss die PND irgendwie hin -->
									<dc:description>Original Content</dc:description>
								</dc:creator>
							</xsl:otherwise>
							<!--  ..::::::::::::::::::::::::::::::::::::::::.. -->	
						</xsl:choose>						
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="modsnamePart" select="mods:namePart[@type='given']" />
						<xsl:variable name="modsFamily"
							select="substring-before($modsnamePart, ',')" />
						<xsl:variable name="modsPrename"
							select="substring-after($modsnamePart, ', ')" />
						<foaf:givenName>
							<xsl:value-of select="$modsPrename" />
						</foaf:givenName>
						<foaf:familyName>
							<xsl:value-of select="$modsFamily" />
						</foaf:familyName>
					</xsl:otherwise>
				</xsl:choose>				
		</xsl:if>
		<xsl:if test="mods:role/mods:roleTerm/text() = 'Content Revision'">
			<dc:creator rdf:parseType="Resource">
				<!--<foaf:name><xsl:value-of select="concat($modsPrename, ' ', $modsFamily)"/></foaf:name> -->
				<xsl:choose>
					<xsl:when test="mods:namePart[@type] != ''">
						<xsl:variable name="modsFamily" select="mods:namePart[@type='family']" />
						<xsl:variable name="modsPrename" select="mods:namePart[@type='given']" />
						<xsl:variable name="normdataFamilyname" select="$normdata//foaf:familyName[text()=$modsFamily]/../@rdf:about" />
						<xsl:variable name="normdataPrename" select="$normdata//foaf:givenName[text()=$modsPrename]/../@rdf:about" />
						<xsl:choose>	
							<!--  ..:: integrate person node if existing ::.. -->													
							<xsl:when test="$normdataFamilyname != '' and $normdataPrename != ''">
								<!--  this condition will be true only for a single family name AND given name -->
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
							<xsl:otherwise>											
								<dc:creator rdf:parseType="Resource">									
									<foaf:givenName>
										<xsl:value-of select="$modsPrename" />
									</foaf:givenName>
									<foaf:familyName>
										<xsl:value-of select="$modsFamily" />
									</foaf:familyName>
									<!--hier muss die PND irgendwie hin -->
									<dc:description>Content Revision</dc:description>
							</xsl:otherwise>
								<!--  ..::::::::::::::::::::::::::::::::::::::::.. -->	
							</xsl:choose>													
					</xsl:when>
					<xsl:otherwise>
					
						<xsl:variable name="modsnamePart" select="mods:namePart[@type='given']" />
						<xsl:variable name="modsFamily"
							select="substring-before($modsnamePart, ',')" />
						<xsl:variable name="modsPrename"
							select="substring-after($modsnamePart, ', ')" />
						<foaf:givenName>
							<xsl:value-of select="$modsPrename" />
						</foaf:givenName>
						<foaf:familyName>
							<xsl:value-of select="$modsFamily" />
						</foaf:familyName>
						<dc:description>Content Revision</dc:description>
						</dc:creator>
					</xsl:otherwise>					
				</xsl:choose>
				<!--hier muss die PND irgendwie hin -->				
			
		</xsl:if>
		<xsl:if test="mods:role/mods:roleTerm/text() = 'Digital Adaptation' ">
			<dc:creator rdf:parseType="Resource">
				<!--<foaf:name><xsl:value-of select="concat($modsPrename, ' ', $modsFamily)"/></foaf:name> -->
				<xsl:choose>
					<xsl:when test="mods:namePart[@type] != ''">
						<xsl:variable name="modsFamily" select="mods:namePart[@type='family']" />
						<xsl:variable name="modsPrename" select="mods:namePart[@type='given']" />
						<xsl:variable name="normdataFamilyname" select="$normdata//foaf:familyName[text()=$modsFamily]/../@rdf:about" />
						<xsl:variable name="normdataPrename" select="$normdata//foaf:givenName[text()=$modsPrename]/../@rdf:about" />
						<xsl:choose>	
							<!--  ..:: integrate person node if existing ::.. -->													
							<xsl:when test="$normdataFamilyname != '' and $normdataPrename != ''">
								<!--  this condition will be true only for a single family name AND given name -->
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
							<xsl:otherwise>	
							
							</xsl:otherwise>
							<!--  ..::::::::::::::::::::::::::::::::::::::::.. -->	
						</xsl:choose>				
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="modsnamePart" select="mods:namePart[@type='given']" />
						<xsl:variable name="modsFamily"
							select="substring-before($modsnamePart, ',')" />
						<xsl:variable name="modsPrename"
							select="substring-after($modsnamePart, ', ')" />
						<foaf:givenName>
							<xsl:value-of select="$modsPrename" />
						</foaf:givenName>
						<foaf:familyName>
							<xsl:value-of select="$modsFamily" />
						</foaf:familyName>
					</xsl:otherwise>
				</xsl:choose>
				<!--hier muss die PND irgendwie hin -->
				<dc:description>Digital Adaptation</dc:description>
			</dc:creator>
		</xsl:if>
		<xsl:if test="./parent::*/name() = 'mods:subject'">
			<!-- im mods:subject enthalten -->
			<dc:subject>
				<!--<foaf:name><xsl:value-of select="concat($modsPrename, ' ', $modsFamily)"/></foaf:name> -->
				<xsl:choose>
					<xsl:when test="mods:namePart[@type] != ''">
						<xsl:variable name="modsFamily" select="mods:namePart[@type='family']" />
						<xsl:variable name="modsPrename" select="mods:namePart[@type='given']" />
						<foaf:givenName>
							<xsl:value-of select="$modsPrename" />
						</foaf:givenName>
						<foaf:familyName>
							<xsl:value-of select="$modsFamily" />
						</foaf:familyName>
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="modsnamePart" select="mods:namePart" />
						<xsl:variable name="modsFamily"
							select="substring-before($modsnamePart, ',')" />
						<xsl:variable name="modsPrename"
							select="substring-after($modsnamePart, ', ')" />
						<foaf:givenName>
							<xsl:value-of select="$modsPrename" />
						</foaf:givenName>
						<foaf:familyName>
							<xsl:value-of select="$modsFamily" />
						</foaf:familyName>
					</xsl:otherwise>
				</xsl:choose>
				<!--hier muss die PND irgendwie hin -->
			</dc:subject>
		</xsl:if>
	</xsl:template>

	<xsl:template match="mods:topic">
		<xsl:variable name="swdTopic" select="current()" />
		<dc:subject>
			<xsl:value-of select="$swdTopic" />
		</dc:subject>
	</xsl:template>

	<xsl:template match="mods:geographic">
		<xsl:variable name="geo" select="current()" />
		<dc:coverage>
			<xsl:value-of select="$geo" />
		</dc:coverage>
	</xsl:template>

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