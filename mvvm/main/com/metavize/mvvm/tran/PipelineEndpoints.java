/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;

import java.net.InetAddress;
import java.util.Date;

import com.metavize.mvvm.argon.SessionEndpoints;
import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.policy.Policy;

/**
 * Used to record the Session endpoints at session end time.
 * PipelineStats and PipelineEndpoints used to be the PiplineInfo
 * object.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="PL_ENDP"
 * mutable="false"
 */
public class PipelineEndpoints extends LogEvent
{
    private static final long serialVersionUID = -5787529995276369804L;

    private int sessionId;

    private short protocol;

    private Date createDate;

    private byte clientIntf;
    private byte serverIntf;

    private InetAddress cClientAddr;
    private InetAddress sClientAddr;

    private InetAddress cServerAddr;
    private InetAddress sServerAddr;

    private int cClientPort;
    private int sClientPort;

    private int cServerPort;
    private int sServerPort;

    private Policy policy;
    private boolean policyInbound;

    // constructors -----------------------------------------------------------

    public PipelineEndpoints() { }

    public PipelineEndpoints(IPSessionDesc begin, IPSessionDesc end,
                             Policy policy, boolean policyInbound)
    {
        // Begin & end and all in between have the same ID
        sessionId = begin.id();

        protocol = (short)begin.protocol();

        createDate = new Date();

        cClientAddr = begin.clientAddr();
        cClientPort = begin.clientPort();
        cServerAddr = begin.serverAddr();
        cServerPort = begin.serverPort();

        sClientAddr = end.clientAddr();
        sClientPort = end.clientPort();
        sServerAddr = end.serverAddr();
        sServerPort = end.serverPort();

        clientIntf = begin.clientIntf();
        serverIntf = end.serverIntf();

        this.policy = policy;
        this.policyInbound = policyInbound;
    }

    // This one is called by ArgonHook, just to get an object which is
    // filled in later.
    public PipelineEndpoints(IPSessionDesc begin)
    {
        sessionId = begin.id();
        protocol = (short)begin.protocol();
        createDate = new Date();
        clientIntf = begin.clientIntf();
        // Don't fill in anything that can change later.
    }

    // business methods -------------------------------------------------------

    public void completeEndpoints(IPSessionDesc begin, IPSessionDesc end,
                                  Policy policy, boolean policyInbound)
    {
        cClientAddr = begin.clientAddr();
        cClientPort = begin.clientPort();
        cServerAddr = begin.serverAddr();
        cServerPort = begin.serverPort();
        sClientAddr = end.clientAddr();
        sClientPort = end.clientPort();
        sServerAddr = end.serverAddr();
        sServerPort = end.serverPort();
        serverIntf = end.serverIntf();
        this.policy = policy;
        this.policyInbound = policyInbound;
    }

    public String getDirectionName()
    {
        return policyInbound ? "inbound" : "outbound";
    }

