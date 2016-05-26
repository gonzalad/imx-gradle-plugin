package com.up.imx

import java.util.Map;

import groovy.sql.Sql
import groovy.transform.TupleConstructor;

@TupleConstructor
class PatchDao {
	Sql sql

	public Map<String,InstalledPatch> getInstalledPatches() {
		Map installedPatches = new HashMap<String,InstalledPatch>()
		sql.eachRow('select id, patch, time from t_patch where status = \'OK\'') { row ->
			InstalledPatch installedPatch = new InstalledPatch()
			installedPatch.id = row.patch
			installedPatch.installId = row.id
			installedPatch.installDate = row.time
			installedPatches.put(installedPatch.id, installedPatch)
		}
		return installedPatches
	}
}
