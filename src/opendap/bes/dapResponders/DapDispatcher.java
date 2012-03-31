/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) $year OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes.dapResponders;

import opendap.bes.BESDataSource;
import opendap.bes.BesDapResponder;
import opendap.coreServlet.*;
import opendap.dap.DapResponder;
import opendap.namespaces.XLINK;
import opendap.namespaces.XML;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/11/11
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class DapDispatcher implements DispatchHandler {




    private Logger log;
    private boolean initialized;
    private HttpServlet dispatchServlet;

    private String systemPath;
    private Element _config;
    private Vector<BesDapResponder> responders;
    private static boolean _allowDirectDataSourceAccess = false;
    private static boolean _useDAP2ResourceUrlResponse = false;

    private BesApi _besApi;


    public DapDispatcher(){
        log = LoggerFactory.getLogger(getClass());
        responders = new Vector<BesDapResponder>();

    }



    public static boolean allowDirectDataSourceAccess(){
        return _allowDirectDataSourceAccess;
    }

    public static boolean useDAP2ResourceUrlResponse(){
        return _useDAP2ResourceUrlResponse;
    }



    public void init(HttpServlet servlet,Element config) throws Exception {

        BesApi besApi = new BesApi();

        init(servlet, config, besApi);


    }


    protected void init(HttpServlet servlet, Element config, BesApi besApi) throws Exception {

        if(initialized) return;

        _besApi = besApi;

        _config = config;
        Element besApiImpl = _config.getChild("BesApiImpl");
        if(besApiImpl!=null){
            String className = besApiImpl.getTextTrim();
            log.debug("Building BesApi: " + className);
            Class classDefinition = Class.forName(className);

            Object classInstance = classDefinition.newInstance();

            if(classInstance instanceof BesApi){
                log.debug("Loading BesApi from configuration.");
                besApi = (BesApi) classDefinition.newInstance();
            }

        }

        log.debug("Using BesApi implementation: {}",besApi.getClass().getName());


        _allowDirectDataSourceAccess = false;
        Element dv = config.getChild("AllowDirectDataSourceAccess");
        if(dv!=null){
            _allowDirectDataSourceAccess = true;
        }


        _useDAP2ResourceUrlResponse = false;
        dv = config.getChild("UseDAP2ResourceUrlResponse");
        if(dv!=null){
            _useDAP2ResourceUrlResponse = true;
        }



        dispatchServlet = servlet;

        dispatchServlet = servlet;

        systemPath = ServletUtil.getSystemPath(dispatchServlet,"");


        BesDapResponder hr;

        responders.add(new Dataset(systemPath, besApi));
        responders.add(new DataDDX(systemPath, besApi));


        hr = new DDX(systemPath,besApi);

        responders.add(hr);


        responders.add(new DDS(systemPath, besApi));
        responders.add(new DAS(systemPath, besApi));
        responders.add(new RDF(systemPath, besApi));

        responders.add(new HtmlDataRequestForm(systemPath, besApi));
        responders.add(new DatasetInfoHtmlPage(systemPath, besApi));

        responders.add(new Dap2Data(systemPath, besApi));
        responders.add(new Ascii(systemPath, besApi));


        responders.add(new NetcdfFileOut(systemPath, besApi));
        responders.add(new XmlData(systemPath, besApi));
        responders.add(new VersionResponse(systemPath, besApi));
        responders.add(new IsoMetadata(systemPath, besApi));
        responders.add(new IsoRubric(systemPath, besApi));


        if(_useDAP2ResourceUrlResponse){
            DatasetFileAccess  dfa = new DatasetFileAccess(systemPath, besApi);
            dfa.setRequestMatchRegex(".*");// The match anything regex.
            dfa.setAllowDirectDataSourceAccess(_allowDirectDataSourceAccess);
            dfa.setDap2Response(true);
            responders.add(dfa);
        }
        else {

            DatasetFileAccess  dfa = new DatasetFileAccess(systemPath, besApi);
            dfa.setAllowDirectDataSourceAccess(_allowDirectDataSourceAccess);
            responders.add(dfa);

            DatasetServices sd = new DatasetServices(systemPath, besApi);
            responders.add(sd);

            sd.setDapResponders(responders);

        }




        log.info("Initialized. Direct Data Source Access: " + (_allowDirectDataSourceAccess ?"Enabled":"Disabled")+
                 "Resource URL returns: " + (_useDAP2ResourceUrlResponse ?"DAP2 File Response":"DAP4 Service Description"));

        initialized = true;



    }


    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

        if(requestDispatch(request,null,false)) {
            log.debug("Request can be handled.");
            return true;
        }
        log.debug("Request can not be handled.");
        return false;
    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        if(!requestDispatch(request,response,true)){
            log.error("Unable to service request.");
        }


    }


    public boolean requestDispatch(HttpServletRequest request,
                              HttpServletResponse response,
                              boolean sendResponse)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        String besDataSourceId = ReqInfo.getBesDataSourceID(relativeUrl);

        log.debug("The client requested this BES DataSource: " + besDataSourceId);

        for (HttpResponder r : responders) {
            log.debug(r.getClass().getSimpleName()+ ".getPathPrefix(): "+r.getPathPrefix());
            if (r.matches(relativeUrl)) {
                log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");
                    if(sendResponse)
                        r.respondToHttpGetRequest(request, response);
                    return true;
            }
        }


        return false;

    }

    public boolean requestDispatch_OLD(HttpServletRequest request,
                              HttpServletResponse response,
                              boolean sendResponse)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        String besDataSourceId = ReqInfo.getBesDataSourceID(relativeUrl);

        DataSourceInfo dsi;

        log.debug("The client requested this BES DataSource: " + besDataSourceId);



        for (HttpResponder r : responders) {
            log.debug(r.getPathPrefix());
            if (r.matches(relativeUrl)) {
                log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");
                dsi = getDataSourceInfo(besDataSourceId);
                if(dsi.isDataset()){
                    if(sendResponse)
                        r.respondToHttpGetRequest(request, response);
                    return true;
                }
            }
        }


        return false;

    }



    public long getLastModified(HttpServletRequest req) {


        String relativeUrl = ReqInfo.getLocalUrl(req);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);


        if(!initialized)
            return -1;

        log.debug("getLastModified(): Tomcat requesting getlastModified() " +
                "for collection: " + dataSource );


        String requestURL = req.getRequestURL().toString();


        for (HttpResponder r : responders) {
            if (r.matches(requestURL)) {
                log.info("The request URL: " + requestURL + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                try {
                    log.debug("getLastModified(): Getting datasource info for "+dataSource);
                    DataSourceInfo dsi = getDataSourceInfo(dataSource);
                    log.debug("getLastModified(): Returning: " + new Date(dsi.lastModified()));

                    return dsi.lastModified();

                } catch (Exception e) {
                    log.debug("getLastModified(): Returning: -1");
                    return -1;
                }

            }

        }

        return -1;


    }

    public DataSourceInfo getDataSourceInfo(String dataSourceName) throws Exception {
        return new BESDataSource(dataSourceName,_besApi);
    }




    public void destroy() {
        log.info("Destroy complete.");

    }
















}
