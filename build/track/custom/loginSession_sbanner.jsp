<%@ taglib uri="./Track" prefix="gts" %>
<%@ page isELIgnored="true" contentType="text/html; charset=utf-8" %>
<%
//response.setContentType("text/html; charset=UTF-8");
//response.setCharacterEncoding("UTF-8");
response.setHeader("CACHE-CONTROL", "NO-CACHE");
response.setHeader("PRAGMA"       , "NO-CACHE");
response.setDateHeader("EXPIRES"  , 0         );
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>
<gts:var ifKey="notDefined" value="true">
</gts:var>
<head>
    <gts:var>
        <meta name="author" content="${pageTitle}"/>
        <meta http-equiv="Content-Type" content='text/html; charset=utf-8'/>
        <meta http-equiv="cache-control" content='no-cache'/>
        <meta http-equiv="pragma" content="no-cache"/>
        <meta http-equiv="expires" content="0"/>
        <meta name="copyright" content="${copyright}"/>
        <meta name="robots" content="all"/>
    </gts:var>

    <gts:var>
    	<title>${pageTitle}</title>
	</gts:var>
	<link href='http://fonts.googleapis.com/css?family=PT+Sans+Narrow' rel='stylesheet' type='text/css' />
	<link href='http://fonts.googleapis.com/css?family=News+Cycle&v1' rel='stylesheet' type='text/css' />
  	<link rel="icon" type="image/vnd.microsoft.icon" href="images/favicon.ico">
	<link rel='stylesheet' type='text/css' href='css/General.css'/>
	<link rel='stylesheet' type='text/css' href='css/MenuBar.css'/>
	<link rel='stylesheet' type='text/css' href='css/Controls.css'/>
   	<script src="js/utils.js" type="text/javascript"></script>

  	<gts:track section="javascript"/>
	<gts:track section="stylesheet"/>    

<!-- Estilos personalizados -->
<link rel='stylesheet' type='text/css' href='estilos/aguila.css'/>

<!-- Jquery Main -->
<script type="text/javascript" src="scripts/jquery-1.10.2.js"></script>
    
<!-- Add fancyBox main JS and CSS files -->
<script type="text/javascript" src="scripts/jquery.fancybox.js?v=2.1.5"></script>
<link rel="stylesheet" type="text/css" href="estilos/jquery.fancybox.css?v=2.1.5" media="screen" />

<!-- Jquery UI -->
<script src="scripts/jquery-ui-1.10.3.custom.js"></script>
<link rel="stylesheet" type="text/css" href="estilos/jquery-ui-1.10.3.custom.css" >

<!-- Velocímetro -->
<script src="scripts/raphael.2.1.0.min.js"></script>
<script src="scripts/justgage.1.0.1.min.js"></script>

<!-- jQPlot -->
<script language="javascript" type="text/javascript" src="scripts/jquery.jqplot.min.js"></script>
<script type="text/javascript" src="scripts/jqplot.pieRenderer.min.js"></script>
<link rel="stylesheet" type="text/css" href="estilos/jquery.jqplot.css" />

<!-- Rotate Background -->
<script src="scripts/jquery.backstretch.min.js"></script>


<!-- Main Routines -->
<script type="text/javascript">
$(document).ready(function () {	
    $('.fancybox').fancybox();

    $(".various").fancybox({
        maxHeight: 700,
        fitToView: false,
        width: '80%',
        height: '80%',
        autoSize: true,
        closeClick: false,
        openEffect: 'none',
        closeEffect: 'none'
    });

    var rutaArchivo;
    var fileImg;
    var nombreArchivo;
    $('.contentMapTableFull #botonera').on({

        mouseover: function () {
            rutaArchivo = $(this).attr('src').split('/');
            fileImg = rutaArchivo[rutaArchivo.length - 1];
            nombreArchivo = fileImg.substring(0, fileImg.length - 4);
            $(this).attr('src', 'images/' + nombreArchivo + '_color.png');
        },
        mouseout: function () {
            $(this).attr('src', 'images/' + nombreArchivo + '.png');
        }
    });

    $(document).tooltip({
        position: {
            my: "center bottom-20",
            at: "center top",
            using: function (position, feedback) {
                $(this).css(position);
                $("<div>")
                    .addClass("arrow")
                    .addClass(feedback.vertical)
                    .addClass(feedback.horizontal)
                    .appendTo(this);
            }
        }
    });
    
	// Expand Panel
	$("#open").click(function(){
		$("div#panel").slideDown("fast");
	
	});
	
	// Collapse Panel
	$("#close").click(function(){
		$("div#panel").slideUp("fast");	
	});
	$("div#panel").mouseleave(function(){
		$("div#panel").slideUp("fast");
		$("#toggle a").toggle();
	});
	
	// Switch buttons from "Log In | Register" to "Close Panel" on click
	$("#toggle a").click(function () {
		$("#toggle a").toggle();
	});
	
	


});
</script>
</head>
<body onLoad="<gts:track section='body.onload'/>" onUnload="<gts:track section='body.onunload'/>">
<!-- First, write your HTML -->


