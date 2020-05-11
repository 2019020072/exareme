/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.worker.art.executionEngine.recovery.retryPolicy;

import madgik.exareme.worker.art.executionEngine.dynamicExecutionEngine.event.exception.OperatorExceptionEventHandler;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;

/**
 * @author herald
 */
public class SimpleRetryPolicy implements RetryPolicy {

    private static final Logger log = Logger.getLogger(SimpleRetryPolicy.class);

    private int numOfFailures = 0;

    public SimpleRetryPolicy(int numOfFailures) {
        this.numOfFailures = numOfFailures;
    }

    @Override
    public boolean retry(Exception exception, int retries) throws RemoteException {
        log.debug("Retry? retries < numOfFailure == " + retries + " < " + numOfFailures);
        return retries < numOfFailures;
    }
}
