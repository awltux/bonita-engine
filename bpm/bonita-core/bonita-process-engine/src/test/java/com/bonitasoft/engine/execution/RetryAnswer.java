/*******************************************************************************
 * Copyright (C) 2009, 2013-2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.execution;

import org.bonitasoft.engine.persistence.SRetryableException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class RetryAnswer implements Answer<Void> {

    private int count = 0;

    public int getCount() {
        return count;
    }

    @SuppressWarnings("unused")
    @Override
    public Void answer(final InvocationOnMock invocation) throws Throwable {
        count++;
        throw new SRetryableException(null);
    }

}
