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

package com.untangle.tran.protofilter.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.logging.EventRepository;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.protofilter.*;

public class LogJPanel extends MLogTableJPanel {

    private static final String BLOCKED_EVENTS_STRING = "Protocol blocked events";

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final ProtoFilter protoFilter = (ProtoFilter)logTransform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<ProtoFilterLogEvent> em = protoFilter.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<ProtoFilterLogEvent> eventManager = protoFilter.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        ProtoFilter protoFilter = (ProtoFilter)logTransform;
        EventManager<ProtoFilterLogEvent> em = protoFilter.getEventManager();
        EventRepository<ProtoFilterLogEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }


    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  100, true,  false, false, true,  String.class, null, "request" );
            addTableColumn( tableColumnModel,  4,  150, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, IPPortString.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<ProtoFilterLogEvent> logList = (List<ProtoFilterLogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for( ProtoFilterLogEvent log : logList ){
                PipelineEndpoints pe = log.getPipelineEndpoints();
                event = new Vector(7);
		event.add( log.getTimeStamp() );
                event.add( log.isBlocked() ? "blocked" : "passed" );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( log.getProtocol() );
                event.add( log.isBlocked() ? "blocked in block list" : "not blocked in block list");
                event.add( null == pe ? "" : pe.getDirectionName() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }

    }

}
