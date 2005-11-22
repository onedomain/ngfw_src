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

package com.metavize.tran.openvpn;

import java.io.Serializable;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

/**
 * A VPN group of address and clients.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_group"
 */
public class VpnGroup extends Rule implements Validatable
{
    // XXX update the serial version id
    // private static final long serialVersionUID = 4143567998376955882L;

    /* The interface that clients from the client pool are associated with */
    private byte intf;

    private IPaddr address;
    private IPaddr netmask;

    /**
     * Hibernate constructor.
     */
    public VpnGroup()
    {
    }

    /**
     * Get the pool of addresses for the clients.
     *
     * @return the pool address to send to the client, don't use in bridging mode.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="address"
     * sql-type="inet"
     */
    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress( IPaddr address )
    {
        this.address = address;
    }

    /**
     * Get the pool of netmaskes for the clients, in bridging mode this must come from
     * the pool that the interface is bridged with.
     *
     * @return the pool netmask to send to the client
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="netmask"
     * sql-type="inet"
     */
    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask( IPaddr netmask )
    {
        this.netmask = netmask;
    }

    /* XXX Use a string or byte */
    /**
     * @return Default interface to associate VPN traffic with.
     * column="intf"
     */
    public byte getIntf()
    {
        return this.intf;
    }

    public void setIntf( byte intf )
    {
        this.intf = intf;
    }

    /* This is the name that is used as the common name in the certificate */
    public String getInternalName()
    {
        return getName().trim().toLowerCase();
    }
    
    public void validate() throws ValidateException
    {
        /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
    }

    /*
     * GUI depends on get name for to string to show the list of clients */
    public String toString()
    {
        return getName();
    }
}
