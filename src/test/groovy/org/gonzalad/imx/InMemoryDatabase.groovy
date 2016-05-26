package com.up.imx;

import java.util.HashMap;
import java.util.Map;

public class InMemoryDatabase extends Database {
	private Map<String,InstalledPatch> installedPatches = new HashMap<>()
			
	@Override
	public Map<String,InstalledPatch> getInstalledPatches() {
		installedPatches
	}
}
