/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.worker.art.container;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import madgik.exareme.common.art.ContainerSessionID;
import madgik.exareme.common.art.PlanSessionID;
import madgik.exareme.worker.art.registry.rmi.RmiArtRegistry;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    private static Logger logger = Logger.getLogger(RmiArtRegistry.class);
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
        logger.debug("Exec jobs");
        TimeLimiter timeLimiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());
        logger.debug("Created time limiter");
        try{
            ContainerJobResults results = timeLimiter.callWithTimeout(() -> execJobsTask(jobs), 30, TimeUnit.SECONDS, true);
            logger.debug("Got results!: " + results);
            return results;
        }catch (Exception e){
            logger.info("Communication timeout with container: " + containerSessionID.getLongId());
            throw new RemoteException("Communication timeout with container: " + containerSessionID.getLongId());
        }
    }

    public ContainerJobResults execJobsTask(ContainerJobs jobs) throws RemoteException {
        jobs.setSession(containerSessionID, sessionID);
        return containerProxy.getRemoteObject().execJobs(jobs);
    }

    public void closeSession() throws RemoteException {
        containerProxy.getRemoteObject().destroyContainerSession(containerSessionID, sessionID);
    }





}
