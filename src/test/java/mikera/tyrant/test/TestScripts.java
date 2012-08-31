package mikera.tyrant.test;

import mikera.tyrant.Food;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Thing;

public class TestScripts extends TyrantTestCase {

    public void testHealScript() {
        final Thing orange1 = Lib.create("orange");
        final Thing orange2 = Lib.create("orange");
        int hps = hero.getStat("HPS");
        hero.incStat("HPS", -1);
        assertEquals(hps - 1,hero.getStat("HPS"));
        Food.eat(hero,orange1);
        assertEquals(hps,hero.getStat("HPS"));
        Food.eat(hero,orange2); // don't go past maxhp
        assertEquals(hps,hero.getStat("HPS"));
    }

}
