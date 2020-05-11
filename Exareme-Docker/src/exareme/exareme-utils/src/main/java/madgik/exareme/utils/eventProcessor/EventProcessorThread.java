/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.eventProcessor;

import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;

public class EventProcessorThread extends Thread {

    private static Logger logger = Logger.getLogger(EventProcessorThread.class);
    private EventQueue eventQueue = null;
    private ExecutorService executor = null;
    private EventProcessor processor = null;

    public EventProcessorThread(EventQueue eventQueue, ExecutorService executor,
                                EventProcessor processor) {
        this.eventQueue = eventQueue;
        this.executor = executor;
        this.processor = processor;
    }

    @Override
    public void run() {
        while (true) {
            logger.debug("Event Processor Thread running...");
            try {
                logger.debug("Event Processor Thread, getting next ....");
                ActiveEvent next = eventQueue.getNext();
                logger.debug("Event Processor Thread, got next");

                EventHandlerRunnable job = new EventHandlerRunnable(next, processor);
                logger.debug("Event Processor Thread, Executor: " + executor);
                logger.debug("Event Processor Thread, Event: " + next);
                logger.debug("Event Processor Thread, Event: " + next.getEvent());
                executor.submit(job);
                logger.debug("Event Processor Thread, Submitted job ");
            } catch (Exception e) {
                logger.error("Cannot run event", e);
            }
        }
    }
}
