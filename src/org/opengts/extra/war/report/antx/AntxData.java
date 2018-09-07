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
//  Report definition based on generic field definitions
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.war.report.antx;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.report.*;

public class AntxData
{

    // ------------------------------------------------------------------------

    public static final String KEY_ACCOUNT      = "$account";
    public static final String KEY_DEVICE       = "$device";

    // ------------------------------------------------------------------------

    private HashMap<String,Object> fieldValues = null;

    public AntxData()
    {
        this.fieldValues = new HashMap<String,Object>();
    }

    // ------------------------------------------------------------------------

    public void setAccount(Account account)
    {
        this.fieldValues.put(KEY_ACCOUNT, account);
    }
    
    public Account getAccount(Account dft)
    {
        Object val = this.fieldValues.get(KEY_ACCOUNT);
        return (val instanceof Account)? (Account)val : dft;
    }

    public Account getAccount()
    {
        return this.getAccount(null);
    }
    
    // ------------------------------------------------------------------------

    public void setDevice(Device device)
    {
        this.fieldValues.put(KEY_DEVICE, device);
    }
    
    public Device getDevice(Device dft)
    {
        Object val = this.fieldValues.get(KEY_DEVICE);
        return (val instanceof Device)? (Device)val : dft;
    }

    public Device getDevice()
    {
        return this.getDevice(null);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void setValue(String key, Object val)
    {
        this.fieldValues.put(key, val);
    }

    public Object getValue(String key, Object dft)
    {
        Object val = this.fieldValues.get(key);
        return (val != null)? val : dft;
    }

    public Object getValue(String key)
    {
        return this.fieldValues.get(key);
    }
    
    public boolean hasValue(String key)
    {
        return this.fieldValues.containsKey(key);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void setString(String key, String val)
    {
        this.fieldValues.put(key, val);
    }

    public String getString(String key, String dft)
    {
        Object val = this.fieldValues.get(key);
        return (val != null)? val.toString() : "";
    }

    public String getString(String key)
    {
        return this.getString(key, "");
    }

    // ------------------------------------------------------------------------

    public void setLong(String key, long val)
    {
        this.fieldValues.put(key, new Long(val));
    }

    public long getLong(String key, long dft)
    {
        Object val = this.fieldValues.get(key);
        return StringTools.parseLong(val, dft);
    }

    public long getLong(String key)
    {
        return this.getLong(key, 0L);
    }

    // ------------------------------------------------------------------------

    public void setInt(String key, int val)
    {
        this.fieldValues.put(key, new Integer(val));
    }

    public int getInt(String key, int dft)
    {
        Object val = this.fieldValues.get(key);
        return StringTools.parseInt(val, dft);
    }

    public int getInt(String key)
    {
        return this.getInt(key, 0);
    }

    // ------------------------------------------------------------------------

    public void setDouble(String key, double val)
    {
        this.fieldValues.put(key, new Double(val));
    }

    public double getDouble(String key, double dft)
    {
        Object val = this.fieldValues.get(key);
        return StringTools.parseDouble(val, dft);
    }

    public double getDouble(String key)
    {
        return this.getDouble(key, 0.0);
    }

    // ------------------------------------------------------------------------

    public void setBoolean(String key, boolean val)
    {
        this.fieldValues.put(key, new Boolean(val));
    }

    public boolean getBoolean(String key, boolean dft)
    {
        Object val = this.fieldValues.get(key);
        return StringTools.parseBoolean(val, dft);
    }

    public boolean getBoolean(String key)
    {
        return this.getBoolean(key, false);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
