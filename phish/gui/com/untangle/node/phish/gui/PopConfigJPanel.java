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


package com.untangle.node.phish.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.spam.*;


public class PopConfigJPanel extends MEditTableJPanel {

    public PopConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        PopTableModel popTableModel = new PopTableModel();
        this.setTableModel( popTableModel );
    }
}


class PopTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 100; /* source */
    private static final int C3_MW = 55;  /* scan */
    private static final int C4_MW = 125; /* action if PHISH detected */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW), 120); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>PHISH detected"));
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, SpamPOPConfig.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector,
                                 boolean validateOnly)
        throws Exception
    {
        SpamPOPConfig spamPopConfig = null;

        for( Vector rowVector : tableVector ){
            spamPopConfig = (SpamPOPConfig) rowVector.elementAt(6);
            spamPopConfig.setScan( (Boolean) rowVector.elementAt(3) );
            spamPopConfig.setMsgAction( (SpamMessageAction) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
            spamPopConfig.setNotes( (String) rowVector.elementAt(5) );
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            SpamSettings spamSettings = (SpamSettings) settings;
            spamSettings.setPopConfig( spamPopConfig );
        }

    }

    public Vector<Vector> generateRows(Object settings) {
        SpamSettings spamSettings = (SpamSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(2);
        int rowIndex = 0;

        rowIndex++;
        Vector row = new Vector(7);
        SpamPOPConfig spamPopConfig = spamSettings.getPopConfig();
        row.add( super.ROW_SAVED );
        row.add( rowIndex );
        row.add( spamPopConfig.getScan() );
        row.add( super.generateComboBoxModel(SpamMessageAction.getValues(), spamPopConfig.getMsgAction()) );
        row.add( spamPopConfig.getNotes() );
        row.add( spamPopConfig );
        allRows.add(row);

        return allRows;
    }
}
