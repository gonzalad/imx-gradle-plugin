package com.up.imx

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ImxTestScriptTask extends SshTask {
    @TaskAction
    def executeScripts() {
	  copyScripts();
      String scriptFolder = scriptFolder()
		println ("after copy")
	  sshServer.service.run {
        session(sshServer.remote) {

			execute ignoreError: true, 'mkdir adrian'
			//ant.fixcrlf( srcdir:'.', includes:'*.ksh', eol:'lf' )
		println ("before put")
			put from: "$scriptFolder/imx_patch_launcher.ksh", into: './adrian'
		println ("after put")
			put from: "$scriptFolder/do_imx_patch_launcher.ksh", into: './adrian'
			put from: "$scriptFolder/watchdog.ksh", into: './adrian'
			put from: "$scriptFolder/watch_imx_patch.ksh", into: './adrian'
	  
			execute """
cd adrian
chmod 770 *.ksh
touch patches_list
"""
	  
			//launch IMX Install
			execute """
cd adrian
./imx_patch_launcher.ksh
"""
	  
			//Watch IMX logs
			execute """
cd adrian
./watch_imx_patch.ksh
#TODO : analyse log files in order to set jenkins exit status ?
# test with do_XXX.ksh sending exit 0 and then exit 1
# look at real do_imx_patch to see if it handles cleanly error codes
"""
        }
      }
    }
	
	private String scriptFolder() {
		return "${project.buildDir}/task/${name}/scripts"
	}
	
	private void copyScripts() {
		def folder = project.file(scriptFolder())
//		if( !folder.exists() ) {
//			// Create all folders
//			folder.mkdirs()
//		}
		copyScript ("imx_patch_launcher.ksh", folder)
		copyScript ("do_imx_patch_launcher.ksh", folder)
		copyScript ("watchdog.ksh", folder)
		copyScript ("watch_imx_patch.ksh", folder)
	}
	
	private void copyScript(String script, File folder) {
		FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("scripts/" + script), new File(folder, script));
	} 
}