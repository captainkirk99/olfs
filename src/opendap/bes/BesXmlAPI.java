/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.bes;

import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.Iterator;

import opendap.ppt.PPTException;
import opendap.ppt.OPeNDAPClient;

/**
 * User: ndp
 * Date: Sep 5, 2008
 * Time: 1:07:46 PM
 */
public class BesXmlAPI {

    public static final Namespace BES_NS = Namespace.getNamespace("http://xml.opendap.org/ns/bes/1.0#");

    public static String DDS        = "dds";
    public static String DAS        = "das";
    public static String DDX        = "ddx";
    public static String DAP2       = "dods";
    public static String STREAM     = "stream";
    public static String ASCII      = "ascii";
    public static String HTML_FORM  = "html_form";
    public static String INFO_PAGE  = "info_page";





    public static String ERRORS_CONTEXT  = "errors";
    public static String XML_ERRORS      = "xml";
    public static String DAP2_ERRORS     = "dap2";
    public static String XMLBASE_CONTEXT = "xml:base";

    private static final String XDAP_ACCEPT_CONTEXT = "xdap_accept";
    private static final String DEFAULT_XDAP_ACCEPT = "2.0";

    private static final String EXPICIT_CONTAINERS_CONTEXT = "dap_explicit_containers";



    private static Logger log;
    private static boolean _initialized = false;


    /**
     * The name of the BES Exception Element.
     */
    private static String BES_ERROR = "BESError";

    /**
     * Initializes logging for the BesAPI class.
     */
    public static void init() {

        if (_initialized) return;

        log = org.slf4j.LoggerFactory.getLogger(BesXmlAPI.class);

        _initialized = true;


    }


    public static boolean isConfigured() {
        return BESManager.isConfigured();
    }


    public static Document getVersionDocument(String path) throws Exception {
        return BESManager.getVersionDocument(path);
    }

    public static Document getCombinedVersionDocument() throws Exception {
        return BESManager.getCombinedVersionDocument();
    }

    public static void configure(OLFSConfig olfsConfig) throws Exception {

        BESManager.configure(olfsConfig.getBESConfig());

    }


