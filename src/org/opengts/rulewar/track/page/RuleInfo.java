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
package org.opengts.rulewar.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.rule.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class RuleInfo
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    private static final boolean SYSRULES_ONLY              = false;
    
    /* include Device pull-down sleection? */
    private static final boolean INCLUDE_PREFERRED_DEVICE   = true;
    private static boolean IncludeDeviceSelection()
    {
        return INCLUDE_PREFERRED_DEVICE;
    }

    /* include Group pull-down sleection? */
    private static boolean IncludeDeviceGroupRules()
    {
        return RuleList.IncludeDeviceGroupRules();
    }

    // ------------------------------------------------------------------------
    
    /* PrivateLabel key prefix for Rule tag desriptions */
    // see also property PrivateLabel.PROP_RuleInfo_ruleTagList
    public static final String   RULETAG_                   = "ruleTag.";

    /* Cron rule tags */
    // see also property PrivateLabel.PROP_RuleInfo_ruleTagList
    // see also file "config.conf" property "Domain.Properties.ruleInfo.ruleTagList"
    // see also file "crontab/crontab.xml"
    public static final String   RULETAG_5MIN               = "5min";
    public static final String   RULETAG_15MIN              = "15min";
    public static final String   RULETAG_30MIN              = "30min";
    public static final String   RULETAG_HOURLY             = "hourly";
    public static final String   RULETAG_DAILY              = "daily";
    public static final String   RULETAG_WEEKLY             = "weekly";
    public static final String   RULETAG_MONTHLY            = "monthly";

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String  FORM_RULE_SELECT           = "RuleInfoSelect";
    public  static final String  FORM_RULE_NEW              = "RuleInfoNew";
    public  static final String  FORM_RULE_EDIT             = "RuleInfoEdit";

    // commands
    public  static final String  COMMAND_INFO_UPDATE        = "update";
    public  static final String  COMMAND_INFO_SELECT        = "select";
    public  static final String  COMMAND_INFO_NEW           = "new";

    // submit
    public  static final String  PARM_SUBMIT_EDIT           = "r_subedit";
    public  static final String  PARM_SUBMIT_VIEW           = "r_subview";
    public  static final String  PARM_SUBMIT_CHG            = "r_subchg";
    public  static final String  PARM_SUBMIT_DEL            = "r_subdel";
    public  static final String  PARM_SUBMIT_NEW            = "r_subnew";

    // buttons
    public  static final String  PARM_BUTTON_CANCEL         = "r_btncan";
    public  static final String  PARM_BUTTON_BACK           = "r_btnbak";

    // parameters
    public  static final String  PARM_NEW_RULE              = "r_newrule";
    public  static final String  PARM_RULE_SELECT           = "r_rule";

    public  static final String  PARM_RULE_SYSRULE          = "r_sysrule";
    public  static final String  PARM_RULE_DESC             = "r_desc";
    public  static final String  PARM_RULE_SELECTOR         = "r_selector";
    public  static final String  PARM_RULE_ACTIVE           = "r_active";
    public  static final String  PARM_RULE_ISCRON           = "r_cron";

    public  static final String  PARM_RULE_ACT_EMAIL        = "ra_email";
    public  static final String  PARM_RULE_ACT_QUEUE        = "ra_queue";
    public  static final String  PARM_RULE_ACT_LISTENER     = "ra_lstnr";
    public  static final String  PARM_RULE_ACT_SAVE         = "ra_save";
    
    public  static final String  PARM_CANNED_ACTIONS        = "r_cra";

    public  static final String  PARM_RULE_EMAIL_ADDR       = "r_addr";
    public  static final String  PARM_RULE_EMAIL_SUBJ       = "r_subj";
    public  static final String  PARM_RULE_EMAIL_TEXT       = "r_text";
    public  static final String  PARM_RULE_EMAIL_WRAP       = "r_wrap";

    public  static final String  PARM_RULE_DEVICE           = "r_devid";
    public  static final String  PARM_RULE_GROUP            = "r_grpid";
    public  static final String  PARM_RULE_STATUS_COD       = "r_code";

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public RuleInfo()
    {
        this.setBaseURI(RequestProperties.TRACK_BASE_URI());
        this.setPageName(PAGE_RULE_INFO);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public boolean getIsEnabled()
    {
        boolean hasRule = DBConfig.hasRulePackage();
        if (!hasRule) {
            Print.logWarn("RuleInfo present, but Rules Engine not found");
        }
        return hasRule;
    }
    
    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RuleInfo.class);
        return super._getMenuDescription(reqState,i18n.getString("RuleInfo.editMenuDesc","View/Edit Rule Information"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RuleInfo.class);
        return super._getMenuHelp(reqState,i18n.getString("RuleInfo.editMenuHelp","View and Edit Rule information"));
    }
    
    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RuleInfo.class);
        return super._getNavigationDescription(reqState,i18n.getString("RuleInfo.navDesc","Rule"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(RuleInfo.class);
        return i18n.getString("RuleInfo.navTab","Rule Admin");
    }

    // ------------------------------------------------------------------------

    private String getRuleTagDescription(PrivateLabel privLabel, Locale locale, String ruleTag)
    {

        /* no ruletag */
        if (StringTools.isBlank(ruleTag)) {
            return ComboOption.getYesNoText(locale,true);
        }

        /* lookup ruleTag description */
        String key  = RULETAG_ + ruleTag;
        String desc = privLabel.getI18NTextString(key, null);
        if (!StringTools.isBlank(desc)) {
            return desc;
        }

        /* default descriptions */
        I18N i18n = I18N.getI18N(RuleInfo.class, locale);
        if (ruleTag.equals(RULETAG_5MIN)) {
            return i18n.getString("RuleInfo.cron.5min"   , "5 Minute");
        } else
        if (ruleTag.equals(RULETAG_15MIN)) {
            return i18n.getString("RuleInfo.cron.15min"  , "15 Minute");
        } else
        if (ruleTag.equals(RULETAG_30MIN)) {
            return i18n.getString("RuleInfo.cron.30min"  , "30 Minute");
        } else
        if (ruleTag.equals(RULETAG_HOURLY)) {
            return i18n.getString("RuleInfo.cron.hourly" , "Hourly");
        } else
        if (ruleTag.equals(RULETAG_DAILY)) {
            return i18n.getString("RuleInfo.cron.daily"  , "Daily" );
        } else
        if (ruleTag.equals(RULETAG_WEEKLY)) {
            return i18n.getString("RuleInfo.cron.weekly" , "Weekly");
        } else
        if (ruleTag.equals(RULETAG_MONTHLY)) {
            return i18n.getString("RuleInfo.cron.monthly", "Monthly");
        }

        /* still not found, return tag itself */
        return ruleTag;
        
    }
    
    private java.util.List<ComboOption> getRuleTagComboOptions(PrivateLabel privLabel, Locale locale)
    {
        I18N i18n = I18N.getI18N(RuleInfo.class, locale);
        java.util.List<ComboOption> tagOptList = new Vector<ComboOption>();
        String tags = privLabel.getStringProperty(PrivateLabel.PROP_RuleInfo_ruleTagList,null);
        if (!StringTools.isBlank(tags)) {
            for (String tag : StringTools.split(tags,',')) {
                if (!StringTools.isBlank(tag)) {
                    String desc = this.getRuleTagDescription(privLabel, locale, tag);
                    tagOptList.add(new ComboOption(tag,desc));
                }
            }
        }
        return tagOptList;
    }

    private ComboMap getRuleTagComboMap(PrivateLabel privLabel, Locale locale, boolean isCronRule, String ruleTag)
    {
        I18N i18n = I18N.getI18N(RuleInfo.class, locale);
        ComboMap tagMap = new ComboMap();
        String   NO     = "0";  // ComboOption.BOOLEAN_NAME_FALSE
        String   YES    = "1";  // ComboOption.BOOLEAN_NAME_TRUE

        /* "No"/"Yes" */
        tagMap.add(NO , ComboOption.getYesNoText(locale, false));
        tagMap.add(YES, ComboOption.getYesNoText(locale, true ));

        /* set default selected option */
        tagMap.setDefaultSelection(isCronRule?
            new ComboOption(YES, ComboOption.getYesNoText(locale,true )) :
            new ComboOption( NO, ComboOption.getYesNoText(locale,false))  );

        /* other tags */
        for (ComboOption tagOpt : this.getRuleTagComboOptions(privLabel,locale)) {
            String tag = tagOpt.getKey();
            tagMap.add(tag, tagOpt.getDescription());
            if (tag.equalsIgnoreCase(ruleTag)) {
                tagMap.setDefaultSelection(tagOpt);
            }
        }

        /* return ComboMap */
        return tagMap;

    }

    // ------------------------------------------------------------------------

    private String filter(String s)
    {
        if (StringTools.isBlank(s)) {
            return "&nbsp;";
        } else {
            return s;
        }
    }

    private String[] getAccountRuleIDs(String currAcctID)
    {
        String ruleList[] = null;
        try {
            ruleList = Rule.getRuleIDs(currAcctID, 
                false/*activeOnly*/, 
                true/*inclCronRules*/, 
                false/*inclSysRules*/);
        } catch (DBException dbe) {
            Print.logError("Error getting list of Rules: " + dbe);
            ruleList = new String[0];
        }
        return ruleList;
    }

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final I18N    i18n         = privLabel.getI18N(RuleInfo.class);
        final Locale  locale       = reqState.getLocale();
        final String  grpTitles[]  = reqState.getDeviceGroupTitles();
        final String  devTitles[]  = reqState.getDeviceTitles();
        final Account currAcct     = reqState.getCurrentAccount(); // never null
        final String  currAcctID   = currAcct.getAccountID(); // never null
        final User    currUser     = reqState.getCurrentUser(); // may be null
        final String  pageName     = this.getPageName();
        final boolean hasRuleFac   = Device.hasRuleFactory();
        String m = pageMsg;
        boolean error = false;

        /* argument rule-id */
        String ruleList[] = this.getAccountRuleIDs(currAcctID);
        String selRuleID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_RULE_SELECT, null);
        if (StringTools.isBlank(selRuleID)) {
            if (ruleList.length > 0) {
                selRuleID = ruleList[0];
            } else {
                selRuleID = "";
            }
        }
        if (ruleList.length == 0) {
            ruleList = new String[] { selRuleID };
        }

        /* rule db */
        final boolean isSystemAcctID = currAcct.isSystemAdmin();
        final boolean isSystemRuleID = !isSystemAcctID && Rule.isSystemAdminRuleID(selRuleID);
        final boolean showCronRules  = isSystemAcctID || privLabel.getBooleanProperty(PrivateLabel.PROP_RuleInfo_showCronRules,true);
        Rule selRule = null;
        try {
            if (isSystemRuleID) {
                // non-SystemAdmin, and a System-RuleID.
                selRule = Rule.getRule(currAcct, selRuleID); // may still be null
            } else {
                // non-System-RuleID or isSystemAdmin
                selRule = Rule.getRule(currAcct, selRuleID); // may still be null
            }
        } catch (DBException dbe) {
            // ignore
            selRule = null;
        }

        /* show rule list fields */
        final boolean showRuleList = !isSystemAcctID || (currAcct.getDeviceCount() > 0L);

        /* System Rules? [user|sys|both] */
        final int showSysRules;
        if (isSystemAcctID) {
            showSysRules = 0; // UserSpecified only
        } else {
            String ssro = privLabel.getStringProperty(PrivateLabel.PROP_RuleInfo_showSysRulesOnly,"yes");
            ssro = privLabel.getStringProperty(PrivateLabel.PROP_RuleInfo_showSysRules,ssro);
            if (ssro.equalsIgnoreCase("no") || ssro.equalsIgnoreCase("false") || ssro.equals("0")) {
                showSysRules = 0; // UserSpecified only
            } else
            if (ssro.equalsIgnoreCase("only")) {
                showSysRules = 1; // SysRules only
            } else {
                showSysRules = 2; // SysRules and UserSpecified
            }
        }
        
        /* show Actions */
        final boolean showActions = true;

        /* show canned actions */
        final int showCannedActions; // 0=false, 1=pull-down 2=textfield
        {
            String pda = privLabel.getStringProperty(PrivateLabel.PROP_RuleInfo_showPredefinedActions,"");
            if (StringTools.isBlank(pda)       ||
                pda.equalsIgnoreCase("false")  ||
                pda.equalsIgnoreCase("no")     ||
                pda.equalsIgnoreCase("0")        ) {
                showCannedActions = 0;  // false
            } else
            if (pda.equalsIgnoreCase("true")   || 
                pda.equalsIgnoreCase("yes")    ||
                pda.equalsIgnoreCase("1")      ||
                pda.equalsIgnoreCase("text")     ) {
                showCannedActions = 2;  // text-field
            } else
            if (pda.equalsIgnoreCase("select") || 
                pda.equalsIgnoreCase("pulldown") ) {
                showCannedActions = 1;  // pull-down
            } else {
                showCannedActions = 0;  // false
            }
        }

        /* ACL allow edit/view */
        boolean allowNew    = !isSystemRuleID && privLabel.hasAllAccess(currUser, this.getAclName());
        boolean allowDelete = allowNew;
        boolean allowEdit   = allowNew  || privLabel.hasWriteAccess(currUser, this.getAclName());
        boolean allowView   = allowEdit || privLabel.hasReadAccess(currUser, this.getAclName());

        /* command */
        String  ruleCmd     = reqState.getCommandName();
        boolean listRules   = false;
        boolean updateRule  = ruleCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectRule  = ruleCmd.equals(COMMAND_INFO_SELECT);
        boolean newRule     = ruleCmd.equals(COMMAND_INFO_NEW);
        boolean deleteRule  = false;
        boolean editRule    = false;
        boolean viewRule    = false;

        /* submit buttons */
        String submitEdit   = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT, "");
        String submitView   = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW, "");
        String submitChange = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG , "");
        String submitNew    = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW , "");
        String submitDelete = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL , "");

        /* sub-command */
        String newRuleID = null;
        if (newRule) {
            if (!allowNew) {
               newRule = false; // not authorized
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newRuleID = AttributeTools.getRequestString(httpReq,PARM_NEW_RULE,"").trim();
                newRuleID = newRuleID.toLowerCase();
                if (StringTools.isBlank(newRuleID)) {
                    m = i18n.getString("RuleInfo.enterNewRule","Please enter a new Rule ID.");
                    error = true;
                    newRule = false;
                } else
                if (!StringTools.isBlank(Rule.SYSTEM_RULE_PREFIX) && newRuleID.startsWith(Rule.SYSTEM_RULE_PREFIX)) {
                    if (!isSystemAcctID) {
                        m = i18n.getString("RuleInfo.invalidIDChar","ID contains invalid characters");
                        error = true;
                        newRule = false;
                    } else
                    if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_RuleInfo_validateNewIDs,*/newRuleID.substring(Rule.SYSTEM_RULE_PREFIX.length()))) {
                        m = i18n.getString("RuleInfo.invalidIDChar","ID contains invalid characters");
                        error = true;
                        newRule = false;
                    } else {
                        // new role ID is ok
                    }
                } else 
                if (!WebPageAdaptor.isValidID(reqState,/*PrivateLabel.PROP_RuleInfo_validateNewIDs,*/newRuleID)) {
                    m = i18n.getString("RuleInfo.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newRule = false;
                } else {
                    // new role ID is ok
                }
            }
        } else
        if (updateRule) {
            if (!allowEdit) {
                updateRule = false; // not authorized
            } else
            if (!SubmitMatch(submitChange,i18n.getString("RuleInfo.change","Change"))) {
                updateRule = false;
            } else
            if (selRule == null) {
                m = i18n.getString("RuleInfo.ruleNotFound","Rule ID does not exist: " + selRuleID);
                error = true;
                updateRule = false;
            }
        } else
        if (selectRule) {
            if (SubmitMatch(submitDelete,i18n.getString("RuleInfo.delete","Delete"))) {
                if (allowDelete) {
                    deleteRule = true;
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("RuleInfo.edit","Edit"))) {
                if (allowEdit) {
                    if (selRule == null) {
                        m = i18n.getString("RuleInfo.pleaseSelectRule","Please select a Rule");
                        error = true;
                        listRules = true;
                    } else {
                        editRule = true;
                        viewRule = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("RuleInfo.view","View"))) {
                if (allowView) {
                    if (selRule == null) {
                        m = i18n.getString("RuleInfo.pleaseSelectRule","Please select a Rule");
                        error = true;
                        listRules = true;
                    } else {
                        viewRule = true;
                    }
                }
            } else {
                listRules = true;
            }
        } else {
            listRules = true;
        }

        /* delete Rule? */
        if (deleteRule) {
            if (selRule == null) {
                m = i18n.getString("RuleInfo.pleaseSelectRule","Please select a Rule");
                error = true;
            } else {
                try {
                    Rule.Key ruleKey = (Rule.Key)selRule.getRecordKey();
                    Print.logWarn("Deleting Rule: " + ruleKey);
                    ruleKey.delete(true); // will also delete dependencies
                    selRuleID = "";
                    selRule = null;
                    ruleList = this.getAccountRuleIDs(currAcctID);
                    if (!ListTools.isEmpty(ruleList)) {
                        selRuleID = ruleList[0];
                        try {
                            selRule = Rule.getRule(currAcct, selRuleID);
                        } catch (DBException dbe) {
                            // ignore
                        }
                    }
                } catch (DBException dbe) {
                    Print.logException("Deleting Rule", dbe);
                    m = i18n.getString("RuleInfo.errorDelete","Internal error deleting Rule");
                    error = true;
                }
            }
            listRules = true;
        }

        /* new rule? */
        if (newRule) {
            boolean createRuleOK = true;
            for (int u = 0; u < ruleList.length; u++) {
                if (ruleList[u].equalsIgnoreCase(newRuleID)) {
                    m = i18n.getString("RuleInfo.alreadyExists","This Rule already exists");
                    error = true;
                    createRuleOK = false;
                    break;
                }
            }
            if (createRuleOK) {
                try {
                    Rule rule = Rule.createNewRule(currAcct, newRuleID); // already saved
                    ruleList = this.getAccountRuleIDs(currAcctID);
                    selRule = rule;
                    selRuleID = rule.getRuleID();
                    m = i18n.getString("RuleInfo.createdRule","New Rule has been created");
                } catch (DBException dbe) {
                    Print.logException("Creating Rule", dbe);
                    m = i18n.getString("RuleInfo.errorCreate","Internal error creating Rule");
                    error = true;
                }
            }
            listRules = true;
        }
        
        /* use email wrapper? */

        /* change/update the Rule info? */
        if (updateRule) {
            String  sysRuleID   = AttributeTools.getRequestString(request, PARM_RULE_SYSRULE    , "");
            String  ruleDesc    = AttributeTools.getRequestString(request, PARM_RULE_DESC       , "");
            String  ruleActive  = AttributeTools.getRequestString(request, PARM_RULE_ACTIVE     , "");
            String  ruleIsCron  = AttributeTools.getRequestString(request, PARM_RULE_ISCRON     , "");
            String  ruleSel     = AttributeTools.getRequestString(request, PARM_RULE_SELECTOR   , "false");
            String  ruleSubj    = AttributeTools.getRequestString(request, PARM_RULE_EMAIL_SUBJ , "");
            String  ruleText    = AttributeTools.getRequestString(request, PARM_RULE_EMAIL_TEXT , "");
            String  cannedAct   = AttributeTools.getRequestString(request, PARM_CANNED_ACTIONS  , "");
            String  emailWrap   = AttributeTools.getRequestString(request, PARM_RULE_EMAIL_WRAP , "");
            String  notifyEmail = AttributeTools.getRequestString(request, PARM_RULE_EMAIL_ADDR , "");
            String  deviceID    = AttributeTools.getRequestString(request, PARM_RULE_DEVICE     , "");
            String  groupID     = IncludeDeviceGroupRules()? AttributeTools.getRequestString(request, PARM_RULE_GROUP, "") : null;
            String  statCode    = AttributeTools.getRequestString(request, PARM_RULE_STATUS_COD , "");
            // action mask
            int     ruleMask    = RuleFactory.ACTION_NONE; // TODO: email, listener, queue, saveLast
            boolean actEmail    = AttributeTools.getRequestBoolean(request, PARM_RULE_ACT_EMAIL   , false);
            boolean actQueue    = AttributeTools.getRequestBoolean(request, PARM_RULE_ACT_QUEUE   , false);
            boolean actListener = AttributeTools.getRequestBoolean(request, PARM_RULE_ACT_LISTENER, false);
            boolean actSave     = AttributeTools.getRequestBoolean(request, PARM_RULE_ACT_SAVE    , false);
            if (actEmail   ) { ruleMask |= RuleFactory.ACTION_EMAIL_ALL;    }
            if (actQueue   ) { ruleMask |= RuleFactory.ACTION_VIA_QUEUE;    }
            if (actListener) { ruleMask |= RuleFactory.ACTION_VIA_LISTENER; }
            if (actSave    ) { ruleMask |= RuleFactory.ACTION_SAVE_LAST;    }
            // update
            listRules = true;
            if (selRule != null) {
                try {
                    if (!isSystemRuleID) {
                        boolean saveOK = true;
                        if (!StringTools.isBlank(sysRuleID)) {
                            try {
                                String sysAdminID = Account.getSystemAdminAccountID();
                                Rule sysRule = Rule.getRule(sysAdminID, sysRuleID);
                                if (sysRule != null) {
                                    ruleDesc   = sysRule.getDescription();
                                    ruleActive = sysRule.getIsActive()? ComboOption.BOOLEAN_NAME_TRUE : ComboOption.BOOLEAN_NAME_FALSE;
                                    ruleIsCron = sysRule.getIsCronRule()?
                                        StringTools.blankDefault(sysRule.getRuleTag(),ComboOption.BOOLEAN_NAME_TRUE) :
                                        ComboOption.BOOLEAN_NAME_FALSE;
                                    ruleSel    = Rule.SYSADMIN_RULE_S + sysRule.getRuleID() + Rule.SYSADMIN_RULE_E;
                                    ruleSubj   = sysRule.getEmailSubject();
                                    ruleText   = sysRule.getEmailText();
                                    ruleMask   = sysRule.getActionMask();
                                    cannedAct  = sysRule.getCannedActions();
                                    emailWrap  = String.valueOf(sysRule.getUseEmailWrapper());
                                }
                            } catch (DBException dbe) {
                                Print.logException("Getting SysAdmin Rule: " + sysRuleID, dbe);
                            }
                        }
                        // description
                        if (!StringTools.isBlank(ruleDesc)) {
                            selRule.setDescription(ruleDesc);
                        }
                        // active
                        boolean ruleActv = ComboOption.parseYesNoText(locale, ruleActive, true);
                        if (selRule.getIsActive() != ruleActv) { 
                            selRule.setIsActive(ruleActv); 
                        }
                        // isCronRule
                        if (!StringTools.isBlank(ruleIsCron)) {
                            if (ruleIsCron.equals(ComboOption.BOOLEAN_NAME_FALSE) ||
                                ruleIsCron.equals("0")                              ) { // "No"?
                                selRule.setIsCronRule(false);
                                selRule.setRuleTag(null);
                            } else
                            if (ruleIsCron.equals(ComboOption.BOOLEAN_NAME_TRUE)  ||
                                ruleIsCron.equals("1")                              ) { // "Yes"?
                                selRule.setIsCronRule(true);
                                selRule.setRuleTag(null);
                            } else {
                                Print.logInfo("CronRule? " + ruleIsCron);
                                selRule.setIsCronRule(true);
                                selRule.setRuleTag(ruleIsCron);
                            }
                        }
                        // rule selector
                        selRule.setSelector(StringTools.blankDefault(ruleSel,"(false)"));
                        // rule action
                        selRule.setActionMask(ruleMask);
                        // canned actions
                        if ((showCannedActions > 0) && !selRule.getCannedActions().equals(cannedAct)) {
                            selRule.setCannedActions(cannedAct);
                        }
                        // email address
                        if (StringTools.isBlank(notifyEmail)) {
                            if (!selRule.getNotifyEmail().equals(notifyEmail)) {
                                selRule.setNotifyEmail(notifyEmail);
                            }
                        } else
                        if (EMail.validateAddresses(notifyEmail,true)) {
                            if (!selRule.getNotifyEmail().equals(notifyEmail)) {
                                selRule.setNotifyEmail(notifyEmail);
                            }
                        } else {
                            m = i18n.getString("RuleInfo.enterEMail","Please enter a valid notification email/sms address");
                            saveOK = false;
                        }
                        // email subject
                        if (StringTools.isBlank(ruleSubj) && privLabel.getEventNotificationDefault()) {
                            String s = StringTools.trim(privLabel.getEventNotificationSubject());
                            ruleSubj = StringTools.encodeNewline(s).replace('\"','\'');
                        }
                        selRule.setEmailSubject(ruleSubj);
                        // email text
                        if (StringTools.isBlank(ruleText) && privLabel.getEventNotificationDefault()) {
                            String b = StringTools.trim(privLabel.getEventNotificationBody());
                            ruleText = StringTools.encodeNewline(b).replace('\"','\'');
                        }
                        selRule.setEmailText(ruleText);
                        // email wrapper
                        boolean wrapEmail = false;
                        if (!StringTools.isBlank(emailWrap)) {
                            // user selection ("Use EMail Wrapper" is displayed)
                            wrapEmail = ComboOption.parseYesNoText(locale, emailWrap, false);
                        } else {
                            // "Use EMail Wrapper" not displayed, default to 'false'
                            wrapEmail = false;
                        }
                        if (selRule.getUseEmailWrapper() != wrapEmail) { 
                            selRule.setUseEmailWrapper(wrapEmail);
                        }
                        // save
                        if (saveOK) {
                            selRule.save();
                            // update "RuleList"
                            selRule.clearRuleListEntries();
                            if (!StringTools.isBlank(statCode)) {
                                int sc = StringTools.parseInt(statCode, RuleList.ALL_CODES);
                                if (!StringTools.isBlank(deviceID) && !deviceID.equalsIgnoreCase(RuleList.NO_DEVICES)) {
                                    //Print.logInfo("RuleList entry: Device=%s, SC=%d", deviceID, sc);
                                    selRule.setSingleDeviceRuleListEntry(deviceID, sc);
                                } else
                                if (!StringTools.isBlank(groupID) && !groupID.equalsIgnoreCase(RuleList.NO_GROUPS)) {
                                    //Print.logInfo("RuleList entry: Group=%s, SC=%d", groupID, sc);
                                    selRule.setSingleGroupRuleListEntry(groupID, sc);
                                } else {
                                    //Print.logInfo("RuleList entry: Device=ALL, SC=%d", devID, sc);
                                    selRule.setSingleDeviceRuleListEntry(RuleList.NO_DEVICES, sc);
                                }
                            }
                            m = i18n.getString("RuleInfo.ruleUpdated","Rule information updated");
                        } else {
                            // should stay on this page
                            editRule = true;
                        }
                    } else {
                        // System Rule-ID : update "RuleList"
                        RuleList.clearRuleListEntries(currAcctID, selRuleID);
                        if (!StringTools.isBlank(statCode)) {
                            int sc = StringTools.parseInt(statCode, RuleList.ALL_CODES);
                            if (!StringTools.isBlank(deviceID) && !deviceID.equalsIgnoreCase(RuleList.NO_DEVICES)) {
                                //Print.logInfo("Setting RuleList entry: Device=%s, StatusCode=%d", deviceID, sc);
                                RuleList.setSingleDeviceRuleListEntry(currAcctID, selRuleID, deviceID, sc);
                            } else
                            if (!StringTools.isBlank(groupID) && !groupID.equalsIgnoreCase(RuleList.NO_GROUPS)) {
                                //Print.logInfo("Setting RuleList entry: Group=%s, StatusCode=%d", groupID, sc);
                                RuleList.setSingleGroupRuleListEntry(currAcctID, selRuleID, groupID, sc);
                            } else {
                                //Print.logInfo("Setting RuleList entry: Device=%s, StatusCode=%d", devID, sc);
                                RuleList.setSingleDeviceRuleListEntry(currAcctID, selRuleID, RuleList.NO_DEVICES, sc);
                            }
                        }
                        m = i18n.getString("RuleInfo.ruleUpdated","Rule information updated");
                    }
                    //return;
                } catch (Throwable t) {
                    Print.logException("Error Updating Rule", t);
                    m = i18n.getString("RuleInfo.errorUpdate","Internal error updating Rule");
                    error = true;
                    //return;
                }
            } else {
                m = i18n.getString("RuleInfo.noRules","There are currently no defined Rules for this account.");
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                //String cssDir = RuleInfo.this.getCssDirectory();
                //WebPageAdaptor.writeCssLink(out, reqState, "RuleInfo.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS), request);
                JavaScriptTools.writeStartJavaScript(out);
                String sysRuleDef   = i18n.getString("RuleInfo.sysRuleWillDefine", "(The System Rule will define this value)").replace('\"','\'');
                String dftEmailSubj = "";
                String dftEmailBody = "";
                if (privLabel.getEventNotificationDefault()) {
                    dftEmailSubj = StringTools.encodeNewline(privLabel.getEventNotificationSubject()).replace('\"','\'');
                    dftEmailBody = StringTools.encodeNewline(privLabel.getEventNotificationBody()).replace('\"','\'');
                }
                out.write("// Onload \n");
                out.write("function ruleOnLoad() {\n");
                out.write("    ruleDeviceGroupChange();\n");
                out.write("}\n");
                
                out.write("// System rule changed\n");
                out.write("var DEFAULT_EMAIL_SUBJECT = \""+dftEmailSubj+"\";\n");
                out.write("var DEFAULT_EMAIL_BODY    = \""+dftEmailBody+"\";\n");
                out.write("function ruleSysRuleChange() {\n");
                out.write("   var index = document."+FORM_RULE_EDIT+"." + PARM_RULE_SYSRULE + ".selectedIndex;\n");
                out.write("   var value = document."+FORM_RULE_EDIT+"." + PARM_RULE_SYSRULE + ".value;\n");
                out.write("   var title = document."+FORM_RULE_EDIT+"." + PARM_RULE_SYSRULE + ".options[index].text;\n");
                out.write("   if (value != '') {\n"); // <-- SysRule is selected
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACTIVE+".disabled=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_DESC+".className='"+CommonServlet.CSS_TEXT_READONLY+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_DESC+".readOnly=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_DESC+".value=title;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_SELECTOR+".className='"+CommonServlet.CSS_TEXT_READONLY+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_SELECTOR+".readOnly=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_SELECTOR+".value='$SYSRULE(\"' + value + '\")';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_SUBJ+".className='"+CommonServlet.CSS_TEXT_READONLY+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_SUBJ+".readOnly=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_SUBJ+".value=\""+sysRuleDef+"\";\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_TEXT+".className='"+CommonServlet.CSS_TEXTAREA_READONLY+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_TEXT+".readOnly=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_TEXT+".value=\""+sysRuleDef+"\";\n");
                if (showActions) {
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_EMAIL+".disabled=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_LISTENER+".disabled=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_SAVE+".disabled=true;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_QUEUE+".disabled=true;\n");
                }
                if (showCannedActions > 0) {
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_CANNED_ACTIONS+".className='"+CommonServlet.CSS_TEXT_READONLY+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_CANNED_ACTIONS+".readOnly=true;\n");
                }
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ISCRON+".disabled=true;\n");
                out.write("   } else {\n");  // <-- SysRule is NOT selected (ie. "n/a" selected)
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACTIVE+".disabled=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_DESC+".className='"+CommonServlet.CSS_TEXT_INPUT+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_DESC+".readOnly=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_DESC+".value='';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_SELECTOR+".className='"+CommonServlet.CSS_TEXT_INPUT+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_SELECTOR+".readOnly=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_SELECTOR+".value='';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_SUBJ+".className='"+CommonServlet.CSS_TEXT_INPUT+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_SUBJ+".readOnly=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_SUBJ+".value=DEFAULT_EMAIL_SUBJECT;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_TEXT+".className='"+CommonServlet.CSS_TEXTAREA_INPUT+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_TEXT+".readOnly=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_EMAIL_TEXT+".value=DEFAULT_EMAIL_BODY;\n");
                if (showActions) {
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_EMAIL+".disabled=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_LISTENER+".disabled=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_SAVE+".disabled=false;\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ACT_QUEUE+".disabled=false;\n");
                }
                if (showCannedActions > 0) {
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_CANNED_ACTIONS+".className='"+CommonServlet.CSS_TEXT_INPUT+"';\n");
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_CANNED_ACTIONS+".readOnly=false;\n");
                }
                out.write("      document."+FORM_RULE_EDIT+"."+PARM_RULE_ISCRON+".disabled=false;\n");
                out.write("   }\n");
                out.write("}\n");
                
                out.write("// Device/Group selectio changed\n");
                out.write("function ruleDeviceGroupChange() {\n");
                out.write("   var devCB  = document.getElementById('"+PARM_RULE_DEVICE+"');\n");
                out.write("   var grpCB  = document.getElementById('"+PARM_RULE_GROUP +"');\n");
                out.write("   var devVal = devCB? devCB.value : null;\n");
                out.write("   var grpVal = grpCB? grpCB.value : null;\n");
                out.write("   if ((devVal != null) && (grpVal != null)) {\n");
                out.write("      if (devVal == '-') {\n");
                out.write("          grpCB.disabled = false\n");
                out.write("          if (grpVal == '-') {\n");
                out.write("             devCB.disabled = false;\n"); // enable device CB
                out.write("          } else {\n");
                out.write("             devCB.disabled = true;\n");  // disable device CB
                out.write("          }\n");
                out.write("      } else {\n");
                out.write("          devCB.disabled = false;\n"); // enable device CB
                out.write("          grpCB.disabled = true;\n");  // disable device CB
                out.write("          grpCB.value = '-';\n");
                out.write("      }\n");
                out.write("   }\n");
                out.write("}\n");
                
                JavaScriptTools.writeEndJavaScript(out);
            }
        };

        /* Content */
        final String     _selRuleID   = selRuleID;
        final Rule       _selRule     = selRule;
        final String     _ruleList[]  = ruleList;
        final boolean    _allowEdit   = allowEdit;
        final boolean    _allowView   = allowView;
        final boolean    _allowNew    = allowNew;
        final boolean    _allowDelete = allowDelete;
        final boolean    _editRule    = _allowEdit && editRule && !isSystemRuleID;
        final boolean    _editStatSel = _allowEdit && editRule;
        final boolean    _viewRule    = _editRule || viewRule;
        final boolean    _listRules   = listRules || (!_editRule && !_viewRule);
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = RuleInfo.this.getPageName();

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,RequestProperties.TRACK_BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = RuleInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String selectURL  = RuleInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String newURL     = RuleInfo.this.encodePageURL(reqState);//,RequestProperties.TRACK_BASE_URI());
                String frameTitle = _allowEdit? 
                    i18n.getString("RuleInfo.viewEditRule","View/Edit Rule Info") : 
                    i18n.getString("RuleInfo.viewRule","View Rule Info");
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                // RuleInfo selection table (Select, RuleInfo ID, RuleInfo Name)
                if (_listRules) {

                    // RuleInfo selection table (Select, RuleInfo ID, RuleInfo Name)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("RuleInfo.selectRule","Select a Rule")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_RULE_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");

                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+i18n.getString("RuleInfo.select","Select")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RuleInfo.ruleID","Rule ID")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RuleInfo.description","Description")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RuleInfo.selector","Selector")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RuleInfo.cron","Cron\nRule")+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+i18n.getString("RuleInfo.active","Active")+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    for (int u = 0; u < _ruleList.length; u++) {
                        if ((u & 1) == 0) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        try {
                            Rule rule = Rule.getRule(currAcct, _ruleList[u]);
                            if (rule != null) {
                                String checked  = _selRuleID.equalsIgnoreCase(rule.getRuleID())? " checked" : "";
                                String ruleID   = rule.getRuleID();
                                String ruleDesc = RuleInfo.this.filter(rule.getDescription());
                                String rsel     = rule.getSelector();
                                String ruleSel  = RuleInfo.this.filter((rsel.length() > 40)? (rsel.substring(0,40)+" ...") : rsel);
                                String ruleActv = RuleInfo.this.filter(ComboOption.getYesNoText(locale,rule.getIsActive()));
                                String ruleTag  = rule.getRuleTag();
                                String ruleCron;
                                if (!rule.getIsCronRule()) {
                                    // not a Cron rule
                                    ruleCron = RuleInfo.this.filter("--");
                                } else 
                                if (StringTools.isBlank(ruleTag)) {
                                    // rule.getIsCronRule() is "true"
                                    ruleCron = RuleInfo.this.filter(ComboOption.getYesNoText(locale,true));
                                } else {
                                    ruleCron = RuleInfo.this.filter(RuleInfo.this.getRuleTagDescription(privLabel,locale,ruleTag));
                                }
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"'><input type='radio' name='"+PARM_RULE_SELECT+"' id='"+ruleID+"' value='"+ruleID+"' "+checked+"></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+ruleID+"'>"+ruleID+"</label></td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+ruleDesc+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap align='left'>"+ruleSel+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap align='center'>"+ruleCron+"</td>\n");
                                out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap align='center'>"+ruleActv+"</td>\n");
                            }
                        } catch (DBException dbe) {
                            // 
                        }
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("RuleInfo.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit  ) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("RuleInfo.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("RuleInfo.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");

                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");

                    /* new rule */
                    if (_allowNew) {
                        out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("RuleInfo.createNewRule","Create a new Rule")+":</h1>\n");
                        out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                        out.write("<form name='"+FORM_RULE_NEW+"' method='post' action='"+newURL+"' target='_top'>");
                        out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                        out.write(i18n.getString("RuleInfo.ruleID","Rule ID")+": <input type='text' class='"+CommonServlet.CSS_TEXT_INPUT+"' name='"+PARM_NEW_RULE+"' value='' size='32' maxlength='32'><br>\n");
                        out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("RuleInfo.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                        out.write("</form>\n");
                        out.write("</div>\n");
                        out.write("<hr>\n");
                    }

                } else {
                    // rule view/edit form

                    /* start of form */
                    out.write("<form name='"+FORM_RULE_EDIT+"' method='post' action='"+editURL+"' target='_top'>\n");
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* status code list */
                    ComboMap codeMap = new ComboMap();
                    Map<Integer,String> _scDescMap = privLabel.getStatusCodeDescriptionMap();
                    for (Integer sc : _scDescMap.keySet()) {
                        int    code    = sc.intValue();
                        String codeStr = "0x" + StringTools.toHexString(code, 16);
                        if (code == RuleList.ALL_CODES) {
                            codeMap.add(codeStr, i18n.getString("RuleInfo.allCodes", "All Codes"));
                        } else {
                            String desc = StatusCode.getDescription(currAcctID, code, privLabel, _scDescMap.get(sc));
                            codeMap.add(codeStr, desc);
                        }
                    }
                    // TODO: sort codeMap?

                    /* get selected Group/Device/StatusCode */
                    String devSel  = RuleList.ALL_DEVICES;
                    String grpSel  = RuleList.NO_GROUPS;
                    int codeSelInt = RuleList.ALL_CODES;
                    if (IncludeDeviceSelection() || IncludeDeviceGroupRules()) {
                        try {
                            Collection<RuleList> ruleList = RuleList.getRuleList(currAcctID, _selRuleID, 1L);
                            if (!ListTools.isEmpty(ruleList)) {
                                RuleList ruleListItem0 = ListTools.itemAt(ruleList,0,null);
                                devSel     = ruleListItem0.getDeviceID();     // may be ALL_DEVICES
                                grpSel     = ruleListItem0.getGroupID();      // may be ALL_GROUPS
                                codeSelInt = ruleListItem0.getStatusCode();   // may be ALL_CODES
                                // one or both of 'devSel'/'grpSel' should be NO_DEVICES/NO_GROUPS
                                if (StringTools.isBlank(devSel)) {
                                    devSel = RuleList.NO_DEVICES;
                                }
                                if (StringTools.isBlank(grpSel)) {
                                    grpSel = RuleList.NO_GROUPS;
                                }
                                if (!devSel.equals(RuleList.NO_DEVICES) && !grpSel.equals(RuleList.NO_GROUPS)) {
                                    Print.logWarn("Neither deviceID/groupID are NO_DEVICES/NO_GROUPS"); 
                                    grpSel = RuleList.NO_GROUPS;
                                }
                            } else {
                                // distinguish between all-devices and no-devices?
                                devSel = RuleList.NO_DEVICES;
                                grpSel = RuleList.NO_GROUPS;
                            }
                            //Print.logInfo("Selected Device: " + devSel);
                        } catch (DBException dbe) {
                            Print.logException("Getting RuleList for Rule " + _selRule, dbe);
                        }
                    } else {
                        /* selected status code (first entry matching AccountID/DeviceALL/RuleID) */
                        try {
                            int codes[] = RuleList.getStatusCodesForDeviceID(_selRule, RuleList.ALL_DEVICES, false);
                            codeSelInt = !ListTools.isEmpty(codes)? codes[0] : RuleList.ALL_CODES;
                        } catch (DBException dbe) {
                            Print.logException("Getting StatusCodes for Rule " + _selRule, dbe);
                        }
                    }
                    String codeSel = "0x" + StringTools.toHexString(codeSelInt, 16);

                    /* EMail subject/body */
                    String emailSubj = (_selRule != null)? _selRule.getEmailSubject() : "";
                    String emailText = (_selRule != null)? _selRule.getEmailText()    : "";
                    boolean showUseWrapper;
                    if (!privLabel.hasEventNotificationEMail()) {
                        // no PrivateLabel email wrapper to use
                        showUseWrapper = false;
                    } else
                    if (privLabel.getEventNotificationDefault()) {
                        // PrivateLabel email is to be used as a 'default' setting
                        showUseWrapper = false;
                        if (StringTools.isBlank(emailSubj)) {
                            emailSubj = privLabel.getEventNotificationSubject();
                        }
                        if (StringTools.isBlank(emailText)) {
                            emailText = privLabel.getEventNotificationBody();
                        }
                    } else {
                        // PrivateLabel email wrapper has been set up to be used as a wrapper
                        showUseWrapper = privLabel.getBooleanProperty(PrivateLabel.PROP_RuleInfo_showEMailWrapper,false);
                    }

                    /* Rule fields */
                    String ruleID         = _selRuleID;
                    String ruleDesc       = (_selRule != null)? _selRule.getDescription()  : "";
                    String ruleSelector   = (_selRule != null)? _selRule.getSelector()     : "";
                    String ntfyEmail      = (_selRule != null)? _selRule.getNotifyEmail()  : "";
                    String emailTextNL    = StringTools.decodeNewline(emailText);
                    int emailBodyRows     = 12; // showUseWrapper? 3 : 12;
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_RULE_SELECT    , false    , i18n.getString("RuleInfo.ruleID"       ,"Rule ID")+":"            , ruleID, 20, 32));
                    if (!isSystemAcctID) {
                    out.println(FormRow_TextField(PARM_RULE_EMAIL_ADDR, _editRule, i18n.getString("RuleInfo.notifyAddress","Notification EMail")+":" , ntfyEmail, 95, 125));
                    }
                    boolean _editSel = _editRule && (showSysRules != 1);
                    
                    if (showSysRules > 0) {
                        Map<String,String> ruleIDMap = null; // <RuleID,RuleDescription>
                        try {
                            ruleIDMap = Rule.getSystemAdminRuleIDMap(showCronRules);
                        } catch (DBException dbe) {
                            Print.logException("Getting RuleID/Description Map", dbe);
                            ruleIDMap = null; // new HashMap<String,String>();
                        }
                        if (!ListTools.isEmpty(ruleIDMap)) {
                            String sysRuleID = StringTools.trim(Rule.GetSysRuleIDFromSelector(ruleSelector)); // may be blank
                            if (_editSel && !StringTools.isBlank(sysRuleID)) {
                                _editSel = false;
                            }
                            ComboMap sysRuleList = new ComboMap(ruleIDMap);
                            sysRuleList.insert("",i18n.getString("RuleInfo.noSysRule","n/a"));
                            out.println(FormRow_ComboBox(PARM_RULE_SYSRULE, _editRule, i18n.getString("RuleInfo.systemRule","System Rule")+":"     , sysRuleID, sysRuleList, "javascript:ruleSysRuleChange()", 50));
                        }
                    }

                    ComboOption ruleActive = ComboOption.getYesNoOption(locale, ((_selRule != null) && _selRule.isActive()));
                    out.println(FormRow_ComboBox (PARM_RULE_ACTIVE    , _editSel , i18n.getString("RuleInfo.active","Active")+":"                  , ruleActive, ComboMap.getYesNoMap(locale), "", -1));

                    // isCronRule
                    boolean isCronRule = ((_selRule != null) && _selRule.getIsCronRule());
                    if (showCronRules || isCronRule) {
                    String ruleTag = (_selRule != null)? _selRule.getRuleTag() : null;
                    ComboMap    ruleCronMap = RuleInfo.this.getRuleTagComboMap(privLabel,locale,isCronRule,ruleTag); // ComboMap.getYesNoMap(locale)
                    ComboOption ruleCronOpt = ruleCronMap.getDefaultSelection(); // ComboOption.getYesNoOption(locale, isCronRule);
                    out.println(FormRow_ComboBox( PARM_RULE_ISCRON    , _editSel , i18n.getString("RuleInfo.isCronRule"   ,"Is Cron Rule" )+":"    , ruleCronOpt, ruleCronMap, "", -1, i18n.getString("RuleInfo.usedForPeriodicRuleTriggering","(Used for periodic Rule triggering)")));
                    }

                    out.println(FormRow_TextField(PARM_RULE_DESC      , _editSel , i18n.getString("RuleInfo.description"  ,"Description"  )+":"    , ruleDesc, 50, 80));

                    out.println(FormRow_Separator());
                    out.println(FormRow_TextField(PARM_RULE_SELECTOR  , _editSel , i18n.getString("RuleInfo.ruleSelector" ,"Rule Selector")+":"    , ruleSelector, 95, 200));

                    if (showActions) {
                        int ruleMask = (_selRule != null)? _selRule.getActionMask() : RuleFactory.ACTION_NONE;
                        //if (ruleMask == RuleFactory.ACTION_NONE) { ruleMask = RuleFactory.ACTION_DEFAULT; }
                        boolean actEmail    = ((ruleMask & RuleFactory.ACTION_VIA_EMAIL   ) != 0);
                        boolean actQueue    = ((ruleMask & RuleFactory.ACTION_VIA_QUEUE   ) != 0);
                        boolean actListener = ((ruleMask & RuleFactory.ACTION_VIA_LISTENER) != 0);
                        boolean actSave     = ((ruleMask & RuleFactory.ACTION_SAVE_LAST   ) != 0);
                        out.print("<tr>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_HEADER+"' nowrap>"+i18n.getString("RuleInfo.triggerAction","Trigger Action")+":</td>");
                        out.print("<td class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE_DATA+"'>");
                        out.print(" &nbsp;&nbsp;" + i18n.getString("RuleInfo.actionEmail","EMail:") + " ");
                        out.print(Form_CheckBox(PARM_RULE_ACT_EMAIL   , PARM_RULE_ACT_EMAIL   , _editSel, actEmail   , null, null));
                        out.print(" &nbsp;&nbsp;" + i18n.getString("RuleInfo.actionListener","Listener:") + " ");
                        out.print(Form_CheckBox(PARM_RULE_ACT_LISTENER, PARM_RULE_ACT_LISTENER, _editSel, actListener, null, null));
                        out.print(" &nbsp;&nbsp;" + i18n.getString("RuleInfo.actionAlert","Save/Alert:") + " ");
                        out.print(Form_CheckBox(PARM_RULE_ACT_SAVE    , PARM_RULE_ACT_SAVE    , _editSel, actSave    , null, null));
                        out.print(" &nbsp;&nbsp;" + i18n.getString("RuleInfo.actionQueue","Queue:") + " ");
                        out.print(Form_CheckBox(PARM_RULE_ACT_QUEUE   , PARM_RULE_ACT_QUEUE   , _editSel, actQueue   , null, null));
                        out.print("</td>");
                        out.print("</tr>\n");
                    }
                    if (showCannedActions == 1) {
                        // ComboBox [TODO: add this "private.xml"]
                        String craSel = (_selRule != null)? _selRule.getCannedActions() : "";
                        ComboMap craMap = new ComboMap();
                        craMap.insert(""                     , i18n.getString("RuleInfo.craNone"           ,"None"                   ));
                        craMap.insert("resetMaint:0"         , i18n.getString("RuleInfo.craResetMaint0"    ,"Reset Maintenance #0"   ));
                        craMap.insert("resetMaint:1"         , i18n.getString("RuleInfo.craResetMaint1"    ,"Reset Maintenance #1"   ));
                        craMap.insert("resetPingCount"       , i18n.getString("RuleInfo.craResetPingCount" ,"Reset Device Locates"   ));
                        craMap.insert("sendCommand:OutputOn" , i18n.getString("RuleInfo.craSendOutputOn"   ,"Send Output On"         ));
                        craMap.insert("sendCommand:OutputOff", i18n.getString("RuleInfo.craSendOutputOff"  ,"Send Output Off"        ));
                        out.println(FormRow_ComboBox(PARM_CANNED_ACTIONS, _editSel, i18n.getString("RuleInfo.cannedActions","Prefedined Actions")+":", craSel, craMap, "", -1));
                    } else
                    if (showCannedActions == 2) {
                        // TextField
                        String craSel = (_selRule != null)? _selRule.getCannedActions() : "";
                        out.println(FormRow_TextField(PARM_CANNED_ACTIONS,_editSel, i18n.getString("RuleInfo.cannedActions","Prefedined Actions")+":", craSel, 80, 80));
                    }

                    out.println(FormRow_Separator());
                    out.println(FormRow_TextField(PARM_RULE_EMAIL_SUBJ, _editSel , i18n.getString("RuleInfo.emailSubject" ,"EMail Subject")+":"    , emailSubj, 60, 100));
                    out.println(FormRow_TextArea( PARM_RULE_EMAIL_TEXT, _editSel , i18n.getString("RuleInfo.emailMessage" ,"EMail Message")+":"    , emailTextNL, emailBodyRows, 70));

                    if (showUseWrapper) {
                        ComboOption wrapEmail = ComboOption.getYesNoOption(locale, (_selRule!=null) && _selRule.getUseEmailWrapper());
                        out.println(FormRow_ComboBox( PARM_RULE_EMAIL_WRAP, _editSel , i18n.getString("RuleInfo.emailWrapper" ,"Use EMail Wrapper")+":", wrapEmail, ComboMap.getYesNoMap(locale), "", -1, i18n.getString("RuleInfo.seeEventNotificationEMail","(See 'EventNotificationEMail' tag in 'private.xml')")));
                    }
                    out.println("</table>");

                    /* (single) RuleList fields (assign Rule to Device/Group) */
                    if (showRuleList) {
                        out.write("<hr style='margin: 5px 0px 5px 0px;'>\n");
                        out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>");
                        out.write(i18n.getString("RuleInfo.deviceStatusCodeSelection","{0}/StatusCode Selection",devTitles) + ":</span>\n");
                        out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                        if (IncludeDeviceSelection() && !IncludeDeviceGroupRules()) {
                            ComboMap devMap = new ComboMap(reqState.createDeviceDescriptionMap(true/*includeID*/));
                            devMap.insert(RuleList.ALL_DEVICES, i18n.getString("RuleInfo.allDevices","All {1}",devTitles));
                            devMap.insert(RuleList.NO_DEVICES , i18n.getString("RuleInfo.noDevices" ,"No {1}",devTitles));
                            out.println(FormRow_ComboBox(PARM_RULE_DEVICE, _editStatSel, i18n.getString("RuleInfo.deviceID","{0} ID",devTitles)+":", devSel, devMap, "", -1));
                        } else
                        if (!IncludeDeviceSelection() && IncludeDeviceGroupRules()) {
                            ComboMap grpMap = new ComboMap(reqState.createGroupDescriptionMap(true/*includeID*/));
                            grpMap.insert(RuleList.ALL_GROUPS, i18n.getString("RuleInfo.allGroups","All {1}",grpTitles));
                            grpMap.insert(RuleList.NO_GROUPS , i18n.getString("RuleInfo.noGroups" ,"No {1}",grpTitles));
                            out.println(FormRow_ComboBox(PARM_RULE_GROUP, _editStatSel, i18n.getString("RuleInfo.groupID"  ,"{0} ID",grpTitles)+":", grpSel, grpMap, "", -1));
                        } else
                        if (IncludeDeviceSelection() && IncludeDeviceGroupRules()) {
                            String onchange = "javascript:ruleDeviceGroupChange()";
                            // --
                            ComboMap devMap = new ComboMap(reqState.createDeviceDescriptionMap(true/*includeID*/));
                            devMap.insert(RuleList.ALL_DEVICES, i18n.getString("RuleInfo.allDevices","All {1}",devTitles));
                            devMap.insert(RuleList.NO_DEVICES , i18n.getString("RuleInfo.noDevicesSel","No {1} (select by {2})",devTitles[0],devTitles[1],grpTitles[0],grpTitles[1]));
                            out.println(FormRow_ComboBox(PARM_RULE_DEVICE, PARM_RULE_DEVICE, _editStatSel, i18n.getString("RuleInfo.byDeviceID","By {0} ID",devTitles)+":", devSel, devMap, onchange, -1, ""));
                            // --
                            ComboMap grpMap = new ComboMap(reqState.createGroupDescriptionMap(true/*includeID*/));
                            grpMap.insert(RuleList.ALL_GROUPS, i18n.getString("RuleInfo.allGroups","All {1}",grpTitles));
                            grpMap.insert(RuleList.NO_GROUPS , i18n.getString("RuleInfo.noGroupsSel" ,"No {1} (select by {2}}",grpTitles[0],grpTitles[1],devTitles[0],devTitles[1]));
                            out.println(FormRow_ComboBox(PARM_RULE_GROUP, PARM_RULE_GROUP, _editStatSel, i18n.getString("RuleInfo.byGroupID"  ,"By {0} ID",grpTitles)+":", grpSel, grpMap, onchange, -1, ""));
                        }
                        out.println(FormRow_ComboBox( PARM_RULE_STATUS_COD, _editStatSel, i18n.getString("RuleInfo.statusCode"   ,"Status Code")  +":"      , codeSel, codeMap, "", -1));
                        out.println("</table>");
                        out.println("<span style='padding-bottom:5px'>&nbsp;</span>");
                    }

                    /* end of form */
                    out.write("<hr style='margin-bottom:5px;'>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editStatSel) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("RuleInfo.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("RuleInfo.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("RuleInfo.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    }
                    out.write("</form>\n");

                }

            }
        };

        /* write frame */
        String ruleOnLoad = "javascript:ruleOnLoad();";
        String onload = (error && !StringTools.isBlank(m))? (ruleOnLoad + JS_alert(false,m)) : ruleOnLoad;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
    // ------------------------------------------------------------------------
}
