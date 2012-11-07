package mikera.tyrant.test;

import mikera.tyrant.AI;
import mikera.tyrant.Monster;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.MapHelper;


public class TestMonster extends TyrantTestCase {
    
    public void testTakeTheMoneyAndRun() {
        String mapString = 
            // 01234
              "-----" + "\n" +
              "|L@.|" + "\n" +
              "-----";
        
        Map map = new MapHelper().createMap(mapString);
        Thing[] money = hero.getFlaggedContents("IsMoney");
        for (int i = 0; i < money.length; i++) {
            Thing coin = money[i];
            coin.remove();
        }
        assertEquals(0, hero.getFlaggedContents("IsMoney").length);
        Thing coins = Lib.create("6 silver coin");
        hero.addThing(coins);
        hero.set(RPG.ST_SK, 1); // make it easy for the monster to hit
        Thing leprechaun = map.getThings(1, 1)[0];
        assertEquals("leprechaun", leprechaun.getName());
        // Make sure he doesn't start out with any silver
        assertNull(leprechaun.getItem(coins.name()));
        // Let him bang away until he gets it right.
        while ((lastMessage() == null) ||
               lastMessage().equals("The leprechaun hits you but fails to do any damage") ||
               lastMessage().equals("The leprechaun misses you")) {
            AI.doAction(leprechaun);
        }
        assertEquals("The leprechaun vanishes in a puff of smoke!", lastMessage());
        // There's only one square to teleport to.
        Thing movedMonster = map.getThings(3,1)[0];
        assertEquals(leprechaun, movedMonster);
        // Make sure he got some of our silver
        assertEquals(coins, movedMonster.getItem(coins.name()));
    }
    
    public void testSpecialHit_teleporting() throws Exception {
        String mapString = 
              "-----" + "\n" +
              "|r.##" + "\n" +
              "-----";
        Map map = new MapHelper().createMap(mapString);
        Thing rabbit = map.getThings(1, 1)[0];
        Monster.SpecialHit specialHit = new Monster.SpecialHit(Monster.TAKE_THE_MONEY_AND_RUN, 100);
        assertTrue(specialHit.teleportAway(rabbit));
        assertLocation(rabbit, 2, 1);
    }
    
    public void testStealingMoney() throws Exception {
        Monster.SpecialHit specialHit = new Monster.SpecialHit(Monster.TAKE_THE_MONEY_AND_RUN, 100);
        person.addThing(Lib.create("6 silver coin"));
        Thing bunny = Lib.create("rabbit");
        assertTrue(specialHit.stealSomething(person, bunny, "IsMoney"));
        assertEquals(0, person.getFlaggedContents("IsMoney").length);
        assertEquals(1, bunny.getFlaggedContents("IsMoney").length);
    }

    public void testStealingMagic() throws Exception {
        Monster.SpecialHit specialHit = new Monster.SpecialHit(Monster.TAKE_THE_MAGIC_AND_RUN, 100);
        person.addThing(Lib.create("[IsScroll]"));
        Thing bunny = Lib.create("rabbit");
        assertTrue(specialHit.stealSomething(person, bunny, "IsMagicItem"));
        assertEquals(1, bunny.getFlaggedContents("IsMagicItem").length);
    }

    public void testStealingMultipleThings() throws Exception {
        Monster.SpecialHit specialHit = new Monster.SpecialHit(Monster.TAKE_THE_MAGIC_AND_RUN, 100);
        person.addThing(Lib.create("[IsScroll]"));
        person.addThing(Lib.create("[IsPotion]"));
        Thing bunny = Lib.create("rabbit");
        assertTrue(specialHit.stealSomething(person, bunny, "IsMagicItem",2));
        assertEquals(0, person.getFlaggedContents("IsMagicItem").length);
        assertTrue(bunny.getFlaggedContents("IsMagicItem").length >= 2);
    }
}
