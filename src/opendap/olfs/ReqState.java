/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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


package opendap.olfs;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.rmi.server.UID;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import opendap.ppt.PPTException;

/**
 * User requests get cached here so that downstream code can access
 * the details of the request information.
 *
 * @author Nathan Potter
 */

public class ReqState {





    /***************************************************************************
     * Default directory for the cached INFO files. This
     * presupposes that the server is going to use locally
     * cached INFO files.
     *
     * @serial
     */
    private  String defaultINFOcache;

    private Document serverVersionDoc = null;

    private final String defaultSchemaName = "opendap-0.0.0.xsd";
    private String defaultSchemaLocation;

    protected String rootPath, contentPath;

    private String dataSetName;
    private String requestSuffix;
    private String CE;
    private Object obj = null;
    private String serverClassName;
    private String requestURL;

    private ServletConfig myServletConfig;
    private HttpServletRequest myHttpRequest;


    public ReqState(HttpServletRequest myRequest,
                    ServletConfig sc,
                    String serverClassName, Document svd)
            throws BadURLException {

        this.myServletConfig = sc;
        this.myHttpRequest = myRequest;
        this.serverClassName = serverClassName;

        // Get the constraint expression from the request object and
        // convert all those special characters denoted by a % sign
        this.CE = prepCE(myHttpRequest.getQueryString());

        // If there was simply no constraint then prepCE() should have returned
        // a CE equal "", the empty string. A null return indicates an error.
        if (this.CE == null) {
            throw new BadURLException();
        }


        processDodsURL();


        defaultINFOcache = this.myServletConfig.getServletContext().getRealPath("datasets" +
                myHttpRequest.getServletPath() + "/info") + "/";


        int index = myHttpRequest.getRequestURL().lastIndexOf(
                myHttpRequest.getServletPath());

        defaultSchemaLocation = myHttpRequest.getRequestURL().substring(0, index) +
                "/schema/" +
                defaultSchemaName;

        //System.out.println("Default Schema Location: "+defaultSchemaLocation);
        //System.out.println("Schema Location: "+getSchemaLocation());

        requestURL = myHttpRequest.getRequestURL().toString();
        serverVersionDoc = svd;



    }

    public String getRequestURL(){
        return requestURL;
    }

    /**
     *  This method will attempt to get the INFO cache directory
     *  name from the servlet's InitParameters. Failing this it
     *  will return the default INFO cache directory name.
     *
     * @return The name of the INFO cache directory.
     */
    public String getINFOCache() {
        String cacheDir = getInitParameter("INFOcache");
        if (cacheDir == null)
            cacheDir = defaultINFOcache;
        return (cacheDir);
    }

    /**
     * Sets the default INFO Cache directory name to
     * the string <i>cachedir</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>INFOcache</i> the default
     * value will be ingnored.
     * @param cachedir
     */
    public void setDefaultINFOCache(String cachedir){
        defaultINFOcache = cachedir;
    }



    /**
     *
     * @return A string containing the value of the XDODS-Server MIME header as ascertained
     * by querying the BES.
     */
    public String getXDODSServer() {

        if(getVersionDocument() != null) {
            Iterator i = getVersionDocument().getRootElement().getChild("BES").getChildren("lib").iterator();

            while(i.hasNext()){
                Element e = (Element) i.next();
                if(e.getChildTextTrim("name").equalsIgnoreCase("libdap")){
                    return("dods/"+e.getChildTextTrim("version"));
                }
            }
        }

        return ("Server-Version-Unknown");
    }

    /**
     *
     * @return A String containing the value of the XOPeNDAP-Server MIME header ascertained by querying
     * the BES and conforming to the DAP4 specification.
     */
    public String getXOPeNDAPServer() {
        if(getVersionDocument() != null) {

            String opsrv = "";

            Iterator i = getVersionDocument().getRootElement().getChildren().iterator();

            while(i.hasNext()){
                Element pkg = (Element) i.next();
                Iterator j = pkg.getChildren("lib").iterator();
                while(j.hasNext()){
                    Element lib = (Element) j.next();
                    opsrv += " "+lib.getChildTextTrim("name")+"/"+lib.getChildTextTrim("version");
                }
            }
            return(opsrv);
        }
        return ("Server-Version-Unknown");
    }

