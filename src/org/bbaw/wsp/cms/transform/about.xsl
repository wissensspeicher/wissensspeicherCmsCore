<?xml version="1.0"?>

<xsl:stylesheet 
  version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:wsp="http://www.bbaw.de/wsp"
  exclude-result-prefixes="xsl xhtml xs wsp"
>

<xsl:function name="wsp:contains" as="xs:boolean">
  <xsl:param name="value" as="xs:anyAtomicType?"/> 
  <xsl:param name="seq" as="xs:anyAtomicType*"/> 
  <xsl:sequence select="$value = $seq"/>   
</xsl:function>
<xsl:function name="wsp:substringAfterLastMatch" as="xs:string">
  <xsl:param name="arg" as="xs:string?"/> 
  <xsl:param name="regex" as="xs:string"/> 
  <xsl:sequence select="replace($arg,concat('^.*',$regex),'')"/>
</xsl:function>

<xsl:output method="xhtml" encoding="utf-8"/>

<xsl:param name="query"></xsl:param>
<xsl:param name="type"></xsl:param>
<xsl:param name="language"></xsl:param>

<xsl:variable name="dbPediaKey" select="/about/query/key"/>
<xsl:variable name="ddc" select="/about/query/ddc"/>
<xsl:variable name="baseUrl" select="/about/query/baseUrl"/>
<xsl:variable name="typeLogo">
  <xsl:choose>
    <xsl:when test="$type = 'person'"><img src="../images/person.png" width="18" height="18" border="0" alt="Person"/></xsl:when>
    <xsl:when test="$type = 'place'"><img src="../images/place.png" width="18" height="18" border="0" alt="Place"/></xsl:when>
    <xsl:otherwise></xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="dbPediaTypes" select="//*:type/@*:resource"/>
<xsl:variable name="dbPediaType">
  <xsl:choose>
    <xsl:when test="contains('http://dbpedia.org/ontology/Person', $dbPediaTypes)"><xsl:value-of select="'person'"/></xsl:when>
    <xsl:when test="contains('http://dbpedia.org/ontology/Organisation', $dbPediaTypes)"><xsl:value-of select="'organisation'"/></xsl:when>
    <xsl:when test="contains('http://dbpedia.org/ontology/Place', $dbPediaTypes)"><xsl:value-of select="'place'"/></xsl:when>
    <xsl:otherwise><xsl:value-of select="'subject'"/></xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="dbpediaServerName">
  <xsl:choose>
    <xsl:when test="$language = 'en'"><xsl:value-of select="'http://dbpedia.org'"/></xsl:when>
    <xsl:otherwise><xsl:value-of select="concat('http://', $language, '.dbpedia.org')"/></xsl:otherwise>
  </xsl:choose>
</xsl:variable>
<xsl:variable name="dbpediaDisambiguationsEvalStr" select="concat('//*:Description[@*:about = ''', $dbpediaServerName, '/resource/', $dbPediaKey, ''']/*:wikiPageDisambiguates/@*:resource')"/>
<xsl:variable name="dbpediaDisambiguations" select="saxon:evaluate($dbpediaDisambiguationsEvalStr)" xmlns:saxon="http://saxon.sf.net/"/>
<xsl:variable name="abstractEvalStr" select="concat('//*:abstract[@xml:lang = ''', $language, ''']')"/>
<xsl:variable name="abstract" select="saxon:evaluate($abstractEvalStr)" xmlns:saxon="http://saxon.sf.net/"/>
<xsl:variable name="labelEvalStr" select="concat('//*:label[@xml:lang = ''', $language, ''']')"/>
<xsl:variable name="label" select="saxon:evaluate($labelEvalStr)" xmlns:saxon="http://saxon.sf.net/"/>
<xsl:variable name="depictions" select="//*:depiction/@*:resource"/>
<xsl:variable name="wikiUrls" select="//*:isPrimaryTopicOf/@*:resource"/>

