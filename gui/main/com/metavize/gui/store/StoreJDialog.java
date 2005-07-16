/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: StoreJDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.store;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;


/**
 *
 * @author  inieves
 */
public class StoreJDialog extends MConfigJDialog implements Savable, Refreshable {

    private static final String NAME_STORE = "Procure a Software Appliance";

    private StoreJPanel storeJPanel;
    private boolean purchasedTransform = false;
    private MTransformJButton mTransformJButton;
    private GridBagConstraints gridBagConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
    
    
    public StoreJDialog( MTransformJButton mTransformJButton ) {
        this.mTransformJButton = mTransformJButton;

        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 480);

        // INIT GENERAL GUI
        storeJPanel = new StoreJPanel(mTransformJButton.getWebpage());
        this.contentJTabbedPane.addTab(NAME_STORE, null, storeJPanel);
        this.setTitle(NAME_STORE);
        setResizable(false);
        
        storeJPanel.mTransformJPanel.add(mTransformJButton, gridBagConstraints);
        storeJPanel.descriptionJTextArea.setText(mTransformJButton.getFullDescription());
        
            
        this.reloadJButton.setText("<html><b>Cancel</b></html>");
        this.saveJButton.setText("<html><b>Procure</b></html>");

	super.savableMap.put(NAME_STORE, this);
	super.refreshableMap.put(NAME_STORE, this);
    }

    public void generateGui(){}
    public void refreshSettings(){}
    public void sendSettings(Object settings) throws Exception {}
    

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        this.purchasedTransform = true;
        
        if(Util.getIsDemo())
            this.purchasedTransform = false;
        
        this.windowClosing(null);
    }

    public void doRefresh(Object settings){
        this.windowClosing(null);	
    }

    public MTransformJButton getPurchasedMTransformJButton(){
        if(purchasedTransform)
            return mTransformJButton;
        else
            return null;
    }
}
