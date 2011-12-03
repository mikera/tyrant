package mikera.tyrant.test;

import mikera.engine.Lib;
import mikera.engine.Thing;
import mikera.tyrant.*;

public class Artifact_TC extends TyrantTestCase {
	public void testUniqueCreation() {
		Thing a=Lib.create("Yanthrall's Sword");
		Thing b=Lib.create("Yanthrall's Sword");
		assertNotNull(a);
		assertEquals(a,b);
		
		
	}
}
