/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event.closeContainerSession;

import madgik.exareme.utils.eventProcessor.EventProcessor;
import madgik.exareme.worker.art.container.ContainerJob;
import madgik.exareme.worker.art.container.ContainerJobResults;
import madgik.exareme.worker.art.container.ContainerJobs;
import madgik.exareme.worker.art.container.ContainerSession;
import madgik.exareme.worker.art.container.job.GetStatisticsJob;
import madgik.exareme.worker.art.container.job.GetStatisticsJobResult;
import madgik.exareme.worker.art.executionEngine.ExecEngineConstants;
import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.PlanEventSchedulerState;
import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event.ExecEngineEventHandler;
import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event.independent.IndependentEventsHandler;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author herald
 */
public class CloseContainerSessionEventHandler
        implements ExecEngineEventHandler<CloseContainerSessionEvent> {
    public static final CloseContainerSessionEventHandler instance =
            new CloseContainerSessionEventHandler();
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CloseContainerSessionEventHandler.class);

    public CloseContainerSessionEventHandler() {
    }

    @Override
    public void preProcess(CloseContainerSessionEvent event, PlanEventSchedulerState state)
            throws RemoteException {
        log.debug("preProcessing closing container session event ...");
        try {
            ExecutorService service =
                    Executors.newFixedThreadPool(ExecEngineConstants.THREADS_PER_INDEPENDENT_TASKS);
            ArrayList<GetStatsAndCloseSession> workers = new ArrayList<GetStatsAndCloseSession>();
            List<ContainerSession> sessions = state.getContSessions(event.containerSessionID);
            log.debug("Sessions " + sessions.size());
            for (ContainerSession session : sessions) {
                log.debug("Getting stats and closing sessions ...");
                GetStatsAndCloseSession w = new GetStatsAndCloseSession(session);
                workers.add(w);
                service.submit(w);
            }
            log.debug("Shut down sessions");
            service.shutdown();
            log.debug("Awaiting termination");
            service.awaitTermination(1, TimeUnit.DAYS);
            log.debug("Continuing...");
            for (GetStatsAndCloseSession w : workers) {
                state.getStatistics().containerStats.add(w.stats.getStats());
            }
            event.messageCount += workers.size();
        } catch (InterruptedException e) {
            throw new RemoteException("Cannot handle close session event", e);
        }

        log.debug("Finished preProcessing closing container session event ...");
    }

    @Override
    public void handle(CloseContainerSessionEvent event, EventProcessor proc)
            throws RemoteException {
    }

    @Override
    public void postProcess(CloseContainerSessionEvent event, PlanEventSchedulerState state)
            throws RemoteException {
        state.getStatistics().incrControlMessagesCountBy(event.messageCount);
    }
}


class GetStatsAndCloseSession extends Thread {
    private static final Logger log = Logger.getLogger(GetStatsAndCloseSession.class);
    public ContainerSession session;
    public ContainerJobResults results;
    public RemoteException exception;
    public GetStatisticsJobResult stats;

    public GetStatsAndCloseSession(ContainerSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        try {
            log.debug("Closing session: " + session.getSessionID().getLongId());
            ContainerJobs jobs = new ContainerJobs();
            jobs.addJob(GetStatisticsJob.instance);
            log.debug("Will execute jobs to close sessions");
            log.debug("jobs: " + jobs.getJobs().size());
            for(ContainerJob job: jobs.getJobs()){
                log.debug("job: " + job.getType());
            }
            results = session.execJobs(jobs);
            log.debug("will get statistics results");
            stats = (GetStatisticsJobResult) results.getJobResults().get(0);
            log.debug("Closing session ... ");
            session.closeSession();
            log.debug("Closed session");
        } catch (RemoteException e) {
            exception = e;
            log.error("Cannot close session", e);
        }
    }
}