<xsl:template match="/">
  <div class="title">
    <span class="query">About: <xsl:value-of select="$query"/></span>
    <span class="about">
      [Adapted from DBpedia]
      <a class="logo" href="http://dbpedia.org">
        <img src="../images/info.png" width="18" height="18" border="0" alt="Info DBpedia"/>
      </a>
      <a class="logo" href="http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License">
        <img src="../images/cc.svg" width="18" height="18" border="0" alt="cc license"/>
      </a>
    </span>
  </div>
  <xsl:if test="$type = 'person'">
    <div class="title">
      <span class="about">
        [Adapted from PDR]
        <a class="logo" href="http://pdr.bbaw.de">
          <img src="../images/info.png" width="18" height="18" border="0" alt="Info PDR"/>
        </a>
        <a class="logo" href="http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License">
          <img src="../images/cc.svg" width="18" height="18" border="0" alt="cc license"/>
        </a>
      </span>
    </div>
  </xsl:if>
  <xsl:if test="not(empty($label))">
    <div class="label">
      <xsl:sequence select="$typeLogo"/>
      <xsl:apply-templates select="$label"/>
    </div>
  </xsl:if>
  <xsl:if test="not(empty($label))">
  <div class="dbpedia">
    <span class="h2"><xsl:value-of select="'DBpedia'"/></span>
    <div class="resource">
      <xsl:variable name="dbPediaUrl" select="concat($dbpediaServerName, '/resource/', $dbPediaKey)"/>
      <span class="bold"><xsl:value-of select="'Resource '"/></span>
	    <ul class="urls">
	      <li class="url">
	        <a class="url" href="{$dbPediaUrl}"><xsl:value-of select="$dbPediaUrl"/></a>
	      </li>
	    </ul> 
	  </div>
	  <xsl:if test="not(empty($dbpediaDisambiguations))">
	    <div class="disambiguations">
	      <span class="bold"><xsl:value-of select="'Disambiguations: '"/></span>
	      <ul class="urls">
	        <xsl:for-each select="$dbpediaDisambiguations">
	          <xsl:variable name="disambKey" select="wsp:substringAfterLastMatch(., '/')"/>
	          <xsl:variable name="url" select="concat($baseUrl, '/query/About?query=', $disambKey, '&amp;type=', $type, '&amp;language=', $language)"/>
	          <li class="url">
	            <a class="url" href="{$url}"><xsl:value-of select="$disambKey"/></a>
	          </li>
	        </xsl:for-each>
	      </ul>
	    </div>
	  </xsl:if>
	  <xsl:if test="not(empty($abstract))">
	    <div class="abstract">
	      <span class="bold"><xsl:value-of select="'Abstract: '"/></span>
	      <xsl:apply-templates select="$abstract"/>
	    </div>
	  </xsl:if>
	  <xsl:if test="not(empty($depictions))">
	    <div class="depictions">
	      <xsl:for-each select="$depictions">
	        <xsl:variable name="url" select="."/>
	        <a href="{$url}">
	          <img class="depiction" src="{$url}" alt="depiction"/>
	        </a>
	      </xsl:for-each>
	    </div>
	  </xsl:if>
	  <xsl:if test="not(empty($wikiUrls))">
	    <div class="wiki">
	      <span class="bold"><xsl:value-of select="'Wikipedia: '"/></span>
	      <ul class="urls">
	        <xsl:for-each select="$wikiUrls">
	          <xsl:variable name="url" select="."/>
	          <li class="url">
	            <a class="url" href="{$url}"><xsl:value-of select="$url"/></a>
	          </li>
	        </xsl:for-each>
	      </ul> 
	    </div>
	  </xsl:if>
  </div>
  </xsl:if>
  <xsl:if test="$type = 'person'">
    <xsl:variable name="pdrHits" select="//*:match"/>
    <xsl:if test="not(empty($pdrHits))">
      <div class="pdr">
        <span class="h2"><xsl:value-of select="'PDR'"/></span>
        <xsl:apply-templates select="$pdrHits"/>
      </div>
    </xsl:if>
  </xsl:if>
  <xsl:if test="$type = 'place' and ($language = 'el' or $language = 'la')">
    <div class="pleiades">
      <span class="h2"><xsl:value-of select="'Pleiades: '"/></span>
      <ul class="urls">
        <xsl:variable name="url" select="concat('http://pleiades.stoa.org/search?portal_type=Place&amp;submit=Search&amp;SearchableText=', $query)"/>
        <li class="url">
          <a class="url" href="{$url}"><xsl:value-of select="$query"/></a>
        </li>
      </ul> 
    </div>
  </xsl:if>
  <xsl:if test="$type = 'ddc'">
    <div class="ddc">
      <span class="h2"><xsl:value-of select="'DDC: '"/></span>
      <ul class="urls">
        <xsl:variable name="url" select="concat('http://vzopc4.gbv.de/DB=38/CMD?ACT=SRCHA&amp;IKT=8562&amp;TRM=', $ddc)"/>
        <li class="url">
          <a class="url" href="{$url}"><xsl:value-of select="$query"/></a>
        </li>
      </ul> 
    </div>
  </xsl:if>
  <xsl:if test="$type = 'swd'">
    <div class="swd">
      <span class="h2"><xsl:value-of select="'SWD '"/></span>
      <ul class="urls">
        <xsl:variable name="url" select="concat('http://melvil.dnb.de/swd-search?term=', $query)"/>
        <li class="url">
          <a class="url" href="{$url}"><xsl:value-of select="$query"/></a>
          <!-- alternativ evtl. noch http://www.hbz-nrw.de/angebote/ -->
        </li>
      </ul> 
    </div>
  </xsl:if>