    /**
     *
     * @return A String containing the XDAP MIME header value that describes the DAP specifcation that
     * the server response conforms to.
     */
    public String getXDAP(HttpServletRequest request) {

        double hval = 0.0;
        String hver = "";

        String clientDapVer = null;

        if(request != null)
            clientDapVer = request.getHeader("XDAP");

        if(getVersionDocument() != null) {

            String responseDAP = null;

            Iterator i = getVersionDocument().getRootElement().getChild("DAP").getChildren("version").iterator();

            while(i.hasNext()){
                Element v = (Element) i.next();
                String ver  = v.getTextTrim();
                double vval = Double.parseDouble(ver);
                if(hval < vval){
                    hval = vval;
                    hver = ver;
                }

                if(clientDapVer != null && clientDapVer.equals(ver))
                    responseDAP = ver;
            }
            if(responseDAP == null)
                return(hver);
            return(responseDAP);
        }

        return ("DAP-Version-Unknown");

    }

    /**
      *
      * @return The OLFS version Document object created and cached shortly after entering the doGet()
      * method of this object. This method will return <coe>null</code> if it is called prior to the
      * call of the private method of the same name.
      */
     public  Document getVersionDocument(){
         return(serverVersionDoc);
     }



    public String getDataSet() {
        return dataSetName;
    }

