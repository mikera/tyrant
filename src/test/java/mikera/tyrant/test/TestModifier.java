package mikera.tyrant.test;

import mikera.tyrant.*;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Modifier;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Script;
import mikera.tyrant.engine.Thing;

/**
 * @author Chris Grindstaff chris@gstaff.org
 */
public class TestModifier extends TyrantTestCase {
    private Thing rabbit;
    private Thing berry;
    private Thing berry2;

    public void setUp() throws Exception {
        super.setUp();
        rabbit = Lib.create("rabbit");
        berry = Lib.extend("berry", "base item");
        berry2 = Lib.extend("berry2", "base item");
    }
   
    public void testLinear_carried() {
       	int baseSK = rabbit.getStat("SK");
    	
        berry.add("CarriedModifiers", Modifier.linear("SK", 200, 9));
        rabbit.addThing(berry);
        assertEquals(2*baseSK+9 , rabbit.getStat("SK"));
    }
    
    public void testLinear_dropped() {
        int baseSK = rabbit.getStat("SK");
        
        berry.add("CarriedModifiers", Modifier.linear("SK", 200, 9));
        rabbit.addThing(berry);
        assertEquals(2*baseSK+9 , rabbit.getStat("SK"));
        rabbit.dropThing(berry);
        assertEquals(baseSK,rabbit.getStat("SK"));
    }

    public void testLinear_carriedSeveral() {
       	int baseSK = rabbit.getStat("SK");
    	
    	berry.add("CarriedModifiers", Modifier.linear("SK", 100, 9));    // 3 + 9  = 12
        berry2.add("CarriedModifiers", Modifier.linear("SK", 100, 9));   // 12 + 9 = 21
        
        rabbit.addThing(berry);
        rabbit.addThing(berry2);
        assertEquals(baseSK+9+9, rabbit.getStat("SK"));
    }

    public void testBonus() {
       	int baseSK = rabbit.getStat("SK");
    	
        berry.add("CarriedModifiers", Modifier.bonus("SK", 17));
        
        rabbit.addThing(berry);
        assertEquals(baseSK+17, rabbit.getStat("SK"));
        assertEquals(baseSK, rabbit.getBaseStat("SK"));
    }
    
    public void testConstant() {
        berry.add("CarriedModifiers", Modifier.constant("SK", 1000));
        
        rabbit.addThing(berry);
        assertEquals(1000, rabbit.getStat("SK"));
    }
	
    @SuppressWarnings("serial")
	public void testScripted() {
        berry.add("CarriedModifiers", Modifier.scripted("ST", new Script() {
			public boolean handle(Thing t, Event e) {
				Thing b=e.getThing("Source");
				int num=b.getNumber();
				e.set("Result",t.getStat("SK")*2+2+num);
				return false;
			}
		}));
        
		int sk=rabbit.getStat("SK");
        rabbit.addThing(berry);
		assertEquals(1,berry.getNumber());
        assertEquals(2*sk+3, rabbit.getStat("ST"));
    }
	
    public void testSelf() {
		berry.add("SelfModifiers", Modifier.constant("MyNumber", 10));
		assertEquals(10,berry.getStat("MyNumber"));
	}
    
    public void testConstant_several() {
        // Is this working as designed? The first constant shortcircuits the other ones
    	// mikera - yes, this is as designed
        berry.add("CarriedModifiers", Modifier.constant("SK", 9));
        berry.add("CarriedModifiers", Modifier.constant("SK", 1));
        
        rabbit.addThing(berry);
        assertEquals(Integer.valueOf(9), rabbit.get("SK"));
    }
    
    public void testConstant_several_flipped() {
        // Is this working as designed? The first constant shortcircuits the other ones
        berry.add("CarriedModifiers", Modifier.constant("SK", 1));
        berry.add("CarriedModifiers", Modifier.constant("SK", 9));
        
        rabbit.addThing(berry);
        assertEquals(Integer.valueOf(1), rabbit.get("SK"));
    }
    
    public void testMultipleModifiers() {
    	int baseSK = rabbit.getStat("SK");

    	berry.add("CarriedModifiers", Modifier.bonus("SK", 9));
        berry.add("CarriedModifiers", Modifier.bonus("SK", 4));
        
        rabbit.addThing(berry);
        assertEquals(baseSK+13, rabbit.getStat("SK"));
    }
    
