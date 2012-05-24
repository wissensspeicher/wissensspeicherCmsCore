<?xml version="1.0"?>

<xsl:stylesheet 
  version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="xsl xlink xhtml"
>

<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:template match="/">
  <tocs>
    <toc>
      <xsl:apply-templates select="//*:div[@type = 'section' or @type = 'chapter']"/>
    </toc>
    <figures>
      <xsl:apply-templates select="//*:figure"/>
    </figures>
    <handwrittens>
      <xsl:apply-templates select="//*:handwritten"/>
    </handwrittens>
  </tocs>
</xsl:template>

<xsl:template match="*:figure">
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <xsl:variable name="number" select="@number"/>
  <figure>
    <xsl:if test="not(empty($number))"><xsl:attribute name="number"><xsl:value-of select="$number"/></xsl:attribute></xsl:if>
    <xsl:if test="not(empty($page))"><xsl:attribute name="page"><xsl:value-of select="$page"/></xsl:attribute></xsl:if>
    <xsl:apply-templates select="*:caption"/>
    <xsl:apply-templates select="*:description"/>
    <xsl:apply-templates select="*:variables"/>
  </figure>
</xsl:template>

<xsl:template match="*:caption">
  <caption><xsl:apply-templates/></caption>
</xsl:template>

<xsl:template match="*:description">
  <description><xsl:apply-templates/></description>
</xsl:template>

<xsl:template match="*:variables">
  <variables><xsl:apply-templates/></variables>
</xsl:template>

<xsl:template match="*:handwritten">
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <xsl:variable name="number" select="@number"/>
  <handwritten>
    <xsl:if test="not(empty($number))"><xsl:attribute name="number"><xsl:value-of select="$number"/></xsl:attribute></xsl:if>
    <xsl:if test="not(empty($page))"><xsl:attribute name="page"><xsl:value-of select="$page"/></xsl:attribute></xsl:if>
  </handwritten>
</xsl:template>

<xsl:template match="*:div">
  <xsl:variable name="level"><xsl:number level="multiple" count="*:div[@type = 'section' or @type = 'chapter']" format="1."/></xsl:variable>
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <entry>
    <xsl:if test="not(empty($level))"><xsl:attribute name="level"><xsl:value-of select="$level"/></xsl:attribute></xsl:if>
    <xsl:if test="not(empty($page))"><xsl:attribute name="page"><xsl:value-of select="$page"/></xsl:attribute></xsl:if>
    <xsl:apply-templates select="*:head"/>
  </entry>
</xsl:template>

<xsl:template match="*:head">
  <head><xsl:apply-templates/></head>
</xsl:template>

</xsl:stylesheet>
