<%@ taglib uri="./Track" prefix="gts" %>
<%@ page isELIgnored="true" contentType="text/html; charset=UTF-8" %>
<%
//response.setContentType("text/html; charset=UTF-8");
//response.setCharacterEncoding("UTF-8");
response.setHeader("CACHE-CONTROL", "NO-CACHE");
response.setHeader("PRAGMA"       , "NO-CACHE");
response.setDateHeader("EXPIRES"  , 0         );
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<!-- <!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"> -->
<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>
<!-- 
  =======================================================================================
  Copyright(C) Halcon GPS Tracking System - All rights reserved
  Developer: Carlos Gonzalez [cgonzalez.dev@gmail.com]
  =======================================================================================
-->
<gts:var ifKey="notDefined" value="true">
</gts:var>

<!-- Head -->
<head>

  <!-- meta -->
  <gts:var>
  <meta name="author" content="Halcon GPS Tracking System"/>
  <meta http-equiv="content-type" content='text/html; charset=UTF-8'/>
  <meta http-equiv="cache-control" content='no-cache'/>
  <meta http-equiv="pragma" content="no-cache"/>
  <meta http-equiv="expires" content="0"/>
  <meta name="copyright" content="${copyright}"/>
  <meta name="robots" content="none"/>
  </gts:var>

  <!-- page title -->
  <gts:var>
  <title>${pageTitle}</title>
  </gts:var>

  <!-- default style -->
  <link rel='stylesheet' type='text/css' href='css/General.css'/>
  <link rel='stylesheet' type='text/css' href='css/MenuBar.css'/>
  <link rel='stylesheet' type='text/css' href='css/Controls.css'/>

  <!-- custom overrides style -->
  <link rel='stylesheet' type='text/css' href='custom/General.css'/>
  <link rel='stylesheet' type='text/css' href='custom/MenuBar.css'/>
  <link rel='stylesheet' type='text/css' href='custom/Controls.css'/>

  <!-- javascript -->
  <gts:track section="javascript"/>

  <!-- local style -->
  <style type="text/css">
    TD.titleText {
        font-family: arial,verdana,sans-serif;
        font-size: 18pt;
        font-weight: bold;
        text-align: center;
        color: #000000;
    }
    TD.contentFullMapCell {
        width: 100%;
        /* height: 100%; */
        padding: 0px 0px 0px 0px;
        border: solid #555555 2px;
        background-color: #FBFBFB;
        font-size: 7pt;
        vertical-align: top;
    }
  </style>
  
  <!-- dashboard menu style -->
  <style type="text/css">
    DIV.dashboardMenuDiv_hidden {
        visibility: collapse;
        display: none;
        height: 0px;
    }
    DIV.dashboardMenuDiv_fixed {
        background-color: white;
        height: 40px;
        width: 500px;
        position: absolute;
        z-Index: 300;
        left: 100px;
        top: 10px;
    }
    DIV.dashboardMenuDiv {
        max-height: 60px;
        height: 60px;
        border: solid 1px black;
        background-color: #FFFFFF;
        left: 0px;
        top: 0px;
        position: absolute;
        z-Index: 300;
        cursor: default;
    }
    TABLE.dashboardMenuTable {
        width: 100%;
        height: 100%;
    }
    TR.dashboardMenuTableRow {
        width: 100%;
        height: 100%;
    }
    TD.dashboardMenuLogoCell {
        /* width: 50; */
        /*height: 100%; */
    }
    TD.dashboardMenuPageCell {
        padding-left: 5px;
        padding-right: 5px;
    }
    IMG.dashboardMenuLogoImg {
        /* width: 190; */
        height: 40px;
    }
  </style>

  <!-- page specific style -->
  <gts:track section="stylesheet"/>

  <!-- custom override style -->
  <link rel='stylesheet' type='text/css' href='custom/Custom.css'/>

</head>

<!-- ======================================================================================= -->

<body onload="<gts:track section='body.onload'/>" onunload="<gts:track section='body.onunload'/>">

<table width='100%' height='100%' border='0' cellspacing='0' cellpadding='0'>
<tbody>

<!-- Begin Page contents ======================================== -->
<tr height='100%'>
  <td class="contentFullMapCell">
      <gts:track section="content.body"/>
  </td>
</tr>
<!-- End Page contents ======================================== -->

</tbody>
</table>

<!-- Begin Dashboard menu ======================================== -->
<div id="dashboardMenuDiv" class="dashboardMenuDiv_fixed">
  <table id="dashboardMenuTable" class="dashboardMenuTable" border="0" cellpadding="0" cellspacing="0">
   <tbody>
    <tr class="dashboardMenuTableRow">
     <td class="dashboardMenuLogoCell"><img class="dashboardMenuLogoImg" src="./images/xMap.png"></td>
     <td class="dashboardMenuPageCell" align="center" height="100%" valign="middle" nowrap>
       <a href='<gts:track section="pageurl" arg="menu.top"/>'        target="_self">Menu</a><br>
       <a href='<gts:track section="pageurl" arg="map.device.full"/>' target="_self">Device</a><br>
       <a href='<gts:track section="pageurl" arg="map.fleet.full"/>'  target="_self">Fleet</a>
     </td>
     <td nowrap align="left" style="font-size:9pt; height:19px;">
       <form id='SelectDeviceForm' name='SelectDeviceForm' method='post' target='_self'>
            <input type='hidden' name='page' value='<gts:track section="pagename"/>'/>
            <input type='hidden' name='date_fr' value=''/>
            <input type='hidden' name='date_to' value=''/>
            <input type='hidden' name='date_tz' value=''/>
            <table cellspacing='0' cellpadding='0' border='0'>
              <tr>
              <td nowrap>
                <input id='deviceSelector' name='device' type='hidden' value=''>
                <input id='deviceDescription' name='deviceDescription' type='text' value='' readonly size='20' style='height:17px; padding:0px 0px 0px 3px; margin:0px 0px 0px 3px; cursor:pointer; border:1px solid gray;' onclick="javascript:fmShowSelector()">
                </td>
              <td>
                <img src='images/Pulldown.png' height='17' style='cursor:pointer;' onclick='javascript:fmShowSelector()'>
                </td>
              <td style='padding-left:12px;'>&nbsp;</td>
              </tr>
            </table>
       </form>
     </td>
     <td align="center" height="100%" width="100%" valign="middle">
        &nbsp;
     </td>
    </tr>
   </tbody>
  </table>
</div>
<!-- End Dashboard menu ======================================== -->

</body>

<!-- ======================================================================================= -->

</html>
