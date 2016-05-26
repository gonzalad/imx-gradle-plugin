package com.up.imx

import org.hidetake.groovy.ssh.session.BadExitStatusException;

class ImxInterruptedInstallationException extends ImxInstallationException {
	ImxInterruptedInstallationException(String message) {
		super (message)
	}
	ImxInterruptedInstallationException(String message, Throwable err) {
		super (message, err)
	}
	ImxInterruptedInstallationException(String message, BadExitStatusException err) {
		super (message, err)
	}
}
