/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2010 OPeNDAP, Inc.
//
// Authors:
//     Haibo Liu  <haibo@iri.columbia.edu>
//     Nathan David Potter  <ndp@opendap.org>
//     M. Benno Blumenthal <benno@iri.columbia.edu>
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
package opendap.semantics.IRISail;

import opendap.coreServlet.Scrub;
import org.openrdf.model.*;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class is the major class that manipulates and maintains the repository up to date.
 * Using this class StartingPoint statements are introduced in the repository; documents
 * or data sets no longer to be served anymore is deleted from the repository.
 * </p>
 * The repository consists of StartingPoint RDF document and RDF documents needed by those
 * StartingPoints. When introducing a StartingPoint into the repository, two things need to
 * be added. One is the StartingPoint statement and the other is the content of the
 * StartingPoint RDF document. e.g. "x rdf:type rdfcache:StartingPoint" is a StartingPoint
 * statement. "x" is the URL of the RDF document of a StartingPoint.
 * The startingpoint statement is added using this class then the actual content is
 * added later using class <code>RdfImporter</code>.
 *
 * rdfcache = <http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#>
 */
public class RepositoryOps {

    private static Logger log = LoggerFactory.getLogger(RepositoryOps.class);
    public static boolean flushRepositoryOnDrop = false;
    public static boolean dropWithMemoryStore   = false;
 
     
    public static void dropStartingPointsAndContexts(Repository repo, Vector<String> startingPointUrls, Vector<String> dropList) throws InterruptedException {
        RepositoryConnection con = null;
        ValueFactory valueFactory;



        try {
            con = repo.getConnection();
            con.setAutoCommit(false);
            
            long beforeDrop = new Date().getTime();
            
            log.debug("dropStartingPointsAndContexts(): AutoCommit = " + con.isAutoCommit());
            valueFactory = repo.getValueFactory();


            /*

            log.info("dropStartingPointsAndContexts(): Pausing for 10 seconds.");
            Thread.sleep(10000);
            log.info("dropStartingPointsAndContexts(): Pausing for 2 seconds.");
            Thread.sleep(4000);
            */

            dropStartingPoints(con, valueFactory, startingPointUrls);
            dropContexts(con, valueFactory, dropList);

            /*
            log.info("dropStartingPointsAndContexts(): Pausing for 10 seconds.");
            Thread.sleep(10000);
            log.info("dropStartingPointsAndContexts(): Pausing for 2 seconds.");
            Thread.sleep(4000);
            */



            log.info("dropStartingPointsAndContexts(): Calling commit.");
            con.commit();
            log.info("dropStartingPointsAndContexts(): Returned from commit().");
            long AfterDrop = new Date().getTime();
            double elapsedTime = (AfterDrop - beforeDrop) / 1000.0;

            /*
            log.info("dropStartingPointsAndContexts(): Pausing for 10 seconds.");
            Thread.sleep(10000);
            log.info("dropStartingPointsAndContexts(): Pausing for 2 seconds.");
            Thread.sleep(4000);
            */

            log.info("dropStartingPointsAndContexts(): Drop operations took " + elapsedTime +" seconds.");

            con.setAutoCommit(true);
            
        } catch (RepositoryException e) {
           log.error("dropStartingPointsAndContexts(): Caught RepositoryException in dropStartingPointsAndContexts. Msg: "
                   + e.getMessage());
        } 
        finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
    }


    /**
     * Remove the startingpoint statement from the repository.
     * @param con-An open connection to the repository from which the staring points will be dropped.
     * @param valueFactory-A ValueFactory object for making URI and Name valued objects.
     * @param startingPointUrls-A list of starting point URLs to drop from the repository.
     */
    public static void dropStartingPoints(RepositoryConnection con, ValueFactory valueFactory, Vector<String> startingPointUrls)throws InterruptedException  {



        URI startingPointValue;
        URI isa = valueFactory.createURI(Terms.rdfType.getUri());
        URI startingPointsContext = valueFactory.createURI(Terms.startingPointsContext.getUri());
        URI startingPointType = valueFactory.createURI(Terms.StartingPoint.getUri());



        try {
            for (String startingPoint : startingPointUrls) {

                startingPointValue = valueFactory.createURI(startingPoint);
                con.remove(startingPointValue, isa, startingPointType, startingPointsContext);

                log.info("dropStartingPoints(): Removed starting point " + startingPoint + " from the repository. (N-Triple: <" + startingPointValue + "> <" + isa
                        + "> " + "<" + startingPointType + "> " + "<" + startingPointsContext + "> )");
            }


        } catch (RepositoryException e) {
            log.error("dropStartingPoints(): In dropStartingPoints, caught an RepositoryException! Msg: "
                    + e.getMessage());

        }

    }


