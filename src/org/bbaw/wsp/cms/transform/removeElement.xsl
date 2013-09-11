<?xml version="1.0"?>

<xsl:stylesheet 
  version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="xsl xlink xhtml"
>

<xsl:output method="xml" encoding="utf-8"/>

<xsl:param name="elemName"></xsl:param>

<xsl:template match="@*|node()">
  <xsl:choose>
    <!-- when it is an element and the name is $elemName then remove it -->
    <xsl:when test="self::* and name() = $elemName">
      <!-- nothing -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
