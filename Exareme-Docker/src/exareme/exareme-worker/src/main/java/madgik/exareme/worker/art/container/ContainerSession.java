/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.worker.art.container;

import madgik.exareme.common.art.ContainerSessionID;
import madgik.exareme.common.art.PlanSessionID;
import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event.closeContainerSession.CloseContainerSessionEventHandler;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * @author Herald Kllapi<br>
 * @author Dimitris Paparas<br>
 * @author Eva Sitaridi<br>
 * {herald,paparas,evas}@di.uoa.gr<br>
 * University of Athens /
 * Department of Informatics and Telecommunications.
 * @since 1.0
 */
public class ContainerSession implements Serializable {
    private static final Logger logger = Logger.getLogger(ContainerSession.class);

    private static final long serialVersionUID = 1L;
    private ContainerSessionID containerSessionID = null;
    private PlanSessionID sessionID = null;
    private ContainerProxy containerProxy = null;

    public ContainerSession(ContainerProxy containerProxy, ContainerSessionID containerSessionID,
                            PlanSessionID sessionID) {
        this.containerProxy = containerProxy;
        this.containerSessionID = containerSessionID;
        this.sessionID = sessionID;
    }

    public PlanSessionID getSessionID() {
        return sessionID;
    }

    public ContainerSessionID getContainerSessionID() {
        return containerSessionID;
    }

    public ContainerJobResults execJobs(ContainerJobs jobs) throws RemoteException {
        logger.debug("Setting session.");
        jobs.setSession(containerSessionID, sessionID);
        logger.debug("Remote Object: " + containerProxy.getRemoteObject());
        logger.debug("Remote Object status: " + containerProxy.getRemoteObject().getStatus());
        logger.debug("Running job: " + jobs.getJobs().get(0) + " - and returning");
        ContainerJobResults result = containerProxy.getRemoteObject().execJobs(jobs);
        logger.debug("Result: " + result);
        return result;
    }

    public void closeSession() throws RemoteException {
        containerProxy.getRemoteObject().destroyContainerSession(containerSessionID, sessionID);
    }
}