    /**
     * Writes an OPeNDAP DDX for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param xmlBase The request URL.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to errors returned by
     *                             the BES..
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws java.io.IOException               .
     * @throws opendap.ppt.PPTException              .
     */
    public static boolean writeDDX(String dataSource,
                                String constraintExpression,
                                String xdap_accept,
                                String xmlBase,
                                OutputStream os,
                                OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDDXRequest(dataSource,constraintExpression,xdap_accept,xmlBase),
                os,
                err);
    }


    public static boolean getDDXDocument(String dataSource,
                                          String constraintExpression,
                                          String xdap_accept,
                                          String xmlBase,
                                          Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

            return besTransaction(
                    dataSource,
                    getDDXRequest(dataSource,constraintExpression,xdap_accept, xmlBase),
                    response);
    }

    /**
     * Writes an OPeNDAP DDS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request.
     * @param xdap_accept The version of the DAP the BES is to use to package the
     * reponse.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDDS(String dataSource,
                                String constraintExpression,
                                String xdap_accept,
                                OutputStream os,
                                OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDDSRequest(dataSource,constraintExpression,xdap_accept),
                os,
                err);
    }



    /**
     * Writes the source data (it is often a file, thus the method name) to
     * the passed stream.
     *
     * @param dataSource     The requested DataSource
     * @param os             The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeFile(String dataSource,
                                 OutputStream os,
                                 OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getStreamRequest(dataSource),
                os,
                err);
    }



    /**
     * Writes an OPeNDAP DAS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDAS(String dataSource,
                                String constraintExpression,
                                String xdap_accept,
                                OutputStream os,
                                OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDASRequest(dataSource,constraintExpression,xdap_accept),
                os,
                err);
    }




    /**
     * Writes an OPeNDAP DAP2 data response for the dataSource to the
     * passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeDap2Data(String dataSource,
                                     String constraintExpression,
                                     String xdap_accept,
                                     OutputStream os,
                                     OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDap2DataRequest(dataSource,constraintExpression,xdap_accept),
                os,
                err);
    }

    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeNetcdfFileOut(String dataSource,
                                             String constraintExpression,
                                            String xdap_accept,
                                            OutputStream os,
                                            OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getNetcdfFileOutRequest(dataSource,constraintExpression,xdap_accept),
                os,
                err);
    }





    /**
     * Writes the ASCII representation _rawOS the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeASCII(String dataSource,
                                  String constraintExpression,
                                  String xdap_accept,
                                  OutputStream os,
                                  OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getAsciiDataRequest(dataSource,constraintExpression,xdap_accept),
                os,
                err);
    }


    /**
     * Writes the HTML data request form (aka the I.F.H.) for the OPeNDAP the
     * dataSource to the passed stream.
     *
     * @param dataSource The requested DataSource
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param url The URL to refernence in the HTML form.
     * @param os  The Stream to which to write the response.
     * @param err The Stream to which to write errors returned by the BES.
     * @return True is everything goes well, false if the BES returns an error.
     * @throws BadConfigurationException .
     * @throws PPTException              .
     * @throws IOException              .
     * @throws BESError              .
     */
    public static boolean writeHTMLForm(String dataSource,
                                        String xdap_accept,
                                        String url,
                                        OutputStream os,
                                        OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getHtmlFormRequest(dataSource,xdap_accept,url),
                os,
                err);
    }


    /**
     * Writes the OPeNDAP INFO response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static boolean writeHtmlInfoPage(String dataSource,
                                            String xdap_accept,
                                            OutputStream os,
                                            OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getHtmlInfoPageRequest(dataSource,xdap_accept),
                os,
                err);
    }



    /**
     * Returns the BES verion document for the BES serving the passed
     * dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it a catalog
     * document or an error) will be placed.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean getVersion(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        boolean ret =  besTransaction(dataSource, showVersionRequest(),response);

        return ret;
    }



    /**
     * Returns the BES INFO document for the spcified dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it a catalog
     * document or an error) will be placed.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean getCatalog(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        boolean ret;


        ret = besTransaction(dataSource,
                showCatalogRequest(dataSource),
                response);

        if(ret){
            // Get the root element.
            Element root = response.getRootElement();

            // Find the top level dataset Element
            Element topDataset = root.getChild("response").getChild("dataset");

            topDataset.setAttribute("prefix", getBESprefix(dataSource));
        }
        return ret;

    }


    /**
     * Returns the BES INFO document for the spcified dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it datasource
     * information or an error) will be placed.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public static boolean getInfo(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        boolean ret;
        Document request = showInfoRequest(dataSource);

        ret = besTransaction(dataSource,
                request,
                response);


        if(ret) {
            // Get the root element.
            Element root = response.getRootElement();

            // Find the top level dataset Element
            Element topDataset = root.getChild("response").getChild("dataset");

            // Add the prefix attribute for this BES.
            topDataset.setAttribute("prefix", getBESprefix(dataSource));
        }
        return ret;

    }




    /**
     * Returns an InputStream that holds an OPeNDAP DAP2 data for the requested
     * dataSource. The DDS header is stripped, so the InputStream holds ONLY
     * the XDR encoded binary data.
     *
     * Written to support SOAP responses. This implementation is deeply flawed
     * because it caches the response data in a memory object.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression .
     * @param xdap_accept The version of the DAP to use in building the response.
     * @return A DAP2 data stream, no DDS just the XDR encoded binary data.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public static InputStream getDap2DataStream(String dataSource,
                                                String constraintExpression,
                                                String xdap_accept)
            throws BadConfigurationException, BESError, PPTException, IOException {

        //@todo Make this more efficient by adding support to the BES that reurns this stream. Caching the resposnse in memory is a BAD BAD thing.

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        writeDap2Data(dataSource, constraintExpression, xdap_accept, baos, baos);

        InputStream is = new ByteArrayInputStream(baos.toByteArray());

        HeaderInputStream his = new HeaderInputStream(is);

        boolean done = false;
        int val;
        while (!done) {
            val = his.read();
            if (val == -1)
                done = true;

        }

        return is;

    }


    /**
     *
     * @param dataSource
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws JDOMException
     */
    public static boolean besTransaction( String dataSource,
                                           Document request,
                                           Document response
                                            )
            throws IOException, PPTException, BadConfigurationException, JDOMException {

        log.debug("besTransaction started.");

        boolean trouble = false;
        Document doc;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        SAXBuilder sb = new SAXBuilder();

        BES bes = BESManager.getBES(dataSource);

        if (bes == null) {
            String msg = "There is no BES to handle the requested data source: " + dataSource;
            log.error(msg);
            throw new BadConfigurationException(msg);
        }

        OPeNDAPClient oc = bes.getClient();

        try {

            if(oc.sendRequest(request,baos,erros)){

                log.debug("BES returned this document:\n" +
                        "-----------\n" + baos + "-----------");

                doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

                // Get the root element.
                Element root = doc.getRootElement();

                // Detach it from the document
                root.detach();

                // Pitch the root element that came with the passed catalog.
                // (There may not be one but whatever...)
                response.detachRootElement();

                // Set the root element to be the one sent from the BES.
                response.setRootElement(root);

                return true;

            }
            else {

                doc = sb.build(new ByteArrayInputStream(erros.toByteArray()));

                Iterator i  = doc.getDescendants(new ElementFilter(BES_ERROR));

                Element err;
                if(i.hasNext()){
                    err = (Element)i.next();
                }
                else {
                    err = doc.getRootElement();
                }

                err.detach();
                response.detachRootElement();
                response.setRootElement(err);
                return false;

            }


        }
        catch (PPTException e) {
            trouble = true;
            String msg = "besTransaction() Problem with OPeNDAPClient. " +
                    "OPeNDAPClient executed " + oc.getCommandCount() + " commands";

            log.error(msg);
            throw new PPTException(msg);
        }
        finally {
            bes.returnClient(oc, trouble);
            log.debug("besTransaction complete.");
        }


    }

    /**
     *
     * @param dataSource
     * @param request
     * @param os
     * @param err
     * @return
     * @throws BadConfigurationException
     * @throws IOException
     * @throws PPTException
     */
    public static boolean besTransaction(String dataSource,
                                             Document request,
                                             OutputStream os,
                                             OutputStream err)
            throws BadConfigurationException, IOException, PPTException {



        log.debug("besTransaction() started.");
        log.debug("besTransaction() request document: \n"+showRequest(request));


        boolean besTrouble = false;
        BES bes = BESManager.getBES(dataSource);
        if (bes == null)
            throw new BadConfigurationException("There is no BES to handle the requested data source: " + dataSource);

        OPeNDAPClient oc = bes.getClient();


        try {
            return oc.sendRequest(request,os,err);

        }
        catch (PPTException e) {
            besTrouble = true;

            String msg = "besGetTransaction()  Problem encountered with OPeNDAPCLient. " +
                    "OPeNDAPClient executed " + oc.getCommandCount() + " commands";
            log.error(msg);

            throw new PPTException(msg);
        }
        finally {
            bes.returnClient(oc, besTrouble);
            log.debug("besGetTransaction complete.");

        }

    }




