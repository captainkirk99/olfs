/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
package opendap.gateway.dapResponders;

import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.User;
import opendap.gateway.BesGatewayApi;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetcdfFileOut extends BesDapResponder {
    private Logger log;


    private BesGatewayApi besGatewayApi;

    private static String defaultRequestSuffix = ".nc";


    public NetcdfFileOut(String sysPath, BesGatewayApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public NetcdfFileOut(String sysPath, String pathPrefix, BesGatewayApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public NetcdfFileOut(String sysPath, String pathPrefix,  String requestSuffix, BesGatewayApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        besGatewayApi = besApi;
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }



    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String fullSourceName = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String dataSourceUrl = besGatewayApi.getDataSourceUrl(request, getPathPrefix());





        User user = new User(request);
        int maxRS = user.getMaxResponseSize();



        log.debug("respondToHttpGetRequest(): Sending netCDF File Out response for dataset: " + dataSource + "?" +
                    constraintExpression);

        String downloadFileName = Scrub.fileName(fullSourceName.substring(fullSourceName.lastIndexOf("/")+1,fullSourceName.length()));
        Pattern startsWithNumber = Pattern.compile("[0-9].*");
        if(startsWithNumber.matcher(downloadFileName).matches())
            downloadFileName = "nc_"+downloadFileName;

        log.debug("sendNetcdfFileOut() downloadFileName: " + downloadFileName );

        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";

        response.setContentType("application/x-netcdf");
        response.setHeader("Content-Disposition", contentDisposition);

        Version.setOpendapMimeHeaders(request, response);

        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        Document reqDoc = besGatewayApi.getRequestDocument(
                                                        BesApi.DAP2,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        maxRS,
                                                        null,
                                                        null,
                                                        "netcdf",
                                                        BesApi.DAP2_ERRORS);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        log.debug("besGatewayApi.getRequestDocument() returned:\n "+xmlo.outputString(reqDoc));

        if(!besGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDDX() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }



        os.flush();
        log.info("Sent DAP2 data as netCDF file.");









    }


}
