package com.up.imx

import groovy.sql.Sql;

class Database {
	String url
	String user
	String password
	String driverClassname
	
	public Map<String,InstalledPatch> getInstalledPatches() {
		//pas terrible l'ouverture / fermeture de connexion

		//chargement du driver
		Class.forName(driverClassname)

		//exécution requÃªte
		Sql sql = Sql.newInstance(url, user, password, driverClassname)
		PatchDao patchDao = new PatchDao(sql)
		try {
			return patchDao.getInstalledPatches()
		} finally {
			sql.close()
		}
	}
}