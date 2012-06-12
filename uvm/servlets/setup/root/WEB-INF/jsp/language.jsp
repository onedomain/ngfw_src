<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" %>

<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>

<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:uvm="http://java.untangle.com/jsp/uvm">
  <head>
    <title>Setup Wizard</title>
    <META content="IE=7.0000" http-equiv="X-UA-Compatible"/>    
    <style type="text/css">
        @import "/ext4/resources/css/ext-all-gray.css?s=${buildStamp}";
        @import "/ext4/examples/ux/css/CheckHeader.css?s=${buildStamp}";
    </style>
    
    <!--     <uvm:skin src="ext-skin.css"  name="${skinSettings.skinName}"/> -->
    <uvm:skin src="admin.css?s=${buildStamp}"  name="${skinSettings.skinName}"/>

    <script type="text/javascript" src="/ext4/builds/ext-core-debug.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext4/ext-all-debug.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/data/PagingMemoryProxy.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/CheckColumn.js?s=${buildStamp}"></script>
    
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/language.js?s=${buildStamp}"></script>

<c:if test="${param['console']==1}">
    <script type="text/javascript">
    if("http:"==window.location.protocol) {
        top.window.moveTo(1,1);
        if(Ext.isIE) {
            top.window.resizeTo(screen.availWidth,screen.availHeight);
        } else {
            top.window.outerHeight = top.screen.availHeight-30;
            top.window.outerWidth = top.screen.availWidth-5;
        }
    }
    </script>
</c:if>
    
    <script type="text/javascript">
      Ung.SetupWizard.currentSkin = "${skinSettings.skinName}";
      Ung.SetupWizard.CurrentValues = {
        languageList : ${languageList},
        language : "${language}"
      };

      Ext.onReady(Ung.Language.init);
    </script>
  </head>

  <body class="wizard">
    <div id="container">
      <!-- These extra divs/spans may be used as catch-alls to add extra imagery. -->
      <div id="extra-div-1"><span></span></div>
      <div id="extra-div-2"><span></span></div>
      <div id="extra-div-3"><span></span></div>
      <div id="extra-div-4"><span></span></div>
      <div id="extra-div-5"><span></span></div>
      <div id="extra-div-6"><span></span></div>
    </div>
  </body>
</html>
