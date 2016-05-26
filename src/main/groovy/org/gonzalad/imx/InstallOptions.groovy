package com.up.imx

import groovy.transform.ToString;

@ToString
class InstallOptions {
	boolean forceUpload
	boolean removeLocks
	/**
	 * Arrête le serveur avant l'installation des patchs
	 * 
	 * Valeur par défaut : true
	 */
	boolean stopBeforeInstall = true
	/**
	 * Cette option affiche les patchs qui seraient installés mais sans les installer.
	 * Idem, pas d'arrêt ou démarrage de IMX.
	 * 
	 * Valeur par défaut : false
	 */
	boolean preview = false
	/**
	 * Redémarre le serveur IMX lorsque l'installation est terminée.
	 * A noter, IMX ne sera pas démarré si l'installation est en erreur, quelle que
	 * soit la valeur de ce flag.
	 * 
	 * Valeur par défaut : true 
	 */
	boolean startAfterInstall = true
	/**
	 * Par défaut, le plugin installe uniquement les patchs qui ne sont pas déjà installés 
	 * (non existants dans t_patch et status <> 'OK').
	 * 
	 * Passer cette option à true force la réinstallation des patchs.
	 * 
	 * Valeur par défaut : false
	 */
	boolean forceInstall
	/**
	 * Si la compilation est désactivée lors de l'installation, le plugin regarde dans le fichier json les
	 * patchs qui réclament une compilation immédiate. Seuls ces patchs sont installés avec la compilation
	 * activée.
	 * 
	 * Une fois tous les patchs installés, l'installeur lance une phase de compilation globale.
	 * 
	 * TODO : ce flag n'est pas pris en compte pour le moment pour la phase d'install, uniquement pour la phase
	 * de release
	 * 
	 * Valeur par défaut : false
	 */
	boolean disableCompilation
}
