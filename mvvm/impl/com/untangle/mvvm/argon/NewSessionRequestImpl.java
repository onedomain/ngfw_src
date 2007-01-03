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

package com.untangle.mvvm.argon;

import com.untangle.jnetcap.NetcapSession;

import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

abstract class NewSessionRequestImpl implements NewSessionRequest
{
    protected final ArgonAgent    argonAgent;
    protected final SessionGlobalState sessionGlobalState;
    
    NewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent )
    {
        this.sessionGlobalState = sessionGlobalState;
        this.argonAgent         = agent;
    }
    
    public ArgonAgent argonAgent()
    {
        return argonAgent;
    }
    
    public NetcapSession netcapSession()
    {
        return sessionGlobalState.netcapSession();
    }

    public SessionGlobalState sessionGlobalState()
    {
        return sessionGlobalState;
    }

    public int id()
    {
        return sessionGlobalState.id();
    }

    public String user()
    {
        return sessionGlobalState.user();
    }

    /**
     * Number of bytes received from the client.
     */
    public long c2tBytes()
    {
        return sessionGlobalState.clientSideListener().rxBytes;
    }

    /**
     * Number of bytes transmitted to the server.
     */
    public long t2sBytes()
    {
        return sessionGlobalState.serverSideListener().txBytes;
    }

    /**
     * Number of bytes received from the server.
     */
    public long s2tBytes()
    {
        return sessionGlobalState.serverSideListener().rxBytes;
    }
    
    /**
     * Number of bytes transmitted to the client.
     */
    public long t2cBytes()
    {
        return sessionGlobalState.clientSideListener().rxBytes;
    }

    /**
     * Number of chunks received from the client.
     */
    public long c2tChunks()
    {
        return sessionGlobalState.clientSideListener().rxChunks;
    }

    /**
     * Number of chunks transmitted to the server.
     */
    public long t2sChunks()
    {
        return sessionGlobalState.serverSideListener().txChunks;
    }

    /**
     * Number of chunks received from the server.
     */
    public long s2tChunks()
    {
        return sessionGlobalState.serverSideListener().rxChunks;
    }
    
    /**
     * Number of chunks transmitted to the client.
     */
    public long t2cChunks()
    {
        return sessionGlobalState.clientSideListener().rxChunks;
    }
}
