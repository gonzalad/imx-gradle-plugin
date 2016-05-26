package com.up.imx

import groovy.transform.ToString;

@ToString
class InstallOptions {
	boolean forceUpload
	boolean removeLocks
	/**
	 * Arr�te le serveur avant l'installation des patchs
	 * 
	 * Valeur par d�faut : true
	 */
	boolean stopBeforeInstall = true
	/**
	 * Cette option affiche les patchs qui seraient install�s mais sans les installer.
	 * Idem, pas d'arr�t ou d�marrage de IMX.
	 * 
	 * Valeur par d�faut : false
	 */
	boolean preview = false
	/**
	 * Red�marre le serveur IMX lorsque l'installation est termin�e.
	 * A noter, IMX ne sera pas d�marr� si l'installation est en erreur, quelle que
	 * soit la valeur de ce flag.
	 * 
	 * Valeur par d�faut : true 
	 */
	boolean startAfterInstall = true
	/**
	 * Par d�faut, le plugin installe uniquement les patchs qui ne sont pas d�j� install�s 
	 * (non existants dans t_patch et status <> 'OK').
	 * 
	 * Passer cette option � true force la r�installation des patchs.
	 * 
	 * Valeur par d�faut : false
	 */
	boolean forceInstall
	/**
	 * Si la compilation est d�sactiv�e lors de l'installation, le plugin regarde dans le fichier json les
	 * patchs qui r�clament une compilation imm�diate. Seuls ces patchs sont install�s avec la compilation
	 * activ�e.
	 * 
	 * Une fois tous les patchs install�s, l'installeur lance une phase de compilation globale.
	 * 
	 * TODO : ce flag n'est pas pris en compte pour le moment pour la phase d'install, uniquement pour la phase
	 * de release
	 * 
	 * Valeur par d�faut : false
	 */
	boolean disableCompilation
}
