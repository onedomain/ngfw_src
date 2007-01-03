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
package com.untangle.tran.spamassassin;

import com.untangle.tran.spam.SpamImpl;

public class SpamAssassinTransform extends SpamImpl
{
    public SpamAssassinTransform()
    {
        super(new SpamAssassinScanner());
    }
}
