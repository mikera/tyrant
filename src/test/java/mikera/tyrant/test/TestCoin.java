package mikera.tyrant.test;

import java.util.StringTokenizer;

import mikera.tyrant.Coin;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Thing;

/**
 * @author Chris Grindstaff chris@gstaff.org
 */

@SuppressWarnings("unused")
public class TestCoin extends TyrantTestCase {

    public void testCreate_cooper() {
        Thing coins = Coin.createRoundedMoney(9);
    }

    public void testCreate_silver() {
        Thing coins = Coin.createRoundedMoney(123);
    }

    public void testCreate_gold() {
        Thing coins = Coin.createRoundedMoney(9999);
    }

	public void testCreate_gold2() {
 		Thing coins = Coin.createRoundedMoney(999999);
    }

    /**
     * @see http://sourceforge.net/tracker/index.php?func=detail&aid=994528&group_id=16696&atid=116696
     */
    public void testRemove() {
        person.addThing(Lib.create("8 gold coin"));
        Coin.removeMoney(person, 2);
        assertTrue(weightOf("7 gold, 9 silver, 8 copper")<= person.getInventoryWeight());
        assertTrue(Coin.getMoney(person)>=798);
    }

    public void testRemove_negative() {
        int initialWeight=person.getInventoryWeight();
    	Coin.removeMoney(person, -2);
        assertEquals(initialWeight+weightOf("2 copper"), person.getInventoryWeight());
        assertEquals(2, Coin.getMoney(person));

    }

    public void testAdd_negative() {
    	Thing rabbit=Lib.create("rabbit");
        Coin.addMoney(rabbit, 10);
        int wt =  rabbit.getInventoryWeight();
        Coin.addMoney(rabbit, -10);
        assertTrue(rabbit.getInventoryWeight() < wt);
        assertEquals(0, Coin.getMoney(rabbit));
    }

    private int weightOf(String toParse) {
        int total = 0;
        StringTokenizer tokenizer = new StringTokenizer(toParse, ",");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken().trim();
            name += name.endsWith(Coin.SOVEREIGN) ? "" : " coin";
            Thing money = Lib.create(name);
            total += money.getWeight();
        }
        return total;
    }
}
