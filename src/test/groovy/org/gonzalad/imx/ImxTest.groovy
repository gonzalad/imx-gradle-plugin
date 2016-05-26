package org.gonzalad.imx

import java.io.File

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.settings.ConnectionSettings
import org.hidetake.groovy.ssh.core.settings.LoggingMethod
import java.nio.file.Paths
import java.net.URL

public class ImxTest {
	static ImxEnvironment newImx() {
		//File identity = new File("src/test/resources/.ssh/up/id_rsa");
		File identity = Paths.get(ImxTest.class.getClassLoader().getResource(".ssh/up/id_rsa").toURI()).toFile();
		println ("file exists : " + identity.exists())
		Remote gateway = new Remote(host:'TODO', port:22, user: 'TODO', identity: identity, knownHosts: ConnectionSettings.Constants.allowAnyHosts)
		Remote remoteImxServer = new Remote(host:'TODO', port:22, user: 'TODO', gateway: gateway, identity: identity, knownHosts: ConnectionSettings.Constants.allowAnyHosts)
		ImxServer imxServer = new ImxServer('idefix', imxHome(), remoteImxServer, Ssh.newService())
		imxServer.service.getSettings().logging = LoggingMethod.stdout
		imxServer.service.getSettings().outputStream = System.out
		Remote remoteFabricationServer = new Remote(host:'TODO', port:22, user: 'TODO', gateway: gateway, identity: identity, knownHosts: ConnectionSettings.Constants.allowAnyHosts)
		FabricationServer fabricationServer = new FabricationServer(remote: remoteFabricationServer, service: Ssh.newService(), patchDir: '/tmp/gradle-plugin-test/fabrication/patchs')
		fabricationServer.service.getSettings().logging = LoggingMethod.stdout
		return new ImxEnvironment(imxServer: imxServer,
		fabricationServer: fabricationServer,
		database: new InMemoryDatabase(),
		installScriptDir: testScriptDirectory(),
		profileScript: '/tmp/gradle-plugin-test/.profile',
		doInstallScriptFilename: 'do_imx_patch_launcher.ksh',
		repository: Repository.newRepository(ImxTest.class.getClassLoader().getResourceAsStream('org/gonzalad/imx/repository-test.json'))
			)	
			
	}
	
	static String imxHome() {
		return '/tmp/gradle-plugin-test/base'
	}
	static String testScriptDirectory() {
		return "/tmp/gradle-plugin-test/uploaded-scripts"
	}
	
	static String testBinDirectory() {
		return imxHome() + "/bin"
	}

	static String testPatchDirectory() {
		return imxHome() + "/patchs"
	}
	
	static List<String> dummyPatches() {
		return ["TstPatch20150521_2.tar.Z", "TstPatch20150601_3.tar.Z", "TstPatch20150701_1.tar.Z"]
	}
	
	static InputStream getPatchInputStream(String patch) {
		return ImxTest.class.getClassLoader().getResourceAsStream(patch);
	}
}
