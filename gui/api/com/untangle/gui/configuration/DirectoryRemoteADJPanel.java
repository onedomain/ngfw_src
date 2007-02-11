/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.configuration;

import com.untangle.mvvm.addrbook.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.snmp.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.user.WMISettings;

import java.awt.*;
import javax.swing.*;

public class DirectoryRemoteADJPanel extends javax.swing.JPanel
    implements Savable<DirectoryCompoundSettings>, Refreshable<DirectoryCompoundSettings> {

    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostname\" must be specified if \"Login\" or \"Password\" are specified.";
    private static final String EXCEPTION_DOMAIN_MISSING = "A \"Search Base\" must be specified.";
    private static final String EXCEPTION_SERVER_ADDRESS = "You must specify a valid IP address for your Lookup Server.";

    public DirectoryRemoteADJPanel() {
        initComponents();
	Util.setPortView(portJSpinner, 25);
    }

    public void doSave(DirectoryCompoundSettings directoryCompoundSettings, boolean validateOnly) throws Exception {

	// ENABLED //
	boolean enabled = adEnabledJRadioButton.isSelected();

	// HOSTNAME ///////
	String host = hostJTextField.getText();

	// PORT //////
	int port = 0;
	if( enabled ){
	    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	    try{ portJSpinner.commitEdit(); }
	    catch(Exception e){ 
		((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
		throw new Exception(Util.EXCEPTION_PORT_RANGE);
	    }
	    port = (Integer) portJSpinner.getValue();
	}

	// LOGIN /////
	String login = loginJTextField.getText();

	// PASSWORD /////
	String password = new String(passwordJPasswordField.getPassword());

        // DOMAIN /////
        String domain = baseJTextField.getText();

	// ORG //
	String org = orgJTextField.getText();

	if( enabled ){
	    // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED /////
	    passwordJPasswordField.setBackground( Color.WHITE );
	    loginJTextField.setBackground( Color.WHITE );
	    if( (login.length() > 0) && (password.length() == 0) ){
		passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_PASSWORD_MISSING);
	    }
	    else if( (login.length() == 0) && (password.length() > 0) ){
		loginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_LOGIN_MISSING);
	    }
	    
	    // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
	    hostJTextField.setBackground( Color.WHITE );
	    if( (login.length() > 0) && (password.length() > 0) && (host.length() == 0) ){
		hostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_HOSTNAME_MISSING);
	    }
	    
	    // CHECK THAT A DOMAIN IS SUPPLIED
	    baseJTextField.setBackground( Color.WHITE );
	    if( domain.length() == 0 ){
		baseJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DOMAIN_MISSING);
	    }
    }
        
        // SERVER ENABLED
        boolean serverEnabled = serverEnabledJRadioButton.isSelected();

        // SERVER ADDRESS
        IPaddr serverIPaddr = null;
        serverIPJTextField.setBackground( Color.WHITE );
        if( enabled && serverEnabled ){
            try{ serverIPaddr = IPaddr.parse(serverIPJTextField.getText()); }
            catch(Exception e){ throw new Exception (EXCEPTION_SERVER_ADDRESS); }
            if( serverIPaddr.isEmpty() )
                throw new Exception(EXCEPTION_SERVER_ADDRESS);
        }


	// SAVE SETTINGS ////////////
	if( !validateOnly ){	    
	    if( enabled ){
		directoryCompoundSettings.setAddressBookConfiguration( AddressBookConfiguration.AD_AND_LOCAL );
		RepositorySettings repositorySettings = directoryCompoundSettings.getAddressBookSettings().getADRepositorySettings();		
		repositorySettings.setSuperuser( login );
		repositorySettings.setSuperuserPass( password );
		repositorySettings.setLDAPHost( host );
		repositorySettings.setLDAPPort( port );
		repositorySettings.setDomain( domain );
		repositorySettings.setOUFilter( org );
        
                WMISettings wmiSettings = directoryCompoundSettings.getWMISettings();
                wmiSettings.setIsEnabled( serverEnabled );
                if( serverEnabled ){
                    wmiSettings.setUsername( login );
                    wmiSettings.setPassword( password );
                    wmiSettings.setAddress( serverIPaddr );
                    // rbs unecessary: wmiSettings.setPort( wmiPort );
                    // rbs unecessary: wmiSettings.setScheme( "https" );
                }
	    }
	    else{
		directoryCompoundSettings.setAddressBookConfiguration( AddressBookConfiguration.LOCAL_ONLY );
	    }
        }

    }

    private boolean enabledCurrent;
    private String hostCurrent;
    private int portCurrent;
    private String loginCurrent;
    private String passwordCurrent;
    private String domainCurrent;
    private String orgCurrent;
    private boolean serverEnabledCurrent;
    private String serverAddressCurrent;
    private String serverURLCurrent;

    public void doRefresh(DirectoryCompoundSettings directoryCompoundSettings){
	RepositorySettings repositorySettings = directoryCompoundSettings.getAddressBookSettings().getADRepositorySettings();
	AddressBookConfiguration addressBookConfiguration = directoryCompoundSettings.getAddressBookConfiguration();

	// AD ENABLED //
	enabledCurrent = addressBookConfiguration.equals( AddressBookConfiguration.AD_AND_LOCAL );
	if( enabledCurrent )
	    adEnabledJRadioButton.setSelected( true );
	else
	    adDisabledJRadioButton.setSelected( true );
	adEnabledDependency( enabledCurrent );

	// HOST /////
	hostCurrent = repositorySettings.getLDAPHost();
	hostJTextField.setText( hostCurrent );
	hostJTextField.setBackground( Color.WHITE );

	// PORT /////
	portCurrent = repositorySettings.getLDAPPort();
	portJSpinner.setValue( portCurrent );
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

	// LOGIN //////
	loginCurrent = repositorySettings.getSuperuser();
	loginJTextField.setText( loginCurrent );
	loginJTextField.setBackground( Color.WHITE );

	// PASSWORD /////
	passwordCurrent = repositorySettings.getSuperuserPass();
	passwordJPasswordField.setText( passwordCurrent );
	passwordJPasswordField.setBackground( Color.WHITE );

	// DOMAIN //////
	domainCurrent = repositorySettings.getDomain();
	baseJTextField.setText( domainCurrent );	
	baseJTextField.setBackground( Color.WHITE );

	// ORG //
	orgCurrent = repositorySettings.getOUFilter();
	orgJTextField.setText( orgCurrent );
	orgJTextField.setBackground( Color.WHITE );

    // TEST BUTTON //
    if( enabledCurrent )
        adTestJButton.setEnabled( true );

    // SERVER ENABLED
	serverEnabledCurrent = directoryCompoundSettings.getWMISettings().getIsEnabled();
	if( serverEnabledCurrent )
	    serverEnabledJRadioButton.setSelected( true );
	else
	    serverDisabledJRadioButton.setSelected( true );
	serverEnabledDependency( serverEnabledCurrent );
    
    // SERVER ADDRESS
    serverIPJTextField.setText( directoryCompoundSettings.getWMISettings().getAddress().toString() );
    serverIPJTextField.setBackground( Color.WHITE );

    // SERVER URL
    urlJTextField.setText( directoryCompoundSettings.getWMISettings().getUrl() );
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                adButtonGroup = new javax.swing.ButtonGroup();
                serverButtonGroup = new javax.swing.ButtonGroup();
                externalRemoteJPanel = new javax.swing.JPanel();
                enableRemoteJPanel = new javax.swing.JPanel();
                serverJLabel1 = new javax.swing.JLabel();
                adDisabledJRadioButton = new javax.swing.JRadioButton();
                adEnabledJRadioButton = new javax.swing.JRadioButton();
                restrictIPJPanel = new javax.swing.JPanel();
                hostJLabel = new javax.swing.JLabel();
                hostJTextField = new javax.swing.JTextField();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                loginJLabel = new javax.swing.JLabel();
                loginJTextField = new javax.swing.JTextField();
                passwordJLabel = new javax.swing.JLabel();
                passwordJPasswordField = new javax.swing.JPasswordField();
                jSeparator2 = new javax.swing.JSeparator();
                restrictIPJPanel1 = new javax.swing.JPanel();
                baseJLabel = new javax.swing.JLabel();
                baseJTextField = new javax.swing.JTextField();
                orgJLabel = new javax.swing.JLabel();
                orgJTextField = new javax.swing.JTextField();
                orgOptionalJLabel = new javax.swing.JLabel();
                jSeparator3 = new javax.swing.JSeparator();
                testJLabel = new javax.swing.JLabel();
                adTestJButton = new javax.swing.JButton();
                jSeparator4 = new javax.swing.JSeparator();
                serverJLabel = new javax.swing.JLabel();
                serverDisabledJRadioButton = new javax.swing.JRadioButton();
                serverEnabledJRadioButton = new javax.swing.JRadioButton();
                restrictIPJPanel2 = new javax.swing.JPanel();
                serverIPJLabel = new javax.swing.JLabel();
                serverIPJTextField = new javax.swing.JTextField();
                urlJLabel = new javax.swing.JLabel();
                urlJTextField = new javax.swing.JTextField();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 545));
                setMinimumSize(new java.awt.Dimension(563, 545));
                setPreferredSize(new java.awt.Dimension(563, 545));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Active Directory (AD) Server", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                serverJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                serverJLabel1.setText("<html>This allows the Untangle Server to connect to an <b>Active Directory Server</b> in order to recognize various users for use in reporting, firewall, router, policies, etc.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 0);
                enableRemoteJPanel.add(serverJLabel1, gridBagConstraints);

                adButtonGroup.add(adDisabledJRadioButton);
                adDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                adDisabledJRadioButton.setText("<html><b>Disabled</b></html>");
                adDisabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
                adDisabledJRadioButton.setFocusPainted(false);
                adDisabledJRadioButton.setFocusable(false);
                adDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                adDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                enableRemoteJPanel.add(adDisabledJRadioButton, gridBagConstraints);

                adButtonGroup.add(adEnabledJRadioButton);
                adEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                adEnabledJRadioButton.setText("<html><b>Enabled</b></html>");
                adEnabledJRadioButton.setFocusPainted(false);
                adEnabledJRadioButton.setFocusable(false);
                adEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                adEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                enableRemoteJPanel.add(adEnabledJRadioButton, gridBagConstraints);

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                hostJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostJLabel.setText("Host:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(hostJLabel, gridBagConstraints);

                hostJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                hostJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                hostJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                hostJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                hostJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(hostJTextField, gridBagConstraints);

                portJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel.setText("Port:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(portJLabel, gridBagConstraints);

                portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                portJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
                portJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                portJSpinnerStateChanged(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(portJSpinner, gridBagConstraints);

                loginJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                loginJLabel.setText("Authentication Login:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(loginJLabel, gridBagConstraints);

                loginJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                loginJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                loginJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                loginJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                loginJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(loginJTextField, gridBagConstraints);

                passwordJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                passwordJLabel.setText("Authentication Password:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(passwordJLabel, gridBagConstraints);

                passwordJPasswordField.setMaximumSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.setMinimumSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.setPreferredSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                passwordJPasswordFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(passwordJPasswordField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 114, 5, 0);
                enableRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

                jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                enableRemoteJPanel.add(jSeparator2, gridBagConstraints);

                restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

                baseJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                baseJLabel.setText("Active Directory Domain:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(baseJLabel, gridBagConstraints);

                baseJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                baseJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                baseJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                baseJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                baseJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel1.add(baseJTextField, gridBagConstraints);

                orgJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                orgJLabel.setText("Active Directory Organization:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
                restrictIPJPanel1.add(orgJLabel, gridBagConstraints);

                orgJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                orgJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                orgJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                orgJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                orgJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
                restrictIPJPanel1.add(orgJTextField, gridBagConstraints);

                orgOptionalJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                orgOptionalJLabel.setText(" (Optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
                restrictIPJPanel1.add(orgOptionalJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 87, 5, 0);
                enableRemoteJPanel.add(restrictIPJPanel1, gridBagConstraints);

                jSeparator3.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                enableRemoteJPanel.add(jSeparator3, gridBagConstraints);

                testJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                testJLabel.setText("<html>The <b>Active Directory Test</b> can be used to test that your settings above are correct.  If you have made changes to the above settings, you must save them before this button will be enabled.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                enableRemoteJPanel.add(testJLabel, gridBagConstraints);

                adTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                adTestJButton.setText("Run Active Directory Test");
                adTestJButton.setFocusPainted(false);
                adTestJButton.setFocusable(false);
                adTestJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                adTestJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                enableRemoteJPanel.add(adTestJButton, gridBagConstraints);

                jSeparator4.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                enableRemoteJPanel.add(jSeparator4, gridBagConstraints);

                serverJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                serverJLabel.setText("<html>The <b>Active Directory Lookup Server</b> can be used to determine which users are logged into which machines for policies, reporting, etc.  You must download the installer using the URL below, install the server, and then specify the IP address of the server in the field below.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                enableRemoteJPanel.add(serverJLabel, gridBagConstraints);

                serverButtonGroup.add(serverDisabledJRadioButton);
                serverDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                serverDisabledJRadioButton.setText("<html><b>Disabled</b></html>");
                serverDisabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
                serverDisabledJRadioButton.setFocusPainted(false);
                serverDisabledJRadioButton.setFocusable(false);
                serverDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                serverDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                enableRemoteJPanel.add(serverDisabledJRadioButton, gridBagConstraints);

                serverButtonGroup.add(serverEnabledJRadioButton);
                serverEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                serverEnabledJRadioButton.setText("<html><b>Enabled</b></html>");
                serverEnabledJRadioButton.setFocusPainted(false);
                serverEnabledJRadioButton.setFocusable(false);
                serverEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                serverEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                enableRemoteJPanel.add(serverEnabledJRadioButton, gridBagConstraints);

                restrictIPJPanel2.setLayout(new java.awt.GridBagLayout());

                serverIPJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                serverIPJLabel.setText("Server IP Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel2.add(serverIPJLabel, gridBagConstraints);

                serverIPJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                serverIPJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                serverIPJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel2.add(serverIPJTextField, gridBagConstraints);

                urlJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                urlJLabel.setText("URL to download installer:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel2.add(urlJLabel, gridBagConstraints);

                urlJTextField.setMaximumSize(new java.awt.Dimension(250, 19));
                urlJTextField.setMinimumSize(new java.awt.Dimension(250, 19));
                urlJTextField.setPreferredSize(new java.awt.Dimension(250, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel2.add(urlJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 80, 5, 0);
                enableRemoteJPanel.add(restrictIPJPanel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                externalRemoteJPanel.add(enableRemoteJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void serverEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverEnabledJRadioButtonActionPerformed
            serverEnabledDependency(true);        
		}//GEN-LAST:event_serverEnabledJRadioButtonActionPerformed

		private void serverDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverDisabledJRadioButtonActionPerformed
            serverEnabledDependency(false);        
		}//GEN-LAST:event_serverDisabledJRadioButtonActionPerformed

		private void orgJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_orgJTextFieldCaretUpdate
            if( !orgJTextField.getText().trim().equals( orgCurrent ) )
                adTestJButton.setEnabled( false );
		}//GEN-LAST:event_orgJTextFieldCaretUpdate

		private void adTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adTestJButtonActionPerformed
            if( Util.getIsDemo() )
                return;
            try{
                DirectoryADConnectivityTestJDialog testJDialog = new DirectoryADConnectivityTestJDialog((JDialog)this.getTopLevelAncestor());
                testJDialog.setVisible(true);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error running AD Test.", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error running AD Test.", f); }
            }
		}//GEN-LAST:event_adTestJButtonActionPerformed

    private void adEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adEnabledJRadioButtonActionPerformed
        adEnabledDependency( true );
    }//GEN-LAST:event_adEnabledJRadioButtonActionPerformed
    
    private void adDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adDisabledJRadioButtonActionPerformed
        adEnabledDependency( false );
    }//GEN-LAST:event_adDisabledJRadioButtonActionPerformed
    
    private void portJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portJSpinnerStateChanged
        adTestJButton.setEnabled( false );
    }//GEN-LAST:event_portJSpinnerStateChanged
    
    private void hostJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_hostJTextFieldCaretUpdate
        if( !hostJTextField.getText().trim().equals( hostCurrent ) )
            adTestJButton.setEnabled( false );
    }//GEN-LAST:event_hostJTextFieldCaretUpdate
    
    private void loginJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_loginJTextFieldCaretUpdate
        if( !loginJTextField.getText().trim().equals( loginCurrent ) )
            adTestJButton.setEnabled( false );
    }//GEN-LAST:event_loginJTextFieldCaretUpdate
    
    private void passwordJPasswordFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_passwordJPasswordFieldCaretUpdate
        String password = new String(passwordJPasswordField.getPassword());
        if( !password.trim().equals(passwordCurrent) )
            adTestJButton.setEnabled( false );
    }//GEN-LAST:event_passwordJPasswordFieldCaretUpdate
    
    private void baseJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_baseJTextFieldCaretUpdate
        if( !baseJTextField.getText().trim().equals( domainCurrent ) )
            adTestJButton.setEnabled( false );
    }//GEN-LAST:event_baseJTextFieldCaretUpdate
    
    private void adEnabledDependency(boolean enabled){
	hostJTextField.setEnabled( enabled );
	hostJLabel.setEnabled( enabled );
	portJSpinner.setEnabled( enabled );
	portJLabel.setEnabled( enabled );
	loginJTextField.setEnabled( enabled );
	loginJLabel.setEnabled( enabled );
	passwordJPasswordField.setEnabled( enabled );
	passwordJLabel.setEnabled( enabled );
	baseJTextField.setEnabled( enabled );
	baseJLabel.setEnabled( enabled );
	orgJTextField.setEnabled( enabled );
	orgJLabel.setEnabled( enabled );
    orgOptionalJLabel.setEnabled( enabled );
    if(!enabled)
        adTestJButton.setEnabled( false );
    
    serverEnabledJRadioButton.setEnabled( enabled );
    serverDisabledJRadioButton.setEnabled( enabled );
    if( !enabled )
        serverEnabledDependency( false );
    else if( serverEnabledJRadioButton.isSelected() )
        serverEnabledDependency( true );
    }

    private void serverEnabledDependency(boolean enabled){        
        serverJLabel.setEnabled( enabled );
        serverIPJLabel.setEnabled( enabled );
        serverIPJTextField.setEnabled( enabled );
        urlJLabel.setEnabled( enabled );
        urlJTextField.setEnabled( enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.ButtonGroup adButtonGroup;
        public javax.swing.JRadioButton adDisabledJRadioButton;
        public javax.swing.JRadioButton adEnabledJRadioButton;
        private javax.swing.JButton adTestJButton;
        private javax.swing.JLabel baseJLabel;
        public javax.swing.JTextField baseJTextField;
        private javax.swing.JPanel enableRemoteJPanel;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JLabel hostJLabel;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JSeparator jSeparator3;
        private javax.swing.JSeparator jSeparator4;
        private javax.swing.JLabel loginJLabel;
        public javax.swing.JTextField loginJTextField;
        private javax.swing.JLabel orgJLabel;
        public javax.swing.JTextField orgJTextField;
        private javax.swing.JLabel orgOptionalJLabel;
        private javax.swing.JLabel passwordJLabel;
        private javax.swing.JPasswordField passwordJPasswordField;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel1;
        private javax.swing.JPanel restrictIPJPanel2;
        private javax.swing.ButtonGroup serverButtonGroup;
        public javax.swing.JRadioButton serverDisabledJRadioButton;
        public javax.swing.JRadioButton serverEnabledJRadioButton;
        private javax.swing.JLabel serverIPJLabel;
        public javax.swing.JTextField serverIPJTextField;
        private javax.swing.JLabel serverJLabel;
        private javax.swing.JLabel serverJLabel1;
        private javax.swing.JLabel testJLabel;
        private javax.swing.JLabel urlJLabel;
        public javax.swing.JTextField urlJTextField;
        // End of variables declaration//GEN-END:variables
    

}
