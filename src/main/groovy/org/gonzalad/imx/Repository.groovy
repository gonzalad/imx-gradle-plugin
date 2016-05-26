package com.up.imx

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.NoSuchFileException

import static groovy.io.FileType.FILES

class Repository {
	File baseFolder
	private List<Patch> patches
	private LinkedHashMap<String,Patch> patchMap

	/**
	 * Retourne le patch id.
	 * 
	 * Si le patch listé n'est pas disponible dans le repository, retourne null
	 */
	Patch getPatch(String id) {
		return getPatchMap().get(id)
	}

	boolean contains(String id) {
		return getPatchMap().containsKey(id)
	}

	boolean isDownloaded(String id) {
		return new File(baseFolder, id).exists()
	}

	List<Patch> getPatches() {
		return patches
	}

	void setPatches(List<Patch> patches) {
		this.patches = patches
		this.patchMap = null
	}

	List<File> getPatchAsFiles() {
		return patches.collect { new File (baseFolder, it.id) }
	}
	
	/**
	 * Retourne une liste des erreurs trouvées dans le repo {@see RepositoryError.ErrorTypeEnum} pour
	 * les types d'erreurs
	 */
	List<RepositoryError> checkValidity() {
		
		List<RepositoryError> errors = []
		
		// 1. détection fichiers patchs dupliqués
		List<File> patchFiles = []
		baseFolder.eachFileRecurse(FILES) {
			if(it.name.endsWith('.tar.Z')) {
				patchFiles += it
			}
		}
		List<String> duplicatePatchFileNames = patchFiles.collect{ it.name }
		duplicatePatchFileNames = duplicatePatchFileNames.findAll {
			String currentFileName = it 
			return (duplicatePatchFileNames.count { it == currentFileName } > 1) 
		} .unique()
		
		duplicatePatchFileNames.each {
			errors.add(new RepositoryError(errorType: RepositoryError.ErrorTypeEnum.DUPLICATE_PATCH_FILE,
				patchId: it, message: "Fichier patch ${it} en plusieurs exemplaires dans repository local"))
		}
		
		// 2. duplicate patchs in local repository (in repository.json)
		List<Patch> duplicatePatches = patches.findAll { patches.count(it) > 1 }.unique()
		duplicatePatches.each {
			errors.add(new RepositoryError(errorType: RepositoryError.ErrorTypeEnum.DUPLICATE_PATCH,
				patchId: it.id, message: "Patch ${it} en plusieurs exemplaires dans repository local"))
		}
		
		//3. TOO_MANY_LOCAL_FILE
		List<String> tooManyLocalFiles = patchFiles.collect{ it.name }
		tooManyLocalFiles = tooManyLocalFiles.findAll { patchMap.containsKey(it) == false }.unique()
		tooManyLocalFiles.each {
			errors.add(new RepositoryError(errorType: RepositoryError.ErrorTypeEnum.TOO_MANY_LOCAL_FILE,
				patchId: it, message: "Le repository.json ne déclare pas le patch ${it} alors qu'un fichier existe. Merci de faire le ménage ou de l'ajouter dans repository.json."))
		}
		
		return errors
	}
	
	/**
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 */
	void copyTo(File destinationFolder) {
		copyTo(patches.collect { it.id }, destinationFolder)
	}

	/**
	 * Copie les patchs <code>patchs</code> du reporsitory local vers le répertoire de destination
	 * 
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 * @exception NoSuchPatchException si un des patchs demandés n'est pas enregistré dans repo local 
	 */
	void copyTo(List<String> patchIds, File destinationFolder) {

		// 1. vérifier que les patchs sont bien présents dans repository local
		List unregisteredPatchs = patchIds.findAll { contains(it) == false }
		if (unregisteredPatchs.size() > 0) {
			throw new NoSuchPatchException(unregisteredPatchs)
		}

		// 2. récupérer les fichiers à copier
		List<File> filesToCopy = patches.findAll { patchIds.contains(it.id) }.collect { new File (baseFolder, it.id) }

		// 3. check file existence
		List<String> inexistentFiles = filesToCopy.findAll { ! Files.exists(it.toPath()) }

		if (inexistentFiles.size() > 0) {
			String inexistentFilesAsString = inexistentFiles.join(',')
			throw new NoSuchFileException("Les fichiers suivants n'existent pas dans le repo local : $inexistentFilesAsString")
		}

		//copy files
		filesToCopy.each {
			println "copie de $it vers " + destinationFolder.toPath()
			Files.copy(it.toPath(), destinationFolder.toPath().resolve(it.getName()))
		}
	}
	
