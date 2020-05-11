/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.eventProcessor;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

/**
 * @author herald
 */
public class EventHandlerRunnable implements Runnable {

    private static Logger logger = Logger.getLogger(EventHandlerRunnable.class);
    private ActiveEvent event = null;
    private EventProcessor eventProcessor = null;

    public EventHandlerRunnable(ActiveEvent event, EventProcessor eventProcessor) {
        this.event = event;
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void run() {
        try {
            logger.debug(event.getEvent().getClass().getName() + " Starting Processing... ");
            event.startProcessing();

            logger.debug(event.getEvent().getClass().getName() + " Handling event... ");
            event.getHandler().handle(event.getEvent(), eventProcessor);

            logger.debug(event.getEvent().getClass().getName() + " Handled event... ");
        } catch (RemoteException e) {
            logger.debug(event.getEvent().getClass().getName() + " Caught Exception... ");
            event.setException(e);
        }

        logger.debug(event.getEvent().getClass().getName() + " Processing listener... ");
        event.getEventListener().processed(event.getEvent(), event.getException(), eventProcessor);
        logger.debug(event.getEvent().getClass().getName() + " Finished processing listener... ");
        event.endProcessing();
        logger.debug(event.getEvent().getClass().getName() + " Event processed times: " +
                event.getWaitTime() + " / " + event.getProcessTime() +
                " (" + event.getEvent().getClass().getName() + ")");
    }
}
