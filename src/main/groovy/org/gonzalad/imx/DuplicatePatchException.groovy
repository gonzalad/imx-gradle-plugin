package com.up.imx

import java.util.List;

class DuplicatePatchException extends RuntimeException {
	private List<String> patchs		DuplicatePatchException (List<String> patchs) {		this ("Les fichier des patchs $patchs sont en plusieurs exemplaires dans le repository local");
		this.patchs = patchs	}	List<String> getPatchs() {		return patchs;	}}