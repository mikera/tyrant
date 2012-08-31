package mikera.tyrant.test;

import java.util.StringTokenizer;

import mikera.engine.Lib;
import mikera.engine.Thing;
import mikera.tyrant.Coin;

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
        assertEquals(weightOf("7 gold, 9 silver, 8 copper"), person.getInventoryWeight());
        assertEquals(798, Coin.getMoney(person));
    }

    public void testRemove_negative() {
        int initialWeight=person.getInventoryWeight();
    	Coin.removeMoney(person, -2);
        assertEquals(initialWeight+weightOf("2 copper"), person.getInventoryWeight());
        assertEquals(2, Coin.getMoney(person));

    }

    public void testAdd() {
        Coin.addMoney(person, 10000);
        assertEquals(weightOf("10 sovereign"), person.getInventoryWeight());
        assertEquals(10000, Coin.getMoney(person));
    }

    public void testAdd_negative() {
        Coin.addMoney(person, 10);
        Coin.addMoney(person, -10);
        assertEquals(0, person.getInventoryWeight());
        assertEquals(0, Coin.getMoney(person));
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