    /**
     * Add startingpoint statements into the repository.
     * @param repo - the repository.
     * @param startingPointUrls - list of StartingPoint.
     */
    public static void addStartingPoints(Repository repo, Vector<String> startingPointUrls) throws InterruptedException {
        RepositoryConnection con = null;
        ValueFactory valueFactory;

        try {
            con = repo.getConnection();
            valueFactory = repo.getValueFactory();
            addStartingPoints(con, valueFactory, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error("addStartingPoints(): "+e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("addStartingPoints(): "+e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }


    }

    /**
     * Adds the passed list of starting points to the repository.
     *
     *
     *
     * @param con - connection to the repository.
     * @param valueFactory - ValueFactory from the repository.
     * @param startingPointUrls - list of StartingPoint.
     */
    public static void addStartingPoints(RepositoryConnection con, ValueFactory valueFactory, Vector<String> startingPointUrls) throws InterruptedException {

        log.debug("Adding StartingPoints...");

        for (String importURL : startingPointUrls) {
            addStartingPoint(con, valueFactory, importURL);
        }
    }

    /**
     * Test if a StartingPoint is already in the repository. Return true if it is already in.
     * @param con - connection to the repository.
     * @param startingPointUrl - a StartingPoint.
     * @return true if the statement is in the repository.
     * @throws RepositoryException - if repository error.
     * @throws MalformedQueryException - if malformed query.
     * @throws QueryEvaluationException - if evaluate query error.
     */
    public static boolean startingPointExists( RepositoryConnection con, String startingPointUrl)  throws InterruptedException,  RepositoryException, MalformedQueryException, QueryEvaluationException{
        TupleQueryResult result;
        boolean hasInternalStaringPoint = false;

        String queryString = "SELECT DISTINCT doc "
            + "FROM {doc} rdf:type {rdfcache:"+Terms.StartingPoint.getLocalId() +"} "
            + "WHERE doc = <" + startingPointUrl + "> "
            + "USING NAMESPACE "
            + "rdfcache = <"+ Terms.rdfCacheNamespace+">";

        log.debug("queryStartingPoints: " + queryString);

        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,queryString);

        result = tupleQuery.evaluate();
        if (result.hasNext()){  //has internal starting point
        hasInternalStaringPoint = true;
        }
        return hasInternalStaringPoint;
    }


    /**
     * Add startingPoint statement into the repository.
     * @param repo - repository.
     * @param startingPointUrl - a StartingPoint.
     */
    public static void addStartingPoint(Repository repo, String startingPointUrl)throws InterruptedException {
        RepositoryConnection con = null;
        ValueFactory valueFactory;

        try {
            con = repo.getConnection();
            valueFactory = repo.getValueFactory();
            addStartingPoint(con, valueFactory, startingPointUrl);
        }
        catch (RepositoryException e) {
            log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }


    }

    /**
     * Adds the passed  starting point to the repository.
     *
     *
     * @param con - connection to the repository.
     * @param valueFactory - ValueFactory from the repository.
     * @param startingPoint - a StartingPoint.
     */
    public static void addStartingPoint(RepositoryConnection con, ValueFactory valueFactory, String startingPoint)throws InterruptedException {

        URI startingPointUri;
        URI isa = valueFactory.createURI(Terms.rdfType.getUri());
        URI startingPointsContext = valueFactory.createURI(Terms.startingPointsContext.getUri());
        URI startingPointContext = valueFactory.createURI(Terms.StartingPoint.getUri());


        try {

            if (startingPoint.startsWith("http://")) { //make sure it's a url and read it in
                startingPointUri = valueFactory.createURI(startingPoint);
                con.add(startingPointUri, isa, startingPointContext, startingPointsContext);


                log.info("addStartingPoint(): Added StartingPoint to the repository <" + startingPointUri + "> <" + isa
                        + "> " + "<" + startingPointContext + "> " + "<" + startingPointsContext + "> ");
            }
            else {
                log.error("addStartingPoint() - The startingPoint '"+startingPoint+"' does not appear to by a URL, skipping.");
            }


        } catch (RepositoryException e) {
            log.error("addStartingPoint(): Caught an RepositoryException! Msg: "
                    + e.getMessage());

        }


    }


    /**
     * Find StartingPoints that are no longer StartingPoint in the repository.
     * @param repo - repository.
     * @param startingPointUrls - list of StartingPoint.
     * @return A Vector of URL Strings of StartingPoint.
     */
    public static Vector<String> findChangedStartingPoints(Repository repo, Vector<String> startingPointUrls) throws InterruptedException{
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return findChangedStartingPoints(con, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error("findChangedStartingPoints(): "+e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("findChangedStartingPoints(): "+e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
        return new Vector<String>();

    }


    /**
     * Return a list of old StartingPoints that are no longer StartingPoints.
     *
     * @param con - connection to the repository.
     * @param startingPointsUrls - list of Strings of StartingPoints.
     * @return - A Vector of URL Strings of StartingPoint.
     */
    public static   Vector<String> findChangedStartingPoints(RepositoryConnection con, Vector<String> startingPointsUrls)throws InterruptedException {
        Vector<String> result;
        Vector<String> changedStartingPoints = new Vector<String> ();
        log.debug("Checking if the old StartingPoint is still a StartingPoint ...");

        try {

            result = findAllStartingPoints(con);

                for (String startpoint : result) {

                    //log.debug("StartingPoints: " + startpoint);
                    if (!startingPointsUrls.contains(startpoint)
                            && !startpoint.equals(Terms.internalStartingPoint)) {
                        changedStartingPoints.add(startpoint);
                        log.debug("Adding to droplist: " + startpoint);
                    }
                }

        } catch (QueryEvaluationException e) {
            log.error("findChangedStartingPoints(): Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (RepositoryException e) {
            log.error("findChangedStartingPoints(): Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("findChangedStartingPoints(): Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        if(changedStartingPoints.size() >0)
            log.info("findChangedStartingPoints(): Located " + changedStartingPoints.size()+" starting points that have been changed.");
        return changedStartingPoints;
    }


    /**
     * Return a list of startingPoint which is not in the repository yet.
     * @param repo - repository.
     * @param startingPointUrls - list of StartingPoints.
     * @return - Vector of new StartingPoint URLs.
     */
    public static Vector<String> findNewStartingPoints(Repository repo, Vector<String> startingPointUrls) throws InterruptedException {
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return findNewStartingPoints(con, startingPointUrls);
        }
        catch (RepositoryException e) {
            log.error("findNewStartingPoints(): "+e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("findNewStartingPoints(): "+e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
        return new Vector<String>();


    }




    /**
     * Find new StartingPoints in the input file but not in the repository yet. If the internal starting point is not
     * present in the repository, then it will be added to the returned list of new starting points.
     *
     * @param con  A connection to the repository to search.
     * @param startingPointUrls A list of candidate starting points
     * @return All of the starting points in the passed startingPointUrls that are not already present in the repository.
     * If the internalStartingPoint is not present in the repository it will be returned too.
     */
    public static  Vector<String> findNewStartingPoints(RepositoryConnection con, Vector<String> startingPointUrls)throws InterruptedException {
        Vector<String> result;
        Vector<String> newStartingPoints = new Vector<String> ();
        log.debug("findNewStartingPoints(): Checking for new starting points...");

        try {

            result = findAllStartingPoints(con);

            if(!result.contains(Terms.internalStartingPoint)){
                log.debug("findNewStartingPoints(): Internal StartingPoint not present in repository, adding to list.");
                newStartingPoints.add(Terms.internalStartingPoint);
            }

            for (String startingPoint : startingPointUrls) {

                if (!result.contains(startingPoint) && !startingPoint.equals(Terms.internalStartingPoint)) {

                    newStartingPoints.add(startingPoint);

                    log.debug("Found New StartingPoint: " + startingPoint);
                }
            }

        } catch (QueryEvaluationException e) {
            log.error("findNewStartingPoints(): Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("findNewStartingPoints(): Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("findNewStartingPoints(): Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        log.info("findNewStartingPoints(): Number of new StartingPoints: " + newStartingPoints.size());
        return newStartingPoints;
    }


    /**
     * Return all startingPoints in the repository. 
     * @param repo -repository.
     * @return Vector of Strings of All StartingPoints.
     * @throws MalformedQueryException - if malformed query.
     * @throws QueryEvaluationException - if evaluate query erroe.
     */
    public static Vector<String> findAllStartingPoints(Repository repo) throws InterruptedException, MalformedQueryException, QueryEvaluationException {
        RepositoryConnection con = null;

        try {
            con = repo.getConnection();
            return findAllStartingPoints(con);
        }
        catch (RepositoryException e) {
            log.error("findNewStartingPoints(): "+e.getClass().getName()+": Failed to open repository connection. Msg: "
                    + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("findNewStartingPoints(): "+e.getClass().getName()+": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
        return new Vector<String>();


    }



    /*
     * Find all starting points in the repository
     *
     */
    public static Vector<String> findAllStartingPoints(RepositoryConnection con) throws InterruptedException, RepositoryException, MalformedQueryException, QueryEvaluationException {

        Vector<String> startingPoints = new Vector <String> ();


        TupleQueryResult result = queryForStartingPoints(con);
        if (result != null) {

            if (!result.hasNext()) {
                log.debug("findAllStartingPoints(): NEW repository!");
            }

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                Value firstValue = bindingSet.getValue("doc");
                String startpoint = firstValue.stringValue();
                //log.debug("StartingPoints: " + startpoint);
                startingPoints.add(startpoint);

            }
        } else {
            log.debug("findAllStartingPoints(): No query result!");

        }
        return startingPoints;

    }


    /**
     * Return all startingPoints as  TupleQueryResult.
     * @param con - connection to the repository.
     * @return TupleQueryResult - Tuple query result.
     * @throws QueryEvaluationException - if evalute query error.
     * @throws MalformedQueryException - malformed query.
     * @throws RepositoryException - if repository error.
     */
    private static TupleQueryResult queryForStartingPoints(RepositoryConnection con) throws InterruptedException, QueryEvaluationException, MalformedQueryException, RepositoryException {
        TupleQueryResult result;

        log.debug("queryForStartingPoints(): Finding StartingPoints in the repository ...");

        String queryString = "SELECT DISTINCT doc "
            + "FROM {doc} rdf:type {rdfcache:"+Terms.StartingPoint.getLocalId() +"} "
            + "USING NAMESPACE "
            + "rdfcache = <"+ Terms.rdfCacheNamespace+">";

        log.debug("queryForStartingPoints(): query='" + queryString+"'");

        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,queryString);

        result = tupleQuery.evaluate();
        return result;

    }

    /**
     * Wipe out the whole repository.
     * @param owlse2 - repository.
     */
    public static void clearRepository(Repository owlse2) throws InterruptedException{

        RepositoryConnection con = null;

        try {
            con = owlse2.getConnection();
            con.clear();
        }
        catch (RepositoryException e) {
            log.error("clearRepository(): Failed to open repository connection. Msg: "+e.getMessage());
        } finally {
            
            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    log.error("clearRepository(): Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }


    }


    /**
     * Write the repository content to a plain ASCII file in N-triples, Trix or trig format depending the file name sufix.
     * @param con - connection to the repository.
     * @param filename - file to hold the repository dump.
     */
    public static void dumpRepository(RepositoryConnection con, String filename) throws InterruptedException {
        FileOutputStream myFileOutputStream = null;
        // export repository to an n-triple file
        try {
            log.info("dumpRepository(): Dumping repository to: '"+filename+"'");
            myFileOutputStream = new FileOutputStream(Scrub.fileName(filename));
            if (filename.endsWith(".nt")) {

                NTriplesWriter myNTRiplesWriter = new NTriplesWriter(
                        myFileOutputStream);

                con.export(myNTRiplesWriter);
                myNTRiplesWriter.startRDF();
                myNTRiplesWriter.endRDF();

            }
            if (filename.endsWith(".trix")) {

                TriXWriter myTriXWriter = new TriXWriter(myFileOutputStream);

                con.export(myTriXWriter);
                myTriXWriter.startRDF();
                myTriXWriter.endRDF();

            }
            if (filename.endsWith(".trig")) {

                TriGWriter myTriGWriter = new TriGWriter(myFileOutputStream);

                con.export(myTriGWriter);
                myTriGWriter.startRDF();
                myTriGWriter.endRDF();

            }
            log.info("dumpRepository(): Completed dumping explicit statements");

        }

        catch (RepositoryException e) {
            log.error("dumpRepository(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (FileNotFoundException e) {
            log.error("dumpRepository(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (RDFHandlerException e) {
            log.error("dumpRepository(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } finally {
            if(myFileOutputStream != null){
                try {
                    myFileOutputStream.close();
                } catch (IOException e) {
                    log.error("dumpRepository(): Failed to close file when dumping repository. msg: "+e.getMessage());
                }
            }
        }

    }

    /**
     * Write the repository content to a plain ASCII file in N-triples, Trix or trig format depending the file name sufix.
     * @param owlse2 - repository.
     * @param filename - file to hold the repository.
     */
    public static void dumpRepository(Repository owlse2, String filename) throws InterruptedException {

        RepositoryConnection con = null;

        try {
            con = owlse2.getConnection();
            dumpRepository(con, filename);
        }
        catch (RepositoryException e) {
            log.error("dumpRepository(): Failed to open repository connection. Msg: "+e.getMessage());
        } finally {
            
            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    log.error("dumpRepository(): Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }


    }

    /**
     * Return all contexts in the repository as a space separated string.
     * @param repository - the repository.
     * @return a String of all contexts.
     */
    public static String showContexts(Repository repository) throws InterruptedException {
        RepositoryConnection con = null;
        String msg;

        try {
            con = repository.getConnection();
            msg =  showContexts(con);
        }
        catch (RepositoryException e) {
            msg = "showContexts(): Failed to open repository connection. Msg: "+e.getMessage();
            log.error(msg);
        } finally {
            //log.debug("Closing repository connection.");

            if(con!=null){
                try {
                    con.close();  //close connection first
                }
                catch(RepositoryException e){
                    log.error("showContexts(): Failed to close repository connection. Msg: "+e.getMessage());
                }
            }
        }
        return msg;

    }

    /**
     * Return all contexts in the repository as a separated string.
     * @param con - connection to the repository.
     * @return a String of all contexts.
     */
    public static String showContexts(RepositoryConnection con) throws InterruptedException {

        String msg = "\nRepository ContextIDs:\n";
        try {
            RepositoryResult<Resource> contextIds = con.getContextIDs();

            for(Resource contextId : contextIds.asList()){
                msg += "    "+contextId+"\n";
            }

        } catch (RepositoryException e) {
            msg = "showContexts(): Failed to open repository connection. Msg: "+e.getMessage();
            log.error(msg);
        }

        return msg;
    }

    /**
     * Return true if import context is newer.
     *
     * @param con - connection to the repository.
     * @param importURL - String of import URL.
     * @return Boolean - true if the import URL is changed.
     */
    public static Boolean olderContext(RepositoryConnection con, String importURL) throws InterruptedException{
        Boolean oldLMT = false;

        String oldltmod = getLastModifiedTime(con, importURL); // LMT from repository

        if (oldltmod.isEmpty()) {
            log.debug("In  olderContext ...");
            log.debug("URI " + importURL);
            log.debug("lastmodified is empty!");
            oldLMT = true;
            return oldLMT;
        }
        String oltd = oldltmod.substring(0, 10) + " "
                + oldltmod.substring(11, 19);
        String ltmod = getLTMODContext(importURL);

        String ltd = ltmod.substring(0, 10) + " " + ltmod.substring(11, 19);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
        Date ltdparseDate;
        try {
            ltdparseDate = dateFormat.parse(ltd);
            log.debug("In  olderContext ...");
            log.debug("URI " + importURL);
            log.debug("lastmodified    " + ltdparseDate.toString());
            Date oldltdparseDate = dateFormat.parse(oltd);
            log.debug("oldlastmodified " + oldltdparseDate.toString());

            if (ltdparseDate.compareTo(oldltdparseDate) > 0) {// if newer
                // context

                log.info("Import context is newer! Will update.");
                oldLMT = true;
            }
        } catch (ParseException e) {
            log.error("Caught an ParseException! Msg: " + e.getMessage());

        }
        return oldLMT;
    }

    /**
     * Return true if import context is newer.
     *
     * @param oldltmod - last modified time from the repository.
     * @param importURL - String of import URL.
     * @return Boolean - true if the import URL is changed.
     */
    public static Boolean olderContext(String oldltmod, String importURL) throws InterruptedException {
        Boolean oldLMT = false;

        if (oldltmod.isEmpty()) {
            log.debug("olderContext():  URI: '" + importURL+"' lastModified is empty!");
            oldLMT = true;
            return oldLMT;
        }
        String oltd = oldltmod.substring(0, 10) + " "
                + oldltmod.substring(11, 19);
        String ltmod = getLTMODContext(importURL);

        String ltd = ltmod.substring(0, 10) + " " + ltmod.substring(11, 19);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
        Date ltdparseDate;
        try {
            ltdparseDate = dateFormat.parse(ltd);
            log.debug("olderContext(): URI: '" + importURL+"' lastModified: '"+ltdparseDate.toString()+"'");
            Date oldltdparseDate = dateFormat.parse(oltd);
            log.debug("olderContext(): oldLastModified " + oldltdparseDate.toString());

            if (ltdparseDate.compareTo(oldltdparseDate) > 0) {// if newer
                // context

                log.info("olderContext(): Import context is newer! Will update.");
                oldLMT = true;
            }
        } catch (ParseException e) {
            log.error("olderContext(): Caught an ParseException! Msg: " + e.getMessage());

        }
        return oldLMT;
    }


    /**
     * Check and return last modified time of a context (URI) via querying
     * against the repository on contexts.
     *
     *
     * @param con - connection to the repository.
     * @param urlstring - an URL String.
     * @return last modified time of the file.
     */
    public static String getLastModifiedTime(RepositoryConnection con, String urlstring) throws InterruptedException {
        TupleQueryResult result = null;
        String ltmodstr = "";
        URI uriaddress = new URIImpl(urlstring);


        String queryString = "SELECT DISTINCT doc,lastmod FROM CONTEXT "
                  + "rdfcache:"+Terms.cacheContext.getLocalId()+" {doc} rdfcache:"+Terms.lastModified.getLocalId() +" {lastmod} "
                  + "where doc=<" + uriaddress + ">"
                  + "USING NAMESPACE "
                  + "rdfcache = <"+ Terms.rdfCacheNamespace+">";
        try {
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);
            result = tupleQuery.evaluate();

            BindingSet bindingSet;
            Value valueOfY;

            while (result.hasNext()) { // should have only one value
                bindingSet =  result.next();
                //Set<String> names = bindingSet.getBindingNames();
                // for (String name : names) {
                // log.debug("BindingNames: " + name);
                // }
                valueOfY =  bindingSet.getValue("lastmod");
                ltmodstr = valueOfY.stringValue();
                //log.debug("last modified time:" + valueOfY.stringValue());

            }

        } catch (QueryEvaluationException e) {
            log.error("getLastModifiedTime(): Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (RepositoryException e) {
            log.error("getLastModifiedTime(): Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("getLastModifiedTime(): Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            if(result!=null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("getLastModifiedTime(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
        }

        return ltmodstr;
    }

    /**
     * Get last_modified_time of a context through the http connection.
     * @param urlstring - the URL of the context.
     * @return  the last modified time as a String.
     */
    public static String getLTMODContext(String urlstring) throws InterruptedException {
        String ltmodstr = "";
        try {
            URL myurl = new URL(urlstring);
            HttpURLConnection hc = (HttpURLConnection) myurl.openConnection();
            long ltmod = hc.getLastModified();
            //log.debug(urlstring + " lastModified: "+ltmod);
            ltmodstr = getLastModifiedTimeString(ltmod);
        } catch (MalformedURLException e) {
            log.error("getLTMODContext(): Caught a MalformedQueryException! Msg: "
                    + e.getLocalizedMessage());
        } catch (IOException e) {
            log.error("getLTMODContext(): Caught an IOException! Msg: " + e.getMessage(), e);
        }
        return ltmodstr;
    }

    /**
     * Convert Date to last_modified_time
     * @param date - a Date object represents the last modified time of the context.
     * @return a string of time.
     */
    public static String getLastModifiedTimeString(Date date) throws InterruptedException {
        return getLastModifiedTimeString(date.getTime());
    }

    /**
     * Convert time in long  to last_modified_time
     * @param epochTime - time in seconds.
     * @return a string of time.
     */
    public static String getLastModifiedTimeString(long epochTime) throws InterruptedException {
        String ltmodstr;
        Timestamp ltmodsql = new Timestamp(epochTime);
        String ltmodstrraw = ltmodsql.toString();
        ltmodstr = ltmodstrraw.substring(0, 10) + "T"
                + ltmodstrraw.substring(11, 19) + "Z";
        return ltmodstr;
    }

    /**
     * Returns a Hash containing last modified times of each context (URI) in the
     * repository, keyed by the context name.
     * @param repository The repository from which to harvest the contexts and their associated last
     * modified times.
     * @return a Hash containing last modified times of each context
     */
    public static HashMap<String, String> getLastModifiedTimesForContexts(Repository repository) throws InterruptedException {
        RepositoryConnection con = null;

        try{
            con = repository.getConnection();
            return getLastModifiedTimesForContexts(con);
        }
        catch (RepositoryException e) {
            log.error("getLastModifiedTimesForContexts(): Caught a RepositoryException! Msg: " + e.getMessage());
        }
        finally {
            if(con!=null){
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("getLastModifiedTimesForContexts(): Caught a RepositoryException! Msg: " + e.getMessage());
                }
            }
        }
        return new HashMap<String, String>();
    }



    /**
     * Returns a Hash containing last modified times of each context (URI) in the
     * repository, keyed by the context name.
     * @param con A connection to the repository from which to harvest the contexts and their associated last
     * modified times.
     * @return a HashMap of last modified times and context pair.
     */
    public static HashMap<String, String> getLastModifiedTimesForContexts(RepositoryConnection con) throws InterruptedException {
        TupleQueryResult result = null;
        String ltmodstr = "";
        String idstr = "";
        HashMap<String, String> idltm = new HashMap<String, String>();
        String queryString = "SELECT DISTINCT id, lmt "
                + "FROM "
                + "{cd} wcs:Identifier {id}; "
                + "rdfs:isDefinedBy {doc} rdfcache:"+Terms.lastModified.getLocalId() +" {lmt} "
                + "using namespace "
                + "rdfcache = <"+ Terms.rdfCacheNamespace+">, "
                + "wcs= <http://www.opengis.net/wcs/1.1#>";

        try {
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);
            result = tupleQuery.evaluate();

            BindingSet bindingSet;
            Value valueOfID = null;
            Value valueOfLMT;

            while (result.hasNext()) {
                bindingSet = (BindingSet) result.next();

                valueOfLMT = (Value) bindingSet.getValue("lmt");
                ltmodstr = valueOfLMT.stringValue();

                valueOfID = (Value) bindingSet.getValue("id");
                idstr = valueOfID.stringValue();

                idltm.put(idstr, ltmodstr);

                // log.debug("ID:" + valueOfID.stringValue());
                // log.debug("LMT:" + valueOfLMT.stringValue());

            }
        } catch (QueryEvaluationException e) {
            log.error("getLastModifiedTimesForContexts(): Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (RepositoryException e) {
            log.error("getLastModifiedTimesForContexts(): Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("getLastModifiedTimesForContexts(): Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("getLastModifiedTimesForContexts(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
        }

        return idltm;
    }


    /**
     * Insert a statement declaring the content type of the document.
     *
     *
     * @param importURL - import URL.
     * @param contentType - Content-Type of the file.
     * @param con - connection to the repository.
     * @param valueFactory - ValueFactory from the repository.
     */
    public static void setContentTypeContext(String importURL, String contentType, RepositoryConnection con, ValueFactory valueFactory) throws InterruptedException {

        URI s = valueFactory.createURI(importURL);
        URI contentTypeContext = valueFactory.createURI(Terms.contentType.getUri());
        URI cacheContext = valueFactory.createURI(Terms.cacheContext.getUri());

        Literal o = valueFactory.createLiteral(contentType);

        try {

            con.add((Resource) s, contentTypeContext, (Value) o, (Resource) cacheContext);

        } catch (RepositoryException e) {
            log.error("setContentTypeContext(): Caught an RepositoryException! Msg: "
                    + e.getMessage());

        }

    }

    /**
     * Set last_modified_time of the URI in the repository.
     *
     * @param importURL - import URL.
     * @param con - connection to the repository.
     * @param valueFactory - from the repository.
     */
    public static void setLTMODContext(String importURL, RepositoryConnection con,ValueFactory valueFactory) throws InterruptedException {
        String ltmod = getLTMODContext(importURL);
        setLTMODContext(importURL, ltmod, con, valueFactory);
    }

    /**
     * Set last_modified_time of the import file in the repository.
     *
     * @param importURL - URL of the import file.
     * @param ltmod - last modified time.
     * @param con - connection to the repository.
     * @param valueFactory - ValueFactory from the Repository.
     */
    public  static void setLTMODContext(String importURL, String ltmod, RepositoryConnection con, ValueFactory valueFactory) throws InterruptedException {

        // log.debug(importURL);
        // log.debug("lastmodified " + ltmod);
        URI s = valueFactory.createURI(importURL);
        URI p = valueFactory.createURI(Terms.lastModified.getUri());
        URI cont = valueFactory.createURI(Terms.cacheContext.getUri());
        URI sxd = valueFactory.createURI("http://www.w3.org/2001/XMLSchema#dateTime");
        Literal o = valueFactory.createLiteral(ltmod, sxd);

        try {

            con.add((Resource) s, p, (Value) o, (Resource) cont);

        } catch (RepositoryException e) {
            log.error("setLTMODContext(): Caught an RepositoryException! Msg: "
                    + e.getMessage());

        }

    }

    /**
     * Update the repository. Drop outdated files, add new files and run construct rules.
     * @param repository        The repository on which to operate.
     * @param startingPointUrls The list of starting point URLs from the configuration file (aka "THE starting point")
     * @param doNotImportTheseUrls  A list of URL's that should not be loaded into the repository, even if they are
     * encountered in the tree of dependencies.
     * @param resourceDir The local system directory in which to find crucial files (for example XSL transforms) used
     * by the semantic processing.
     * @return Returns true if the update results in changes to the repository.
     * @throws InterruptedException If the thread of execution is interrupted.
     * @throws RepositoryException  When there are problems working with the repository.
     * @throws IOException if MemoryStore SailRepository error.
     * @throws RDFParseException if parse the repository dump error.
     */
    public static boolean updateSemanticRepository(Repository repository, Vector<String> startingPointUrls, Vector<String> doNotImportTheseUrls, 
            String resourceDir, String catalogCacheDirectory)
            throws InterruptedException, RepositoryException, RDFParseException, IOException {


        Vector<String> dropList = new Vector<String>();
        Vector<String> newStartingPoints = new Vector<String>();
        Vector<String> startingPointsToDrop = null;
        boolean repositoryHasBeenChanged = false;

        RdfImporter rdfImporter = new RdfImporter(resourceDir);


        Date startTime = new Date();
        log.debug("-----------------------------------------------------------------------");
        log.debug("updateSemanticRepository(): Start.");
        log.debug(showContexts(repository));
        RepositoryConnection con = null;
        try {



            try {
                con = repository.getConnection();
                if (con.isOpen()) {
                    log.debug("updateSemanticRepository(): Connection is OPEN!");


                    newStartingPoints = findNewStartingPoints(con, startingPointUrls);

                    dropList.addAll(findUnneededRDFDocuments(con));
                    startingPointsToDrop = findChangedStartingPoints(con, startingPointUrls);
                    dropList.addAll(startingPointsToDrop);
                    dropList.addAll(findChangedRDFDocuments(con));
                   
                }
            } catch (RepositoryException e) {
                log.error("updateSemanticRepository(): Caught RepositoryException. Msg: " +
                        e.getMessage());
            } finally {
                if (con != null)
                    con.close();
                log.debug("updateSemanticRepository(): Connection is Closed!");
            }

            ProcessController.checkState();

            log.debug(showContexts(repository));


            boolean modelChanged = false;


            if (!dropList.isEmpty()) {
                log.debug("updateSemanticRepository(): Add external inferencing contexts to dropList");
                dropList.addAll(findExternalInferencingContexts(repository));
                
                if(flushRepositoryOnDrop){
                    log.warn("updateSemanticRepository(): Repository content has been changed! Flushing Repository!");

                    clearRepository(repository);


                    String filename =  "PostRepositoryClear.trig";
                    
                    dumpRepository(repository, filename);

                    //repository is empty, find all StartingPoint 
                    newStartingPoints = findNewStartingPoints(repository, startingPointUrls);


                }else if(dropWithMemoryStore){
                    log.warn("updateSemanticRepository(): Repository content has been changed! Do drop with MemoryStore!");
                    
                    Repository memRepository = setupMemoryStoreSailRepository();
                    
                    log.warn("updateSemanticRepository(): Flushing MemoryStoreRepository!");
                    clearRepository(memRepository); //make sure memory store is empty
                    RepositoryConnection conMem = memRepository.getConnection();
                    RepositoryConnection conOwlim = repository.getConnection();
                    
                    log.info("updateSemanticRepository(): Loading Owlim Repository to MemoryStore");
                    conMem.add(conOwlim.getStatements(null, null, null, false));
                    
                    log.info("updateSemanticRepository(): Dropping StartingPoint and contexts from MemoryStore ...");
                    
                    dropStartingPointsAndContexts(memRepository, startingPointsToDrop, dropList);
                    
                    log.warn("updateSemanticRepository(): Flushing OwlimRepository!");
                    clearRepository(repository);
                    
                    log.info("updateSemanticRepository(): Reloading MemoryStore back to Owlim Repository");
                    conOwlim.add(conMem.getStatements(null, null, null, true));
                    
                    conOwlim.close();
                    conMem.close();                    
                    memRepository.shutDown();
                }
                else {
                    dropStartingPointsAndContexts(repository, startingPointsToDrop, dropList); 

                }
                
                modelChanged = true;

            }//if (!dropList.isEmpty()) 

            ProcessController.checkState();


            if (!newStartingPoints.isEmpty()) {

                log.info("updateSemanticRepository(): Adding new starting points ...");
                addStartingPoints(repository, newStartingPoints);
                log.info("updateSemanticRepository(): Finished adding new starting points.");

                log.debug(showContexts(repository));
                modelChanged = true;

            }

            ProcessController.checkState();

            log.info("updateSemanticRepository(): Checking for referenced documents that are not already in the repository.");
            boolean foundNewDocuments = rdfImporter.importReferencedRdfDocs(repository, doNotImportTheseUrls);
            if(foundNewDocuments){
                modelChanged = true;
            }

            ProcessController.checkState();

            repositoryHasBeenChanged = modelChanged;

            log.info("updateSemanticRepository(): Updating repository ...");
            ConstructRuleEvaluator constructRuleEvaluator = new ConstructRuleEvaluator();

            log.info("updateSemanticRepository(): Running construct rules ...");
            boolean firstPass = true;
            while (modelChanged || firstPass) {

                firstPass = false;

                repositoryHasBeenChanged = repositoryHasBeenChanged || constructRuleEvaluator.runConstruct(repository);

                ProcessController.checkState();

                log.debug(showContexts(repository));

                modelChanged = rdfImporter.importReferencedRdfDocs(repository, doNotImportTheseUrls);
                
                repositoryHasBeenChanged = repositoryHasBeenChanged || modelChanged;

                ProcessController.checkState();

            }





        } catch (RepositoryException e) {
            log.error("updateSemanticRepository(): Caught RepositoryException. Message:"
                    + e.getMessage());

        }


        double elapsedTime = (new Date().getTime() - startTime.getTime())/1000.0;
        
        log.info("updateSemanticRepository() End. Elapsed time: " + elapsedTime + " seconds  repositoryHasBeenChanged: "+repositoryHasBeenChanged);
        log.debug("-----------------------------------------------------------------------");


        return repositoryHasBeenChanged;
    }


    /**
     * Remove contexts from the repository.
     * @param repository - the repository.
     * @param dropList - list of RDF files to delete from the repository.
     * @throws InterruptedException - if the process interrrupted.
     */
    public static void dropContexts(Repository repository, Vector<String> dropList) throws InterruptedException {
        RepositoryConnection con = null;

        log.debug("dropContexts(): Dropping changed RDFDocuments and external inferencing contexts...");

        try {
            con = repository.getConnection();
            con.setAutoCommit(false);
            dropContexts(con,con.getValueFactory(),dropList);
            con.commit();
        } catch (RepositoryException e) {
            log.error("dropContexts(): Caught RepositoryException! Msg: " + e.getMessage());
        }
        finally {
            try {
                if (con != null)
                    con.close();
            } catch (RepositoryException e) {
                log.error("dropContexts(): Caught RepositoryException! while closing connection: "
                        + e.getMessage());
            }
        }
        log.info("dropContexts(): Finished dropping changed RDFDocuments and external inferencing contexts.");

    }
    /**
     * Drop contexts through a connection.
     * @param con
     * @param valueFactory
     * @param dropList
     * @throws InterruptedException
     */
    public static void dropContexts(RepositoryConnection con, ValueFactory valueFactory, Vector<String> dropList) throws InterruptedException {

        log.debug("dropContexts(): Dropping changed RDFDocuments and external inferencing contexts...");

        try {

            log.info("dropContexts(): Deleting contexts in drop list ...");

            for (String drop : dropList) {
                log.debug("dropContexts(): Dropping context URI: " + drop);
                URI contextToDrop = valueFactory.createURI(drop);
                URI cacheContext = valueFactory.createURI(Terms.cacheContext.getUri());

                log.debug("dropContexts(): Removing context: " + contextToDrop);
                con.clear(contextToDrop);

                log.debug("dropContexts(): Removing last_modified: " + contextToDrop);
                con.remove(contextToDrop, null, null, cacheContext); // remove last_modified

                log.info("dropContexts(): Finished removing context: " + contextToDrop);

                ProcessController.checkState();
            }

        } catch (RepositoryException e) {
            log.error("dropContexts(): In dropContexts caught RepositoryException! Msg: " + e.getMessage());

        }
        log.debug("dropContexts(): Finished dropping changed RDFDocuments and external inferencing contexts.");

    }
    /**
     * Locate all of the of the contexts generated by externbal inferencing (construct rule) activities.
     *
     * @param repository The repository to operate on.
     * @return A lists of contexts that were generated by construct rules (i.e. external inferencing)
     */
    static Vector<String> findExternalInferencingContexts(Repository repository)throws InterruptedException  {
        RepositoryConnection con = null;
        TupleQueryResult result = null;

        //List<String> bindingNames;
        Vector<String> externalInferencing = new Vector<String>();

        log.debug("findExternalInferencingContexts(): Finding ExternalInferencing ...");

        try {
            con = repository.getConnection();

            String queryString = "select distinct crule from context crule {} prop {} "
                    + "WHERE crule != rdfcache:"+Terms.cacheContext.getLocalId()+" "
                    + "AND crule != rdfcache:"+Terms.startingPointsContext.getLocalId()+" "
                    + "AND NOT EXISTS (SELECT DISTINCT time FROM CONTEXT rdfcache:"+Terms.cacheContext.getLocalId()+" "
                    + "{crule} rdfcache:"+Terms.lastModified.getLocalId() +" {time}) "
                    + "using namespace "
                    + "rdfcache = <" + Terms.rdfCacheNamespace + ">";

            log.debug("queryString: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                //bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue("crule");

                    String contextUrl = firstValue.stringValue();
                    if (!externalInferencing.contains(contextUrl)) {
                        externalInferencing.add(contextUrl);
                        log.debug("Adding to external inferencing list: " + contextUrl);
                    }
                }
            } else {
                log.info("findExternalInferencingContexts(): No construct rule found!");
            }
        }
        catch (RepositoryException e) {
            log.error("findExternalInferencingContexts(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("findExternalInferencingContexts(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("findExternalInferencingContexts(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("findExternalInferencingContexts(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
            if(con!=null){
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("findExternalInferencingContexts(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
        }

        log.info("findExternalInferencingContexts(): Located "
                + externalInferencing.size() + " context generated by external inferencing (construct rules).");


        return externalInferencing;

    }


    /**
     * Return a list of files not needed any more in the repository.
     * @param con - connection to the repository.
     * @return list of RDF documents to delete from the repository.
     */
    static Vector<String> findUnneededRDFDocuments(RepositoryConnection con) throws InterruptedException {
        TupleQueryResult result = null;
        //List<String> bindingNames;
        Vector<String> unneededRdfDocs = new Vector<String>();


        log.debug("findUnneededRDFDocuments(): Locating unneeded RDF files left over from last update ...");

        try {

            String queryString = "(SELECT DISTINCT doc "
                    + "FROM CONTEXT rdfcache:"+Terms.cacheContext.getLocalId()+" "
                    + "{doc} rdfcache:"+Terms.lastModified.getLocalId() +" {lmt} "
                    //+ "WHERE doc != <" + Terms.externalInferencingUri+"> "
                    + "MINUS "
                    + "SELECT DISTINCT doc "
                    + "FROM {doc} rdf:type {rdfcache:"+Terms.StartingPoint.getLocalId() +"}) "
                    + "MINUS "
                    + "SELECT DISTINCT doc "
                    + "FROM {tp} rdf:type {rdfcache:"+Terms.StartingPoint.getLocalId() +"}; rdfcache:"+Terms.dependsOn.getLocalId() +" {doc} "
                    + "USING NAMESPACE "
                    + "rdfcache = <" + Terms.rdfCacheNamespace + ">";

            log.debug("queryUnneededRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                //bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue("doc");
                    if (!unneededRdfDocs.contains(firstValue.stringValue())) {
                        unneededRdfDocs.add(firstValue.stringValue());

                        log.debug("Found unneeded RDF Document: "
                                + firstValue.toString());
                    }
                }
            } else {
                log.debug("No query result!");
            }
        }

        catch (RepositoryException e) {
            log.error("findUnneededRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("findUnneededRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("findUnneededRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("findUnneededRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }

        }

        if(unneededRdfDocs.size()>0)
            log.info("findUnneededRDFDocuments(): Identified " + unneededRdfDocs.size() + " unneeded RDF documents.");
        return unneededRdfDocs;

    }

    
    /**
     * Return a list of files changed in the repository since last update.
     * @param con - connection to the repository.
     * @return list of RDF documents that changed.
     */
    static Vector<String> findChangedRDFDocuments(RepositoryConnection con) throws InterruptedException {
        TupleQueryResult result = null;
        //List<String> bindingNames;
        Vector<String> changedRdfDocuments = new Vector<String>();

        log.debug("findChangedRDFDocuments(): Locating changed files ...");

        try {
            String queryString = "SELECT DISTINCT doc,lastmod "
                    + "FROM CONTEXT rdfcache:"+Terms.cacheContext.getLocalId()+" "
                    + "{doc} rdfcache:"+Terms.lastModified.getLocalId() +" {lastmod} "
                    + "USING NAMESPACE "
                    + "rdfcache = <" + Terms.rdfCacheNamespace + ">";

            log.debug("findChangedRDFDocuments(): query string '" + queryString+"'");

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                //bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue("doc");
                    String importURL = firstValue.stringValue();
                    Value secondtValue = bindingSet.getValue("lastmod");
                    // log.debug("DOC: " + importURL);
                    // log.debug("LASTMOD: " + secondtValue.stringValue());

                    if (olderContext(secondtValue.stringValue(), importURL) && !changedRdfDocuments.contains(importURL)) {

                        changedRdfDocuments.add(importURL);

                        log.debug("findChangedRDFDocuments(): Add to changedRdfDocuments list: " + importURL);

                    }
                }
            } else {
                log.info("findChangedRDFDocuments(): No query result!");
            }
        }

        catch (RepositoryException e) {
            log.error("findChangedRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("findChangedRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("findChangedRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("findChangedRDFDocuments(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }

        }

        if(changedRdfDocuments.size()>0)
            log.info("findChangedRDFDocuments(): Number of changed RDF documents detected:  "
                + changedRdfDocuments.size());

        return changedRdfDocuments;
    }

    /**
     * Return URL of the transformation file.
     * @param importUrl-the file to transform
     * @param repository-the repository instance
     * @return xsltTransformationFileUrl-Url of the transformation stylesheet
     */
    public static String getUrlForTransformToRdf(Repository repository, String importUrl)throws InterruptedException {
        RepositoryConnection con = null;
        String xsltTransformationFileUrl = null;
        ValueFactory valueFactory;
        RepositoryResult<Statement> statements = null;

        try {
            con = repository.getConnection();
            valueFactory = repository.getValueFactory();


            // Get all of the statements in the repository that
            statements =
                    con.getStatements(
                            valueFactory.createURI(importUrl),
                            valueFactory.createURI(Terms.hasXslTransformToRdf.getUri()),
                            null,
                            true);

            while (statements.hasNext()){
                if(xsltTransformationFileUrl!=null){
                    log.error("getUrlForTransformToRdf(): Error!!! Found multiple XSL transforms associated with url: "+importUrl+" Lacking further instructions. DISCARDING: "+xsltTransformationFileUrl);
                }
                Statement s = statements.next();
                xsltTransformationFileUrl= s.getObject().stringValue();
                log.debug("getUrlForTransformToRdf(): Found Transformation file= " + xsltTransformationFileUrl);
            }
        }
        catch (RepositoryException e) {
            log.error("getUrlForTransformToRdf(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
        }
        finally {
            if (statements != null) {
                try {
                    statements.close();
                } catch (Exception e) {
                    log.error("getUrlForTransformToRdf(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("getUrlForTransformToRdf(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }

            }
        }

        return xsltTransformationFileUrl;
    }
    
    /**
     * Setup and initialize a MemoryStore SailRepository. 
     * @return repository - a reference to the repository
     * @throws RepositoryException - if cannot setup the repository.
     * @throws InterruptedException - if the process is interrupted.
     */
    private static Repository setupMemoryStoreSailRepository() throws RepositoryException, InterruptedException {



        
        log.info("setupMemoryStoreSailRepository(): Configuring Semantic Repository.");
        String catalogCacheDirectory = "./";
        File storageDir = new File(catalogCacheDirectory + "MemoryStore"); //define local copy of repository
        MemoryStore memStore = new MemoryStore(storageDir);
        memStore.setPersist(true);
        memStore.setSyncDelay(1000L);
        
        Repository repository = new SailRepository(memStore); 
        
        log.info("setupMemoryStoreSailRepository(): Intializing Semantic Repository.");

        // Initialize repository
        repository.initialize(); //needed

        log.info("setupMemoryStoreSailRepository(): Semantic Repository Ready.");


        ProcessController.checkState();


        return repository;

    }
    
    public static void loadRepositoryFromTrigFile(Repository repo, String rdfFileName) throws InterruptedException, RepositoryException, IOException, RDFParseException {


        RepositoryConnection con = null;

        try {
            con = repo.getConnection();

            if(rdfFileName.startsWith("http://")){
                URL rdfUrl = new URL(rdfFileName);
                con.add(rdfUrl, null, RDFFormat.TRIG);
            }
            else {
                File rdfFile = new File(rdfFileName);
                con.add(rdfFile, null, RDFFormat.TRIG);
            }
            con.commit();
        }
        finally {
            if (con != null) {
                try {
                    con.close();  //close connection first
                }
                catch (RepositoryException e) {
                    log.error("loadRepositoryFromTrigFile(): Failed to close repository connection. Msg: " + e.getMessage());
                }
            }
        }


    }

    

}
