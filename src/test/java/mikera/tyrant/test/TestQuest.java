/*
 * Created on 02-Apr-2005
 *
 * By Mike Anderson
 */
package mikera.tyrant.test;

import mikera.engine.Lib;
import mikera.engine.Map;
import mikera.engine.RPG;
import mikera.engine.Thing;
import mikera.tyrant.*;

/**
 * @author Mike
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TestQuest extends TyrantTestCase {
	public void testVisitMap() {
		Map map=new Map(1,1);
		Thing q=Quest.createVisitMapQuest("Visit the map",map);
		
		// check set-up
		assertNotNull(q.get("TargetMap"));
		assertEquals(map,q.get("TargetMap"));
		
		Thing h=Game.hero();
		Quest.addQuest(h,q);
		
		map.addThing(h,0,0);
		
		assertFalse(q.getFlag("IsComplete"));
		Time.advance(0);
		assertTrue(q.getFlag("IsComplete"));
	}
	
	public void testKillQuest() {
		Thing rat=Lib.create("rat");
		Thing q=Quest.createKillQuest("Kill the rat",rat);

		Thing h=Game.hero();
		Quest.addQuest(h,q);
		
		assertFalse(rat.isDead());
		assertFalse(q.getFlag("IsComplete"));
		assertTrue(q.getFlag("IsActive"));
		
		Damage.inflict(rat,1000,RPG.DT_SPECIAL);
		
		assertTrue(rat.isDead());
		assertTrue(q.getFlag("IsComplete"));
		assertFalse(q.getFlag("IsActive"));
	}
	
	public void testKillNumberQuest() {
		Thing q1=Quest.createKillNumberQuest("Kill the goblins","goblin",2);
		Thing q2=Quest.createKillNumberQuest("Kill the goblinoids","[IsGoblinoid]",2);

		assertEquals("IsGoblinoid",q2.getString("TargetType"));
		assertEquals("goblin",q1.getString("TargetName"));
		
		Thing h=Game.hero();
		Quest.addQuest(h,q1);
		Quest.addQuest(h,q2);
		
		// kill irrelevant stuff
		Being.registerKill(h,Lib.create("rat"));
		Being.registerKill(h,Lib.create("field mouse"));
		
		assertFalse(q1.getFlag("IsComplete"));
		assertFalse(q2.getFlag("IsComplete"));
		
		// kill one goblin
		Being.registerKill(h,Lib.create("goblin"));

		assertFalse(q1.getFlag("IsComplete"));
		assertFalse(q2.getFlag("IsComplete"));

		// kill a different goblinoid
		Being.registerKill(h,Lib.create("orc"));
		
		assertFalse(q1.getFlag("IsComplete"));
		assertTrue(q2.getFlag("IsComplete"));
		
		// kill a second goblin
		Being.registerKill(h,Lib.create("goblin"));

		assertTrue(q1.getFlag("IsComplete"));
		assertTrue(q2.getFlag("IsComplete"));
	}
	
	public void testMeetQuest() {
		Map map=new Map(3,3);
		
		Thing rat=Lib.create("rat");
		Thing q=Quest.createMeetQuest("Met the rat",rat);

		Thing h=Game.hero();
		Quest.addQuest(h,q);
		
		map.addThing(Game.hero(),0,0);
		map.addThing(rat,2,2);
		
		Time.advance(0);
		
		assertFalse(q.getFlag("IsComplete"));
		assertTrue(q.getFlag("IsActive"));
		
		map.addThing(Game.hero(),1,1);
		Time.advance(0);
		
		assertTrue(q.getFlag("IsComplete"));
		assertFalse(q.getFlag("IsActive"));
	}
}
