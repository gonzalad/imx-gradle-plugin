package com.up.imx;

public class UnavailablePatchException extends RuntimeException {
	List<String> patchIds;
	
	public UnavailablePatchException(String message, List<String> patchIds) {
		super(message + " {" + String.join(",", patchIds) + "}")
	}
}
