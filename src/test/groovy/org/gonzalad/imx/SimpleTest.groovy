package com.up.imx

import java.util.regex.Matcher;

import org.junit.Test;

class SimpleTest {

	@Test
	public void test() {
		String statusString = '''
imxServer|[H[2J======================================================================================================
imxServer|                                 [1miMX Status Report of instance: OBELIX[0m
imxServer|======================================================================================================
imxServer|DB access                                                                                 [1m[ OK ][0m
imxServer|DB listener                                                                               [1m[ OK ][0m
imxServer|OS X11 vfb                                                                                [1m[ OK ][0m
imxServer|OS Spooler                                                                                [1m[ OK ][0m
imxServer|Oracle Fusion Forms                                                                       [1m[ OK ][0m
imxServer|iMX Systeme Expert                                                                        [1m[ OK ][0m
imxServer|iMX Publishing                                                                            [1m[ OK ][0m
imxServer|iMX Document Server                                                                       [1m[ OK ][0m
imxServer|iMX Mail Incoming                                                                         [1m[ DISABLED ][0m
imxServer|iMX Mail Outgoing                                                                         [1m[ DISABLED ][0m
imxServer|iMX IMXSERVER                                                                             [1m[ DISABLED ][0m
imxServer|iMX Telephony                                                                             [1m[ FAILED ][0m
imxServer|iMX FAX                                                                                   [1m[ DISABLED ][0m
imxServer|iMX SMS                                                                                   [1m[ OK ][0m
imxServer|[1mExit code: [0m1
imxServer|
imxServer|[1mLogfile name:[0m /var/logapp/IMX/int/logs/imxstatus_20160316171327_40763644_0.log
imxServer|[1mOutfile name:[0m /var/logapp/IMX/int/logs/imxstatus_20160316171327_40763644_0.out
imxServer|---------------------------------------------------------------------------------------------
'''
		componentStatus('DB access', statusString)
		componentStatus('DB listener', statusString)
		componentStatus('OS X11 vfb', statusString)
		componentStatus('OS Spooler', statusString)
		componentStatus('Oracle Fusion Forms', statusString)
		componentStatus('iMX Systeme Expert', statusString)
		componentStatus('iMX Publishing', statusString)
		componentStatus('iMX Document Server', statusString)
		componentStatus('iMX SMS', statusString)
	}

	private void componentStatus(String component, String statusString) {
		Matcher matcher = statusString =~ /(?m)$component\ +.{1}\[1m\[ OK \]/
		if (! matcher) {
			throw new RuntimeException("$component not activated")
		}
	}
}
