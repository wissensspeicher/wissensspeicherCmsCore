<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY nbsp "&#xa0;">
<!ENTITY apos "&#x2019;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:aodl="http://pdr.bbaw.de/namespaces/aodl/" xmlns:podl="http://pdr.bbaw.de/namespaces/podl/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="xs aodl podl mods" version="1.0">
    <!--<desc>
          <p>Created on: Jan 8, 2014</p>
          <p>Author: Anke Maiwald</p>
          <p>Affiliation: DFG-Projekt "Personendaten-Repositorium", Berlin-Brandenburgische Akademie der Wissenschaften</p>
          
          <p>Stylesheet for PDR idi interface<p>
          
          <p>update: Feb 3, 2014 (A. Maiwald)<p>
          <p>update: Feb 19, 2014 (A. Maiwald)<p>
          <p>update: Feb 20, 2014 (A. Maiwald)<p>
          <p>last update: Apr 28, 2014 (A. Maiwald)<p>
      </desc>-->

    <xsl:output method="html" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" encoding="UTF-8" indent="yes"/>

    <xsl:strip-space elements="*"/>
    <xsl:preserve-space elements="aodl:notification"/>

    <!-- Bei Bedarf neues Projekt ergänzen -->
    <xsl:template name="get_project_name">
        <xsl:param name="person_id"/>
        <xsl:variable name="proj_id">
            <!--Example: extract from "pdrPo.001.005.000000086" instance- and projectnumber -> "001.005" -->
            <xsl:value-of select="concat(substring-before(substring-after($person_id,'.'),'.'),'.',substring-before(substring-after(substring-after($person_id,'.'),'.'),'.'))"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$proj_id = '001.001'">
                <xsl:value-of select="'Protokolle des preußischen Staatsministeriums'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.002'">
                <xsl:value-of select="'Mitglieder der Vorgängerakademien der BBAW'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.003'">
                <xsl:value-of select="'Sekundärliteraturdatenbank zu Alexander von Humboldt'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.004'">
                <xsl:value-of select="'Personenregister der Marx-Engels-Gesamtausgabe'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.005'">
                <xsl:value-of select="'Berliner Klassik: Virtuelles Berlin um 1800'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.006'">
                <xsl:value-of select="'Sitzungsprotokolle der Akademie'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.007'">
                <xsl:value-of select="'Berliner Klassik: Historisches Berlin um 1800'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.008'">
                <xsl:value-of select="'Jahresberichte für deutsche Geschichte'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.009'">
                <xsl:value-of select="'Personenregister der Marx-Engels-Gesamtausgabe (B)'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.010'">
                <xsl:value-of select="'Preußen als Kulturstaat'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.011'">
                <xsl:value-of select="'Alexander-von-Humboldt-Bibliographie'"/>
            </xsl:when>
            <xsl:when test="$proj_id = '001.202'">
                <xsl:value-of select="'Fallmerayer-Briefwechsel'"/>
            </xsl:when>
            <xsl:otherwise>
                <!--<xsl:variable name="url">
                    <xsl:text>https://pdrprod.bbaw.de/idi/pdescr/</xsl:text>
                    <xsl:value-of select="$person_id"/>
                </xsl:variable>
                <xsl:value-of select="document($url)/text()"/>-->
                <xsl:value-of select="$proj_id"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--bei Bedarf weitere sem. Kategorien für die explizite Abfolge ergänzen (mit Komma trennen)-->
    <xsl:variable name="first_semKat">
        <!--Names-->
        <xsl:text>NormName_DE</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>Name</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>pseudonym</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>penName</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>biographicalData</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>principalDescription</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>education</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>career</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>curriculum</xsl:text>
    </xsl:variable>

    <!--bei Bedarf weitere offiziele Provider ergänzen, die angezeigt werden sollen; und in Template "get_Normdaten_URL" URL ergänzen (mit Komma trennen)-->
    <xsl:variable name="showID">
        <xsl:text>GND</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>PND</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>LCCN</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>VIAF</xsl:text>
        <xsl:text>,</xsl:text>
        <xsl:text>PeEnDe</xsl:text>
    </xsl:variable>

    <!-- URL für Normdaten -->
    <xsl:template name="get_Normdaten_URL">
        <xsl:param name="provider"/>
        <xsl:variable name="url">
            <xsl:variable name="id_name">
                <xsl:value-of select="translate(@provider, $uppercase, $smallcase)"/>
            </xsl:variable>
            <xsl:choose>
                <!--GND-->
                <xsl:when test="$id_name ='gnd'">
                    <xsl:text>http://d-nb.info/gnd/</xsl:text>
                </xsl:when>
                <!--PND-->
                <xsl:when test="$id_name ='pnd'">
                    <xsl:text>FALSE</xsl:text>
                </xsl:when>
                <!--VIAF-->
                <xsl:when test="$id_name ='viaf'">
                    <xsl:text>http://viaf.org/viaf/</xsl:text>
                </xsl:when>
                <!--PeEnDe-->
                <xsl:when test="$id_name ='peende'">
                    <xsl:text>http://toolserver.org/~apper/pd/person/peende/</xsl:text>
                </xsl:when>
                <!--LCCN-->
                <xsl:when test="$id_name ='lccn'">
                    <xsl:text>http://lccn.loc.gov/</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="$url"/>
    </xsl:template>

    <!-- Hilfsvariablen als Ersatz für lower_case() und upper_case() -->
    <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyzäöü'"/>
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ'"/>


    <xsl:template match="/">
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                <title>PDR Person <xsl:value-of select="results/@id"/></title>
                <style type="text/css">
                    <xsl:text disable-output-escaping="yes">
                        &lt;!--
                         body, html { margin: 0;
			                 padding: 0; 
			                 font-family: Verdana, Geneva, Arial, Helvetica, sans-serif; 
			                 font-size: 12px; 
			                 background-color: #d9ddde;
			                 
			                 width: 980px;
                             margin-left: auto;
	                         margin-right: auto;
	                         background-color: #fff;
	                         min-height: 762px;}
	                    
	                    a {text-decoration:none;}
	                    
                        #head h1 { font-size: 18px;}
		   
                        #body h2 { font-size: 14px;
                              line-height: 1.5;
                              font-weight: bold;
                              padding-top: 1.5ex;}
                        #body ul { list-style-position: outside;
                              margin: 0; padding-left: 20px;}
                        
                        #foot { height: 70px;
		                      width: 980px;
		                      font-size: 10px; 
		                      display: block;
		                      padding: 5px;
		                      background-color: #fff;;
		                      border-top: 1px dotted #d9ddde;
		                      margin-top: 20px;}
		                 
		                 span.markup a {
		                    position:relative;
                            z-index:1;
                            color:#000000;
                            text-decoration:none;}
                         span.markup a:hover {
                            z-index:2;}
                         span.markup a span {
                            display: none;}
                         span.markup a:hover span {
                            display:block;
                            position:absolute;
                            top:2em;
                            left:1em;
                            border:1px solid #6E6E6E;
                            background-color:#FAFAFA;
                            color:#000000;
                            padding: 3px;
                            font-size: 0.8em;}
                         a.persName {border-bottom: 2px dotted #AC58FA;}
                         a.persName:hover {background-color:#F2E0F7; border: 0px;}
                         a.date {border-bottom: 2px dotted #2E9AFE;}
                         a.date:hover {background-color:#CEECF5; border: 0px;}
                         a.placeName {border-bottom: 2px dotted #B45F04;}
                         a.placeName:hover {background-color:#F3E2A9; border: 0px;}
                         a.orgName {border-bottom: 2px dotted #AEB404;}
                         a.orgName:hover {background-color:#F3F781; border: 0px;}
                         a.name {border-bottom: 2px dotted #31B404;}
                         a.name:hover {background-color:#BCF5A9; border: 0px;}

		                .provider {font-size: 10px;
		                      font-style: italic;
		                      font-weight: lighter;}
		                 span.project {font-size: 10px;
		                      font-style: italic;
		                      color: #6D6666;}
		                 span.quelle {font-size: 10px;
		                      font-style: normal;
		                      color: #6D6666;}
		                 li.beziehungen {list-style-type:none; }
		                 li.space {margin-top: 1.5ex;}
			            //--&gt;
                    </xsl:text>
                </style>
            </head>
            <body>
                <div id="header">
                    <!--den NormNamen des ersten Personenobjekts ausgeben-->
                    <h1>
                        <xsl:choose>
                            <xsl:when test="//result//aodl:notification[parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname_de')][not(preceding::aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname_de')])]]">
                                <xsl:call-template name="title">
                                    <xsl:with-param name="name" select="//result//aodl:notification[parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname_de')][not(preceding::aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname_de')])]]"/>
                                </xsl:call-template>
                            </xsl:when>
                            <xsl:when test="//result//aodl:notification[parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname')][not(preceding::aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname')])]]">
                                <xsl:value-of select="//result//aodl:notification[parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname')][not(preceding::aodl:semanticStm[contains(translate(text(),$uppercase,$smallcase),'normname')])]]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="/results/@id"/>
                                <xsl:text disable-output-escaping="yes">&lt;br/&gt;&lt;span class=&quot;quelle&quot; style=&quot;font-weight:normal&quot;&gt;[kein Name vorhanden]&lt;/span&gt;</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </h1>
                </div>

                <div id="body">
                    <!-- Aspekte (anhand ihrer semantischen Kategorie gruppieren) -->
                    <!-- *A) sem. Kategorien in der Reihenfolge ausgeben, wie sie in $first_semKat angeordnet sind -->
                    <xsl:call-template name="list_sem_Kategorie">
                        <xsl:with-param name="array" select="$first_semKat"/>
                    </xsl:call-template>

                    <!-- *B) alle übrigen sem. Kategorien, unsortiert ausgeben -->
                    <xsl:for-each select="//aodl:semanticStm">
                        <xsl:variable name="semStm">
                            <xsl:value-of select="."/>
                        </xsl:variable>
                        <xsl:variable name="test_group1">
                            <xsl:choose>
                                <xsl:when test="preceding::aodl:semanticStm[contains(.,$semStm)]">
                                    <xsl:text>no</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>yes</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <!-- Gruppieren: nach sem. Kategorie -->
                        <xsl:if test="$test_group1='yes'">
                            <xsl:if test="(not(contains($first_semKat,.)))">
                                <xsl:call-template name="list_sem_Kategorie">
                                    <xsl:with-param name="array" select="."/>
                                </xsl:call-template>
                            </xsl:if>
                        </xsl:if>
                    </xsl:for-each>


                    <!--Beziehungen-->
                    <xsl:if test="//aodl:relationDim/aodl:relationStm[aodl:relation[not(text()='aspect_of')]]">
                        <h2>relations</h2>
                        <ul>
                            <xsl:for-each select="//aodl:relationDim/aodl:relationStm[aodl:relation[not(text()='aspect_of')]]">
                                <xsl:variable name="subject_id">
                                    <xsl:value-of select="@subject"/>
                                </xsl:variable>
                                <xsl:variable name="test_group1">
                                    <xsl:choose>
                                        <xsl:when test="preceding::aodl:relationDim/aodl:relationStm[aodl:relation[not(text()='aspect_of')]]/@subject[contains(.,$subject_id)]">
                                            <xsl:text>no</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:text>yes</xsl:text>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:variable>
                                <!-- Gruppieren: nach @subject -->
                                <xsl:if test="$test_group1='yes'">
                                    <xsl:variable name="url">
                                        <xsl:text>https://pdrprod.bbaw.de/idi/pdr/</xsl:text>
                                    </xsl:variable>
                                    <li class="space">
                                        <!--PersonID (@subject) ausgeben-->
                                        <xsl:choose>
                                            <!-- Personennamen nur verlinken, wenn Person nicht Hauptperson ist -->
                                            <xsl:when test="not(//result/@id=$subject_id)">
                                                <a href="{concat($url,@subject)}" onclick="window.open('{concat($url,@subject)}'); return false">
                                                    <xsl:value-of select="@subject"/>
                                                </a>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="@subject"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                        <ul>
                                            <xsl:for-each select="//aodl:relationDim/aodl:relationStm/aodl:relation[not(text()='aspect_of')][parent::aodl:relationStm[@subject=$subject_id]]">
                                                <xsl:variable name="object_id" select="@object"/>
                                                <xsl:variable name="test_group2">
                                                    <xsl:choose>
                                                        <xsl:when test="preceding::aodl:relationDim/aodl:relationStm/aodl:relation[not(text()='aspect_of')][parent::aodl:relationStm[@subject=$subject_id]]/@object[contains(.,$object_id)]">
                                                            <xsl:text>no</xsl:text>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:text>yes</xsl:text>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:variable>
                                                <!-- Gruppieren: nach @object -->
                                                <xsl:if test="$test_group2='yes'">
                                                    <!-- PersonID (@object) ausgeben -->
                                                    <li class="beziehungen">
                                                        <xsl:text>...</xsl:text>
                                                        <xsl:text> </xsl:text>
                                                        <!-- relation ausgeben -->
                                                        <xsl:value-of select="."/>
                                                        <xsl:text> </xsl:text>
                                                        <!-- PersonID (@object) ausgeben -->
                                                        <xsl:choose>
                                                            <!-- Personennamen nur verlinken, wenn Person nicht Hauptperson ist -->
                                                            <xsl:when test="not(//result/@id=$object_id)">
                                                                <a href="{concat($url,@object)}" onclick="window.open('{concat($url,@object)}'); return false">
                                                                    <xsl:value-of select="@object"/>
                                                                </a>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of select="@object"/>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </li>
                                                </xsl:if>
                                            </xsl:for-each>
                                        </ul>
                                    </li>
                                </xsl:if>
                            </xsl:for-each>
                        </ul>
                    </xsl:if>


                    <!-- Quellen -->
                    <h2>references</h2>
                    <ul>
                        <xsl:for-each select="//result/@id">
                            <xsl:variable name="project_id">
                                <!--Example: extract from "pdrPo.001.005.000000086" instance- and projectnumber-> "001.005" -->
                                <xsl:value-of select="concat(substring-before(substring-after(.,'.'),'.'),'.',substring-before(substring-after(substring-after(.,'.'),'.'),'.'))"/>
                            </xsl:variable>
                            <xsl:variable name="person_id">
                                <xsl:value-of select="."/>
                            </xsl:variable>
                            <xsl:variable name="test_group1">
                                <xsl:choose>
                                    <xsl:when test="preceding::result/@id[contains(.,$project_id)]">
                                        <xsl:text>no</xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text>yes</xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <!-- Gruppieren: nach Projekt -->
                            <xsl:if test="$test_group1='yes'">
                                <li class="space">
                                    <xsl:text>Projekt&nbsp;</xsl:text>
                                    <i>
                                        <xsl:text>&quot;</xsl:text>
                                        <xsl:call-template name="get_project_name">
                                            <xsl:with-param name="person_id" select="$person_id"/>
                                        </xsl:call-template>
                                        <xsl:text>&quot;</xsl:text>
                                    </i>
                                    <ul style="list-style-type:none; list-style-position:outside; margin:0; padding-left:20px;">
                                        <xsl:for-each select="//mods:titleInfo/mods:title[contains(ancestor::result/@id,$project_id)]">
                                            <xsl:variable name="title" select="."/>
                                            <xsl:variable name="test_group2">
                                                <xsl:choose>
                                                    <!--Vergleich, ob Titel übereinstimmt-->
                                                    <xsl:when test="preceding::mods:titleInfo/mods:title[contains(.,$title)][contains(ancestor::result/@id,$project_id)]">
                                                        <xsl:text>no</xsl:text>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:text>yes</xsl:text>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:variable>
                                            <!-- Gruppieren: nach Titel -->
                                            <xsl:if test="$test_group2='yes'">
                                                <li class="space">
                                                    <!-- Genre -->
                                                    <xsl:if test="ancestor::mods:mods/mods:genre">
                                                        <xsl:text>[</xsl:text>
                                                        <xsl:value-of select="ancestor::mods:mods/mods:genre"/>
                                                        <xsl:text>] </xsl:text>
                                                    </xsl:if>
                                                    <xsl:choose>
                                                        <!-- Quellen verlinken, wenn URL vorhanden (außer zu pdrdev.bbaw.de oder pdrdeve.bbaw.de)-->
                                                        <xsl:when test="(ancestor::mods:mods/mods:location/mods:url) and (not(contains(ancestor::mods:mods/mods:location/mods:url,'pdrdev')))">
                                                            <xsl:variable name="url">
                                                                <xsl:value-of select="ancestor::mods:mods/mods:location/mods:url"/>
                                                            </xsl:variable>
                                                            <a href="{$url}" onclick="window.open('{$url}'); return false">
                                                                <!--Titel anzeigen-->
                                                                <span style="font-style: italic">
                                                                    <xsl:value-of select="."/>
                                                                </span>
                                                            </a>
                                                        </xsl:when>
                                                        <xsl:otherwise>
                                                            <!--Titel anzeigen-->
                                                            <span style="font-style: italic">
                                                                <xsl:value-of select="."/>
                                                            </span>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                    <!-- beteiligte Personen -->
                                                    <xsl:if test="ancestor::mods:mods/mods:name">
                                                        <br/>
                                                        <xsl:for-each select="ancestor::mods:mods/mods:name">
                                                            <xsl:if test="mods:namePart[@type='given']">
                                                                <xsl:value-of select="mods:namePart[@type='given']"/>
                                                                <xsl:text> </xsl:text>
                                                            </xsl:if>
                                                            <xsl:if test="mods:namePart[@type='family']">
                                                                <xsl:value-of select="mods:namePart[@type='family']"/>
                                                            </xsl:if>
                                                            <xsl:if test="mods:role">
                                                                <xsl:variable name="role">
                                                                    <xsl:choose>
                                                                        <xsl:when test="mods:role/mods:roleTerm">
                                                                            <xsl:value-of select="mods:role/mods:roleTerm"/>
                                                                        </xsl:when>
                                                                        <xsl:otherwise>
                                                                            <xsl:value-of select="mods:role"/>
                                                                        </xsl:otherwise>
                                                                    </xsl:choose>
                                                                </xsl:variable>
                                                                <xsl:text> (</xsl:text>
                                                                <xsl:value-of select="$role"/>
                                                                <xsl:text>)</xsl:text>
                                                            </xsl:if>
                                                            <xsl:if test="following-sibling::mods:name">
                                                                <xsl:text>, </xsl:text>
                                                            </xsl:if>
                                                        </xsl:for-each>
                                                    </xsl:if>
                                                    <!-- originInfo -->
                                                    <xsl:if test="ancestor::mods:mods/mods:originInfo">
                                                        <xsl:if test="ancestor::mods:mods/mods:name">
                                                            <xsl:text>; </xsl:text>
                                                        </xsl:if>
                                                        <xsl:if test="ancestor::mods:mods/mods:originInfo/mods:publisher">
                                                            <xsl:value-of select="ancestor::mods:mods/mods:originInfo/mods:publisher[1]"/>
                                                            <xsl:text> (pub.)</xsl:text>
                                                        </xsl:if>
                                                        <xsl:if test="ancestor::mods:mods/mods:originInfo/mods:place">
                                                            <xsl:if test="ancestor::mods:mods/mods:originInfo/mods:publisher">
                                                                <xsl:text>, </xsl:text>
                                                            </xsl:if>
                                                            <xsl:value-of select="ancestor::mods:mods/mods:originInfo/mods:place"/>
                                                        </xsl:if>
                                                        <xsl:if test="ancestor::mods:mods/mods:originInfo/node()[contains(translate(name(),$uppercase,$smallcase),'date')]">
                                                            <xsl:text> [</xsl:text>
                                                            <xsl:value-of select="name(ancestor::mods:mods/mods:originInfo/node()[contains(translate(name(),$uppercase,$smallcase),'date')])"/>
                                                            <xsl:text>:] </xsl:text>
                                                            <xsl:choose>
                                                                <xsl:when test="contains(ancestor::mods:mods/mods:originInfo/node()[contains(translate(name(),$uppercase,$smallcase),'date')],'T')">
                                                                    <xsl:value-of select="substring-before(ancestor::mods:mods/mods:originInfo/node()[contains(translate(name(),$uppercase,$smallcase),'date')],'T')"/>
                                                                </xsl:when>
                                                                <xsl:otherwise>
                                                                    <xsl:value-of select="ancestor::mods:mods/mods:originInfo/node()[contains(translate(name(),$uppercase,$smallcase),'date')]"/>
                                                                </xsl:otherwise>
                                                            </xsl:choose>
                                                        </xsl:if>
                                                    </xsl:if>
                                                    <xsl:text>.</xsl:text>
                                                </li>
                                            </xsl:if>
                                        </xsl:for-each>
                                    </ul>
                                </li>
                            </xsl:if>
                        </xsl:for-each>
                    </ul>



                    <!-- Projekt/e -->
                    <h2>projects</h2>
                    <ul>
                        <xsl:for-each select="//result/@id">
                            <xsl:variable name="project_id">
                                <!--Example: extract from "pdrPo.001.005.000000086" instance- and projectnumber-> "001.005" -->
                                <xsl:value-of select="concat(substring-before(substring-after(.,'.'),'.'),'.',substring-before(substring-after(substring-after(.,'.'),'.'),'.'))"/>
                            </xsl:variable>
                            <xsl:variable name="person_id">
                                <xsl:value-of select="."/>
                            </xsl:variable>
                            <xsl:variable name="test_group1">
                                <xsl:choose>
                                    <xsl:when test="preceding::result/@id[contains(.,$project_id)]">
                                        <xsl:text>no</xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text>yes</xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <!-- Gruppieren: nach Projekt -->
                            <xsl:if test="$test_group1='yes'">
                                <li>
                                    <xsl:call-template name="get_project_name">
                                        <xsl:with-param name="person_id" select="$person_id"/>
                                    </xsl:call-template>
                                </li>
                            </xsl:if>
                        </xsl:for-each>
                    </ul>

                    <!-- IDs/Normdaten -->
                    <h2>identifier</h2>
                    <ul>
                        <li>
                            <!-- PDR-Personen-IDs ermitteln -->
                            <xsl:text>PDR-ID: </xsl:text>
                            <ul>
                                <xsl:for-each select="//result/podl:person/@id">
                                    <li>
                                        <a href="https://pdrprod.bbaw.de/idi/pdr/{.}" onclick="window.open('https://pdrprod.bbaw.de/idi/pdr/{.}'); return false">
                                            <xsl:value-of select="."/>
                                        </a>
                                    </li>
                                </xsl:for-each>
                            </ul>
                        </li>
                    </ul>
                    <ul>
                        <!-- Sonstige IDs ermitteln -->
                        <xsl:for-each select="//podl:identifier">
                            <xsl:variable name="group_provider">
                                <xsl:value-of select="@provider"/>
                            </xsl:variable>
                            <xsl:variable name="test_group1">
                                <xsl:choose>
                                    <xsl:when test="preceding::node()//podl:identifier/@provider[contains(.,$group_provider)]">
                                        <xsl:text>no</xsl:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text>yes</xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <!-- Gruppieren: nach Provider -->
                            <xsl:if test="$test_group1 = 'yes'">
                                <xsl:if test="contains(translate($showID, $uppercase, $smallcase),translate(@provider,$uppercase,$smallcase))">
                                    <xsl:for-each select="//podl:identifier[@provider=$group_provider]">
                                        <xsl:variable name="group_id">
                                            <xsl:value-of select="."/>
                                        </xsl:variable>
                                        <xsl:variable name="test_group2">
                                            <xsl:choose>
                                                <xsl:when test="preceding::node()//podl:identifier[. = $group_id][contains(@provider,$group_provider)]">
                                                    <xsl:text>no</xsl:text>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:text>yes</xsl:text>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:variable>
                                        <!-- Gruppieren: nach ID -->
                                        <xsl:if test="$test_group2 = 'yes'">
                                            <xsl:variable name="provider_name">
                                                <xsl:choose>
                                                    <xsl:when test="contains(@provider,'.')">
                                                        <xsl:value-of select="substring-before(@provider,'.')"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of select="@provider"/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:variable>
                                            <xsl:variable name="url">
                                                <xsl:call-template name="get_Normdaten_URL">
                                                    <xsl:with-param name="provider" select="$provider_name"/>
                                                </xsl:call-template>
                                            </xsl:variable>
                                            <xsl:variable name="id">
                                                <xsl:choose>
                                                    <xsl:when test="translate($provider_name, $uppercase, $smallcase)='lccn'">
                                                        <xsl:value-of select="translate(.,'/','')"/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <xsl:value-of select="."/>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </xsl:variable>
                                            <li>
                                                <xsl:value-of select="$provider_name"/>
                                                <xsl:text>: </xsl:text>
                                                <xsl:choose>
                                                    <xsl:when test="$url='FALSE'">
                                                        <xsl:value-of select="."/>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <a href="{$url}{$id}" onclick="window.open('{$url}{$id}'); return false">
                                                            <xsl:value-of select="."/>
                                                        </a>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                            </li>
                                        </xsl:if>
                                    </xsl:for-each>
                                </xsl:if>
                            </xsl:if>
                        </xsl:for-each>
                    </ul>
                </div>

                <!-- Fußzeile -->
                <div id="foot">
                    <p>Hinweis: Dieses Personendatenblatt wurde automatisch erzeugt. Es enthält die Zusammenstellung der im <a href="http://www.personendaten.org/" onclick="window.open('http://www.personendaten.org/'); return false">PDR</a> öffentlich zugänglichen Daten zu einer Person aus verschiedenen Projekten. URL:&nbsp;<xsl:element name="a">
                            <xsl:attribute name="href">
                                <xsl:text>https://pdrprod.bbaw.de/idi/</xsl:text>
                                <xsl:value-of select="translate(results/@provider,$uppercase,$smallcase)"/>
                                <xsl:text>/</xsl:text>
                                <xsl:value-of select="/results/@id"/>
                            </xsl:attribute>
                            <xsl:text>https://pdrprod.bbaw.de/idi/</xsl:text>
                            <xsl:value-of select="translate(results/@provider,$uppercase,$smallcase)"/>
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="/results/@id"/>
                        </xsl:element>
                    </p>
                </div>

            </body>
        </html>
    </xsl:template>

    <!-- Transformation des Markups im Notification-Element -->
    <xsl:template match="aodl:notification/*">
        <xsl:choose>
            <!-- Zeilenumbruch in html -->
            <xsl:when test="name()='lb'">
                <br/>
            </xsl:when>
            <!-- farbliche Hinterlegung und Tooltip für Markups in Notification -->
            <xsl:otherwise>
                <span class="markup">
                    <a class="{name()}" href="#">
                        <xsl:apply-templates/>
                        <span>
                            <b>
                                <xsl:value-of select="name()"/>
                            </b>
                            <br/>
                            <xsl:for-each select="@*">
                                <xsl:text>&nbsp;&nbsp;</xsl:text>
                                <i>
                                    <xsl:value-of select="name()"/>
                                </i>
                                <xsl:text>:&nbsp;</xsl:text>
                                <xsl:value-of select="."/>
                                <xsl:if test="position() != last()">
                                    <br/>
                                </xsl:if>
                            </xsl:for-each>
                        </span>
                    </a>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- ************** named templates *************** -->

    <!--sem. Kategorien ausgeben, die explizit per Variable übergeben wurden-->
    <xsl:template name="list_sem_Kategorie">
        <xsl:param name="array"/>
        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="contains($array,',')">
                    <xsl:value-of select="substring-before($array,',')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$array"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="//aodl:semanticStm[text()=$name]">
            <h2>
                <xsl:value-of select="$name"/>
                <xsl:variable name="provider_normname">
                    <xsl:for-each select="//aodl:semanticStm[text()=$name]/@provider">
                        <xsl:variable name="provider">
                            <xsl:value-of select="."/>
                        </xsl:variable>
                        <xsl:variable name="test_group1">
                            <xsl:choose>
                                <xsl:when test="preceding::aodl:semanticStm[text()=$name]/@provider[contains(.,$provider)]">
                                    <xsl:text>no</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>yes</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <!-- Gruppieren: nach Provider -->
                        <xsl:if test="$test_group1='yes'">
                            <xsl:value-of select="."/>
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:variable>
                <br/>
                <span class="provider">
                    <xsl:text>classification provider: </xsl:text>
                    <xsl:value-of select="substring($provider_normname,1,(string-length($provider_normname)-2))"/>
                </span>
            </h2>
            <ul>
                <xsl:for-each select="//aodl:notification[parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[text()=$name]]">
                    <xsl:variable name="semStm">
                        <xsl:value-of select="."/>
                    </xsl:variable>
                    <xsl:variable name="test_group2">
                        <xsl:choose>
                            <xsl:when test="preceding::aodl:notification[parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[text()=$name]][contains(.,$semStm)]">
                                <xsl:text>no</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>yes</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <!-- Gruppieren: nach notification Text -->
                    <xsl:if test="$test_group2='yes'">
                        <xsl:call-template name="write_aspects">
                            <xsl:with-param name="node" select="."/>
                            <xsl:with-param name="semKat" select="$name"/>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:for-each>
            </ul>
        </xsl:if>
        <!--rekursiver Aufruf des Templates: Template solange wiederholen, bis Array kein Komma mehr enthält und der letzte Wert erreicht ist-->
        <xsl:if test="contains($array,',')">
            <xsl:call-template name="list_sem_Kategorie">
                <xsl:with-param name="array" select="substring-after($array,',')"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!-- Ausgabe der Aspekte -->
    <xsl:template name="write_aspects">
        <xsl:param name="node"/>
        <xsl:param name="semKat"/>
        <xsl:choose>
            <!-- Projekt: 001.005 (Berliner Klassik: Stadtplan) -->
            <!-- Sonderfall: sem. Kat. "images" in Projekt: 001.005 -->
            <xsl:when test="(ancestor::aodl:aspect[contains(@id,'pdrAo.001.005.')]) and ($semKat='images')">
                <!--Bild für die sem. Kat. "images" anzeigen-->
                <xsl:variable name="url">
                    <xsl:value-of select="substring-before(ancestor::aodl:aspect//aodl:validation/aodl:validationStm/aodl:reference[contains(@internal,'get_abbildung.php')]/@internal,'(')"/>
                </xsl:variable>
                <li class="space" style="list-style-type: none; max-width:400px">
                    <img src="{$url}" alt="[ ]" style="max-width:400px max-height:400px"/>
                    <br/>
                    <!--Notification anzeigen-->
                    <xsl:apply-templates select="ancestor::aodl:aspect//aodl:notification"/>
                    <xsl:text> </xsl:text>
                    <!-- Quelle/n ermitteln und anzeigen -->
                    <xsl:for-each select="ancestor::aodl:aspect//aodl:validation/aodl:validationStm/aodl:reference[contains(@internal,'get_abbildung.php')]">
                        <xsl:call-template name="get_references">
                            <xsl:with-param name="pdrRo_id" select="."/>
                            <xsl:with-param name="show_url" select="'yes'"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </li>
            </xsl:when>
            <!-- Projekt: 001.002 (Altmitglieder) -->
            <!-- Sonderfall: sem. Kat. "portraits" in Projekt: 001.002 -->
            <xsl:when test="(ancestor::aodl:aspect[contains(@id,'pdrAo.001.002.')]) and ($semKat='portraits')">
                <!--Bild für die sem. Kat. "portraits" anzeigen-->
                <xsl:variable name="url">
                    <xsl:value-of select="substring-before(ancestor::aodl:aspect//aodl:validation/aodl:validationStm/aodl:reference[contains(@internal,'/bilder/')]/@internal,'(')"/>
                </xsl:variable>
                <li class="space" style="list-style-type: none; max-width:400px">
                    <img src="{$url}" alt="[ ]" style="max-width:400px; max-height:200px"/>
                    <br/>
                    <!--Notification anzeigen-->
                    <xsl:apply-templates select="ancestor::aodl:aspect//aodl:notification"/>
                    <xsl:text> </xsl:text>
                    <!-- Quelle/n ermitteln und anzeigen -->
                    <xsl:for-each select="ancestor::aodl:aspect//aodl:validation/aodl:validationStm/aodl:reference[contains(@internal,'/bilder/')]">
                        <xsl:call-template name="get_references">
                            <xsl:with-param name="pdrRo_id" select="."/>
                            <xsl:with-param name="show_url" select="'yes'"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </li>
            </xsl:when>
            <!--Sonstige Ausgabe, ohne Berücksichtigung von Projektbesonderheiten-->
            <xsl:otherwise>
                <li class="space">
                    <!--Notification anzeigen-->
                    <xsl:apply-templates/>
                    <br/>
                    <!-- Projekt/e und Quellen ermitteln und anzeigen -->
                    <xsl:call-template name="write_aspect_notes">
                        <xsl:with-param name="node" select="$node"/>
                        <xsl:with-param name="semKat" select="$semKat"/>
                    </xsl:call-template>
                </li>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Projekt/e für Aspekt ermitteln (Quellen projektweise ausgeben)-->
    <xsl:template name="write_aspect_notes">
        <xsl:param name="node"/>
        <xsl:param name="semKat"/>
        <!-- Projekt/e ermitteln und anzeigen -->
        <xsl:for-each select="//result[.//aodl:notification[.=$node][parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[text()=$semKat]]]">
            <xsl:variable name="project_id">
                <!--Example: extract from "pdrPo.001.005.000000086" instance- and projectnumber -> "001.005" -->
                <xsl:value-of select="concat(substring-before(substring-after(@id,'.'),'.'),'.',substring-before(substring-after(substring-after(@id,'.'),'.'),'.'))"/>
            </xsl:variable>
            <xsl:variable name="person_id">
                <xsl:value-of select="@id"/>
            </xsl:variable>
            <xsl:variable name="test_group1">
                <xsl:choose>
                    <xsl:when test="preceding::result[contains(@id,$project_id)][.//aodl:notification[.=$node][parent::aodl:aspect/aodl:semanticDim/aodl:semanticStm[text()=$semKat]]]">
                        <xsl:text>no</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>yes</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <!-- Gruppieren: nach Projekt -->
            <xsl:if test="$test_group1='yes'">
                <span class="project">
                    <xsl:text>Projekt&nbsp;&quot;</xsl:text>
                    <xsl:call-template name="get_project_name">
                        <xsl:with-param name="person_id" select="$person_id"/>
                    </xsl:call-template>
                    <xsl:text>&quot;</xsl:text>
                </span>
                <ul style="list-style-type:none">
                    <!-- Quelle/n ermitteln und anzeigen -->
                    <xsl:for-each select="//aodl:validationStm/aodl:reference[ancestor::result[contains(@id,$project_id)]][ancestor::aodl:aspect/aodl:notification[.=$node]][ancestor::aodl:aspect/aodl:semanticDim/aodl:semanticStm[text()=$semKat]]">
                        <xsl:variable name="reference_id">
                            <xsl:value-of select="."/>
                        </xsl:variable>
                        <xsl:variable name="test_group2">
                            <xsl:choose>
                                <xsl:when test="preceding::aodl:validationStm/aodl:reference[ancestor::result[contains(@id,$project_id)]][ancestor::aodl:aspect/aodl:notification[.=$node]][ancestor::aodl:aspect/aodl:semanticDim/aodl:semanticStm[text()=$semKat]]">
                                    <xsl:text>no</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>yes</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <!-- Gruppieren: nach Quelle -->
                        <xsl:if test="$test_group2='yes'">
                            <li>
                                <xsl:call-template name="get_references">
                                    <xsl:with-param name="pdrRo_id" select="."/>
                                    <xsl:with-param name="show_url" select="'no'"/>
                                </xsl:call-template>
                            </li>
                        </xsl:if>
                    </xsl:for-each>
                </ul>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <!--Quelle/n von Aspekten ermitteln-->
    <xsl:template name="get_references">
        <xsl:param name="pdrRo_id"/>
        <xsl:param name="show_url"/>
        <span class="quelle">
            <xsl:text>Quelle:&nbsp;</xsl:text>
            <!-- Titel der Quelle ermitteln -->
            <xsl:for-each select="//mods:mods[@ID=$pdrRo_id]/mods:titleInfo/mods:title">
                <xsl:variable name="title">
                    <xsl:value-of select="."/>
                </xsl:variable>
                <xsl:variable name="test_group1">
                    <xsl:choose>
                        <xsl:when test="preceding::mods:mods[@ID=$pdrRo_id]/mods:titleInfo/mods:title[contains(.,$title)]">
                            <xsl:text>no</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>yes</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <!-- Gruppieren: nach Titel -->
                <xsl:if test="$test_group1='yes'">
                    <xsl:value-of select="."/>
                </xsl:if>
            </xsl:for-each>
            <!-- @internal ermitteln -->
            <xsl:if test="@internal!=''">
                <xsl:text> (</xsl:text>
                <xsl:choose>
                    <!-- Link anzeigen, wenn vorhanden -->
                    <xsl:when test="contains(@internal,'http')">
                        <xsl:choose>
                            <!-- als "Link" -->
                            <xsl:when test="$show_url='no'">
                                <xsl:variable name="link">
                                    <xsl:value-of select="substring-before(@internal,' (')"/>
                                </xsl:variable>
                                <a href="{$link}" onclick="window.open('{$link}'); return false">Link</a>
                            </xsl:when>
                            <!-- als vollständige URL -->
                            <xsl:when test="$show_url='yes'">
                                <xsl:variable name="link">
                                    <xsl:value-of select="substring-before(@internal,' (')"/>
                                </xsl:variable>
                                <a href="{$link}" onclick="window.open('{$link}'); return false">
                                    <xsl:value-of select="@internal"/>
                                </a>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@internal"/>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text>)</xsl:text>
            </xsl:if>
            <!-- Interpretation an Quelle anhängen -->
            <xsl:if test="following-sibling::aodl:interpretation!=''">
                <xsl:text>; </xsl:text>
                <xsl:value-of select="following-sibling::aodl:interpretation"/>
            </xsl:if>
            <xsl:if test="$show_url='yes'">
                <xsl:text>. </xsl:text>
            </xsl:if>
        </span>
    </xsl:template>

    <!-- ändert die Anzeige für spezielle Wörter im Titel (z.B. "genannt" ist nicht fett und kleiner) -->
    <xsl:template name="title">
        <xsl:param name="name"/>

        <xsl:variable name="part">
            <xsl:choose>
                <xsl:when test="contains($name,' ')">
                    <xsl:value-of select="substring-before($name,' ')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$name"/>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:variable>
        <xsl:choose>
            <xsl:when test="translate($part,$uppercase,$smallcase)='genannt'">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:when test="translate($part,$uppercase,$smallcase)='deckname'">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:when test="contains(translate($part,$uppercase,$smallcase),'pseudonym')">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:when test="translate($part,$uppercase,$smallcase) = 'eigtl.'">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:when test="translate($part,$uppercase,$smallcase) = 'bekannt '">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:when test="translate($part,$uppercase,$smallcase) = 'als'">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:when test="translate($part,$uppercase,$smallcase) = 'geb.'">
                <span style="font-weight:normal; font-size:10pt">
                    <xsl:value-of select="$part"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$part"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="substring-after($name, ' ')!=''">
            <xsl:text> </xsl:text>
            <xsl:call-template name="title">
                <xsl:with-param name="name" select="substring-after($name, ' ')"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
