package mikera.tyrant.test;

import mikera.tyrant.*;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Thing;

public class TestArtifact extends TyrantTestCase {
	public void testUniqueCreation() {
		Thing a=Lib.create("Yanthrall's Sword");
		Thing b=Lib.create("Yanthrall's Sword");
		assertNotNull(a);
		assertEquals(a,b);
		
		
	}
}
