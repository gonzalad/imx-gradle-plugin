package com.up.imx

class InstalledPatch {
	String id
	Date installDate
	Long installId

	String toString() {
		return super.toString() + "{id: $id}"
	}
}
