<?xml version="1.0"?>

<xsl:stylesheet 
  version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="xsl xlink xhtml"
>

<xsl:output method="xhtml" encoding="utf-8"/>

<xsl:variable name="query" select="/*:concordance/*:request/*:name"/>
<xsl:variable name="hits" select="//*:match"/>

<xsl:template match="/">
  <xsl:if test="not(empty($hits))">
    <span class="query">Person knowledge about: <xsl:value-of select="$query"/></span>
    <span class="about">[This is PDR service]<a href="http://pdr.bbaw.de"><img src="../images/info.png" width="18" height="18" border="0" alt="Info PDR"/></a></span>
    <p></p>
    <div class="hits">
      <xsl:apply-templates select="$hits"/>
    </div>
  </xsl:if>
</xsl:template>

<!--  Person matches -->
<xsl:template match="*:match">
  <xsl:variable name="name" select="person/name"/>
  <xsl:variable name="otherNames" select="person/otherNames[normalize-space() != '']"/>
  <xsl:variable name="dateOfBirth" select="person/dateOfBirth[normalize-space() != '']"/>
  <xsl:variable name="placeOfBirth" select="person/placeOfBirth[normalize-space() != '']"/>
  <xsl:variable name="dateOfDeath" select="person/dateOfDeath[normalize-space() != '']"/>
  <xsl:variable name="placeOfDeath" select="person/placeOfDeath[normalize-space() != '']"/>
  <xsl:variable name="description" select="person/description[normalize-space() != '']"/>
  <xsl:variable name="wikiUrl" select="person/reference/@url[normalize-space() != '']"/>
  <xsl:variable name="furtherProviders" select="identifiers"/>
  <xsl:variable name="pndUrl" select="identifiers/personId[@provider = 'PeEnDe']/@url[normalize-space() != '']"/>
  <xsl:variable name="gndUrl" select="identifiers/personId[@provider = 'GND']/@url[normalize-space() != '']"/>
  <xsl:variable name="lccnUrl" select="identifiers/personId[@provider = 'LCCN']/@url[normalize-space() != '']"/>
  <xsl:variable name="viafUrl" select="identifiers/personId[@provider = 'VIAF']/@url[normalize-space() != '']"/>
  <div class="person">
    <div class="name"><xsl:value-of select="$name"/></div>
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
    <xsl:if test="not(empty($wikiUrl))"><div class="wikiUrl"><a class="url" href="{$wikiUrl}"><xsl:value-of select="$wikiUrl"/></a></div></xsl:if>
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
