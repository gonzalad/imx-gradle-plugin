package com.up.imx

class RepositoryError {
	ErrorTypeEnum errorType
	String patchId
	String message
	
	static enum ErrorTypeEnum {
		/** Si patch en double dans repository.json */
		DUPLICATE_PATCH,
		/** Si fichier patch en double dans dossier folderRepo */
		DUPLICATE_PATCH_FILE,
		/** 
		 * Si des fichiers patchs (tar.Z) existent dans le repository local 
		 * alors qu'il ne sont pas déclarés dans le repository.json 
		 */
		TOO_MANY_LOCAL_FILE

		//		/** Si fichier patch non existant dans folderRepo */
//		PATCH_NOT_DEFINED_IN_REPO
	}
}
