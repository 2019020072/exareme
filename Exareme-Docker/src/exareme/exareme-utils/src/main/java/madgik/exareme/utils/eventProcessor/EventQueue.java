/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.eventProcessor;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * @author herald
 */
public class EventQueue {
    private Semaphore count = null;
    private List<ActiveEvent> queue = null;

    private static final Logger logger = Logger.getLogger(EventProcessor.class);
    public EventQueue() {
        this.count = new Semaphore(0);
        this.queue = Collections.synchronizedList(new LinkedList<ActiveEvent>());
    }

    public void queue(ActiveEvent event) {
        queue.add(event);
        logger.debug("queue, count = " + count.getQueueLength());
        count.release();
    }

    public ActiveEvent getNext() throws InterruptedException {

        logger.debug("getNext, count = " + count.getQueueLength());
        count.acquire();
        logger.debug("Event queue, acquired lock ");
        return queue.remove(0);
    }
}
