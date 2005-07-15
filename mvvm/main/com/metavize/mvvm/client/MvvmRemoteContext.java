/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.client;

import java.io.IOException;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.ConnectivityTester;
import com.metavize.mvvm.ToolboxManager;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.tran.TransformManager;

/**
 * Provides an interface to get major MVVM components that are
 * accessible a remote client.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface MvvmRemoteContext
{
    /**
     * Get the <code>ToolboxManager</code> singleton.
     *
     * @return the ToolboxManager.
     */
    ToolboxManager toolboxManager();

    /**
     * Get the <code>TransformManager</code> singleton.
     *
     * @return the TransformManager.
     */
    TransformManager transformManager();

    /**
     * Get the <code>LoggingManager</code> singleton.
     *
     * @return the LoggingManager.
     */
    LoggingManager loggingManager();

    /**
     * Get the <code>AdminManager</code> singleton.
     *
     * @return the AdminManager.
     */
    AdminManager adminManager();

    /**
     * Get the <code>ArgonManager</code> singleton.
     *
     * @return the ArgonManager.
     */
    ArgonManager argonManager();

    /**
     * Get the <code>NetworkingManager</code> singleton.
     *
     * @return the NetworkingManager.
     */
    NetworkingManager networkingManager();

    /**
     * Get the <code>ConnectivityTester</code> singleton.
     *
     * @return the ConnectivityTester
     */
    ConnectivityTester getConnectivityTester();

    /**
     * Save settings to local hard drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void localBackup() throws IOException;

    /**
     * Save settings to USB key drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void usbBackup() throws IOException;

    void shutdown();

    // debugging / performance management
    void doFullGC();
}
