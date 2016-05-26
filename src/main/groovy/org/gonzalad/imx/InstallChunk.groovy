package com.up.imx

class InstallChunk {
	List<Patch> patchs
	boolean disableCompilation
	int chunkCount
	InstallScript installScript
	
	/**
	 * Retourne la patches_list contenant tous les patchs de ce lot d'installation
	 */
	public String getPatchesList() {
		return patchs.collect { it.id }.join('\n')
	}
}