<!-- Panel -->
<div id="toppanel">
	<div id="panel">
		<div class="content clearfix">
        <table width="1000" border="0" align="center" cellpadding="0" cellspacing="0">
          <tr>
            <td width="210" height="138" align="center" valign="top">
            <gts:var ifKey="isLoggedIn" value="true">
            <table width="202" border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td width="202"><img src="./images/logos/logo.png" width="203" height="70" /></td>
              </tr>
              <tr>
                <td class="accountDesc"><span>${accountDesc}</span></td>
              </tr>
              <tr>
                <td class="accountPhone"><span>${accountPhone}</span></td>
              </tr>
              <tr>
                <td class="accountEmail"><span>${accountEmail}</span></td>
              </tr>
              <tr>
                <td><!-- Adicional --></td>
              </tr>
            </table></gts:var></td>
			<gts:var ifKey="isLoggedIn" value="true">
            <td width="790" align="center" valign="top"><table width="780" border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td width="156" align="center" valign="middle" class="menuEvNormal">${i18n.Map}</td>
                <td width="156" align="center" valign="middle" class="menuEvNormal">${i18n.Report}</td>
                <td width="156" align="center" valign="middle" class="menuEvNormal">${i18n.Administration}</td>
                <td width="156" align="center" valign="middle" class="menuEvNormal">
				</gts:var>
                <gts:var ifKey="userID" value="admin">
                ${i18n.System}
                </gts:var>
                </td>   
                <td width="156" align="center" valign="middle" class="menuEvNormal">
                <gts:var ifKey="accountID" value="sysadmin">
                ${i18n.Manager}
                </gts:var> 
                </td> 				
              </tr>
			  <gts:var ifKey="isLoggedIn" value="true">
              <tr>			  
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=map.device">${i18n.Vehicle}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=menu.rpt.devDetail">${i18n.Details}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=dev.info">${i18n.Devices}</a></td>
				</gts:var>
                <td align="center" valign="middle" id="tdEvolution">
                <gts:var ifKey="userID" value="admin">
                <a href="./Track?page=acct.info">${i18n.Account1}</a>
                </gts:var>
                </td>  
                <td align="center" valign="middle" id="tdEvolutionAdmin">
                <gts:var ifKey="accountID" value="sysadmin">
                <a href="./Track?page=sysAdmin.info">${i18n.SystemInfo}</a>
                </gts:var></td> 				
              </tr>
			  <gts:var ifKey="isLoggedIn" value="true">
              <tr>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=map.fleet">${i18n.Fleet}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=menu.rpt.grpDetail">${i18n.Fleets}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=group.info">${i18n.Fleets}</a></td>
				</gts:var>
                <td align="center" valign="middle" id="tdEvolution">
                <gts:var ifKey="userID" value="admin">
                <a href="./Track?page=user.info">${i18n.User1}</a>
                </gts:var>
                </td>
                <td align="center" valign="middle" id="tdEvolutionAdmin">
                <gts:var ifKey="accountID" value="sysadmin">
                <a href="./Track?page=sysAdmin.accounts">${i18n.Accounts1}</a>
                </gts:var></td>                
              </tr>
			    <gts:var ifKey="isLoggedIn" value="true">
              <tr>  
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=map.device.last">${i18n.LastMap}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=menu.rpt.grpSummary">${i18n.Summary}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=zone.info">${i18n.Geozones}</a></td>
				</gts:var>
                <td align="center" valign="middle" id="tdEvolution">					  
                <gts:var ifKey="userID" value="admin">
                <a href="./Track?page=driver.info">${i18n.Drivers}</a>
                </gts:var>
                </td> 
                <td align="center" valign="middle" id="tdEvolutionAdmin">
                <gts:var ifKey="accountID" value="sysadmin">
                <a href="./Track?page=sysAdmin.devices">${i18n.SearchDevices}</a>
                </gts:var></td> 				
              </tr>
			  <gts:var ifKey="isLoggedIn" value="true">
              <tr>
                <td align="center" valign="middle" id="tdEvolution">&nbsp;</td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=menu.rpt.devPerf">${i18n.Performance}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=corridor.info">${i18n.Corridor}</a></td>
                <td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=rule.info">${i18n.Rule}</a></td>	
				</gts:var>
                <td align="center" valign="middle" id="tdEvolutionAdmin">
                <gts:var ifKey="accountID" value="sysadmin">
                <a href="./Track?page=menu.rpt.sysSummary">${i18n.SystemReports}</a>
                </gts:var></td> 				 				
              </tr>
			  <tr>
			  <gts:var ifKey="isLoggedIn" value="true">
                <td align="center" valign="middle" id="tdEvolution">&nbsp;</td>
				<td align="center" valign="middle" id="tdEvolution">&nbsp;</td>
				<td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=passwd">${i18n.Password}</a></td>
				<td align="center" valign="middle" id="tdEvolution"><a href="./Track?page=dev.alerts">${i18n.Alert}</a></td> 
              </gts:var>				
				<td align="center" valign="middle" id="tdEvolutionAdmin">
                <gts:var ifKey="accountID" value="sysadmin">
                <a href="./Track?page=laf.info">${i18n.LookandFeel}</a>
                </gts:var></td> 				
              </tr>
			  </table></td>
          </tr>
        </table>
		</div>
