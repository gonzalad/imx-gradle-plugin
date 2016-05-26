package org.gonzalad.imx

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.NoSuchFileException

import static org.junit.Assert.*

import org.junit.Test
import org.junit.Before

class ImxRepositoryTest {
	private Repository repository
	
	@Test
	void testNewRepository() {
		assertEquals("nombre patchs repo invalide", 3, repository.getPatches().size())
		String id
		id = "TstPatch20150521_2.tar.Z"
		assertNotNull("patch $id ne doit pas être null", repository.getPatch(id))
		assertFalse("compilation patch $id invalide", repository.getPatch(id).compilation)
		id = "TstPatch20150601_3.tar.Z"
		assertNotNull("patch $id ne doit pas être null", repository.getPatch(id))
		assertTrue("compilation patch $id invalide", repository.getPatch(id).compilation)
		id = "TstPatch20150701_1.tar.Z"
		assertNotNull("patch $id ne doit pas être null", repository.getPatch(id))
		assertFalse("compilation patch $id invalide", repository.getPatch(id).compilation)
	}

	@Test
	void testCopyTo() {

		//on crée des fichiers bidon
		repository.getPatchAsFiles().each {
			it << "dummyPatch"
		}

		//test de la copie
		Path destPath = Files.createTempDirectory(null)
		repository.copyTo(destPath.toFile())
		repository.patches.each {
			assertTrue("Le fichier devrait exister", Files.exists(destPath.resolve(it.id)))
		}
	}
	
	@Test
	void testCheckValidity() {

		//on crée des fichiers bidon
		repository.getPatchAsFiles().each {
			it << "dummyPatch"
		}
		
		List<RepositoryError> errors = repository.checkValidity()
		assertEquals("nombre erreurs devrait être vide : ${errors}", 0, errors.size())
	}
	
	@Test
	void testCheckValidity_DuplicateFile() {
		
		//on crée des fichiers bidon
		repository.getPatchAsFiles().each {
			it << "dummyPatch"
		}
		//on crée un fichier patch en doublon
		File subdir = new File(repository.baseFolder, "sub1")
		subdir.mkdir()
		String duplicatePatchId = repository.getPatches().get(0).getId()
		File duplicatePatchFile = new File(subdir, duplicatePatchId)
		duplicatePatchFile << "duplicateContent"
		
		List<RepositoryError> errors = repository.checkValidity()
		
		assertEquals("nombre erreurs invalide : ${errors}", 1, errors.size())
		errors.each {
			assertEquals(RepositoryError.ErrorTypeEnum.DUPLICATE_PATCH_FILE, it.errorType)
			assertEquals(duplicatePatchId, it.patchId)
		}
	}
	
	@Test
	void testCheckValidity_DuplicatePatch() {
		
		//on ajoute un duplicata
		repository.getPatches().add(repository.getPatches()[0])

		//on crée des fichiers bidon
		repository.getPatchAsFiles().each {
			it << "dummyPatch"
		}
		
		List<RepositoryError> errors = repository.checkValidity()
		
		assertEquals("nombre erreurs invalide : ${errors}", 1, errors.size())
		assertEquals(RepositoryError.ErrorTypeEnum.DUPLICATE_PATCH, errors[0].errorType)
		assertEquals(repository.getPatches()[0].id, errors[0].patchId)
	}
	
	@Test
	void testCheckValidity_TooManyFiles() {

		//on crée des fichiers bidon
		repository.getPatchAsFiles().each {
			it << "dummyPatch"
		}
		
		List<String> tooManyPatchIds = [repository.getPatches()[1], repository.getPatches()[2]]
		//on ne conserve que le premier patch dans repository.json
		repository.setPatches([repository.getPatches()[0]])

		List<RepositoryError> errors = repository.checkValidity()
		
		assertEquals("nombre erreurs invalide : ${errors}", 2, errors.size())
		errors.each {
			assertEquals(RepositoryError.ErrorTypeEnum.TOO_MANY_LOCAL_FILE, it.errorType)
			assertTrue("patch non attendu $it.patchId", !tooManyPatchIds.contains(it.patchId))
		}
	}

	@Test(expected = NoSuchPatchException.class)
	void testCopyToPatchInexistent() {

		String inexistentPatchId = 'patchnonenregistre.tar.Z'
		//on crée des fichiers bidon
		List<File> patchsAsFiles = repository.getPatchAsFiles()
		patchsAsFiles += new File (patchsAsFiles.get(0).getParent(), inexistentPatchId)
		repository.getPatchAsFiles().each {
			it << "dummyPatch"
		}
		
		//test de la copie
		Path destPath = Files.createTempDirectory(null)
		try {
			repository.copyTo([inexistentPatchId], destPath.toFile())
		} catch (NoSuchPatchException err) {
			assertNotNull('Liste des patchs inexistant ne devrait pas être null', err.getPatchs())
			assertEquals('Liste des patchs inexistant invalide', [inexistentPatchId], err.getPatchs())
			throw err
		}
	}

	@Test(expected = NoSuchFileException.class)
	void testCopyToFileInexistent() {
		//on crée un seul patch (sur les 3)
		repository.getPatchAsFiles().first() << "dummyPatch"

		Path destPath = Files.createTempDirectory(null)
		repository.copyTo(destPath.toFile())
		repository.patches.each {
			assertTrue("Le fichier devrait exister", Files.exists(Paths.get(destPath, it.id)))
		}
	}
	
	@Test
	void testCopyDocTo() {

		//on crée des docs bidon
		repository.getPatches().findAll { it.doc }.collect { new File(repository.baseFolder, it.doc) }.each {
			it << "dummyDoc"
		} 
		

		//test de la copie
		Path destPath = Files.createTempDirectory(null)
		repository.copyDocsTo(destPath.toFile())
		int copyCount = 0
		repository.patches.findAll { it.doc }.each { 
			assertTrue("Le fichier $it.doc devrait exister", Files.exists(destPath.resolve(it.doc)))
			copyCount ++
		}
		assertEquals('Deux docs auraient dû être copiées', 2, copyCount)
	}
	
	@Test
	void testResourcesDocTo() {

		//on crée des docs bidon
		repository.getPatches().findAll { it.resources?.size() > 0 }.collect { it.resources }
			.flatten().collect { new File(repository.baseFolder, it) }.each {
				it << "dummyResources"
		}
		
		//test de la copie
		Path destPath = Files.createTempDirectory(null)
		repository.copyResourcesTo(destPath.toFile())
		int copyCount = 0
		repository.patches.findAll { it.resources }.collect { it.resources }.flatten().each {
			assertTrue("Le fichier $it devrait exister", Files.exists(destPath.resolve(it)))
			copyCount ++
		}
		assertEquals('Deux ressources auraient dû être copiées', 3, copyCount)
	}

	@Before
	void setUp() {
		repository = Repository.newRepository(getClass().getClassLoader().getResourceAsStream("org/gonzalad/imx/repository-test.json"))
		//supprime le contenu du répertoire si il n'était pas vide
		Path destPath = Files.createTempDirectory(null)
		destPath.toFile().deleteDir()
		//recrée le répertoire (bizarre de devoir faire ça)
		destPath = Files.createTempDirectory(null)
		repository.baseFolder = destPath.toFile()
	}
}
