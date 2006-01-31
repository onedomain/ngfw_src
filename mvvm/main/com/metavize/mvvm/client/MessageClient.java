/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.client;

import com.metavize.mvvm.MessageQueue;
import com.metavize.mvvm.toolbox.ToolboxMessage;
import com.metavize.mvvm.toolbox.ToolboxMessageVisitor;
import org.apache.log4j.Logger;

public class MessageClient
{
    private final MvvmRemoteContext mvvmContext;
    private final PollWorker pollWorker = new PollWorker();
    private final Logger logger = Logger.getLogger(getClass());

    private volatile ToolboxMessageVisitor toolboxMessageVisitor;

    // constructors -----------------------------------------------------------

    public MessageClient(MvvmRemoteContext mvvmContext)
    {
        this.mvvmContext = mvvmContext;
    }

    // accessors --------------------------------------------------------------

    public void setToolboxMessageVisitor(ToolboxMessageVisitor v)
    {
        this.toolboxMessageVisitor = v;
    }

    // lifecycle methods ------------------------------------------------------

    public void start()
    {
        pollWorker.start();
    }

    public void stop()
    {
        pollWorker.stop();
    }

    // inner classes ----------------------------------------------------------

    private class PollWorker implements Runnable
    {
        private volatile Thread thread;

        public void run()
        {
            Thread t = Thread.currentThread();

            MessageQueue<ToolboxMessage> toolQ = mvvmContext.toolboxManager()
                .subscribe();

            while (thread == t) {
                ToolboxMessageVisitor tmv = toolboxMessageVisitor;

                if (null != tmv) {
                    for (ToolboxMessage msg : toolQ.getMessages()) {
                        msg.accept(toolboxMessageVisitor);
                    }
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException exn) {
                    /* reevaluate loop condition */
                }
            }
        }

        public synchronized void start()
        {
            if (null != thread) {
                logger.warn("MessageClient already running");
            } else {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.start();
            }
        }

        public synchronized void stop()
        {
            if (null != thread) {
                logger.warn(("MessageClient not running"));
            } else {
                Thread t = thread;
                thread = null;
                t.interrupt();
            }
        }
    }
}
