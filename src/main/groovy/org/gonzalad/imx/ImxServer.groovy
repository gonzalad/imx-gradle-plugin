package com.up.imx

import org.hidetake.groovy.ssh.core.Remote;
import org.hidetake.groovy.ssh.core.Service;

import groovy.lang.Delegate;

class ImxServer extends SshServer {
	
	//Nom logique du serveur IMX (i.e. asterix, etc...)
	String id

	//Imx Home directory
	String imxHome = '$H'
	
	public ImxServer(String id, String imxHome, Remote remote, Service service) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("Argument 'id' doit être renseigné")
		}
		if (imxHome == null || imxHome.trim().length() == 0) {
			throw new IllegalArgumentException("Argument 'imxHome' doit être renseigné")
		}
		if (remote == null) {
			throw new IllegalArgumentException("Argument 'remote' doit être renseigné") 
		}
		if (service == null) {
			throw new IllegalArgumentException("Argument 'service' doit être renseigné") 
		}
		this.id = id
		this.imxHome = imxHome
		this.remote = remote
		this.service = service
	}
	
	/**
	 * Upload les patches du repo local vers le serveur IMX.
	 *
	 * Par défaut, seuls les patchs qui ne sont pas déjà sur le serveur IMX sont uploadés.
	 *
	 * @param patches liste des patchs à uploader
	 * @param repositoryBaseFolder emplacement du repo local contenant les patchs à uploader
	 * @param forceUpload force l'upload des patchs si il existent déjà sur le serveur IMX.
	 */
	void uploadPatches(List<String> patchesId, String repositoryBaseFolder, boolean forceUpload) {
		
		// 1. Déterminer les patchs à uploader
		List<String> patchesToUpload = []
		if (! forceUpload) {
			List<String> patchesOnServer = getPatchesOnServer()
			patchesId.each { 
				if (! patchesOnServer.contains(it)) {
					patchesToUpload.add(it)
				} 
			}
		} else {
			patchesToUpload = patchesId
		}

		// 2. Upload des patchs
		service.run {
			session(remote) { 
			println "repositoryBaseFolder/it: $repositoryBaseFolder/$it"
			println "getPatchFolder: " + getPatchFolder()
			patchesToUpload.each {
				put from: "$repositoryBaseFolder/$it", into: getPatchFolder()
			}
		  }
		}
	}

	void uploadPatches(List<String> patchesId, File repositoryBaseFolder, boolean forceUpload) {
		uploadPatches(patchesId, repositoryBaseFolder.toString(), forceUpload)
	}
	
	private List<String> getPatchesOnServer() {
		// ignoreError = true car erreur si ls *.tar.Z ne trouve aucun patch 
		String patchList = service.run {
			session(remote) {
				println "imxHome: $imxHome"
				execute ignoreError: true, """cd $imxHome
cd patchs
ls *.tar.Z
""" 
			}
		}
		if (patchList == null) {
			patchList = []
		}
		List<String> patchIds = new ArrayList()
		((CharSequence) patchList).eachLine { patchIds.add(it) }
		return patchIds;
	}
	
	/**
	 * @param removeLocks if true, remove all imxHome/patchs/*.lock files.
	 * If false and a lock file exists, it throws an exception
	 *
	 * @exception LockException if removeLocks=false and lock file exists, it throws an exception
	 */
	void manageLocks(boolean removeLocks) {
		if (removeLocks) {
			service.run {
				session(remote) {
					execute ignoreError: true, """cd $imxHome/patchs
	rm *.lock
	rm imx_patch.conf
	""" 
				}
			}
		} 
		// ignoreError = true car erreur si ls *.tar.Z ne trouve aucun patch
		String lockFilesResult = service.run {
			session(remote) {
				execute ignoreError: true, """cd $imxHome/patchs
ls *.lock
ls imx_patch.conf
""" 
			}
		}
		if (lockFilesResult == null) {
			lockFilesResult = []
		}
		List<String> lockFiles = new ArrayList()
		((CharSequence) lockFilesResult).eachLine { lockFiles.add(it) }
		if (lockFiles.size() > 0) {
			throw new LockException ("Une installation ne s'est pas terminée proprement ou est en cours (fichiers *.lock ou imx_patch.conf présents dans IMX_HOME/patchs : $lockFiles)")
		}
	}
	
	public String getPatchFolder() {
		return "$imxHome/patchs"
	}

}
