/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spam.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.spam.*;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.node.Node;

public class LogJPanel extends MLogTableJPanel {

    private static final String SPAM_EVENTS_STRING = "Spam detected events";

    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);

        final SpamNode spam = (SpamNode)logNode;

        setTableModel(new LogTableModel());

        EventManager<SpamEvent> eventManager = spam.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        SpamNode spam = (SpamNode)logNode;
        EventManager<SpamEvent> em = spam.getEventManager();
        EventRepository<SpamEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ           def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,   90, true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  100, true,  false, false, true,  String.class, null, "subject" );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, "receiver" );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, "sender" );
            addTableColumn( tableColumnModel,  6,   55, true,  false, false, false, Float.class,  null, sc.html("SPAM<br>score") );
            addTableColumn( tableColumnModel,  8,  165, true,  false, false, false, IPPortString.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(Object settings){
            List<SpamEvent> requestLogList = (List<SpamEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( SpamEvent requestLog : requestLogList ){
                event = new Vector(9);
                event.add( requestLog.getTimeStamp() );
                event.add( requestLog.getActionName() );
                event.add( new IPPortString(requestLog.getClientAddr(), requestLog.getClientPort()) );
                event.add( requestLog.getSubject() );
                event.add( requestLog.getReceiver() );
                event.add( requestLog.getSender() );
                event.add( requestLog.getScore() );
                event.add( new IPPortString(requestLog.getServerAddr(), requestLog.getServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }
    }

}