    public void testAddModifier() {
    	int baseST = rabbit.getStat("ST");
    	
        assertEquals(baseST, rabbit.getStat("ST"));
        berry.addThing(Lib.create("strength rune"));
        rabbit.addThing(berry);
        assertEquals(baseST, rabbit.getStat("ST"));
        assertTrue(rabbit.wield(berry, RPG.WT_MAINHAND));
        assertTrue(baseST< rabbit.getStat("ST"));
    }
    
    public void testDynamicModifiers() {
    	int baseSK = rabbit.getStat("SK");
   
        berry.add("CarriedModifiers", Modifier.bonus("SK", 1));
        rabbit.addThing(berry);
        assertEquals(baseSK+1, rabbit.getStat("SK"));
        
        berry.add("CarriedModifiers", Modifier.bonus("SK", 2));
        assertEquals(baseSK+3, rabbit.getStat("SK"));
    }
    
    public void testMultilevel() {
    	int baseSK = rabbit.getStat("SK");
    	   
        berry2.add("CarriedModifiers", Modifier.bonus("SK", 10));
        berry.add("CarriedModifiers", Modifier.bonus("SK", 1));
        berry.addThing(berry2);
        rabbit.addThing(berry);
        
        assertEquals(baseSK+1, rabbit.getStat("SK"));
    }
    
    public void testPriority() {
        Thing r1=Lib.create("ring of blindness");
        Thing r2=Lib.create("ring of prevent blindness");
        
        rabbit.addThing(r1);
        rabbit.addThing(r2);
        
        // ring of prevent blindness has higher priority
        // so should cancal effect of other ring
        assertTrue(rabbit.wield(r1,RPG.WT_LEFTRING));
        assertTrue(rabbit.getFlag("IsBlind"));
        assertTrue(rabbit.wield(r2,RPG.WT_RIGHTRING));
        assertFalse(rabbit.getFlag("IsBlind"));
    }
	
    public void testLocationModifiers() {
        Thing a=Lib.create("apple");
		Thing c=Lib.create("carrot");
        Thing h=Lib.create("human");
		
        a.add("LocationModifiers", Modifier.bonus("SK", 1));
        a.add("LocationModifiers", Modifier.bonus("SK", 2));
        a.add("LocationModifiers", Modifier.constant("IsModifiedByApple", 1));
	    c.add("LocationModifiers", Modifier.bonus("SK", 4));
		c.add("LocationModifiers", Modifier.constant("IsModifiedByCarrot", 1));
       
		int sk=h.getStat("SK");
		
		Map m=new Map(3,3);
        
		m.addThing(h,0,0);
        assertEquals(sk,h.getStat("SK"));
		assertFalse(h.getFlag("IsModifiedByApple"));
        
		m.addThing(a,0,0);
		
        assertEquals(sk+3,h.getStat("SK"));
		assertTrue(h.getFlag("IsModifiedByApple"));
		
		h.remove();
		
        assertEquals(sk,h.getStat("SK"));
		assertFalse(h.getFlag("IsModifiedByApple"));

		m.addThing(h,0,0);
        assertEquals(sk+3,h.getStat("SK"));
		assertTrue(h.getFlag("IsModifiedByApple"));

		a.remove();
        assertEquals(sk,h.getStat("SK"));
		assertFalse(h.getFlag("IsModifiedByApple"));

		m.addThing(a,0,0);
		
        assertEquals(sk+3,h.getStat("SK"));
		assertTrue(h.getFlag("IsModifiedByApple"));

		m.addThing(h,1,1);
		
        assertEquals(sk,h.getStat("SK"));
		assertFalse(h.getFlag("IsModifiedByApple"));

		m.addThing(c,1,1);
		
        assertEquals(sk+4,h.getStat("SK"));
		assertFalse(h.getFlag("IsModifiedByApple"));
		assertTrue(h.getFlag("IsModifiedByCarrot"));

		m.addThing(a,1,1);
        assertEquals(sk+7,h.getStat("SK"));
		assertTrue(h.getFlag("IsModifiedByApple"));
		
    }
    
}