	/**
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 */
	void copyResourcesTo(File destinationFolder) {
		copyResourcesTo(patches.collect { it.id }, destinationFolder)
	}
	
	/**
	 * Copie les fichiers sql et autres ressources des patchs <code>patchs</code> du reporsitory local vers le répertoire de destination
	 *
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 * @exception NoSuchPatchException si un des patchs demandés n'est pas enregistré dans repo local
	 */
	void copyResourcesTo(List<String> patchIds, File destinationFolder) {
		copyTo (patchIds, { it.resources }, destinationFolder)
	}

	/**
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 */
	void copyDocsTo(File destinationFolder) {
		copyDocsTo(patches.collect { it.id }, destinationFolder)
	}

	/**
	 * Copie les fiches documentation des patchs <code>patchs</code> du reporsitory local vers le répertoire de destination
	 *
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 * @exception NoSuchPatchException si un des patchs demandés n'est pas enregistré dans repo local
	 */
	void copyDocsTo(List<String> patchIds, File destinationFolder) {
		copyTo (patchIds, { it.docs }, destinationFolder)
	}

	/**
	 * Copie les fiches documentation des patchs <code>patchs</code> du reporsitory local vers le répertoire de destination
	 *
	 * @exception NoSuchFileException si un des fichiers du repo local n'existe pas en local
	 * (si il n'a pas encore été downloadé).
	 * @exception NoSuchPatchException si un des patchs demandés n'est pas enregistré dans repo local
	 */
	void copyTo(List<String> patchIds, Closure fileNamesProvider, File destinationFolder) {

		// 1. vérifier que les patchs sont bien présents dans repository local
		List unregisteredPatchs = patchIds.findAll { contains(it) == false }
		if (unregisteredPatchs.size() > 0) {
			throw new NoSuchPatchException(unregisteredPatchs)
		}

		// 2. récupérer les fichiers à copier
		List<File> filesToCopy = patches.findAll { fileNamesProvider(it) && patchIds.contains(it.id) }
									.collect { fileNamesProvider(it) }.flatten().unique().collect { new File (baseFolder, it) }
		//List<File> filesToCopy = fileNamesProvider().unique().collect { new File (baseFolder, it) }
		
//		List<File> filesToCopy = patches.findAll { it.doc && patchIds.contains(it.id) }.collect { it.doc }
//									.unique().collect { new File (baseFolder, it) }

		// 3. check file existence
		List<String> inexistentFiles = filesToCopy.findAll { ! Files.exists(it.toPath()) }

		if (inexistentFiles.size() > 0) {
			String inexistentFilesAsString = inexistentFiles.join(',')
			throw new NoSuchFileException("Les fichiers suivants n'existent pas dans le repo local : $inexistentFilesAsString")
		}

		//copy files
		filesToCopy.each {
			println "copie de $it vers " + destinationFolder.toPath()
			Files.copy(it.toPath(), destinationFolder.toPath().resolve(it.getName()))
		}
	}
	
	Map<String,Patch> getPatchMap() {
		if (patchMap == null) {
			patchMap = new LinkedHashMap<String,Patch>()
			patches.each {  patchMap.put(it.id, it) }
		}
		return patchMap
	}

	static newRepository(InputStream jsonStream) {
		ObjectMapper mapper = new ObjectMapper()
		mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		return mapper.readValue((InputStream) jsonStream, Repository.class)
	}

	static newRepository(File file) {
		return newRepository(new FileInputStream(file))
	}

	//	static newRepository(File jsonFile) {
	//		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
	//		mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
	//		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
	//		return mapper.readValue(jsonFile, com.up.imx.Repository.class)
	//	}
}
