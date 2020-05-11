/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event;

import madgik.exareme.utils.eventProcessor.Event;
import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.PlanEventSchedulerState;
import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event.independent.IndependentEventsHandler;
import org.apache.log4j.Logger;

import java.util.concurrent.Semaphore;

/**
 * @author heraldkllapi
 */
public abstract class ExecEngineEvent implements Event {
    public final PlanEventSchedulerState state;
    public Semaphore wait = null;

    private static final Logger logger = Logger.getLogger(ExecEngineEvent.class);
    public ExecEngineEvent(PlanEventSchedulerState state) {
        this.state = state;
    }

    public void done() {
        logger.debug("Event Done!");
        if (wait != null) {
            logger.debug("Releasing.");
            wait.release();
        }
    }
}