</div> <!-- /login -->	

	<!-- The tab on top -->	
	<div class="tab"><gts:var ifTrue="isLoggedIn">
		<ul class="login">
			<li class="left">&nbsp;</li>
			<li><a class="back" href="javascript:history.go(-1)" title="Regresar"></a></li>			
			<li><a class="home" href="./Track?page=menu.top" title="Home">&nbsp;</a></li>
			<li><a class="devmap" href="./Track?page=map.device" title="Mapa de dispositivo">&nbsp;</a></li>			
			<li>${i18n.WELCOME}, <span>${userDesc}!</span><span class="bluetext">&nbsp;(${userID})</li>
			<li class="sep">|</li>
			<li id="toggle">
				<a id="open" class="open" href="#" title="${i18n.OpenMenu}"></a>
				<a id="close" class="close" href="#" style="display: none;"></a>		
			</li>
			<li class="sep" id="tdEvolution2">
            <a href="./Track?page=menu.top" title="${i18n.MenuTitle}">&nbsp;${i18n.Menu}&nbsp;</a>
            </li>
            <li class="sep" id="tdEvolution2">
            <a href="./Track?page=login" title="${i18n.ExitTitle}">&nbsp;${i18n.Exit}&nbsp;</a>
            </li>			
			<li class="right">&nbsp;</li>
		</ul></gts:var>
	</div> <!-- / top -->

</div> <!--panel -->

<div id="systemContainer">
    <gts:var ifKey="isLoggedIn" value="false">
	<div id="dvLoading"></div>
	</gts:var>
    <table width="100%" height="100%" align="center" border="0" cellspacing="0" cellpadding="0">
    <tr>
    <td align="center">

    </td>
    </tr>
        <!-- 2da Fila de Contenido-->
        <tr height="100%">
        <td height="100%" valign="top">
            <table width="100%" height="100%" border="0" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td align="center" valign="top" height="100%">
                        <table class="<gts:track section='content.class.table'/>" border="0" cellspacing="0" cellpadding="0" >
                            <tbody>
                                <tr>
                                	<td height="30px">
                                    <!-- No usado -->
                                    </td>
                                </tr>
                                <tr height="100%">
                                    <td class="<gts:track section='content.class.cell'/>">
                                        <!-- Empieza content body -->
                                        <gts:track section="content.body"/>
                                    </td>
                                </tr>
                                <gts:var ifKey="isLoggedIn" value="true">
                                <tr>
                                    <td id="<gts:track section='content.id.message'/>" class="<gts:track section='content.class.message'/>">
                                        <gts:track section="content.message"/>
                                    </td>
                                </tr>
                                </gts:var>
                            </tbody>
                        </table>
                        
                    </td>
                </tr>
                </tbody>
            </table>
        </td>
      </tr>
      <tr>
		<td>
		<gts:var ifKey="isLoggedIn" value="true">
			<div id="plataformaFooter">
            ${copyright}
            </div>
		</gts:var>
        

		</td>
      </tr>
    </table>
    
<!-- Fin de systemContainer -->
</div>
</body>
</html>