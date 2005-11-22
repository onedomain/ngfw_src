/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.script;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.apache.log4j.Logger;

public class ScriptWriter
{
    private static final Logger logger = Logger.getLogger( ScriptWriter.class );
    
    public static final String COMMENT         = "#";

    /* Split so that other script writers can use the constant */
    public static final String METAVIZE_HEADER = "AUTOGENERATED BY METAVIZE DO NOT MODIFY\n\n";
    private static final String SCRIPT_HEADER   = "#!/bin/sh\n\n" + COMMENT + METAVIZE_HEADER;
    
    private static final String EXPORT_FLAG     = "export";

    private static final String EMPTY_HEADER[]  = new String[0];

    private final StringBuilder sb;

    public ScriptWriter()
    {
        this( EMPTY_HEADER );
    }

    public ScriptWriter( String header[] )
    {
        this.sb = new StringBuilder();
        this.sb.append( header());

        appendLines( header );
    }

    public void appendLine()
    {
        this.sb.append( "\n" );
    }

    /** Kind of a silly helper functions to avoid "\n" everywhere " */
    public void appendLine( String text )
    {
        this.sb.append( text + "\n" );
    }

    public void appendLines( String text[] )
    {
        for ( String line : text ) appendLine( line );
    }

    public void appendComment( String text )
    {
        for ( String line : text.split( "\n" )) this.sb.append( comment() + " " + line + "\n" );
    }
    
    // Really only for shell scripts
    public void appendVariable( String variable, String value, boolean isGlobal )
    {
        if (( variable == null ) || ( value == null )) {
            logger.warn( "NULL variable[" + variable +"] or value[" + variable + "], ignoring" );
            return;
        }
        
        variable = variable.trim();
        value    = value.trim();

        if ( variable.length() == 0 ) {
            /* This is a jenky way to get a stack trace */
            logger.warn( "Empty variable name, ignoring", new Exception());
            return;
        }

        appendLine((( isGlobal ) ? EXPORT_FLAG + " " : "" ) + variable + "=\"" + value + "\"" );
    }

    public void appendVariable( String variable, String value )
    {
        appendVariable( variable, value, false );
    }

    /* Structured this way so different script writers can use different comment indicators */
    protected String comment()
    {
        return COMMENT;
    }

    protected String header()
    {
        return SCRIPT_HEADER;
    }

    public String getContents()
    {
        return sb.toString();
    }

    public void writeFile( String fileName ) 
    {
        BufferedWriter out = null;
        
        /* Open up the interfaces file */
        try {
            String data = sb.toString();
            
            out = new BufferedWriter(new FileWriter( fileName ));
            out.write( data, 0, data.length());
        } catch ( Exception ex ) {
            /* XXX May need to catch this exception, restore defaults
             * then try again */
            logger.error( "Error writing file " + fileName + ":", ex );
        }
        
        try {
            if ( out != null ) out.close();
        } catch ( Exception ex ) {
            logger.error( "Unable to close file", ex );
        }
    }
}
