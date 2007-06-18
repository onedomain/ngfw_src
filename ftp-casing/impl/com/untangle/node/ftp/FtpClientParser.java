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

package com.untangle.node.ftp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenStreamer;
import org.apache.log4j.Logger;

public class FtpClientParser extends AbstractParser
{
    private static final char SP = ' ';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Fitting fitting;

    private final Logger logger = Logger.getLogger(FtpClientParser.class);

    // constructors -----------------------------------------------------------

    FtpClientParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(true);

        Pipeline p = LocalUvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());
        fitting = p.getClientFitting(session.mPipe());
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        if (Fitting.FTP_CTL_STREAM == fitting) {
            return parseCtl(buf);
        } else {
            Chunk c = new Chunk(buf.duplicate());
            List<Token> l = Arrays.asList(new Token[] { c });
            return new ParseResult(l, null);
        }
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        if (Fitting.FTP_DATA_STREAM == fitting) {
            List<Token> l = Arrays.asList(new Token[] { EndMarker.MARKER });
            return new ParseResult(l, null);
        } else {
            if (buf.hasRemaining()) {
                logger.warn("unread data in read buffer: " + buf.remaining());
            }
            return new ParseResult();
        }
    }

    public TokenStreamer endSession() { return null; }

    // private methods --------------------------------------------------------

    private ParseResult parseCtl(ByteBuffer buf) throws ParseException
    {
        if (completeLine(buf)) {
            byte[] ba = new byte[buf.remaining()];
            buf.get(ba);

            FtpFunction fn;
            String arg;
            if (2 == ba.length) {       /* empty line */
                fn = null;
                arg = null;
            } else if (5 > ba.length) { /* weird command */
                String weird = new String(ba);
                logger.warn("strange ftp command: " + weird);
                // XXX we need to do our own 500 response here
                fn = null;
                arg = null;
            } else {                    /* compliant */
                int i = Character.isWhitespace((char)ba[3]) ? 3 : 4;
                String fnStr = new String(ba, 0, i);
                fn = FtpFunction.valueOf(fnStr.toUpperCase());
                if (null == fn) {
                    throw new ParseException("Unknown FTP function: " + fnStr);
                }

                while (SP == ba[++i]);

                arg = (ba.length - 2 <= i) ? null
                    : new String(ba, i, ba.length - i - 2); // no CRLF
            }

            FtpCommand cmd = new FtpCommand(fn, arg);
            List<Token> l = Arrays.asList(new Token[] { cmd });
            return new ParseResult(l, null);
        } else {
            return new ParseResult(buf);
        }
    }

    /**
     * True the buffer contains a complete line <CRLF>. Obsolete line
     * terminators are not accepted.
     *
     * @param buf to check.
     * @return true if a complete line.
     */
    private boolean completeLine(ByteBuffer buf)
    {
        int l = buf.limit();

        return buf.remaining() >= 2 && buf.get(l - 2) == CR
            && buf.get(l - 1) == LF;
    }
}
