package com.up.imx

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotEquals

import org.junit.After
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test
import org.sonatype.aether.repository.LocalRepository;;

class FabricationServerTest {

	private static ImxEnvironment imx
	private static String testPatchDirectory = ImxTest.testPatchDirectory()
	private File localRepository
	private static List<String> patchListInServer = ImxTest.dummyPatches()
	
	@Test
	void testDownload() {
		String[] localRepoContent
		List<String> patchsToDownload
		
		//download 1 seul patch
		patchsToDownload = [patchListInServer.get(0)]
		imx.fabricationServer.download(toPatchs(patchsToDownload), localRepository, false)
		localRepoContent = localRepository.list()
		assertEquals ("Le repository local devrait contenir 1 fichier", 1, localRepoContent.length)
		assertEquals ("Fichier attendu invalide", patchsToDownload.get(0), localRepoContent[0])
		
		//download 2 patchs
		cleanupLocalRepository()
		newLocalRepository()
		patchsToDownload = [patchListInServer.get(0), patchListInServer.get(2)]
		imx.fabricationServer.download(toPatchs(patchsToDownload), localRepository, false)
		localRepoContent = localRepository.list()
		assertEquals ("Le repository local devrait contenir 2 fichier", 2, localRepoContent.length)
		assertTrue ("Nom des fichiers attendus incorrect", patchsToDownload.containsAll(localRepoContent))
	}
	
	/**
	 * Si un patch existe déjà en repo local, alors il ne doit pas être écrasé
	 */
	@Test
	void testDownloadPatchAlreadyAvailable() {
		
		//on vérifie que les seuls patchs downloadés sont ceux non déjà présents
		cleanupLocalRepository()
		newLocalRepository()
		
		//create dummy patch
		File file = new File(localRepository, patchListInServer.get(0))
		file << "h"
		long expectedFileSize = file.length()
		
		
		imx.fabricationServer.download(toPatchs(patchListInServer), localRepository, false)
		String[] localRepoContent = localRepository.list()
		assertEquals ("Le repository local devrait contenir 3 fichier", 3, localRepoContent.length)
		assertTrue ("Nom des fichiers attendus incorrect", patchListInServer.containsAll(localRepoContent))
		file = new File(localRepository, patchListInServer.get(0))
		assertEquals("Patch file size invalid", expectedFileSize, file.length())
	}
	
	/**
	 * Si un patch existe déjà en repo local, alors il doit être écrasé lorsqu'on utilise 
	 * l'option forceDownload
	 */
	@Test
	void testDownloadPatchAlreadyAvailableWithForceDownload() {
		
		//create dummy patch
		File file = new File(localRepository, patchListInServer.get(0))
		file << "h"
		long expectedFileSize = file.length()
		
		imx.fabricationServer.download(toPatchs(patchListInServer), localRepository, true)
		String[] localRepoContent = localRepository.list()
		assertEquals ("Le repository local devrait contenir 3 fichier", 3, localRepoContent.length)
		assertTrue ("Nom des fichiers attendus incorrect", patchListInServer.containsAll(localRepoContent))
		file = new File(localRepository, patchListInServer.get(0))
		assertNotEquals("Patch file size invalid", expectedFileSize, file.length())
	}
	
	/**
	 * Un patch demandé doit être présent sur le serveur de fabrication, sinon une erreur est générée
	 */
	@Test(expected=UnavailablePatchException.class)
	void testDownloadMissingPatch() {
		imx.fabricationServer.download(toPatchs(["inexistent.tar.Z"]), localRepository, true)
	}

	@Test
	void testContainsListOfString() {
		List<String> patchs;
		
		patchs = patchListInServer
		assertTrue("Serveur devrait contenir patchs " + String.join(",", patchs), imx.fabricationServer.contains(patchs))
		
		patchs = patchListInServer + "Inexistant.tar.Z"
		assertFalse("Serveur ne devrait contenir pas contenir un des patchs " + String.join(",", patchs), imx.fabricationServer.contains(patchs))
		
		patchs = ["Inexistant.tar.Z"]
		assertFalse("Serveur ne devrait contenir pas contenir un des patchs " + String.join(",", patchs), imx.fabricationServer.contains(patchs))
	}

	@Test
	void testContainsString() {
		String patch;
		
		patch = patchListInServer.get(0)
		assertTrue("Serveur devrait contenir patchs " + patch, imx.fabricationServer.contains(patch))
		
		patch = "Inexistant.tar.Z"
		assertFalse("Serveur ne devrait contenir pas contenir un des patchs " + patch, imx.fabricationServer.contains(patch))
	}

	@Before
	void setUp() {
		cleanupLocalRepository()
		newLocalRepository()
	}
		
	@After
	void tearDown() {
		cleanupLocalRepository()
	}
	
	private void newLocalRepository() {
		localRepository = File.createTempDir()
	}
	
	void cleanupLocalRepository() {
		localRepository?.deleteDir()
//		if (localRepository != null) {
//			localRepository.deleteDir()
//		}
	}
	
	List<Patch> toPatchs(List<String> patchNames) {
		return patchNames.collect { new Patch(id: it) }
	}
	
	@BeforeClass
	static void beforeSetupBeforeClass() {
		imx = ImxTest.newImx()
		imx.fabricationServer.patchDir = ImxTest.testPatchDirectory()
		copyPatchs()
	}
	
	private static void copyPatchs() {
		deletePatchs()
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {
				
				//create test directory
				execute ignoreError: true, "mkdir -p $testPatchDirectory"
				
				//upload dummy patches.tar.Z
				patchListInServer.each {
					put from: ImxTest.getPatchInputStream("com/up/imx/$it"), into: "${testPatchDirectory}/$it"
				}
//				put from: getPatchInputStream("com/up/imx/TstPatch20150521_2.tar.Z"), into: "${testPatchDirectory}/TstPatch20150521_2.tar.Z"
//				put from: getPatchInputStream("com/up/imx/TstPatch20150601_3.tar.Z"), into: "${testPatchDirectory}/TstPatch20150601_3.tar.Z"
//				put from: getPatchInputStream("com/up/imx/TstPatch20150701_1.tar.Z"), into: "${testPatchDirectory}/TstPatch20150701_1.tar.Z"
			}
		}
	}
	
	private static void deletePatchs() {
		if (!testPatchDirectory) {
			throw new IllegalStateException ("testPatchDirectory ne doit pas être vide : $testPatchDirectory")
		}
		imx.imxServer.service.run {
			session(imx.imxServer.remote) {
				execute ignoreError: true, "rm -r $testPatchDirectory"
			}
		}
	}
}
