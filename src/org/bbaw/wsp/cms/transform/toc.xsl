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
  <list>
    <list type ="toc">
      <head>Table of contents</head>
      <xsl:apply-templates select="//*:div[@type = 'section' or @type = 'chapter']"/>
    </list>
    <list type="figures">
      <head>Figures</head>
      <xsl:apply-templates select="//*:figure"/>
    </list>
    <list type="handwritten">
      <head>Handwritten figures</head>
      <xsl:apply-templates select="//*:handwritten"/>
    </list>
    <list type="pages">
      <head>Pages</head>
      <xsl:apply-templates select="//*:pb"/>
    </list>
  </list>
</xsl:template>

<xsl:template match="*:figure">
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <xsl:variable name="number" select="@number"/>
  <item>
    <xsl:if test="not(empty($number))"><xsl:attribute name="n"><xsl:value-of select="$number"/></xsl:attribute></xsl:if>
    <xsl:apply-templates select="*:caption"/>
    <xsl:apply-templates select="*:description"/>
    <xsl:apply-templates select="*:variables"/>
    <xsl:if test="not(empty($page))"><ref><xsl:value-of select="$page"/></ref></xsl:if>
  </item>
</xsl:template>

<xsl:template match="*:caption"><xsl:value-of select="' '"/>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="*:description">
  <xsl:apply-templates/><xsl:value-of select="' '"/>
</xsl:template>

<xsl:template match="*:variables">
  <xsl:apply-templates/><xsl:value-of select="' '"/>
</xsl:template>

<xsl:template match="*:handwritten">
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <xsl:variable name="number" select="@number"/>
  <item>
    <xsl:if test="not(empty($number))"><xsl:attribute name="n"><xsl:value-of select="$number"/></xsl:attribute></xsl:if>
    <xsl:if test="not(empty($page))"><ref><xsl:value-of select="$page"/></ref></xsl:if>
  </item>
</xsl:template>

<xsl:template match="*:div">
  <xsl:variable name="level"><xsl:number level="multiple" count="*:div[@type = 'section' or @type = 'chapter']" format="1."/></xsl:variable>
  <xsl:variable name="depth" select="string-length(replace($level, '[^\\.]', ''))"/>
  <xsl:variable name="pb" select="./preceding::*:pb[1]"/>
  <xsl:variable name="o" select="$pb/@o"/>
  <xsl:variable name="oNorm" select="$pb/@o-norm"/>
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <item>
    <xsl:if test="not(empty($level))"><xsl:attribute name="n"><xsl:value-of select="$level"/></xsl:attribute></xsl:if>
    <xsl:if test="not(empty($level))"><xsl:attribute name="lv"><xsl:value-of select="$depth"/></xsl:attribute></xsl:if>
    <xsl:apply-templates select="*:head"/>
    <xsl:if test="not(empty($page))">
      <ref>
        <xsl:if test="not(empty($o))"><xsl:attribute name="o"><xsl:value-of select="$o"/></xsl:attribute></xsl:if>
        <xsl:if test="not(empty($oNorm))"><xsl:attribute name="o-norm"><xsl:value-of select="$oNorm"/></xsl:attribute></xsl:if>
        <xsl:value-of select="$page"/>
      </ref>
    </xsl:if>
  </item>
</xsl:template>

<xsl:template match="*:head">
  <xsl:apply-templates/><xsl:value-of select="' '"/>
</xsl:template>

<xsl:template match="*:pb">
  <xsl:variable name="page" select="count(./preceding::*:pb) + 1"/>
  <item>
    <xsl:attribute name="n"><xsl:value-of select="$page"/></xsl:attribute>
    <xsl:if test="not(empty(@o))"><xsl:attribute name="o"><xsl:value-of select="@o"/></xsl:attribute></xsl:if>
    <xsl:if test="not(empty(@o-norm))"><xsl:attribute name="o-norm"><xsl:value-of select="@o-norm"/></xsl:attribute></xsl:if>
  </item>
</xsl:template>

</xsl:stylesheet>
