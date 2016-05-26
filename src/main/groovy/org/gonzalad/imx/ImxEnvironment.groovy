package com.up.imx

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

import org.hidetake.groovy.ssh.session.BadExitStatusException

@Slf4j
class ImxEnvironment {
	private ImxServer imxServer
	FabricationServer fabricationServer
	Database database
	Repository repository
	boolean manualInstall
	
	//String releaseDir

	//This profile script must contain all variables definitions (including variable $H)
	protected String profileScript = '. ~/.profile'
	//Imx Scripts
	protected String doInstallScriptFilename = 'do_imx_patch.ksh'
	protected String statusScript = '$H/bin/imxstatus.sh'
	protected String startScript = '$H/bin/start_instance.sh'
	protected String stopScript = '$H/bin/shutdown_iMX.sh'
	
	// plugin scripts (will be uploaded to installScriptDir)
	/** Valeur par défaut : /tmp/${imxServer.name}/imx-gradle-plugin/scripts */
	private String installScriptDir
	protected String watchInstallScriptFilename = 'watch_imx_patch.ksh'
	protected String installScriptFilename = 'imx_patch_launcher.ksh'
	protected String watchInstallContent = null
	protected String installScriptContent = null
	protected String watchedProcess = null
	
	void start() {
		if (imxServer == null) {
			throw new IllegalStateException("Propriété 'imxServer' non valorisée")
		}
		log.info('Début démarrage IMX')
		//		imxServer.session {
		//			execute '''
		//~/.profile
		//$H/bin/start_instance.ksh
		//'''
		//		}
		imxServer.service.run {
			session(imxServer.remote) { execute """
$profileScript
$startScript
""" }
		}
		log.info('IMX démarré')
	}

	void status() {
		if (imxServer == null) {
			throw new IllegalStateException("Propriété 'imxServer' non valorisée")
		}
		println """status : going to execute :
$profileScript
$statusScript
"""
		String statusString = imxServer.service.run {
			session(imxServer.remote) { 
				execute ignoreError: true, """
$profileScript
$statusScript
""" 
			}
		}
		componentStatus('DB access', statusString)
		componentStatus('DB listener', statusString)
		componentStatus('OS X11 vfb', statusString)
		componentStatus('OS Spooler', statusString)
		componentStatus('Oracle Fusion Forms', statusString)
		componentStatus('iMX Systeme Expert', statusString)
		// non utilisé en INT
		//componentStatus('iMX Document Server', statusString)
	}
	
