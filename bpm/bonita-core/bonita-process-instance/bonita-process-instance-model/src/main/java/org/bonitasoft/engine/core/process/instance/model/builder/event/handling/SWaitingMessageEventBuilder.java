/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.instance.model.builder.event.handling;

import org.bonitasoft.engine.core.process.instance.model.event.handling.SWaitingMessageEvent;

/**
 * @author Elias Ricken de Medeiros
 * @author Celine Souchet
 */
public interface SWaitingMessageEventBuilder extends SWaitingEventKeyProvider, SCorrelationContainerBuilder {

    public static final int PROGRESS_FREE_KEY = 0;

    public static final int PROGRESS_IN_TREATMENT_KEY = 1;

    SWaitingMessageEventBuilder createNewWaitingMessageStartEventInstance(long processdefinitionId, final String messageName, final String processName,
            final long flowNodeDefinitionId, final String flowNodeName);

    SWaitingMessageEventBuilder createNewWaitingMessageEventSubProcInstance(final long processdefinitionId, final long parentProcessInstanceId,
            final long rootProcessInstanceId, final String messageName, final String processName, final long flowNodeDefinitionId, final String flowNodeName,
            final long subProcessId);

    SWaitingMessageEventBuilder createNewWaitingMessageIntermediateEventInstance(long processdefinitionId, final long processInstanceId,
            final long flowNodeInstanceId, final String messageName, final String processName, final long flowNodeDefinitionId, final String flowNodeName);

    SWaitingMessageEventBuilder createNewWaitingMessageBoundaryEventInstance(long processdefinitionId, final long processInstanceId,
            final long flowNodeInstanceId, final String messageName, final String processName, final long flowNodeDefinitionId, final String flowNodeName);

    SWaitingMessageEventBuilder createNewInstance(SWaitingMessageEvent waitingMessage);

    SWaitingMessageEvent done();

    String getMessageNameKey();

    String getLockedKey();

    String getProgressKey();

}