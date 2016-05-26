package com.up.imx

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

class DatabaseTest {
	private Database database
	
	@Test
	public void testNewRepository() {

		Map<String,InstalledPatch> patchesInDatabase = database.getInstalledPatches()
		List<String> expectedInstalledPatchs = ['DsPatch20150713_1.tar.Z', 'DsPatch20150528_2.tar.Z', 'DmPatch20150521_6.tar.Z']
		List<String> expectedInexistantPatchs = ['Toto.Z', 'Toto.tar.Z']

		assertTrue("La base de données devrait contenir les patchs $expectedInstalledPatchs", 
			expectedInstalledPatchs.every { patchesInDatabase.containsKey(it) } )
		assertTrue("La base de données ne devrait contenir aucun patch $expectedInexistantPatchs", 
			expectedInexistantPatchs.every { ! patchesInDatabase.containsKey(it) } )
	}

	@Before
	void setUp() {
		database = new Database(url: 'jdbc:oracle:thin:@parva4117036:1521:D04355AP10',
			user: 'gen$huis',
			password: 'manager', 
			driverClassname: 'oracle.jdbc.OracleDriver')
	}
}