/*##########################################################################*/





    public static Element setContainerElement(String name,
                                               String space,
                                               String source,
                                               String type) {

        Element e = new Element("setContainer",BES_NS);
        e.setAttribute("name",name);
        e.setAttribute("space",space);
        if(type.equals(STREAM))
            e.setAttribute("type",type);
        e.setText(source);
        return e;
    }

    public static Element defineElement(String name,
                                         String space) {

        Element e = new Element("define",BES_NS);
        e.setAttribute("name",name);
        e.setAttribute("space",space);
        return e;
    }


    public static Element containerElement(String name) {
        Element e = new Element("container",BES_NS);
        e.setAttribute("name",name);
        return e;
    }


    public static Element constraintElement(String ce) {
        Element e = new Element("constraint",BES_NS);
        e.setText(ce);
        return e;
    }

    public static Element getElement(String type,
                                      String definition,
                                      String url,
                                      String returnAs ) {

        Element e = new Element("get",BES_NS);

        e.setAttribute("type",type);
        e.setAttribute("definition",definition);
        if(url!=null)
            e.setAttribute("url",url);
        if(returnAs!=null)
            e.setAttribute("returnAs",returnAs);
        return e;
    }


    public static Element setContextElement(String name, String value) {
        Element e = new Element("setContext",BES_NS);
        e.setAttribute("name",name);
        e.setText(value);
        return e;
    }



