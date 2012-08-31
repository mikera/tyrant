package mikera.tyrant.test;

import mikera.engine.Map;
import mikera.engine.Thing;
import mikera.tyrant.Tile;

/**
 * @author Chris Grindstaff chris@gstaff.org
 */
public class TestMap extends TyrantTestCase {
    
    public void testAdding() {
        Map map = new Map(4, 4);
        Thing aStone = map.addThing("stone", 0, 0);
        assertEquals(1, map.getThings().length);
        assertEquals(aStone, map.getThings(0, 0)[0]);
    }

    public void testAdding_removes() {
        Map map = new Map(4, 4);
        Thing aStone = map.addThing("stone", 0, 0);
        assertSame(aStone, map.getThings(0, 0)[0]);
        map.addThing(aStone, 0, 1);
        assertEquals(0, map.getThings(0, 0).length);
        assertSame(aStone, map.getThings(0, 1)[0]);
    }
    
    /**
     * Check that random adds add Things to different squares
     * assuming a free square is available
     *
     */
    public void testRandomAdds() {
    	Map map=new Map(2,1);
    	map.fillArea(0,0,1,0,Tile.CAVEFLOOR);
    	
    	Thing a=new Thing();
    	Thing b=new Thing();
    	map.addThing(a);
    	map.addThing(b);
    	
    	assertTrue(a.getMap()==map);
    	assertTrue(b.getMap()==map);
    	assertTrue(a.y==0);
    	assertTrue(b.y==0);
    	assertTrue(a.x!=b.x);
    	
    	Thing c=new Thing();
    	map.addThing(c);
    	assertTrue(c.getMap()==map);
    }

    
    public void testWidth() {
        assertEquals(50, new Map(50, 50).getWidth());
    }
}
