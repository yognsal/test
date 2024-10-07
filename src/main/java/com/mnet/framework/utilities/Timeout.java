package com.mnet.framework.utilities;

import com.mnet.framework.reporting.FrameworkLog;

/**
 * Provides common functionality to put active thread to sleep.
 */
public interface Timeout {

	/**
	 * Puts active thread to sleep for designated timeout period.
	 * @param log Logger from test class.
	 * @param timeout Duration to put thread to sleep (in ms).
	 **/
	static void waitForTimeout(FrameworkLog log, long timeout) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException ie) {
			log.error("Thread sleep interrupted");
			log.printStackTrace(ie);
			throw new RuntimeException(ie);
		}
	}
	
}
