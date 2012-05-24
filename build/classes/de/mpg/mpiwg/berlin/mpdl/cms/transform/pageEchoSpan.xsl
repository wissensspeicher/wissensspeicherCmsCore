<?xml version="1.0"?>
<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:functx="http://www.functx.com"
  xmlns:saxon="http://saxon.sf.net/"
  xmlns:dc="http://purl.org/dc/elements/1.1/" 
  xmlns:dcterms="http://purl.org/dc/terms"
  xmlns:echo="http://www.mpiwg-berlin.mpg.de/ns/echo/1.0/" 
  xmlns:math="http://www.w3.org/1998/Math/MathML"
  xmlns:mml="http://www.w3.org/1998/Math/MathML"
  xmlns:svg="http://www.w3.org/2000/svg"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="xsl xlink xs functx saxon dc dcterms echo math mml svg xhtml"
  >

<xsl:output method="xhtml" encoding="utf-8"/>

<xsl:param name="mode"></xsl:param>
<xsl:param name="normalization"></xsl:param>

<xsl:variable name="dictionaryServiceName" select="'http://mpdl-service.mpiwg-berlin.mpg.de/mpiwg-mpdl-lt-web/lt/GetDictionaryEntries'"/>

<xsl:template match="*:echo">
  <span class="page">
    <xsl:apply-templates mode="text"/>
  </span>
</xsl:template>

<xsl:template match="*:head|*:div|*:p|*:s|*:pb|*:cb|*:expan|*:emph|*:quote|*:q|*:blockquote|*:set-off|*:reg|*:var|*:num|*:gap|*:anchor|*:note|*:figure|*:caption|*:description|*:handwritten|*:place|*:person" mode="text">
  <xsl:element name="span">
    <xsl:attribute name="class"><xsl:value-of select="name()"/></xsl:attribute>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="text"/>
  </xsl:element>
</xsl:template>

<!-- MathML    -->
<xsl:template match="math:*" mode="text">
  <xsl:element name="{name()}" namespace="">
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="text"/>
  </xsl:element>
</xsl:template>

<!-- SVG    -->
<xsl:template match="svg:*" mode="text">
  <xsl:element name="{name()}" namespace="">
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="text"/>
  </xsl:element>
</xsl:template>

<xsl:template match="*:lb" mode="text">
  <br/><xsl:apply-templates mode="text"/>
</xsl:template>

<!-- XHTML: remove the xhtml namespace   -->
<xsl:template match="xhtml:*" mode="text">
  <xsl:variable name="hasLabel" select="string(@xlink:label) != ''"/>
  <xsl:variable name="isTable" select="name() = 'table'"/>
  <xsl:choose>
    <xsl:when test="(not($hasLabel)) or ($isTable and $hasLabel)">
      <xsl:element name="{name()}" namespace="">
        <xsl:copy-of select="@*"/>
        <xsl:apply-templates mode="text"/>
      </xsl:element>
    </xsl:when>
    <xsl:otherwise></xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- words  -->
<xsl:template match="*:w" mode="text">
  <xsl:variable name="wordLanguage" select="@lang"/>
  <xsl:variable name="form" select="encode-for-uri(string(@form))"/>
  <xsl:variable name="formNotUrlEncoded" select="string(@form)"/>
  <xsl:variable name="formRegularized" select="string(@formRegularized)"/>
  <xsl:variable name="formNormalized" select="string(@formNormalized)"/>
  <xsl:variable name="lemmas" select="string(@lemmas)"/>
  <xsl:variable name="dictionary" select="string(@dictionary)"/>
  <xsl:variable name="displayWord">
    <xsl:choose>
      <xsl:when test="$normalization = 'orig'"><xsl:apply-templates mode="text"/></xsl:when>
      <xsl:when test="$normalization = 'reg' and $formRegularized = ''"><xsl:apply-templates mode="text"/></xsl:when>
      <xsl:when test="$normalization = 'reg' and $formRegularized != ''"><xsl:sequence select="$formRegularized"/></xsl:when>
      <xsl:when test="$normalization = 'norm'"><xsl:apply-templates mode="text"/></xsl:when>
      <xsl:otherwise><xsl:apply-templates mode="text"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="displayWordUrlEncoded" select="encode-for-uri($displayWord)"/>
  <xsl:choose>
    <xsl:when test="$dictionary = 'true' and $mode = 'tokenized'">
      <span class="w">
        <xsl:copy-of select="@*"/>
        <xsl:attribute name="href"><xsl:value-of select="concat($dictionaryServiceName, '?query=', $form, '&amp;queryDisplay=', $displayWordUrlEncoded, '&amp;language=', $wordLanguage, '&amp;outputFormat=html', '&amp;outputType=morphCompact&amp;outputType=dictFull')"/></xsl:attribute>
        <xsl:sequence select="$displayWord"/>
      </span>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="$displayWord"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="text()" mode="text">
  <xsl:value-of select="."/>
</xsl:template>

</xsl:stylesheet>
