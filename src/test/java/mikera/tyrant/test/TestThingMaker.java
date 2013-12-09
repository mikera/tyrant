package mikera.tyrant.test;

import junit.framework.TestCase;
import mikera.tyrant.author.Designer;
import mikera.tyrant.author.MapMaker;
import mikera.tyrant.author.ThingMaker;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Thing;

public class TestThingMaker extends TestCase {
    private ThingMaker thingMaker;
    private StringBuffer actual;

    private static final String NL=MapMaker.NL;

    protected void setUp() throws Exception {
        thingMaker = new ThingMaker();
        actual = new StringBuffer();
    }
    
    public void testStoring() throws Exception {
        Map map = new Map(3, 3);
        map.addThing("carrot", 0, 0);
        map.addThing("parsnip", 1, 0);
        map.addThing("beefcake", 1, 0);
        String expected = NL +
            "---Things---" + NL + 
            "0x0 carrot" + NL + 
            "1x0 beefcake" + NL +
            "1x0 parsnip" + NL + 
            "---Things---" + NL + 
            "";
        thingMaker.storeThings(map, actual);
        assertEquals(expected, actual.toString());
    }
    
    public void testStoringIs() throws Exception {
        Map map = new Map(3, 3);
        map.addThing(Designer.getFlagged("IsFood"), 0, 0);
        String expected = NL +
            "---Things---"+ NL  + 
            "0x0 [IsFood]"+ NL  + 
            "---Things---"+ NL  + 
            "";
        thingMaker.storeThings(map, actual);
        String actuals=actual.toString();
        assertEquals(expected.length(),actuals.length());
        assertEquals(expected, actuals);
    }
    
    public void testThings_modififed() throws Exception {
        Map map = new Map(3, 3);
        Thing carrot = Lib.create("carrot");
        carrot.set("Number", "6");
        carrot.set("Level", "16");
        map.addThing(carrot, 0, 0);
        map.addThing("parsnip", 1, 0);
        map.addThing("beefcake", 1, 0);
        String expected = NL +
            "---Things---"+ NL  + 
            "0x0 carrot" + NL + 
            ThingMaker.SPACES_3 + "Level = 16" + NL + 
            ThingMaker.SPACES_3 + "Number = 6" + NL + 
            "1x0 beefcake" + NL + 
            "1x0 parsnip" + NL +
            "---Things---"+ NL ; 
        thingMaker.storeThings(map, actual);
        assertEquals(expected, actual.toString());
    }


    public void testThings_create() throws Exception {
        String mapText = "" +
                "---Legend---"+ NL  + 
                "Width = 3 " + NL + 
                "Height = 3" + NL + 
                "EntranceX = 3 " + NL + 
                "EntranceY = 3" + NL + 
                "---Legend---" + NL + 
                "---Things---" + NL + 
                "0x0 carrot" + NL + 
                "2x2 carrot" + NL +
                "2x2 parsnip" + NL +
                "---Things---";
        Map map = new Map(3, 3);
        thingMaker.addThingsToMap(map, mapText);
        assertEquals("carrot", map.getThings(0, 0)[0].name());
        assertEquals("parsnip", map.getThings(2, 2)[0].name());
        assertEquals("carrot", map.getThings(2, 2)[1].name());
    }
    
    public void testThings_createWithAttributes() throws Exception {
        String mapText = "" +
                "---Things---" + NL + 
                "0x0 carrot" + NL +
                ThingMaker.SPACES_3 + "Level = 16" + NL + 
                ThingMaker.SPACES_3 + "Number = 6" + NL + 
                "2x2 carrot" + NL +
                "2x2 parsnip" + NL +
                "---Things---";
        Map map = new Map(3, 3);
        thingMaker.addThingsToMap(map, mapText);
        Thing carrot = map.getThings(0, 0)[0];
        assertEquals("carrot", carrot.name());
        assertEquals(16, carrot.getStat("Level"));
        assertEquals(6, carrot.getStat("Number"));
        assertEquals("parsnip", map.getThings(2, 2)[0].name());
        assertEquals("carrot", map.getThings(2, 2)[1].name());
    }
}
