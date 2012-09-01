package mikera.tyrant.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mikera.tyrant.*;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Modifier;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Script;
import mikera.tyrant.engine.Thing;

/**
 * @author Chris Grindstaff chris@gstaff.org
 */
public class TestLib extends TyrantTestCase {
    
    private List<Thing> all=null;

    public void testEmpty() {
        Lib lib = new Lib();
        assertEquals(0, lib.getAll().size());
    }
    
    public void testExtend() {
    	Thing test1=Lib.extend("extend test item 1","base item");
    	assertEquals(1,test1.getStat("IsItem"));
    	assertNotNull(test1.getInherited());
    	
    	Thing test2=Lib.extendCopy("extend test item 2","base item");
    	assertEquals(1,test2.getStat("IsItem"));
    	assertEquals(null,test2.getInherited());
    }

    public void testLibLinks() {
    	assertEquals(Game.instance().get("Library"),Lib.instance());
    	
    }
    
    public void testIdentity() {
        Thing rabbit = Lib.create("rabbit");
        Thing rabbit2 = Lib.create("rabbit");
        assertNotSame(rabbit, rabbit2);
    }
    
    public void testCreate_count() {
        assertEquals(500, Lib.create("500 carrot juice").getNumber());
        assertEquals(501, Lib.create("501carrot juice").getNumber());
    }
    
    public void testCreate_percentage() {
        assertNull(Lib.create("0%         carrot juice"));
        assertNotNull(Lib.create("100%         carrot juice"));
    }
    
    public void testCreate_KleeneStar() {
        //RPG.setRandSeed(0);
        Thing carrotJuice = Lib.create("100* carrot juice");
        assertTrue(carrotJuice.getNumber()>0);
        //assertEquals(61, carrotJuice.getNumber());
        assertEquals(1, Lib.create("0* carrot juice").getNumber());
    }
    

    /**
     * Helper function to work out the type of property
     * 
     * @param value Any object found in Thing properties
     * @return String detailing the type of the object
     */
    private String getPropertyType(Object value) {
    	if (value==null) return null;
    	if (value instanceof String) return "String";
    	if (value instanceof Integer) return "Integer";
    	if (value instanceof Modifier[]) return "Modifier[]";
    	if (value instanceof Modifier) return "Modifier";
    	if (value instanceof Script) return "Script";
    	return "Object";
    }

    public void setUp() throws Exception {
    	super.setUp();
    	all=Lib.instance().getAll();
    }

    /**
     * Test creation of all objects in the game library
     * Name should be preserved
     *
     */
    public void testCreate() {
    	Iterator<Thing> it=all.iterator();
    	while (it.hasNext()) {
            Thing p = it.next();
    		String name=(String)p.get("Name");
    		
    		Thing t=Lib.create((String)p.get("Name"));
    		
    		assertTrue("["+name+"] not created with correct Name",t.name()==name);
    	}
    }
    
    public void testCreateIgnoreCase() {
    	Thing t=Lib.createIgnoreCase("fIreBAll");
    	assertEquals(t.name(),"Fireball");
    	
    }


    /**
     * Test all objects flagged as items
     *
     */
    public void testItems() {
    	Iterator<Thing> it=all.iterator();
    	while (it.hasNext()) {
            Thing p = it.next();	
    		String name=(String)p.get("Name");
    		if (name.startsWith("base ")) continue;
    		
    		Thing t=Lib.create(name);
    		
    		if (t.getFlag("IsItem")) {
    			assertTrue("["+name+"] has no weight",t.getStat("ItemWeight")>0);
    			assertTrue("["+name+"] has no LevelMin",t.getStat("LevelMin")>0);
    			assertTrue("["+name+"] has no HPS",t.getStat("HPS")>0);
    		
    			// some test functions
    			try {
    				Item.value(t);
    				Item.inspect(t);
    				Item.bless(t);
    				Item.curse(t);
    				Item.repair(t,true);
    			
    			
    			} catch (Throwable e) {
    				throw new Error("Exception testing ["+name+"]",e);
    			}
    			
    			if (t.getFlag("WieldType")) {
    				hero.addThing(t);
    				hero.wield(t);
    				t.remove();
    			}
    		}
    	}
    }

    /**
     * Test all properties in library for type consistency
     *
     */
    public void testPropertyTypes() {
    	Iterator<Thing> it=all.iterator();
    	HashMap<String, String> types=new HashMap<>();
    	HashMap<String, String> seen=new HashMap<>();
    	while (it.hasNext()) {
            Thing p = it.next();
    		String name=(String)p.get("Name");
    		
    		Thing t=Lib.create(name);
    		Map<String,Object> h = t.getCollapsedMap();
    		
    		for (Iterator<String> hi=h.keySet().iterator();hi.hasNext();) {
    			String key=hi.next();
    			
    			Object value=t.get(key);
    			
    			String type=getPropertyType(value);
    			String seenType=types.get(key);
    			if ((type!=null)&&(seenType!=null)) {
    				assertTrue("["+name+"] has property ["+key+"] of unexpected type ["+type+"] "+seen.get(key),type.equals(seenType));
    			} else if (type!=null) {
    				// record the observed type
    				types.put(key,type);
    				seen.put(key,"("+type+" for "+name+")");
    			}			
    		}
    	}
    }
    
    /**
     * TEST for https://sourceforge.net/forum/message.php?msg_id=2916833
     */
    public void testArmour() throws Exception {
        for (Iterator<Thing> iter = all.iterator(); iter.hasNext();) {
            Thing thingAsProperties = iter.next();
            String name = (String) thingAsProperties.get("Name");
            if (name.startsWith("base ")) continue;
            Thing thing = Lib.create(name);
            if (thing.getFlag("IsArmour") && thing.getStat("WieldType") == RPG.WT_BOOTS) {
                assertTrue("boot " + name + " should specify attack", thing.getStat(RPG.ST_ASKMULTIPLIER) > 0);
            }
        }
    }
    
    public void testCreateTypeAndLevels() throws Exception {
        Lib oldLib=Lib.instance();
    	try {
            Lib.setInstance(new Lib());
            Thing thing = new Thing();
            thing.set("Name", "tree");
            thing.set("IsTree", true);
            thing.set("LevelMin", 7);
            thing.set("Frequency", 100);
            thing.set("LevelMax", 19);
            Lib.add(thing);
            assertNotNull(Lib.createType("IsTree", 7));
            assertNotNull(Lib.createType("IsTree", 1));
            assertNotNull(Lib.createType("IsTree", 20));
        } finally {
            Lib.setInstance(oldLib);
        }
    }
}
