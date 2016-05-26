package com.up.imx

/**
 * Thrown if a lock file (due to a previous failed or unfinished install) exists.
 */
class LockException extends RuntimeException {
	public LockException (String message) {
		super (message)
	}
}