/*##########################################################################*/


    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDX is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the dap that should be used to build the
     * response.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public static Document getDDXRequest(String dataSource,
                                         String ce,
                                         String xdap_accept,
                                         String xmlBase)
            throws BadConfigurationException {

        return getRequestDocument(DDX,dataSource,ce,xdap_accept,xmlBase,null,null,DAP2_ERRORS);

    }

    /**
     *  Returns the DDS request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public static Document getDDSRequest(String dataSource,
                                         String ce,
                                         String xdap_accept)
            throws BadConfigurationException {

        return getRequestDocument(DDS,dataSource,ce,xdap_accept,null,null,null,DAP2_ERRORS);

    }


    public static Document getDASRequest(String dataSource,
                                         String ce,
                                         String xdap_accept)
            throws BadConfigurationException {

        return getRequestDocument(DAS,dataSource,ce,xdap_accept,null,null,null,DAP2_ERRORS);

    }

    public static Document getDap2DataRequest(String dataSource,
                                              String ce,
                                              String xdap_accept)
            throws BadConfigurationException {

        return getRequestDocument(DAP2,dataSource,ce,xdap_accept,null,null,null,DAP2_ERRORS);

    }

    public static Document getAsciiDataRequest(String dataSource,
                                               String ce,
                                               String xdap_accept)
            throws BadConfigurationException {

        return getRequestDocument(ASCII,dataSource,ce,xdap_accept,null,null,null,XML_ERRORS);

    }


    public static Document getHtmlFormRequest(String dataSource,
                                              String xdap_accept,
                                              String URL)
            throws BadConfigurationException {

        return getRequestDocument(HTML_FORM,dataSource,null,xdap_accept,null,URL,null,XML_ERRORS);

    }

    public static Document getStreamRequest(String dataSource)
            throws BadConfigurationException{

        return getRequestDocument(STREAM,dataSource,null,null,null,null,null,XML_ERRORS);

    }


    public static Document getHtmlInfoPageRequest(String dataSource, String xdap_accept)
            throws BadConfigurationException {

        return getRequestDocument(INFO_PAGE,dataSource,null,xdap_accept,null,null,null,XML_ERRORS);

    }

    public static Document getNetcdfFileOutRequest(String dataSource, String ce, String xdap_accept)
            throws BadConfigurationException {


        return getRequestDocument(DAP2,dataSource,ce,xdap_accept,null,null,"netcdf",DAP2_ERRORS);


    }


    public static  Document getRequestDocument(String type,
                                                String dataSource,
                                                String ce,
                                                String xdap_accept,
                                                String xmlBase,
                                                String formURL,
                                                String returnAs,
                                                String errorContext)
                throws BadConfigurationException {

        String besDataSource = getBES(dataSource).trimPrefix(dataSource);

        Element e, request = new Element("request", BES_NS);
        request.setAttribute("reqID","###");


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));


        request.addContent(setContainerElement("catalogContainer","catalog",besDataSource,type));

        Element def = defineElement("d1","default");
        e = (containerElement("catalogContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        return new Document(request);

    }


    public static  Document getWcsRequestDocument(String type,
                                                String wcsRequestURL,
                                                String ce,
                                                String xdap_accept,
                                                String URL,
                                                String returnAs,
                                                String errorContext)
                throws BadConfigurationException {


        Element e, request = new Element("request", BES_NS);
        request.setAttribute("reqID","###");


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        request.addContent(setContainerElement("wcsContainer","wcsg",wcsRequestURL,type));

        Element def = defineElement("d1","default");
        e = (containerElement("wcsContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",URL,returnAs);

        request.addContent(e);

        return new Document(request);

    }








    public static Document showVersionRequest()
        throws BadConfigurationException {
        return showRequestDocument("showVersion",null);

    }


    public static Document showCatalogRequest(String dataSource)
            throws BadConfigurationException {
        return showRequestDocument("showCatalog",dataSource);

    }


    public static Document showInfoRequest(String dataSource)
            throws BadConfigurationException {
        return showRequestDocument("showInfo",dataSource);
    }



    public static Document showRequestDocument(String type, String dataSource)
            throws BadConfigurationException {


        Element e, request = new Element("request", BES_NS);
        request.setAttribute("reqID","###");
        request.addContent(setContextElement(ERRORS_CONTEXT,XML_ERRORS));

        e = new Element(type,BES_NS);

        if(dataSource!=null){
            String besDataSource = getBES(dataSource).trimPrefix(dataSource);
            e.setAttribute("node",besDataSource);
        }

        request.addContent(e);

        return new Document(request);

    }






    public static BES getBES(String dataSource) throws BadConfigurationException {
        BES bes = BESManager.getBES(dataSource);

        if (bes == null)
            throw new BadConfigurationException("There is no BES associated with the data source: " + dataSource);
        return bes;
    }

    public static String getBESprefix(String dataSource) throws BadConfigurationException {
        BES bes = BESManager.getBES(dataSource);

        if (bes == null)
            throw new BadConfigurationException("There is no BES associated with the data source: " + dataSource);
        return bes.getPrefix();
    }


    static void showRequest(Document request, OutputStream os) throws IOException{
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(request, os);


    }

    static String showRequest(Document request) throws IOException{
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(request);

    }


}






