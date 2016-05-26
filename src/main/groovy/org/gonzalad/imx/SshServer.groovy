package com.up.imx

import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.SessionHandler

class SshServer {
	
	@Delegate
    Service service
	
	@Delegate
    Remote remote
	
    String encoding
	
	//pour le moment, marche pas
	protected Object session(@DelegatesTo(SessionHandler) Closure closure) {
		
		service.run {
			session(remote) {
				return closure.call()
			}
		}
	}

	public getEncoding() {
		return encoding != null ? encoding : 'UTF-8'
	}
	
//	def methodMissing(String name, args){
//		println("hellooo!!!!")
//	}
}