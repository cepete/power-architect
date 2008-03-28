/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.ddl;

import java.util.Arrays;

import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLSequence;

/**
 * A DDLWarning for invalid name of a SQLSequence that can be 
 * fixed by calling setName() on one of the involved objects.
 * It has a side effect of setting the given SQLColumn's 
 * autoIncrementSequenceName as well.
 */
public class InvalidSeqNameDDLWarning extends AbstractDDLWarning {

    protected String whatQuickFixShouldCallIt;
    private final SQLColumn col;

    public InvalidSeqNameDDLWarning(String message,
            SQLSequence seq, SQLColumn col,
            String quickFixMesssage,
            String whatQuickFixShouldCallIt)
    {
        super(Arrays.asList(new SQLObject[]{seq}),
                message, true, quickFixMesssage,
                seq, "name");
        this.col = col;
        this.whatQuickFixShouldCallIt = whatQuickFixShouldCallIt;
    }

    public boolean quickFix() {
        // XXX need differentiator for setName() vs setPhysicalName()
        whichObjectQuickFixFixes.setName(whatQuickFixShouldCallIt);
        col.setAutoIncrementSequenceName(whatQuickFixShouldCallIt);
        return true;
    }
}
