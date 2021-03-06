/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * ReplacementGenerator for Virus.
 */
class VirusReplacementGenerator extends ReplacementGenerator<VirusBlockDetails>
{
// THIS IS FOR ECLIPSE - @formatter:off
    
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site is blocked because it contains a virus.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";
// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Constructor
     * 
     * @param tid
     *        Application settings
     */
    VirusReplacementGenerator(AppSettings tid)
    {
        super(tid);
    }

    /**
     * Get the replacement
     * 
     * @param details
     *        The block details
     * @return The replacement
     */
    @Override
    protected String getReplacement(VirusBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getVendor(), details.getHost(), details.getUri(), details.getReason(), uvm.brandingManager().getContactHtml());
    }

    /**
     * Get the redirect URL
     * 
     * @param nonce
     *        The nonce
     * @param host
     *        The host
     * @param appSettings
     *        The application settings
     * @return The redirect URL
     */
    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        return "http://" + host + "/virus/blockpage?nonce=" + nonce + "&tid=" + appSettings.getId();
    }
}
