/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.synchro.jms;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.engine.bpm.process.ProcessInstanceState;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.events.model.SEvent;

/**
 * @author Baptiste Mesta
 */
public class ProcessInstanceFinishedHandler extends AbstractUpdateHandler {

    private static final long serialVersionUID = 1L;
    
    public ProcessInstanceFinishedHandler(final long tenantId, final long messageTimeout) {
        super(tenantId, messageTimeout);
    }

    @Override
    protected Map<String, Serializable> getEvent(final SEvent sEvent) {
        final SProcessInstance instance = (SProcessInstance) sEvent.getObject();
        return PerfEventUtil.getProcessInstanceFinishedEvent(instance.getId());
    }

    @Override
    public boolean isInterested(final SEvent event) {
        final Object object = event.getObject();
        if (object instanceof SProcessInstance) {
    		final SProcessInstance pi = (SProcessInstance) event.getObject();
    		boolean result = pi.getStateId() == ProcessInstanceState.COMPLETED.getId();
    		return result;
        }
        return false;
    }

}