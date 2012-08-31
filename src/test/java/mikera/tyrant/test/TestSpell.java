package mikera.tyrant.test;

import mikera.tyrant.*;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Thing;

public class TestSpell extends TyrantTestCase {
	public void testCreate() {
		// valid spell
		Thing t=Spell.create("Fireball");
		
		assertNotNull(t);
		
		// invlaid spell
		try {
			t=Spell.create("Not A Valid Spell Name");
			fail("Invalid spell created?!?");
		} catch (Throwable x) {
			//
		}
	}
	
	public void testAddSpell() {
		Thing b=Lib.create("human");
		Thing s=Lib.create("Fireball");
		b.addThing(s);
		
		assertTrue(Spell.maxCharges(b,s)>0);
		assertTrue(Spell.canCast(b,s));
		assertTrue(Spell.castTime(b)==200);
		
		s.set("Charges",0);
		
		assertFalse(Spell.canCast(b,s));
		
		Spell.rechargeSpells(b,10000);
		// assertTrue(Spell.canCast(b,s));
	}
}
