package org.bonitasoft.engine.repair.helper;

import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.repair.helper.designer.SimpleProcessDesigner;

/**
 * Created by Vincent Elcrin
 * Date: 16/12/13
 * Time: 11:40
 */
public abstract class ProcessDeployer {

    ProcessDefinition processDefinition;

    public void deploy(SimpleProcessDesigner design) throws BonitaException {
        processDefinition = deploy(design.done());
    };

    public abstract ProcessDefinition deploy(DesignProcessDefinition design) throws BonitaException;

    public void clean() throws BonitaException {
        clean(processDefinition);
    }

    public abstract void clean(ProcessDefinition processDefinition) throws BonitaException;
}
