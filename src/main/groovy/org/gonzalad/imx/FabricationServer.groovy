package com.up.imx

import groovy.util.logging.Slf4j;

@Slf4j
class FabricationServer extends SshServer {
	String patchDir

	/**
	 * @param patchs patchs � downloader
	 * @param outputFolder dossier dans lequel les patchs sont download�s
	 * @param forceDownload si positionn� � true, les patchs du serveur de fabrication �crasent les patchs
	 * existant en local. Si false, alors on ne download que les patchs qui n'existent pas dans outputFolder
	 * 
	 * @exception UnavailablePatchException si un des patchs a downloader n'est pas dispo
	 * dans le serveur de fabrication.
	 */
	void download(List<Patch> patchs, File outputFolder, boolean forceDownload) throws UnavailablePatchException {

		if (patchs == null || outputFolder == null) {
			throw new IllegalArgumentException("Arguments patchIds et outputFolder obligatoires")
		}
		if (!outputFolder.exists()) {
			throw new IllegalArgumentException("le r�pertoire $outputFolder n'existe pas")
		}
		
		log.info("Download des patchs du serveur de fabrication forceDownload=$forceDownload")
		
		// 1. R�cup�ration de la liste des patchs � downloader (on ne redownload pas forc�ment les patchs)
		List<String> patchIds = patchs.collect { it.id } 
		List<String> patchIdsToDownload = getPatchesToDownload(patchIds, outputFolder, forceDownload)
		log.debug("Patchs � downloader : {}", patchIdsToDownload.join(","))
		
		// 2. v�rification que les patchs sont disponibles sur serveur de fabrication
		checkIfPatchesAreAvailable(patchIdsToDownload)
		
		// 3. download des patchs
		String patchList = service.run {
			session(remote) {
				for (String patchId : patchIdsToDownload) {
					get from: "$patchDir/$patchId", into: "$outputFolder/$patchId"
				}
			}
		}
		log.info("Patchs download�s")
	}
	
	/**
	 * V�rifie que les patchs � downloader sont disponibles sur serveur de fabrication.
	 * Sinon g�n�re une UnavailablePatchException.
	 * 
	 * @exception UnavailablePatchException si un des patchs n'est pas disponible
	 */	
	 void checkIfPatchesAreAvailable(List<String> patchIds) throws UnavailablePatchException {
		List<String> patchInFabricationServer =  getPatchIdList()
		List missingPatches = patchIds.minus(patchInFabricationServer)
		if (missingPatches.size() != 0) {
			throw new UnavailablePatchException("Patchs non disponibles sur le serveur d'int�gration", missingPatches);
		}
	}
	
	
	/**
	 * Retourne la liste des patchs � downloader du serveur de fabrication.
	 * 
	 * La liste retourn�e ne peut �tre null.
	 */
	List<String> getPatchesToDownload(List<String> patchIds, File outputFolder, boolean forceDownload) {
		List<String> patchToDownload;
		if (forceDownload == false) {
			//Set<String> patchInFabricationServer = getPatchIdSet();
			patchToDownload = new ArrayList<String>()
			for (String patchId: patchIds) {
				if (! (new File(outputFolder, patchId).exists())) {
					patchToDownload.add(patchId)
				}
			}
		} else {
			patchToDownload = patchIds
		}
		return patchToDownload
	}

	boolean contains(List<String> patchIds) {
		Set<String> availablePatches = getPatchIdSet()
		for (String patch : patchIds) {

			if (! availablePatches.contains(patch)) {
				return false
			}
		}
		return true
		//		imxServer.service.run {
		//			session(imxServer.remote) {
		//				for (String id : patchIds) {
		//					String patch = execute "ls -l ${patchDir}/${id}.tar.Z"
		//					if (patch.trim().length() == 0) {
		//						return false;
		//					}
		//				}
		//			}
		//		}
		//		return true;
	}

	boolean contains(String patchId) {
		return contains(Collections.singletonList(patchId))
	}

	List<String> getPatchIdList() {
		String patchList = service.run {
			session(remote) { 
				def command = """cd ${patchDir}
ls *.tar.Z
""" 
				println "going to execute $command"
				def returnCode = execute command 
				//seul les returnCode OK ou pas de fichiers tar.Z sont tol�r�s 
//				if (returnCode != 0 && returnCode != 2) {
//					throw new RuntimeException ("Erreur $returnCode a l'execution de la commande $command")
//				}
			}
		}
		List<String> patchIds = new ArrayList()
		((CharSequence) patchList).eachLine { patchIds.add(it) }
		return patchIds;
	}

	Set<String> getPatchIdSet() {
		Set<String> patchIdSet = new HashSet<String>();
		return getPatchIdList().each{ patchIdSet.add(it) }
	}
}
