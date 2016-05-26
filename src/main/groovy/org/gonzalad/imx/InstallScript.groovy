package com.up.imx

import groovy.transform.ToString;
import groovy.transform.TupleConstructor;

@ToString(includeNames=true)
class InstallScript {
	List<Patch> patchs
	boolean disableCompilation
	List<InstallChunk> chunks

	public InstallScript(List<Patch> patchs, boolean disableCompilation) {
		this.patchs = patchs
		this.disableCompilation = disableCompilation
		this.chunks = initializeChunks()
	}

	private List<InstallChunk> initializeChunks() {
		List<InstallChunk> chunks = []
		if (disableCompilation) {
			int chunkCount = 1
			InstallChunk currentChunk = null
			for (int i = 0; i < patchs.size(); i++) {
				Patch currentPatch = patchs.get(i)
				//ajout d'un nouveau lot
				if (currentChunk == null) {
					currentChunk = new InstallChunk(patchs: [], disableCompilation: !currentPatch.compilation, chunkCount: chunkCount, installScript: this)
					chunks.add(currentChunk)
					chunkCount++
				} else {
					//attention double n�gation
					if (currentChunk.disableCompilation == currentPatch.compilation) {
						currentChunk = new InstallChunk(patchs: [], disableCompilation: !currentPatch.compilation, chunkCount: chunkCount, installScript: this)
						chunks.add(currentChunk)
						chunkCount++
					}
				}
				//ajout du patch dans le lot
				currentChunk.patchs.add(currentPatch)
			}
		} else {
			//la compilation est activ�e : on n'a qu'un seul lot
			chunks.add(new InstallChunk(patchs: patchs, chunkCount: 1, installScript: this))
		}
		return chunks;
	}
	
	/**
	 * Retourne la liste des patchs qui serait install�s par les chunk de cette instance d'InstallScript
	 * 
	 * Utile pour afficher l'info � l'utilisateur.
	 * 
	 * Le script d'installation se r�f�re plut�t � {@link InstallChunk#getPatchesList()}
	 */
	public String getPatchesList() {
		chunks.collect { it.patchs.collect { it.id }.flatten() }.flatten()
	}
}