    /* This doesn't really belong here */
    public String getProtocolName()
    {
        switch (protocol) {
        case SessionEndpoints.PROTO_TCP: return "TCP";
        case SessionEndpoints.PROTO_UDP: return "UDP";
        default: return "unknown";
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * Session id.
     *
     * @return the id of the session
     * @hibernate.property
     * column="SESSION_ID"
     */
    public int getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(int sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     *
     * @return the id of the session
     * @hibernate.property
     * column="PROTO"
     */
    public short getProtocol()
    {
        return protocol;
    }

    public void setProtocol(short protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Time the session began
     *
     * @return the time the session began
     * @hibernate.property
     * column="CREATE_DATE"
     */
    public Date getCreateDate()
    {
        return createDate;
    }

    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    /**
     * Client interface number (at client).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the client
     * @hibernate.property
     * column="CLIENT_INTF"
     */
    public byte getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(byte clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    public String getClientIntf(byte clientInf)
    {
        return 0 == clientIntf ? "outside" : "inside";
    }

    /**
     * Server interface number (at server).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the server
     * @hibernate.property
     * column="SERVER_INTF"
     */
    public byte getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf(byte serverIntf)
    {
        this.serverIntf = serverIntf;
    }

    public String getServerIntf(byte serverIntf)
    {
        return 0 == serverIntf ? "outside" : "inside";
    }

    /**
     * Client address, at the client side.
     *
     * @return the address of the client (as seen at client side of pipeline)
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="C_CLIENT_ADDR"
     * sql-type="inet"
     */
    public InetAddress getCClientAddr()
    {
        return cClientAddr;
    }

    public void setCClientAddr(InetAddress cClientAddr)
    {
        this.cClientAddr = cClientAddr;
    }

    /**
     * Client address, at the server side.
     *
     * @return the address of the client (as seen at server side of pipeline)
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="S_CLIENT_ADDR"
     * sql-type="inet"
     */
    public InetAddress getSClientAddr()
    {
        return sClientAddr;
    }

    public void setSClientAddr(InetAddress sClientAddr)
    {
        this.sClientAddr = sClientAddr;
    }

    /**
     * Server address, at the client side.
     *
     * @return the address of the server (as seen at client side of pipeline)
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="C_SERVER_ADDR"
     * sql-type="inet"
     */
    public InetAddress getCServerAddr()
    {
        return cServerAddr;
    }

    public void setCServerAddr(InetAddress cServerAddr)
    {
        this.cServerAddr = cServerAddr;
    }

    /**
     * Server address, at the server side.
     *
     * @return the address of the server (as seen at server side of pipeline)
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="S_SERVER_ADDR"
     * sql-type="inet"
     */
    public InetAddress getSServerAddr()
    {
        return sServerAddr;
    }

    public void setSServerAddr(InetAddress sServerAddr)
    {
        this.sServerAddr = sServerAddr;
    }

    /**
     * Client port, at the client side.
     *
     * @return the port of the client (as seen at client side of pipeline)
     * @hibernate.property
     * column="C_CLIENT_PORT"
     */
    public int getCClientPort()
    {
        return cClientPort;
    }

    public void setCClientPort(int cClientPort)
    {
        this.cClientPort = cClientPort;
    }

    /**
     * Client port, at the server side.
     *
     * @return the port of the client (as seen at server side of pipeline)
     * @hibernate.property
     * column="S_CLIENT_PORT"
     */
    public int getSClientPort()
    {
        return sClientPort;
    }

    public void setSClientPort(int sClientPort)
    {
        this.sClientPort = sClientPort;
    }

    /**
     * Server port, at the client side.
     *
     * @return the port of the server (as seen at client side of pipeline)
     * @hibernate.property
     * column="C_SERVER_PORT"
     */
    public int getCServerPort()
    {
        return cServerPort;
    }

    public void setCServerPort(int cServerPort)
    {
        this.cServerPort = cServerPort;
    }

    /**
     * Server port, at the server side.
     *
     * @return the port of the server (as seen at server side of pipeline)
     * @hibernate.property
     * column="S_SERVER_PORT"
     */
    public int getSServerPort()
    {
        return sServerPort;
    }

    public void setSServerPort(int sServerPort)
    {
        this.sServerPort = sServerPort;
    }

    /**
     * Policy that was applied for this pipeline.
     *
     * @return Policy for this pipeline
     * @hibernate.many-to-one
     * column="POLICY_ID"
     */
    public Policy getPolicy()
    {
        return policy;
    }

    public void setPolicy(Policy policy)
    {
        this.policy = policy;
    }

    /**
     * Was the the inbound side of the policy chosen?  If false, the
     * outbound side was chosen.
     *
     * @return true if the inbound side of policy was chosen, false if outbound
     * @hibernate.property
     * column="POLICY_INBOUND"
     * not-null="true"
     */
    public boolean isInbound()
    {
        return policyInbound;
    }

    public void setInbound(boolean inbound)
    {
        this.policyInbound = inbound;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("endpoints");
        sb.addField("create-date", createDate);
        sb.addField("session-id", sessionId);
        sb.addField("protocol", getProtocolName());

        sb.addField("policy", (( policy == null ) ? "<none>" : policy.getName()));
        sb.addField("policy-direction", getDirectionName());

        sb.addField("client-iface", getClientIntf(clientIntf));
        //Client address, at the client side.
        sb.addField("client-addr", cClientAddr);
        //Client port, at the client side.
        sb.addField("client-port", cClientPort);
        //Server address, at the client side.
        sb.addField("server-addr", cServerAddr);
        //Server port, at the client side.
        sb.addField("server-port", cServerPort);

        sb.addField("server-iface", getServerIntf(serverIntf));
        //Client address, at the server side.
        sb.addField("client-addr", sClientAddr);
        //Client port, at the server side.
        sb.addField("client-port", sClientPort);
        //Server address, at the server side.
        sb.addField("server-addr", sServerAddr);
        //Server port, at the server side.
        sb.addField("server-port", sServerPort);
    }

    // reuse default getSyslogId
    // reuse default getSyslogPriority

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (o instanceof PipelineEndpoints) {
            PipelineEndpoints pe = (PipelineEndpoints)o;
            return sessionId == pe.sessionId;
        } else {
            return false;
        }
    }

    public int hashCode()
    {
        return sessionId;
    }
}
