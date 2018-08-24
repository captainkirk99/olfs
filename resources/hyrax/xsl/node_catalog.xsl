<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ /////////////////////////////////////////////////////////////////////////////
  ~ // This file is part of the "Hyrax Data Server" project.
  ~ //
  ~ //
  ~ // Copyright (c) 2016 OPeNDAP, Inc.
  ~ // Author: Nathan David Potter  <ndp@opendap.org>
  ~ //
  ~ // This library is free software; you can redistribute it and/or
  ~ // modify it under the terms of the GNU Lesser General Public
  ~ // License as published by the Free Software Foundation; either
  ~ // version 2.1 of the License, or (at your option) any later version.
  ~ //
  ~ // This library is distributed in the hope that it will be useful,
  ~ // but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  ~ // Lesser General Public License for more details.
  ~ //
  ~ // You should have received a copy of the GNU Lesser General Public
  ~ // License along with this library; if not, write to the Free Software
  ~ // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
  ~ //
  ~ // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
  ~ /////////////////////////////////////////////////////////////////////////////
  -->
<!DOCTYPE xsl:stylesheet [
<!ENTITY NBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;</xsl:text>" >
<!ENTITY DBSP "<xsl:text disable-output-escaping='yes'>&amp;nbsp;&amp;nbsp;</xsl:text>" >
]>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:thredds="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:bes="http://xml.opendap.org/ns/bes/1.0#"
        >
    <xsl:param name="dapService"/>
    <xsl:param name="allowDirectDataSourceAccess"/>
    <xsl:param name="useDAP2ResourceUrlResponse"/>
    <xsl:param name="ncWmsServiceBase"/>
    <xsl:param name="ncWmsDynamicServiceId"/>
    <xsl:param name="WcsServices"/>

    <xsl:output method='xml' version='1.0' encoding='UTF-8' indent='yes'/>
    <xsl:key name="service-by-name" match="//thredds:service" use="@name"/>


    
    <xsl:variable name="besPrefix">
        <xsl:choose>
            <xsl:when test="/bes:response/bes:showNode/@prefix!='/'">
                <xsl:value-of select="concat(/bes:response/bes:showNode/@prefix,'/')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="/bes:response/bes:showNode/@prefix"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>


    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:showNode">
        <thredds:catalog>
            <besPrefix><xsl:value-of select="$besPrefix"/></besPrefix>
            <thredds:service name="dap" serviceType="OPeNDAP" base="{$dapService}"/>
            <thredds:service name="file" serviceType="HTTPServer" base="{$dapService}"/>
            <xsl:if test="$ncWmsServiceBase">
                <thredds:service name="wms" serviceType="WMS" base="{$ncWmsServiceBase}" />
            </xsl:if>
            <xsl:if test="$WcsServices">
                <xsl:apply-templates select="$WcsServices" mode="serviceBase"/>
            </xsl:if>

            <xsl:apply-templates />
        </thredds:catalog>
    </xsl:template>

    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:response">
            <xsl:apply-templates />
    </xsl:template>

    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:item">
        <xsl:variable name="ID">
            <xsl:choose>
                <xsl:when test="../@name='/'">
                    <xsl:value-of select="concat($dapService,$besPrefix,@name)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="$besPrefix='/'"><xsl:value-of select="concat($dapService,../@name,@name)" /></xsl:when>
                        <xsl:otherwise><xsl:value-of select="concat($dapService,$besPrefix,../@name,@name)" /></xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="lower-case(@type)='node'">
                <thredds:catalogRef name="{@name}" xlink:href="{@name}/catalog.xml" xlink:title="{@name}" xlink:type="simple" ID="{$ID}/" />
            </xsl:when>
            <xsl:otherwise>
                <!-- It's a leaf, aka a file or a granule or whatnot -->
                <thredds:dataset name="{@name}" ID="{$ID}" >
                    <thredds:dataSize units="bytes"><xsl:value-of select="@size" /></thredds:dataSize>
                    <thredds:date type="modified"><xsl:value-of select="@lastModified" /></thredds:date>
                    <xsl:call-template name="DatasetAccess"/>
                </thredds:dataset>
            </xsl:otherwise>
        </xsl:choose>



    </xsl:template>

    <!--***********************************************
       -
       -
     -->
    <xsl:template match="bes:node">

        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="@name='/'" >
                    <xsl:value-of select="/bes:response/bes:showNode/@prefix" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="$besPrefix='/'"><xsl:value-of select="@name"/></xsl:when>
                        <xsl:otherwise><xsl:value-of select="concat($besPrefix,@name)"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="ID">
            <xsl:value-of select="concat($dapService, $name)" />
        </xsl:variable>

        <thredds:dataset name="{$name}" ID="{$ID}">
            <xsl:apply-templates />
        </thredds:dataset>

    </xsl:template>

    <xsl:template name="DatasetAccess">

        <xsl:variable name="urlPath">
            <xsl:choose>
                <xsl:when test="../@name='/'" >
                    <xsl:value-of select="concat($besPrefix,@name)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="$besPrefix='/'"><xsl:value-of select="concat(../@name,@name)" /></xsl:when>
                        <xsl:otherwise><xsl:value-of select="concat($besPrefix,../@name,@name)" /></xsl:otherwise>
                    </xsl:choose>

                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>


        <xsl:choose>
            <xsl:when test="@isData='true'">
                <thredds:access>
                    <xsl:attribute name="serviceName">dap</xsl:attribute>
                    <xsl:attribute name="urlPath"><xsl:value-of select="$urlPath" /></xsl:attribute>
                </thredds:access>
                <xsl:if test="$allowDirectDataSourceAccess='true'">
                    <thredds:access>
                        <xsl:attribute name="serviceName">file</xsl:attribute>
                        <xsl:attribute name="urlPath">
                            <xsl:value-of select="$urlPath" />
                            <xsl:if test="$useDAP2ResourceUrlResponse!='true'">.file</xsl:if>
                        </xsl:attribute>
                    </thredds:access>
                </xsl:if>
                <xsl:if test="$ncWmsServiceBase">
                    <thredds:access>
                        <xsl:attribute name="serviceName">wms</xsl:attribute>
                        <xsl:attribute name="urlPath">?DATASET=<xsl:value-of select="$ncWmsDynamicServiceId" /><xsl:value-of select="$urlPath" />&amp;SERVICE=WMS&amp;VERSION=1.3.0&amp;REQUEST=GetCapabilities</xsl:attribute>
                    </thredds:access>
                </xsl:if>
                <xsl:if test="$WcsServices">
                    <xsl:apply-templates select="$WcsServices" mode="dataAccess">
                        <xsl:with-param name="urlPath"><xsl:value-of select="$urlPath"/></xsl:with-param>
                    </xsl:apply-templates>
                </xsl:if>
            </xsl:when>
            <xsl:otherwise>
                <thredds:access>
                    <xsl:attribute name="serviceName">file</xsl:attribute>
                    <xsl:attribute name="urlPath"><xsl:value-of select="$urlPath" /></xsl:attribute>
                </thredds:access>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


    <xsl:template match="Wcs" mode="serviceBase">
        <thredds:service name="{@name}" serviceType="WCS" base="{@base}" />
    </xsl:template>

    <xsl:template match="Wcs" mode="dataAccess">
        <xsl:param name="urlPath"/>

        <xsl:if test="matches($urlPath, @matchRegex)" >
            <thredds:access>
                <xsl:attribute name="serviceName"><xsl:value-of select="@name"/></xsl:attribute>
                <xsl:attribute name="urlPath">/<xsl:value-of select="@dynamicServiceId" /><xsl:value-of select="$urlPath" />?SERVICE=WCS&amp;REQUEST=GetCapabilities</xsl:attribute>
            </thredds:access>
        </xsl:if>



    </xsl:template>




</xsl:stylesheet>
