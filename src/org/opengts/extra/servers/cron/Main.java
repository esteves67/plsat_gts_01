// ----------------------------------------------------------------------------
// Copyright 2007-2014, OpenGTS
// All rights reserved
// ----------------------------------------------------------------------------
//
// This source module is PROPRIETARY and CONFIDENTIAL.
// NOT INTENDED FOR PUBLIC RELEASE.
// 
// Use of this software is subject to the terms and conditions outlined in
// the 'Commercial' license provided with this software.  If you did not obtain
// a copy of the license with this software please request a copy from the
// Software Provider.
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Main Entry point for the Cron scheduler server
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.servers.cron;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.extra.util.*;

public class Main
{

    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,false);  // main

        /* invoke Cron handler */
        org.opengts.extra.util.Cron.main(null);

    }
    
}