</xsl:template>

<!--  PDR person matches -->
<xsl:template match="*:match">
  <xsl:variable name="name" select="person/name"/>
  <xsl:variable name="otherNames" select="person/otherNames[normalize-space() != '']"/>
  <xsl:variable name="dateOfBirth" select="person/dateOfBirth[normalize-space() != '']"/>
  <xsl:variable name="placeOfBirth" select="person/placeOfBirth[normalize-space() != '']"/>
  <xsl:variable name="dateOfDeath" select="person/dateOfDeath[normalize-space() != '']"/>
  <xsl:variable name="placeOfDeath" select="person/placeOfDeath[normalize-space() != '']"/>
  <xsl:variable name="description" select="person/description[normalize-space() != '']"/>
  <xsl:variable name="gender" select="person/gender[normalize-space() != '']"/>
  <xsl:variable name="yearOfActivity" select="person/yearOfActivity[normalize-space() != '']"/>
  <xsl:variable name="countryOfActivity" select="person/countryOfActivity[normalize-space() != '']"/>
  <xsl:variable name="furtherProviders" select="identifiers"/>
  <xsl:variable name="pndUrl" select="identifiers/personId[@provider = 'PeEnDe']/@url[normalize-space() != '']"/>
  <xsl:variable name="gndUrl" select="identifiers/personId[@provider = 'GND']/@url[normalize-space() != '']"/>
  <xsl:variable name="lccnUrl" select="identifiers/personId[@provider = 'LCCN']/@url[normalize-space() != '']"/>
  <xsl:variable name="viafUrl" select="identifiers/personId[@provider = 'VIAF']/@url[normalize-space() != '']"/>
  <div class="person"> 
    <div class="name">
      <xsl:sequence select="$typeLogo"/>
      <xsl:value-of select="$name"/>
    </div>
    <xsl:if test="not(empty($otherNames))"><div class="otherNames"><xsl:value-of select="$otherNames"/></div></xsl:if>
    <xsl:if test="not(empty($description))"><div class="description"><xsl:value-of select="$description"/></div></xsl:if>
    <xsl:if test="not(empty($dateOfBirth))">
      <div class="birth">
        <xsl:value-of select="$dateOfBirth"/>
        <xsl:if test="not(empty($placeOfBirth))">
          <xsl:value-of select="concat(', ', $placeOfBirth)"/>
        </xsl:if>
      </div>
    </xsl:if>
    <xsl:if test="not(empty($dateOfDeath))">
      <div class="death">
        <xsl:value-of select="$dateOfDeath"/>
        <xsl:if test="not(empty($placeOfDeath))">
          <xsl:value-of select="concat(', ', $placeOfDeath)"/>
        </xsl:if>
      </div>
    </xsl:if>
    <xsl:if test="not(empty($furtherProviders))">
      <ul class="furtherProviders">
        <xsl:if test="not(empty($pndUrl))"><li class="provider"><a class="url" href="{$pndUrl}"><xsl:value-of select="$pndUrl"/></a></li></xsl:if>
        <xsl:if test="not(empty($gndUrl))"><li class="provider"><a class="url" href="{$gndUrl}"><xsl:value-of select="$gndUrl"/></a></li></xsl:if>
        <xsl:if test="not(empty($lccnUrl))"><li class="provider"><a class="url" href="{$lccnUrl}"><xsl:value-of select="$lccnUrl"/></a></li></xsl:if>
        <xsl:if test="not(empty($viafUrl))"><li class="provider"><a class="url" href="{$viafUrl}"><xsl:value-of select="$viafUrl"/></a></li></xsl:if>
      </ul>
    </xsl:if>
  </div>
</xsl:template>

</xsl:stylesheet>
