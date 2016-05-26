package com.up.imx

import org.hidetake.groovy.ssh.Ssh;
import org.hidetake.groovy.ssh.core.Remote
import org.junit.AfterClass
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test

import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.BadExitStatusException;

import static org.junit.Assert.fail

import java.io.File
import java.util.List

class ImxEnvironmentTest {

	private static ImxEnvironment imx;
	private static String testBinDirectory = ImxTest.testBinDirectory();
	private static String testPatchDirectory = ImxTest.testPatchDirectory()
	private static String testScriptDirectory = ImxTest.testScriptDirectory();
	private static List<String> patchListInServer = ImxTest.dummyPatches()
	private static File localRepository

	@Test
	public void testStart() {
		imx.start()
	}

	@Test
	public void testStop() {
		imx.stop()
	}

	@Test
	public void testRestart() {
		imx.restart()
	}

	@Test
	public void testInstall() {
		imx.download()
		imx.install()
	}
	
	@Test(expected = LockException.class )
	public void testInstallWithLocks() {
		//create lock file
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {
				execute ignoreError: true, """
touch ${imx.imxServer.imxHome}/patchs/test.lock
touch ${imx.imxServer.imxHome}/patchs/imx_patch.conf
"""
			}
		}
		imx.download()
		imx.install(new InstallOptions(removeLocks: false))
	}

	@Test
	public void testInstallWithLocks_DeleteLocks() {
		//create lock file
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {
				execute ignoreError: true, """
touch ${imx.imxServer.imxHome}/patchs/test.lock
touch ${imx.imxServer.imxHome}/patchs/imx_patch.conf
"""
			}
		}
		imx.download()
		imx.install(new InstallOptions(removeLocks: true))
	}

	@Test(expected=ImxInstallationException.class)
	public void testInstall_ImxPatchLogInError() {
		imx.doInstallScriptFilename = 'do_imx_patch_launcher.ksh true'
		imx.download()
		imx.install()
	}
	
	@Test(expected=ImxInterruptedInstallationException.class)
	public void testInstall_ImxPatchLogInterrupted() {
		imx.doInstallScriptFilename = 'do_imx_patch_launcher.ksh true true'
		imx.download()
		imx.install()
	}
	
	@Test
	public void testManualInstall() {
		imx.manualInstall = true
		imx.download()
		imx.install()
	}

/*
 * 	TODO : dummyInstall (pour n'installer que les scripts et éventuellement message indiquant côté serfveur comment exécuter manuellement)?
	TODO : créer script gradle incluant un autre avec toutes les définitions de taches et dépendances entre elles
	TODO : script gradle pour utiliser le mode dryRun
 */
		
	@BeforeClass
	static void beforeSetupBeforeClass() {
		localRepository = File.createTempDir()
		imx = ImxTest.newImx()
		imx.repository.baseFolder = localRepository
		imx.watchedProcess = imx.doInstallScriptFilename
		copyResources()
	}

	@AfterClass
	static void tearDownAfterClass() {
		localRepository?.deleteDir()
	}

	private static void copyResources() {
		deleteResources()
		
		String testStartScript = "$testBinDirectory/test_start_instance.ksh"
		String testShutdownScript = "$testBinDirectory/test_shutdown_imx.ksh"
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {

				//create directories
				String fabricationPatchesDir = imx.fabricationServer.patchDir
				String imxProfileDir = imx.profileScript.substring(0, imx.profileScript.lastIndexOf('/'))
				execute ignoreError: true, """
rm -rf $fabricationPatchesDir
rm -rf $testScriptDirectory
rm -rf ${imx.imxServer.imxHome}
rm -rf $imxProfileDir

mkdir -p $fabricationPatchesDir
mkdir -p $testScriptDirectory
mkdir -p ${imx.imxServer.imxHome}/bin
mkdir -p ${imx.imxServer.imxHome}/patchs
mkdir -p $imxProfileDir
"""

				//upload dummy patches to fabrication server
				patchListInServer.each {
					put from: ImxTest.getPatchInputStream("com/up/imx/$it"), into: "${fabricationPatchesDir}/$it"
				}

				//create dummy profile
				put text: '''#!/bin/ksh
''', into: "${imx.profileScript}"

				//create dummy start script
				put text: '''#!/bin/ksh
echo "start instance stub"
''', into: "$testStartScript"

				//create dummy stop script
				put text: '''#!/bin/ksh
echo "start instance stub"
''', into: "$testShutdownScript"

				//create dummy doXXX.ksh (install)
				put text: executeTemplate(ImxTest.getPatchInputStream('com/up/imx/do_imx_patch_launcher.ksh').getText()), into: "${imx.imxServer.imxHome}/patchs/do_imx_patch_launcher.ksh"
				
				//chmod
				execute ignoreError: true, """chmod 770 $testScriptDirectory/*.ksh 
chmod 770 ${imx.profileScript}
chmod 770 ${imx.imxServer.imxHome}/bin/*.ksh
chmod 770 ${imx.imxServer.imxHome}/patchs/*.ksh
"""
			}
		}
		imx.startScript = "$testStartScript"
		imx.stopScript = "$testShutdownScript"
		imx.doInstallScriptFilename = 'do_imx_patch_launcher.ksh'
	}
	
	@Before
	void setUp() {

		// on ne conserve pas mémoire de patchs installés entre les tests
		imx.database = new InMemoryDatabase()
		
		imx.doInstallScriptFilename = 'do_imx_patch_launcher.ksh'
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {
				execute ignoreError: true, "rm -f ${imx.imxServer.imxHome}/patchs/imx_patch.conf"
			}
		}
	}

	private static String executeTemplate(String templateText) {
		[
			'\\$imxHome': imx.imxServer.imxHome
		].each{
			k, v -> templateText = templateText.replaceAll(k, v)
		}
		return templateText
	}

	private static void deleteResources() {
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {
				String fabricationPatchesDir = imx.fabricationServer.patchDir
				execute ignoreError: true, "rm -r $testScriptDirectory; rm -r $fabricationPatchesDir"
			}
		}
	}
	//
	//	void convertCRLF( File input, File output ) {
	//		output << input.text.replaceAll( '\r\n', '\n' )
	//	}
}
