package com.up.imx

import static org.junit.Assert.*

import org.junit.BeforeClass;
import org.junit.Test;

class ImxStatusTest {

	static ImxEnvironment imx
	
	@Test
	public void testStatus() {
		imx.status()
	}
	
	@BeforeClass
	static void beforeSetupBeforeClass() {
		imx = ImxTest.newImx()
		//imx.repository.baseFolder = localRepository
	}
}
