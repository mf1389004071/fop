/*
   Copyright 2004 The Apache Software Foundation.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 * Created on 26/01/2004
 * $Id$
 */
package org.apache.fop.area;

import org.apache.fop.datastructs.Node;
import org.apache.fop.datastructs.SyncedNode;

/**
 * The base class for all areas.  <code>Area</code> extends <code>Node</code>
 * because all areas will find themselves in a tree of some kind.
 * @author pbw
 * @version $Revision$ $Name$
 */
public class Area extends SyncedNode {

    protected Integer iPDim = null;
    protected Integer iPDimMax = null;
    protected Integer iPDimMin = null;
    protected Integer bPDim = null;
    protected Integer bPDimMax = null;
    protected Integer bPDimMin = null;
    
    /**
     * @param parent
     * @param index
     * @throws IndexOutOfBoundsException
     */
    public Area(Node parent, int index, Object areaSync)
        throws IndexOutOfBoundsException {
        super(parent, index, areaSync);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param parent
     * @throws IndexOutOfBoundsException
     */
    public Area(Node parent, Object areaSync)
        throws IndexOutOfBoundsException {
        super(parent, areaSync);
        // TODO Auto-generated constructor stub
    }

}
