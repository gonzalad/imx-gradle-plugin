package com.up.imx

class Patch {
	/** 
	 * patch id (i.e. DmPatch20150915_1.tar.Z) 
	 */
	String id
	
	/** 
	 * if this patch needs immediate Compilation 
	 */
	boolean compilation
	/**
	 * Fiche de livraison du patch
	 */
	List<String> docs
	/**
	 * Resources liées au patch (i.e SQL, autre...)
	 */
	List<String> resources
	
	public void setDoc(String doc) {
		docs = [doc]
	}
	
	public String getDoc() {
		return docs.size() > 0 ? docs.get(0) : null 
	}

	String toString() {
		return super.toString() + "{id: $id, compilation: $compilation, docs: $docs}"
	}
}
