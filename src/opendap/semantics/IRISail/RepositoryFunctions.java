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

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 7, 2010
 * Time: 10:43:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryFunctions {




    /***************************************************************************
     * function join to concatenate strings
     *
     * @param RDFList
     * @param createValue
     * @return
     */
    public static Value join(List<String> RDFList, ValueFactory createValue) {
        int i = 0;
        boolean joinStrIsURL = false;
        String targetObj = "";
        if (RDFList.get(1).startsWith("http://")) {
            joinStrIsURL = true;
        }
        for (i = 1; i < RDFList.size() - 1; i++) {
            targetObj += RDFList.get(i) + RDFList.get(0); // rdfList.get(0) +
            // separator
            // log.debug("Component("+i+")= " + RDFList.get(i));
        }

        targetObj += RDFList.get(i); // last component no separator

        Value stObjStr;
        if (joinStrIsURL) {
            stObjStr = createValue.createURI(targetObj);
        } else {
            stObjStr = createValue.createLiteral(targetObj);
        }

        return stObjStr;
    }

    public static Value localName(List<String> RDFList, ValueFactory createValue) {
       
        
        String targetObj = "";
       
      
            targetObj = RDFList.get(0); // rdfList.get(0) +
            targetObj = targetObj.substring(targetObj.indexOf("#")+1);
           

        
        

        return createValue.createLiteral(targetObj);
    }


    
}
