/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SaveProceedDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.widgets.dialogs;


/**
 *
 * @author inieves
 */
final public class SaveProceedDialog extends MTwoButtonJDialog {
    
    public SaveProceedDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        this.cancelJButton.setText("<html><b>Cancel</b> save</html>");
        this.proceedJButton.setText("<html><b>Continue</b> saving</html>");
        messageJLabel.setText("<html><center>" + applianceName + " is about to save its settings.  These settings are critical to proper network operation and you should be sure these are the settings you want.<br><br><b>Would you like to proceed?<b></center></html>");
        this.setVisible(true);
    }
    
}
