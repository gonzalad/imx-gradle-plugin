package com.up.imx

import org.hidetake.groovy.ssh.session.BadExitStatusException;

class ImxInstallationException extends RuntimeException {
	final int exitStatus
	ImxInstallationException(String message) {
		super (message)
	}
	ImxInstallationException(String message, Throwable err) {
		super (message, err)
	}
	ImxInstallationException(String message, BadExitStatusException err) {
		super (message, err)
		this.exitStatus = err.exitStatus;
	}
}