    public String getServerClassName() {
        return serverClassName;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public String getConstraintExpression() {
        return CE;
    }


    /**
     * This method will attempt to get the Schema Location
     * name from the servlet's InitParameters. Failing this it
     * will return the default Schema Location.
     *
     * @return The Schema Location.
     */
    public String getSchemaLocation() {
        String cacheDir = getInitParameter("SchemaLocation");
        if (cacheDir == null)
            cacheDir = defaultSchemaLocation;
        return (cacheDir);
    }

    /**
     * Sets the default Schema Location to
     * the string <i>location</i>. Note that if the servlet configuration
     * conatins an Init Parameter <i>SchemaLocation</i> the default
     * value will be ingnored.
     *
     * @param location
     */
    public void setDefaultSchemaLocation(String location) {
        defaultSchemaLocation = location;
    }




    /**
     * *************************************************************************
     * This method is used to convert special characters into their
     * actual byte values.
     * <p/>
     * For example, in a URL the space character
     * is represented as "%20" this method will replace that with a
     * space charater. (a single value of 0x20)
     *
     * @param ce The constraint expresion string as collected from the request
     *           object with <code>getQueryString()</code>
     * @return A string containing the prepared constraint expression. If there
     *         is a problem with the constraint expression a <code>null</code> is returned.
     */
    private String prepCE(String ce) {

        int index;

        //System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        //System.out.println("Prepping: \""+ce+"\"");

        if (ce == null) {
            ce = "";
            //System.out.println("null Constraint expression.");
        } else if (!ce.equals("")) {

            //System.out.println("Searching for:  %");
            index = ce.indexOf("%");
            //System.out.println("index of %: "+index);

            if (index == -1)
                return (ce);

            if (index > (ce.length() - 3))
                return (null);

            while (index >= 0) {
                //System.out.println("Found % at character " + index);

                String specChar = ce.substring(index + 1, index + 3);
                //System.out.println("specChar: \"" + specChar + "\"");

                // Convert that bad boy!
                char val = (char) Byte.parseByte(specChar, 16);
                //System.out.println("                val: '" + val + "'");
                //System.out.println("String.valueOf(val): \"" + String.valueOf(val) + "\"");


                ce = ce.substring(0, index) + String.valueOf(val) + ce.substring(index + 3, ce.length());
                //System.out.println("ce: \"" + ce + "\"");

                index = ce.indexOf("%");
                if (index > (ce.length() - 3))
                    return (null);
            }
        }

//      char ca[] = ce.toCharArray();
//	for(int i=0; i<ca.length ;i++)
//	    System.out.print("'"+(byte)ca[i]+"' ");
//	System.out.println("");
//	System.out.println(ce);
//	System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");

//        System.out.println("Returning CE: \""+ce+"\"");
        return (ce);
    }
    /***************************************************************************/


    /**
     * *************************************************************************
     * Processes an incoming <code>HttpServletRequest</code>. Uses the content of
     * the <code>HttpServletRequest</code>to create a <code>ReqState</code>
     * object in that caches the values for:
     * <ul>
     * <li> <b>dataSet</b> The data set name.(Accessible using
     * <code> setDataSet() </code>
     * and <code>getDataSet()</code>)</li>
     * <li> <b>CE</b> The constraint expression.(Accessible using
     * <code> setCE() </code>
     * and <code>getCE()</code>)</li>
     * <li> <b>requestSuffix</b> The request suffix, used by OPeNDAP to indicate
     * the type of response desired by the client.
     * (Accessible using
     * <code> setRequestSuffix() </code>
     * and <code>getRequestSuffix()</code>)</li>
     * <li> <b>isClientCompressed</b> Does the requesting client
     * accept a compressed response?</li>
     * <li> <b>ServletConfig</b> The <code>ServletConfig</code> object
     * for this servlet.</li>
     * <li> <b>ServerName</b> The class name of this server.</li>
     * <li> <b>RequestURL</b> THe URL that that was used to call thye servlet.</li>
     * </ul>
     *
     * @see ReqState
     */

    protected void processDodsURL() {

        // Figure out the data set name.
        this.dataSetName = myHttpRequest.getPathInfo();
        this.requestSuffix = null;
        if (this.dataSetName != null) {
            // Break the path up and find the last (terminal)
            // end.
            StringTokenizer st = new StringTokenizer(this.dataSetName, "/");
            String endOPath = "";
            while (st.hasMoreTokens()) {
                endOPath = st.nextToken();
            }

            // Check the last element in the path for the
            // character "."
            int index = endOPath.lastIndexOf('.');

            //System.out.println("last index of . in \""+ds+"\": "+index);

            // If a dot is found take the stuff after it as the OPeNDAP suffix
            if (index >= 0) {
                // pluck the OPeNDAP suffix off of the end
                requestSuffix = endOPath.substring(index + 1);

                // Set the data set name to the entire path minus the
                // suffix which we know exists in the last element
                // of the path.
                this.dataSetName = this.dataSetName.substring(1, this.dataSetName.lastIndexOf('.'));
            } else { // strip the leading slash (/) from the dataset name and set the suffix to an empty string
                requestSuffix = "";
                this.dataSetName = this.dataSetName.substring(1, this.dataSetName.length());
            }
        }
    }

    /**
     * *************************************************************************
     * Evaluates the (private) request object to determine if the client that
     * sent the request accepts compressed return documents.
     *
     * @return True is the client accpets a compressed return document.
     *         False otherwise.
     */

    public boolean getAcceptsCompressed() {

        boolean isTiny;

        isTiny = false;
        String Encoding = this.myHttpRequest.getHeader("Accept-Encoding");

        if (Encoding != null)
            isTiny = Encoding.equalsIgnoreCase("deflate");
        else
            isTiny = false;

        return (isTiny);
    }

    /**
     * ***********************************************************************
     */


    public Enumeration getInitParameterNames() {
        return (myServletConfig.getInitParameterNames());
    }

    public String getInitParameter(String name) {
        return (myServletConfig.getInitParameter(name));
    }


    // for debugging, extra state, etc
    public Object getUserObject() {
        return obj;
    }

    public void setUserObject(Object userObj) {
        this.obj = userObj;
    }

    public String toString() {
        String ts;

        ts = "ReqState:\n";
        ts += "  serverClassName:    '" + serverClassName + "'\n";
        ts += "  dataSet:            '" + dataSetName + "'\n";
        ts += "  requestSuffix:      '" + requestSuffix + "'\n";
        ts += "  CE:                 '" + CE + "'\n";
        ts += "  compressOK:          " + getAcceptsCompressed() + "\n";

        ts += "  InitParameters:\n";
        Enumeration e = getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = getInitParameter(name);

            ts += "    " + name + ": '" + value + "'\n";
        }

        return (ts);
    }


}


