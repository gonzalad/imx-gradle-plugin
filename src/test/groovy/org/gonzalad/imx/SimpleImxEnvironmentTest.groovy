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

class SimpleImxEnvironmentTest {

	private static ImxEnvironment imx;
	private static String testBinDirectory = ImxTest.testBinDirectory();
	private static String testPatchDirectory = ImxTest.testPatchDirectory()
	private static String testScriptDirectory = ImxTest.testScriptDirectory();
	private static List<String> patchListInServer = ImxTest.dummyPatches()
	private static File localRepository

	@Test
	public void testStatus() {
		imx.status()
	}
		
	@BeforeClass
	static void beforeSetupBeforeClass() {
		localRepository = File.createTempDir()
		imx = ImxTest.newImx()
		imx.repository.baseFolder = localRepository
		imx.profileScript = '. ~/.profile'
	}
}
