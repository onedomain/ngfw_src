/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyRule;
import com.untangle.uvm.node.Node;
import org.apache.log4j.Logger;

/**
 * Describes the fittings for this pipe.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class PipeSpec
{
    private final Logger logger = Logger.getLogger(getClass());

    private final String name;
    private final Node node;

    private volatile Set<Subscription> subscriptions;
    private volatile boolean enabled = true;

    // constructors -----------------------------------------------------------

    /**
     * Creates a new PipeSpec, with a subscription for all traffic.
     *
     * @param name display name for this MPipe.
     */
    protected PipeSpec(String name, Node node)
    {
        this.name = name;
        this.node = node;

        this.subscriptions = new CopyOnWriteArraySet<Subscription>();
        this.subscriptions.add(new Subscription(Protocol.TCP));
        this.subscriptions.add(new Subscription(Protocol.UDP));
    }

    /**
     * Creates a new PipeSpec.
     *
     * @param name display name of the PipeSpec.
     * @param subscriptions set of Subscriptions.
     */
    protected PipeSpec(String name, Node node, Set subscriptions)
    {
        this.name = name;
        this.node = node;

        this.subscriptions = null == subscriptions
            ? new CopyOnWriteArraySet<Subscription>()
            : new CopyOnWriteArraySet<Subscription>(subscriptions);
    }

    /**
     * Creates a new PipeSpec.
     *
     * @param name display name of the PipeSpec.
     * @param subscription the Subscription.
     */
    protected PipeSpec(String name, Node node,
                       Subscription subscription)
    {
        this.name = name;
        this.node = node;

        this.subscriptions = new CopyOnWriteArraySet<Subscription>();
        if (null != subscription) {
            this.subscriptions.add(subscription);
        }
    }

    // public abstract methods ------------------------------------------------

    public abstract void connectMPipe();
    public abstract void disconnectMPipe();
    public abstract void dumpSessions();
    public abstract IPSessionDesc[] liveSessionDescs();

    // accessors --------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public Node getNode()
    {
        return node;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    // public methods ---------------------------------------------------------

    public boolean matches(PolicyRule pr, com.untangle.uvm.node.IPSessionDesc sd)
    {
        Policy tp = node.getTid().getPolicy();
        Policy p = null == pr ? null : pr.getPolicy();
        boolean sessionInbound = null == pr ? true : pr.isInbound();

        // We want the node if its policy matches, or the node has no
        // policy (is a service).
        if (enabled && (null == tp || tp.equals(p))) {
            Set s = subscriptions;

            for (Iterator i = s.iterator(); i.hasNext(); ) {
                Subscription subscription = (Subscription)i.next();
                if (subscription.matches(sd, sessionInbound)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean matchesPolicy(Policy p)
    {
        Policy tp = node.getTid().getPolicy();

        return null == tp || tp.equals(p);
    }

    public void setSubscriptions(Set<Subscription> subscriptions)
    {
        synchronized (this) {
            this.subscriptions = new CopyOnWriteArraySet<Subscription>(subscriptions);
        }
    }

    public void addSubscription(Subscription subscription)
    {
        synchronized (this) {
            subscriptions.add(subscription);
        }
    }

    public void removeSubscription(Subscription subscription)
    {
        synchronized (this) {
            subscriptions.remove(subscription);
        }
    }
}
