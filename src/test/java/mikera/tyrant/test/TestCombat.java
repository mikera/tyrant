package mikera.tyrant.test;

import mikera.tyrant.Combat;
import mikera.tyrant.Game;


public class TestCombat extends TyrantTestCase {
    /*
     * See defect http://sourceforge.net/tracker/index.php?func=detail&aid=1059445&group_id=16696&atid=116696
     */
    
    public void testKick() {
        String mapString = 
            // 0123456789
              "----------" + "\n" +
              "|..%@~...|" + "\n" +
              "|...|....|" + "\n" +
              "|........|" + "\n" +
              "|........|" + "\n" +
              "----------";
        
        // hack to ensure we have right actor
        Game.actor=hero;
        
        new MapHelper().createMap(mapString);

        Combat.kick(hero, -1, 1); //thin air
        assertEquals("You kick thin air", lastMessage());
        Game.messagepanel.clear();
        
        Combat.kick(hero, -1, 0); //food
        assertTrue(lastMessage().startsWith("You kick the beefcake"));
        Game.messagepanel.clear();
        
        Combat.kick(hero, 1, 0); //river
        assertEquals("Your attempt to kick the water only succeeds in getting you wet!", lastMessage());
        Game.messagepanel.clear();

        Combat.kick(hero, 0, 1); //wall
        assertEquals("You kick the wall - ouch!", lastMessage());
    }
}
