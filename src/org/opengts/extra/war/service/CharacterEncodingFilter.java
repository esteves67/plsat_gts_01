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
// ----------------------------------------------------------------------------
package org.opengts.extra.war.service;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;

public class CharacterEncodingFilter 
    implements Filter 
{

    private FilterConfig filterConfig = null;
    private String       preferredEncoding = null;
    private boolean      overrideRequest = false;

    /**
    *** Initializes filter 
    *** @param filterConfig  The FilterConfig
    **/
    public void init(FilterConfig filterConfig) 
    {
        this.filterConfig = filterConfig;
        this.preferredEncoding = filterConfig.getInitParameter("characterEncoding");
        if (StringTools.isBlank(this.preferredEncoding)) {
            this.preferredEncoding = StringTools.getCharacterEncoding(); // default to platform encoding
        }
        this.overrideRequest = StringTools.parseBoolean(filterConfig.getInitParameter("overrideRequestEncoding"),false);
        //Print.logInfo("Character Encoding: " + this.preferredEncoding);
        //Print.logInfo("Override Request  : " + this.overrideRequest);
    }

    /**
    *** Allows this filter to set attributes
    *** @param req  The ServletRequest
    *** @param res  The ServletResponse
    *** @param next The next filter in the chain
    **/
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain next)
        throws IOException, ServletException 
    {

        //CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML(), this.preferredEncoding);
        res.setCharacterEncoding(this.preferredEncoding);
        if (this.overrideRequest || (req.getCharacterEncoding() == null)) {
            //Print.logInfo("Setting Request Character Encoding: %s", this.preferredEncoding);
            req.setCharacterEncoding(this.preferredEncoding);
        }

        /* call other filters */
        next.doFilter(req, res);

    }

    /**
    *** Destroys filter 
    **/
    public void destroy() 
    {
        this.filterConfig = null;
        this.preferredEncoding = null;
    }

}
