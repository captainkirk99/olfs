<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
                xmlns:wcs="http://www.opengis.net/wcs"
                xmlns:gml="http://www.opengis.net/gml"
                xmlns:dapwcs="http://www.opendap.org/ns/dapwcs"

                >
    <xsl:output method='html' version='1.0' encoding='UTF-8' indent='yes'/>


    <xsl:template match="wcs:WCS_Capabilities">
        <html>
            <head>
                <link rel='stylesheet' href='/opendap/docs/css/contents.css'
                      type='text/css'/>
                <title>OPeNDAP Hyrax: WCS Coverage Offerings at <xsl:value-of select="wcs:Service/wcs:name"/></title>
            </head>





            <body>
                <!-- ****************************************************** -->
                <!--                      PAGE BANNER                       -->
                <!--                                                        -->
                <!--                                                        -->

                <img alt="OPeNDAP Logo" src='/opendap/docs/images/logo.gif'/>
                <h1>
                    <xsl:value-of select="wcs:Service/wcs:label"/>
                </h1>
                <hr size="1" noshade="noshade"/>

                <!-- ****************************************************** -->
                <!--                       PAGE BODY                        -->
                <!--                                                        -->
                <!--                                                        -->
                <pre>
                    <table border="0" width="100%">
                        <tr>
                            <th align="left">Name</th>
                            <!-- <th align="center">Description</th> -->
                            <th align="center">Lat/Lon Envelope</th>
                        </tr>
                        <tr>
                            <td>
                                <xsl:if test="/dataset/name!='/'" >
                                    <a href="..">Parent Directory/</a>
                                </xsl:if>
                                <xsl:if test="/dataset/@prefix!='/'" >
                                    <xsl:if test="/dataset/name='/'" >
                                        <a href="..">Parent Directory/</a>
                                    </xsl:if>
                                </xsl:if>
                            </td>
                        </tr>

                        <!-- Process Coverage Offerings -->
                        <xsl:for-each select="wcs:ContentMetadata/wcs:CoverageOfferingBrief">
                                <tr>
                                    <td align="left">
                                        <a href="{dapwcs:DapLink}">
                                            <xsl:value-of select="wcs:label"/>
                                        </a>
                                    </td>
                                    <!--
                                    <td align="left">
                                        <xsl:choose>
                                            <xsl:when test="starts-with(wcs:description,'http://')">
                                                <a href="{wcs:description}"> Description </a>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="wcs:description" />
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </td>
                                    -->
                                    <td align="left">
                                        <xsl:for-each select="wcs:lonLatEnvelope/gml:pos">
                                            [<xsl:value-of select="."/>]
                                        </xsl:for-each>
                                    </td>
                                </tr>
                        </xsl:for-each>
                    </table>
                </pre>

                <!-- ****************************************************** -->
                <!--                              FOOTER                    -->
                <!--                                                        -->
                <!--                                                        -->
                <hr size="1" noshade="noshade"/>
                <table width="100%" border="0">
                    <tr>
                        <td>
                            <div class="small" align="left">
                                THREDDS Catalog
                                <a href="/opendap/catalog.html">
                                    HTML
                                </a>
                                &NBSP;
                                <a href="/opendap/catalog.xml">
                                    XML
                                </a>
                            </div>
                        </td>
                        <td>
                            <div class="small" align="right">
                                Hyrax development sponsored by
                                <a href='http://www.nsf.gov/'>NSF</a>
                                ,
                                <a href='http://www.nasa.gov/'>NASA</a>
                                , and
                                <a href='http://www.noaa.gov/'>NOAA</a>
                            </div>
                        </td>
                    </tr>
                </table>

                <!-- ****************************************************** -->
                <!--                                                        -->
                <h3>OPeNDAP Hyrax WCS Gateway

                    <br/>
                    <a href='/opendap/docs/'>Documentation</a>
                </h3>


            </body>
        </html>
    </xsl:template>




</xsl:stylesheet>
