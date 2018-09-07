// ----------------------------------------------------------------------------
// Copyright 2007-2014, OpenGTS
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.extra.war.report.field;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.report.field.*;

public class AccountReport
    extends ReportData
{

    // ------------------------------------------------------------------------
    // Summary report
    // 1 record per Account
    // ------------------------------------------------------------------------

    private java.util.List<FieldData>   rowData = null;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private I18N                        i18n    = null;

    // ------------------------------------------------------------------------

    /**
    *** Device Report Constructor
    *** @param rptEntry The ReportEntry that generated this report
    *** @param reqState The session RequestProperties instance
    *** @param devList  The list of devices
    **/
    public AccountReport(ReportEntry rptEntry, RequestProperties reqState, ReportDeviceList devList)
        throws ReportException
    {
        super(rptEntry, reqState, devList);
        if (this.getAccount() == null) {
            throw new ReportException("Account-ID not specified");
        }
        this.i18n = reqState.getPrivateLabel().getI18N(AccountReport.class);
    }

    // ------------------------------------------------------------------------

    /**
    *** Post report initialization
    **/
    public void postInitialize()
    {
        //ReportConstraints rc = this.getReportConstraints();
        //Print.logInfo("LimitType=" + rc.getSelectionLimitType() + ", Limit=" + rc.getSelectionLimit());
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public static ReportLayout GetReportLayout()
    {
        // bind the report format to this data
        return FieldLayout.getReportLayout();
    }

    /**
    *** Gets the bound ReportLayout singleton instance for this report
    *** @return The bound ReportLayout
    **/
    public ReportLayout getReportLayout()
    {
        // bind the report format to this data
        return GetReportLayout();
    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the body of this report.
    *** @return The body row data iterator
    **/
    public DBDataIterator getBodyDataIterator()
    {

        /* init */
        this.rowData = new Vector<FieldData>();

        /* account information */
        Account currAcct = this.getAccount();

        /* get account list */
        Collection<String> accountList = null;
        try {
            accountList = Account.getAuthorizedAccounts(currAcct);
        } catch (DBException dbe) {
            Print.logError("Error reading authorized Accounts");
            accountList = new Vector<String>();
            accountList.add(currAcct.getAccountID());
        }

        /* loop through Accounts */
        for (String accountID : accountList) {
            try {
                Account account = Account.getAccount(accountID);
                if (account != null) {
                    FieldData fd = new FieldData();
                    fd.setAccount(account);
                    this.rowData.add(fd);
                } else {
                    // should never occur
                    Print.logError("'Account' is null: " + accountID);
                }
            } catch (DBException dbe) {
                Print.logError("Error retrieving Account: " + accountID);
            }
        }

        /* return data iterator */
        FieldData.sortByDeviceDescription(this.rowData);
        return new ListDataIterator(this.rowData);

    }

    // ------------------------------------------------------------------------

    /**
    *** Creates and returns an iterator for the row data displayed in the total rows of this report.
    *** @return The total row data iterator
    **/
    public DBDataIterator getTotalsDataIterator()
    {
        return null;
    }

    // ------------------------------------------------------------------------

}
