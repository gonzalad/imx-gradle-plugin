package com.up.imx

class NoSuchPatchException extends RuntimeException {
	private List<String> patchs
	
	NoSuchPatchException (List<String> patchs) {
		this ("Les patchs $patchs ne font pas partie du repository local, vérifier qu'ils ont été enregistrés dans repository.json", patchs);
	}

	NoSuchPatchException (String message, String patch) {
		this (message, new ArrayList(patch))
	}
	
	NoSuchPatchException (String message, List<String> patchs) {
		super (message)
		this.patchs = patchs
	}

	List<String> getPatchs() {
		return patchs;
	}
	
	public String toString() {
		return super.toString() + ", patchs=$patchs"
	}	
}
