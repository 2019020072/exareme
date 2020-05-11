/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.eventProcessor;

import madgik.exareme.utils.eventProcessor.event.stop.StopEvent;
import madgik.exareme.utils.eventProcessor.event.stop.StopEventHandler;
import madgik.exareme.utils.eventProcessor.event.stop.StopEventListener;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * @author herald
 */
public class EventProcessor {

    private static final Logger logger = Logger.getLogger(EventProcessor.class);
    private int concurrentEvents = 0;
    private EventQueue eventQueue = null;
    private ExecutorService executor = null;
    private EventProcessorThread processorThread = null;

    public EventProcessor(int concurrentEvents) {
        this.concurrentEvents = concurrentEvents;
        this.eventQueue = new EventQueue();
        this.executor = Executors.newFixedThreadPool(this.concurrentEvents);
    }

    public EventFuture queue(Event event, EventHandler handler, EventListener listener) {
        logger.debug("Queued event: " + event.getClass().getName());
        EventFuture future = new EventFuture(event);
        ActiveEvent activeEvent = new ActiveEvent(event, handler, listener, future);
        logger.debug("Adding event to eventQueue.");
        eventQueue.queue(activeEvent);
        logger.debug("Finished adding event to eventQueue.");
        return future;
    }

    public void start() {

        logger.debug("Event Processor starting...");
        processorThread = new EventProcessorThread(eventQueue, executor, this);
        processorThread.start();

        logger.debug("Event Processor started.");
    }

    public void stop() {
        this.stop(false);
    }

    public void stop(boolean now) {

        logger.debug("Event Processor stopping ...");
        if (now) {

            logger.debug("Now?");
            // TODO(herald): Take buckup!
            processorThread.stop();
        } else {
            logger.debug("Not now...");
            Semaphore semaphore = new Semaphore(0);
            StopEventHandler eventHandler = new StopEventHandler(semaphore);
            StopEventListener eventListener = new StopEventListener();
            logger.debug("Queueing stop event.");
            queue(new StopEvent(), eventHandler, eventListener);
            logger.debug("Queued stop event.");
            /* Wait for termination */
            try {
                logger.debug("Acquiring semaphore");
                semaphore.acquire();
                logger.debug("Acquired semaphore");
            } catch (Exception e) {
            }
            processorThread.stop();
        }
        executor.shutdownNow();
        logger.info("Event Processor Stopped!");
    }
}
