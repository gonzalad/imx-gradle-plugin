package com.up.imx

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction;

class ImxStartTask extends DefaultTask {

	ImxEnvironment imx;
	
	@TaskAction
	def start() {
//        sshServer.session ({
//          execute "echo 'starting IMX'"
//          println (sshServer.service.class)
//          println (sshServer.remote.class)
//        })

//		sshServer.service.run {
//			session(sshServer.remote) {
//				execute "echo 'starting IMX'"
//				println (sshServer.service.class)
//				println (sshServer.remote.class)
//			}
//		}

//		imx.imxServer.service.run {
//			session(imx.imxServer.remote) {
//				execute "echo 'starting IMX'"
//				println (imx.imxServer.service.class)
//				println (imx.imxServer.remote.class)
//			}
//		}
		imx.start()
	}
}