	private void componentStatus(String component, String statusString) {
		Matcher matcher = statusString =~ /(?m)$component\ +.{1}\[1m\[ OK \]/
		if (! matcher) {
			throw new RuntimeException("$component not activated")
		}
	}

	void stop() {
		if (imxServer == null) {
			throw new IllegalStateException("Propriété 'imxServer' non valorisée")
		}
		log.info('Début arrêt IMX')
		imxServer.service.run {
			session(imxServer.remote) { execute """
$profileScript
$stopScript
""" }
		}
		log.info('IMX arrêté')
	}

	void restart() {
		if (imxServer == null) {
			throw new IllegalStateException("Propriété 'imxServer' non valorisée")
		}
		log.info('Début redémarrage IMX')
		imxServer.service.run {
			session(imxServer.remote) { execute """
$profileScript
$stopScript
$startScript
""" }
		}
		log.info('IMX démarré')
	}
	
	/**
	 * Install les patchs sans écraser les patchs existants dans le répertoires patchs du serveur IMX.
	 * 
	 * Voir aussi {@link #install(boolean)} avec forceUpload à false 
	 */
	void install() {
		install(repository.patches.collect { it.id } )
	}
	
	/**
	 * Installs all patches contained in the repository {@link #getRepository()}
	 *
	 * <p>
	 * This method :
	 * <ul>
	 * <li> checks if all patches are available in local repository (otherwise an exception is thrown).</li>
	 * <li> checks if all patches are already installed, if that's the case, this method ends.</li>
	 * <li> stop IMX server.</li>
	 * <li> installs the patches (only the patches that are not already installed).</li>
	 * <li> restarts the server.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * If you need to synchronize local repository with fabrication Server, be sure to call {@link #download()} 
	 * method before calling <code>install</code>.
	 * </p>
	 */
	void install(InstallOptions options) {
		install (null, options)
	}

	void install(List<String> patchs) {
		install(patchs, new InstallOptions())
	}
	
	void install(List<String> patchs, InstallOptions options) {
		if (imxServer == null) {
			throw new IllegalStateException("Propriété 'imxServer' non valorisée")
		}

		log.info('Début installation IMX')
		
		if (patchs == null) {
			patchs = repository.patches.collect { it.id }
		} else {

			// on vérifie que tous les patchs à installer sont dans repository.json
			List<String> patchesNotInRepository = []
			List<String> repositoryPatchIds = repository.patches.collect { it.id }
			patchs.each { 
				if (! repositoryPatchIds.contains(it)) {
					patchesNotInRepository.add(it)
				} 
			}
			if (patchesNotInRepository.size() > 0) {
				throw new IllegalArgumentException("Les patchs $patchesNotInRepository ne sont pas enregistrés dans repository.json");
			}
		}
		
		//0. On plante si fichiers de patchs sont en plusieurs exemplaires
		//   le ménage doit être fait à la main
		checkRepositoryValidity()

		// 1. création lots installation (on enlève les patchs déjà installés en SGBD + compilation activée)
		InstallScript script = createInstallScript(patchs, options)
		
		if (options.preview) {
			log.info('[Mode preview] : liste des patchs qui seraient installés : ' + script.getPatchesList())
			return
		}

		// 2. Optimisation : si pas de patchs à installer, on rend la main
		if (script.getPatchs().size() == 0) {
			log.info('Installation terminée : tous les patchs ont déjà été installés')
			return
		}
		
		// 4. Copie scripts d'installation MARK
		copyInstallScripts()

		// 5. Copie scripts d'installation
		uploadPatches(script.patchs, options.forceUpload)
		
		// 6. Exécution de l'installation
		if (manualInstall) {
			installManually(script)
		} else {
		
			// a. gestion des locks (suppression ou génération d'une erreur LockException) 
			imxServer.manageLocks(options.removeLocks)

			// b. arrêt IMX
			if (options.stopBeforeInstall) {
				stop()
			}

			// c. installation
			script.chunks.each { installChunk(it) }

			// d. démarrage IMX
			if (options.startAfterInstall) {
				start()
			}
		}
		
		log.info('Installation terminée')

	}

	
	private void checkRepositoryValidity() {
		
		List<RepositoryError> errors = repository.checkValidity()

		// 1. duplicate patch files -> error
		List<String> duplicatePatchFileNames = errors.findAll { it.errorType == RepositoryError.ErrorTypeEnum.DUPLICATE_PATCH_FILE }
											.collect { it.patchId }
		if (duplicatePatchFileNames.size() > 0) {
			throw new DuplicatePatchException(duplicatePatchFileNames)
		}
		
		//2. More patch files than patches declared in repository.json
		List<RepositoryError> tooManyLocalFiles = errors.findAll { it.errorType == RepositoryError.ErrorTypeEnum.TOO_MANY_LOCAL_FILE }
		tooManyLocalFiles.each {
			log.warn(it.message)
		}
	}

	/**
	 * Upload les patches du repo local vers le serveur IMX.
	 * 
	 * Par défaut, seuls les patchs qui ne sont pas déjà sur le serveur IMX sont uploadés.
	 * 
	 * @param patches liste des patchs à uploader
	 * @param forceUpload force l'upload des patchs si il existent déjà sur le serveur IMX.
	 */
	void uploadPatches(List<Patch> patches, boolean forceUpload) {
		if (imxServer.remote.host.equals(fabricationServer.host)
			&& imxServer.getPatchFolder().equals(fabricationServer.patchDir)) {
			
			log.warn('Le répertoire de patchs du serveur IMX est identique à celui du serveur de fabrication ' 
				+ imxServer.getPatchFolder()  + '. Upload des patchs (repo local -> serveur IMX) est DESACTIVE.')
			return
		}
		imxServer.uploadPatches(patches.collect { it.id }, repository.baseFolder, forceUpload)
	}
	
	/**
	 * Copie les patchs indiqués dans repository dans le répertoire de release
	 */
	void release(String releaseDir) {
		if (releaseDir == null) {
			throw new IllegalStateException('Propriété releaseDir non valorisée')
		}
		release (null, releaseDir)
	}

	/**
	 * Copie les patchs patchs du repository locale dans le répertoire de release.
	 */
	void release(List<String> patchs, String releaseDir) {
		if (releaseDir == null) {
			throw new IllegalStateException('Propriété releaseDir non valorisée')
		}
		
		checkRepositoryValidity()

		//si on n'indique pas de patchs, on release la totale
		if (patchs == null || patchs.size() == 0) {
			patchs = repository.getPatches().collect { it.id };
		}
		InstallScript script = createInstallScript(patchs, new InstallOptions(forceInstall: true))
		if (script.getChunks().size() == 1) {
			new File(new File(releaseDir), 'patches_list') << script.getChunks()[0].getPatchesList()
		} else if (script.getChunks().size() > 1) {
			script.getChunks().eachWithIndex{ chunk, idx -> 
				new File(new File(releaseDir), "patches_list$idx") << chunk.getPatchesList()
			}
		}
		repository.copyTo(patchs, new File(releaseDir))
	}
	
	/**
	 * Copie les patchs indiqués dans repository dans le répertoire de release
	 */
	void releaseResources(String releaseDir) {
		releaseResources (null, releaseDir)
	}

	/**
	 * Copie les patchs patchs du repository locale dans le répertoire de release.
	 */
	void releaseResources(List<String> patchs, String releaseDir) {
		if (releaseDir == null) {
			throw new IllegalStateException('Propriété releaseDir non valorisée')
		}
		//si on n'indique pas de patchs, on release la totale
		if (patchs == null || patchs.size() == 0) {
			patchs = repository.getPatches().collect { it.id };
		}
		repository.copyResourcesTo(patchs, new File(releaseDir))
	}

	/**
	 * Copie les patchs indiqués dans repository dans le répertoire de release
	 */
	void releaseDocs(String releaseDir) {
		releaseDocs (null, releaseDir)
	}

	/**
	 * Copie les patchs patchs du repository locale dans le répertoire de release.
	 */
	void releaseDocs(List<String> patchs, String releaseDir) {
		if (releaseDir == null) {
			throw new IllegalStateException('Propriété releaseDir non valorisée')
		}
		//si on n'indique pas de patchs, on release la totale
		if (patchs == null || patchs.size() == 0) {
			patchs = repository.getPatches().collect { it.id };
		}
		repository.copyDocsTo(patchs, new File(releaseDir))
	}

	private void installChunk(InstallChunk chunk) {
		log.info("Installation du lot : $chunk.chunkCount")
		log.info("patches_list à installer :\n$chunk.patchesList")

		imxServer.service.run {
			session(imxServer.remote) {
				
				// création patches_list
				put text: chunk.getPatchesList(), into: "${imxServer.imxHome}/patchs/patches_list" //, encoding: encoding

				// lancement Install IMX
				execute """
$profileScript
cd $installScriptDir
./$installScriptFilename
"""

				// watch Install Logs & wait end of install
				try {
					String returnCode = execute """
$profileScript
cd $installScriptDir
./$watchInstallScriptFilename
"""
					println "install returnCode: $returnCode"
				} catch (BadExitStatusException err) {
					if (err.exitStatus == 166) {
						throw new ImxInstallationException("Installation en erreur, vérifiez les logs : " + err.toString(), err)
					}
					if (err.exitStatus == 167) {
						throw new ImxInterruptedInstallationException("Installation interrompue, vérifiez les logs : " + err.toString(), err)
					}
					throw new ImxInstallationException("Erreur lors de l'analyse des logs d'install. L'installation est peut être en cours. Veuillez vérifier manuellement. Détails : " + err.toString(), err)
				}
			}
		}
		log.info("Installation du lot $chunk.chunkCount terminée")
	}
	
	/** 
	 * stocke le fichier ksh d'installation a lancer pour une installation manuelle
	 * 
	 * A ne pas utiliser pour des vrais installations (i.e. CTRL+C annule les installations des lots suivants)
	 */
	private void installManually(InstallScript script) {
		log.debug("Installation manuelle, upload des scripts à exécuter pour l'installation manuelle")

		imxServer.service.run {
			session(imxServer.remote) {

				String installSh = """#/usr/bin/ksh
"""
				script.chunks.each { chunk ->
					
					// création patches_list
					log.info("patches_list à installer : $chunk.patchesList")
					put text: chunk.getPatchesList(), into: "$installScriptDir/patches_list_${chunk.chunkCount}" //, encoding: encoding
					
					//ajout commandes au script
					installSh += """
#Installation Lot ${chunk.chunkCount}/${chunk.installScript.chunks.size()}
rm -f ${imxServer.imxHome}/patchs/patches_list
cd $installScriptDir				
cp patches_list_${chunk.chunkCount} ${imxServer.imxHome}/patchs/patches_list
./$installScriptFilename				

#Attente fin installation Lot ${chunk.chunkCount}/${script.chunks.size()}
./$watchInstallScriptFilename
"""				
				}
				
				
				// création scripts d'installation
				put text: installSh, into: "$installScriptDir/up_manual_patch_install.ksh" //, encoding: encoding
				
				execute "chmod 770 $installScriptDir/up_manual_patch_install.ksh"
			}
		}
		log.info("Pour compléter l'installation manuelle, se connecter au serveur IMX et lancer $installScriptDir/up_manual_patch_install.ksh")
	}

	public String getWatchInstallScript() {
		return "$installScriptDir/$watchInstallScriptFilename"
	}
	
	public String getInstallScript() {
		return "$installScriptDir/$installScriptFilename"
	}
	
	public String getWatchInstallContent() {
		String content = (watchInstallContent != null ? watchInstallContent : getScriptContentFromClasspath("scripts/watch_imx_patch.ksh"))
		return executeTemplate(content)
	}
	
	public String getInstallScriptContent() {
		String content = (installScriptContent != null ? installScriptContent : getScriptContentFromClasspath("scripts/imx_patch_launcher.ksh"))
		return executeTemplate(content)
	}
	
	private String executeTemplate(String templateText) {
		[
			'$imxHome': imxServer.imxHome,
			'$doInstallScriptFilename': doInstallScriptFilename,
			'$watchedProcess': watchedProcess
		].each{ 
			k, v -> templateText = templateText.replace(k, v) 
		}
		return templateText
	}

	private void copyInstallScripts() {
		log.debug("Copie des scripts d'installation vers $installScriptDir")
		imxServer.service.run {
			session(imxServer.remote) {

				execute "mkdir -p $installScriptDir"
				put text: getWatchInstallContent(), into: "$watchInstallScript" //, encoding: encoding
				put text: getScriptContentFromClasspath("scripts/watchdog.ksh"), into: "$installScriptDir/watchdog.ksh" //, encoding: encoding
				put text: getInstallScriptContent(), into: "$installScript" //, encoding: encoding
				execute """chmod 770 $watchInstallScript
chmod 770 $installScriptDir/watchdog.ksh
chmod 770 $installScript
"""
			}
		}
		log.debug("Scripts d'installation copiés")
	}

	private String getScriptContentFromClasspath(String script) {
		return getClass().getClassLoader().getResourceAsStream(script).getText(); // TODO : passer charSet à text
	}
	
	void download() {
		download(false)
	}

	void download(boolean forceDownload) {
		download(repository.patches.collect { it.id } , forceDownload)
	}
	
	void download(List<String> patches) {
		download(patches, false)
	}

	void download(List<String> patchIds, boolean forceDownload) {
		if (repository == null) {
			throw new IllegalStateException("Propriété 'repository' non valorisée")
		}
		if (fabricationServer == null) {
			throw new IllegalStateException("Propriété 'fabricationServer' non valorisée")
		}
		List<Patch> patches;
		if (patchIds == null || patchIds.size() == 0) {
			patches = repository.patches
		} else {
			patches = repository.patches.findAll { patchIds.contains(it.id) }
		}
		fabricationServer.download(patches, repository.baseFolder, forceDownload)
	}

	InstallScript createInstallScript(List<String> patchIds, InstallOptions options) {
		if (repository == null) {
			throw new IllegalStateException("Propriété 'repository' non valorisée")
		}
		if (options == null) {
			options = new InstallOptions()
		}
		List<Patch> patchs;
		if (patchIds == null || patchIds.size() == 0) {
			patchs = repository.patches
		} else {
			patchs = repository.patches.findAll { patchIds.contains(it.id) }
		}
		List patchsToInstall
		log.info("options: $options")
		if (! options.forceInstall) {
			if (database == null) {
				throw new IllegalStateException("Propriété 'database' non valorisée")
			}
			Map<String,InstalledPatch> installedPatches = database.getInstalledPatches()
			patchsToInstall = patchs.findAll {  !installedPatches.containsKey(it.id) }
			/*log.warn("patchs: $patchs")
			log.warn("installedPatches: $installedPatches")
			log.warn("patchsToInstall: $patchsToInstall")*/
		} else {
			patchsToInstall = patchs
		}
		return new InstallScript(patchsToInstall, options.disableCompilation)
	}
	
	public void setImxServer(ImxServer imxServer) {
		this.imxServer = imxServer
		if (getInstallScriptDir() == null) {
			setInstallScriptDir("/tmp/${imxServer.id}/imx-gradle-plugin/scripts")
		}
		if (getWatchedProcess() == null) {
			setWatchedProcess("tee -a ${imxServer.imxHome}/patchs/imx_patch.log")
		}
	}
	
	public String getWatchedProcess() {
		return watchedProcess
	}
	
	public void setWatchedProcess(String watchedProcess) {
		this.watchedProcess = watchedProcess
	}
	
	public String getInstallScriptDir() {
//		if (installScriptDir == null) {
//			return "/tmp/${imxServer.id}/imx-gradle-plugin/scripts"
//		} else {
		return installScriptDir
//		}
	}
	
	public void setInstallScriptDir(String installScriptDir) {
		log.warn("setInstallScriptDir: " + installScriptDir)
		this.installScriptDir = installScriptDir
		log.warn("this.installScriptDir: " + this.installScriptDir)
	}
}

