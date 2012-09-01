package mikera.tyrant.engine;

import java.io.Serializable;
import java.util.*;
import java.util.Map;

import mikera.tyrant.engine.BaseObject;
import mikera.tyrant.AI;
import mikera.tyrant.Animal;
import mikera.tyrant.Artifact;
import mikera.tyrant.Aura;
import mikera.tyrant.Being;
import mikera.tyrant.Chest;
import mikera.tyrant.Coin;
import mikera.tyrant.Decoration;
import mikera.tyrant.Door;
import mikera.tyrant.Effect;
import mikera.tyrant.EndGame;
import mikera.tyrant.Event;
import mikera.tyrant.Food;
import mikera.tyrant.Game;
import mikera.tyrant.GoblinVillage;
import mikera.tyrant.Gods;
import mikera.tyrant.Item;
import mikera.tyrant.Missile;
import mikera.tyrant.Monster;
import mikera.tyrant.Person;
import mikera.tyrant.Potion;
import mikera.tyrant.Quest;
import mikera.tyrant.RangedWeapon;
import mikera.tyrant.Recipe;
import mikera.tyrant.Scenery;
import mikera.tyrant.Skill;
import mikera.tyrant.Special;
import mikera.tyrant.Spell;
import mikera.tyrant.SpellBook;
import mikera.tyrant.Temple;
import mikera.tyrant.Tile;
import mikera.tyrant.Trap;
import mikera.tyrant.Tutorial;
import mikera.util.Maths;
import mikera.util.Rand;
/**
 * Lib implements the Tyrant game library
 * 
 * The library stores "template" properties for all game objects
 * Copies of these templates can be constructed on demand and
 * used within the game.
 * 
 * e.g. Thing goblin = Lib.create("goblin warrior");
 * 
 * All library initialisation is done through Lib.init(),
 * which calls initialisation routines for other classes of items,
 * e.g. Food.init() or Spell.init()
 * 
 * Lib.init() should be called only once per game, just before
 * hero creation. This should be done as am asynchronous 
 * background task by calling Lib.asynchronousCreate().
 * 
 * At the end of the game, the library should be cleared for the
 * next game with Lib.clear()
 * 
 * @author Mike
 *
 */
public class Lib extends Object implements Serializable, Cloneable {
    private static final long serialVersionUID = 3978145456561862453L;


    // all library objects, indexed by Name
	private HashMap<String, Object> lib = new HashMap<>();
	private HashMap<String, String> lowerCaseNames=new HashMap<>();
	
    private List<Thing> all = new ArrayList<>();
    private transient Map<String, Map<Integer, List<Thing>>> types;
    private Map<String, Thing> createdUniques=new HashMap<>();
    private List<Thing> uniques=new ArrayList<>();
    private static Lib instance;
    private int createCount=0;
    
    public Lib() {
        clearTypes();
    }
    
    public void addArtifact(Thing t) {
    	t.set("IsArtifact",1);
    	t.set("IsDestructible",0);
    	t.set("Frequency",0);
    	Lib.add(t);
    }
    
    public void clearTypes() {
    	types = new HashMap<>();
    }
    
    public HashMap<String, Object> getLib() {
        return lib;
    }
    
    public List<Thing> getAll() {
        return all;
    }
    
    public Map<String, Map<Integer, List<Thing>>> getTypes() {
        return types;
    }
    
    public Object intern(Object o) {
        if (o instanceof String) {
            return ((String)o).intern();
        }
        return o;
    }
    
    private static void initBase() {
        Thing t;
        
        t = new Thing();
        t.set("Name", "base thing");
        t.set("IsThing", 1);
        t.set("ImageSource", "Items");
        t.set("Image", 0);
        t.set("Number", 1);
        Lib.add(t);
    }
    
    public static Thing create(String name) {
    	return create(name,Game.level());
    }
    
    
    public static Thing createIgnoreCase(String name) {
    	name=instance.lowerCaseNames.get(name.toLowerCase());
    	if (name==null) return null;
    	return create(name,Game.level());
    }
    
    public static Thing create(String name, int level) {
    	int number = 0;
    	if (Character.isDigit(name.charAt(0))) {
    		
    		while (Character.isDigit(name.charAt(0))) { // count or percent chance
    			number = (number * 10) + Character.getNumericValue(name.charAt(0));
                name = name.substring(1);
    		}
    		if (name.charAt(0) == '*') { // random up to n
                number = Rand.r(number) + 1;
                name = name.substring(1);
    		} else if (name.charAt(0) == '%') { // percentage
                if (Rand.r(100) >= number) {
                    return null;
                }
                return create(name.substring(1).trim(),level);
            }
            name = name.trim();
        }
    	 
    	Thing t;
    	if (name.charAt(0) == '[') {
    		t = Lib.createType(name.substring(1,name.length() - 1),level);
    	} else {	
    	    BaseObject aThing = get(name);
	        if (aThing == null) {
	        	// this is an error, so send warning
	            Game.warn("Lib: Can't create " + name);
	        	
	            return (!name.equals("strange rock")) ? Lib.create("strange rock") : null;
	        }
	        t = createThing(aThing);
	        
	        // if it's a unique, return the specific instance
	    	if (aThing.getFlag("IsUnique")) {
	    		return Lib.getArtifact(aThing.getString("Name"));
	    	}
	    }
        
    	if ((number > 0) && t.getFlag("IsItem")) {
    		t.set("Number",number);
    	}

        return t;
    }
    
    public String stats() {
    	BaseObject p=new BaseObject();
    	
    	for (int i=0; i<all.size(); i++) {
    		Thing t=new Thing(all.get(i));
    		if (!isBaseClass(t)) {
    			t.flattenProperties();
    			Iterator<String> it=t.getCollapsedMap().keySet().iterator();
    			while(it.hasNext()) {
    				String s=it.next();
    				if (s.startsWith("Is")&&t.getFlag(s)) {
    					p.set(s,p.getStat(s)+1);
    				}
    			}
    		}
    	}
    	
    	return p.report();
    }
    
    public static Thing createThing(BaseObject aThing) {
    	if (!aThing.getFlag("IsOptimized")) {
    		aThing.optimize();
    	}
		

    	
    	Thing newThing= new Thing(aThing);
        if (newThing.handles("OnCreate")) {
            newThing.handle(new Event("Create"));
        }
        
        String s=newThing.getString("DefaultThings");
        if (s!=null) createDefaultThings(newThing,s);
        
    	if (newThing.getFlag("IsBeing")) {
        	if (Rand.d(100)<=3) {
        		newThing.addThing(Lib.createItem(newThing.getLevel()));
        	}	
        	Being.utiliseItems(newThing);
    	}

    	instance.createCount++;
    	return newThing;
    }
    
    public static int getCreateCount() {
    	return instance.createCount;
    }
    
    /**
     * Create the default inventory for a newly created thing
     * Uses coded string format:
     * e.g. "iron sword" = specific iron sword
     * e.g. "[IsFood]" = some kind of food
     * e.g. "50% ham" = percentage chance of ham
     * 
     * @param t Newly created thing
     * @param s Coded string of default items
     */
    private static void createDefaultThings(Thing t, String s) {
    	String[] ts=s.split(",");
    	for (int i=0; i<ts.length; i++) {
    		ts[i]=ts[i].trim();
    		try {
				createDefaultThing(t,ts[i]);
    		} catch (Throwable x) {
				x.printStackTrace();
    		}
    	}
    }
    
    private static void createDefaultThing(Thing t, String s) {
    	int p=s.indexOf('%');
    	if (p>0) {
    		try {
    			int prob=Integer.parseInt(s.substring(0,p).trim());
    			if (Rand.r(100)>=prob) return;
    			s=s.substring(p+1,s.length()).trim();
    		} catch (Exception x) {
    			x.printStackTrace();
    			Game.warn("createDefaultThing parse error: "+s);
    			return;
    		}
    	}
    	Thing nt;
    	if (s.charAt(0)=='[') {
    		String stype=s.substring(1,s.length()-1);
    		int level=t.getLevel();
    		level=level+Rand.d(2,6)-Rand.d(2,6);
    		nt=Lib.createType(stype,level);
    		//Game.warn("Default thing ["+stype+"] Lv."+level+" = "+nt.name());
    	} else {
    	 	nt=Lib.create(s);		
    	}
    	t.addThingWithStacking(nt);
    }
    
    public static int currentLevel() {
        return Game.level();
    }
    
    public static Thing createType(String flag) {
        return createType(flag,Rand.d(currentLevel()));
    }

    private BaseObject getThingFromType(String type, int level) {
        // get list of possibilities at this level
        List<Thing> things = getTypeArray(type, level);
        // search surrounding levels if nothing found
        for (int i = 1; things.isEmpty() && i < 50;) {
            things = getTypeArray(type, level + i);
            i = (i > 0) ? -i : -i + 1;
        }

        if (things.isEmpty()) {
            throw new Error("Can't create type [" + type + "] at level " + level);
        }
        BaseObject aThing = null;
        for (int i = 0; i < 100; i++) {
            aThing = things.get(Rand.r(things.size()));
            Integer freq = (Integer)aThing.get("Frequency");
            if ((freq == null) || (Rand.r(100) < freq.intValue())) {
                break;
            }
        }
        
        if (aThing==null) {
            throw new Error("Can't find type [" + type + "] at level " + level);
        }
        return aThing;
    }
    
    /*
     * Build the type arrays for IsXXX properties
     */
    private void buildTypeArrays() {
    	clearTypes();
    	for (Iterator<Thing> it=all.iterator(); it.hasNext();) {
    		Thing t=it.next();
    		addThingToTypeArray(t);
    	}
    	
    }
    
    private void addThingToTypeArray(Thing thing) {
        if (thing.getStat("Frequency") <= 0) return;
        if (((String) thing.get("Name")).indexOf("base ") >= 0) return;
        Integer min = (Integer) thing.get("LevelMin");
        Integer maxInteger = (Integer) thing.get("LevelMax");
        if (min == null) return;
        int max = maxInteger == null ? 50 : maxInteger.intValue();
        String[] ifs = thing.findAttributesStartingWith("Is");
        for (int i = 0; i < ifs.length; i++) {
            String ifAttribute = ifs[i];
            
            // skip adding if attribute is not set
            if (!thing.getFlag(ifAttribute)) continue;
            
            Map<Integer, List<Thing>> levels = types.get(ifAttribute);
            if(levels == null) {
                levels = new HashMap<>();
                types.put(ifAttribute, levels);
            }
            for (int level = min.intValue(); level < max; level++) {
                Integer levelIndex = new Integer(level);
                List<Thing> stuff = levels.get(levelIndex);
                if (stuff == null) {
                    stuff = new ArrayList<Thing>();
                    levels.put(levelIndex, stuff);
                }
                stuff.add(thing);
            }
        }
    }
        
    public static Thing createType(String flag, int level) {
        if (level<0) level=0;
    	
    	// special case for coin quantity
    	if (flag.equals("IsCoin")) {
        	return Coin.createLevelMoney(level);
        }
		
		if (flag.equals("IsRandomArtifact")) {
        	return Lib.createArtifact(level);
        }
    	
        BaseObject aThing = instance().getThingFromType(flag,level);

        Thing t = createThing(aThing);
        
        if ((Rand.d(3) == 1) && t.getFlag("IsBeing")) {
            // TODO level enhancements
            Being.gainLevel(t,RPG.rspread(level,t.getLevel()));
        }
        return t;
    }
    
    public List<Thing> getTypeArray(String flag, int level) {
        if (level < 1) {
            level = 1;
        }
        if (types == null) {
            types = new Hashtable<>();
        }
        Map<Integer,List<Thing>> levels = types.get(flag);
        if (levels == null) {
            levels = new HashMap<>();
            types.put(flag, levels);
        }
        Integer levelIndex = new Integer(level);
        List<Thing> itemsAtLevel = levels.get(levelIndex);
        if (itemsAtLevel == null) {    	
        	itemsAtLevel=Collections.EMPTY_LIST;
        }
        return itemsAtLevel;
    }
    
    /*
    public List buildTypeArray(String flag, int level) {
    	clearTypes();
    	
    	ArrayList al=new ArrayList();
    	
    	Iterator it=all.iterator();
    	while (it.hasNext()) {
    		BaseObject b=(BaseObject)it.next();
    		
    		if (!b.getFlag(flag)) continue;
    		
            if (b.getStat("Frequency") <= 0) continue;
            if (((String) b.get("Name")).indexOf("base ") >= 0) continue;
            int min = b.getStat("LevelMin");
            int max = b.getStat("LevelMax");
            max = (max==0) ? 50 : max;

            if ((level>=min)&&(level<=max)) {
            	al.add(b);
            }
    	}
    	
    	return al;
    }
    */

    private static Object libLock=new Object();
    
    public static Lib instance() {
        synchronized (libLock) {
        	if(instance == null) {
	            instance = new Lib();
	            Lib.init();
	        }
	    }
        return instance;
    }
    
    public static void clear() {
        synchronized (libLock) {
        	instance=null;
        }
    }
    
    public static BaseObject getThing(String name) {
        return get(name);
    }
    
    public static int getDefaultStat(Thing t, String s) {
        Integer i=(Integer)getDefault(t,s);
        return (i==null)?0:i.intValue();
    }
    
    public static Object getDefault(Thing t, String s) {
        BaseObject baseObject = get(t.getString("Name"));
        if (baseObject==null) return null;
        return baseObject.get(s);
    }
    
    public static Thing get(String name) {
        return (Thing) Lib.instance().lib.get(name);
    }
    
    public Object getObject(String name) {
        return lib.get(name);
    }
    
    public static List<Thing> getUniques() {
        return instance().uniques;
    }
    
    public static Map<String, Thing> getCreatedUniques() {
        return instance().createdUniques;
    }
    
    public static Thing extend(String newName, String baseName) {
        BaseObject baseObject = get(baseName);
        if (baseObject == null) throw new Error("Can't find base properties [" + baseName + "]");
        Thing newThing = new Thing(baseObject);
        newThing.set("Name", newName);
        return newThing;
    }
    
    /**
     * Extend an item by creating a complete copy of all properties
     * 
     * Use this to increase performance for very frequently accessed base objects
     * e.g. "base item"
     * 
     * @param newName
     * @param baseName
     * @return
     */
    public static Thing extendCopy(String newName, String baseName) {
        BaseObject baseObject = get(baseName);
        if (baseObject == null) throw new Error("Can't find base properties [" + baseName + "]");
        Thing newThing = new Thing(baseObject.getPropertyHashMap(),null);
        newThing.set("Name", newName);
        return newThing;
    }
    
    public static Thing extendNamed(String newName, String baseName) {
    	Thing t=Lib.extend("newthing",baseName);
    	AI.name(t,newName);
    	return t;
    }
    
    /**
     * Add a new Thing type to the library
     * 
     * @param thing Thing type to add
     */
    public static void add(Thing thing) {
        Lib library = instance();
        if (library == null)
            throw new Error("Game.hero.lib not available!");
        String name = (String) thing.get("Name");
        if (name == null)
            throw new Error("Trying to add unnamed object to Library!");

        prepareAdd(thing);
        
        if (library.lib.get(name)!=null) {
            Game.warn("Trying to add duplicate object ["+name+"] to library!");      
        }
        library.lib.put(name, thing);
        library.lowerCaseNames.put(name.toLowerCase(),name);
        library.all.add(thing);
        library.addThingToTypeArray(thing);
        if (thing.getFlag("IsUnique")) {
        	library.uniques.add(thing);
    	}
    }
    
    public static Thing getLibraryInstance(String name) {
    	return (Thing)instance.lib.get(name);
    }

    private static boolean isBaseClass(Thing t) {
        return t.getString("Name").indexOf("base ")==0;
    }
    
    // pre=processing before addition to library
    private static void prepareAdd(Thing t) {
        // ensure LevelMin set if not a "base" item
        // since this allows creation!
        String name=t.getString("Name");
        if (!Lib.isBaseClass(t)) {
            if (t.getStat("LevelMin")<=0) {
                t.set("LevelMin",1);
                Game.warn("Warning: no LevelMin for "+name);
            }
        }
        if (t.getStat("Level")<=t.getStat("LevelMin")) {
            t.set("Level",t.getStat("LevelMin"));
        }
       
        
        // make sure destructible items have correct hps
        // also ensure HPS=HPSMAX (where HPS overrides)
        if (t.getFlag("IsDestructible")) {
        	int hps=t.getStat("HPS");
        	if (hps<=0) {
        		if (t.getFlag("IsBeing")) {
    	            hps=t.getStat(RPG.ST_TG);
    	        } 
        		if (hps<=0) {
        	        throw new Error(t.name()+" has no HPS!");
        		} 
        	}
    		if (t.getFlag("IsBeing")) {
	            hps=Maths.max(hps,t.getStat(RPG.ST_TG));
	        } 
        	t.set("HPSMAX",hps);
        	t.set("HPS",hps);
        }
        
        if (t.getFlag("IsBeing")) {
            t.set("MPSMAX",t.getStat(RPG.ST_WP));
        }
        
        if (t.getFlag("MPSMAX")) {
        	t.set("MPS",t.getStat("MPSMAX"));
        }
        
        // openable items
        if (t.getFlag("IsOpenable")&&(t.get("ImageOpen")==null)) {
            // Game.warn("Lib warning: "+t.getName()+" has no open image!");
            t.set("ImageOpen",1);
        }
        
        t.set("Seed",Rand.r(1000000));
    }
    
    private ArrayList<Thing> tileList=new ArrayList<Thing>();
    
    public ArrayList<Thing> getTiles() {
    	return tileList;
    }
    
    // And now for the standard descriptions......
    // PLEASE keep alphabetical by type!!!
    
    // artifacts
    public static final Description DESC_IMPERIALCROWN = new Describer(
    "The Crown of Daedor",
    "",
    "The priceless crown of the Daedorian Empire. This artifact is rumoured to bestow remarkable powers on the wearer.",
    Description.NAMETYPE_PROPER, Description.GENDER_NEUTER);
    
    // Clothing
    public static final Description DESC_HAT = new Describer("hat",
    "A hat of fine quality.");
    public static final Description DESC_TROUSERS = new Describer("trousers",
    "pairs of trousers", "A pair of serviceable trousers.",
    Description.NAMETYPE_QUANTITY, Description.GENDER_NEUTER);
    public static final Description DESC_ROBE = new Describer("robe",
    "A well-made robe.");
    public static final Description DESC_MAGICROBE = new Describer("robe",
    "A robe covered with runes and mystic sigils.");
    public static final Description DESC_GLOVES = new Describer("gloves",
    "pairs of gloves", "A pair of soft leather gloves.",
    Description.NAMETYPE_QUANTITY, Description.GENDER_NEUTER);
    
    // treasure
    public static final Description DESC_TREASURE = new Describer("treasure",
    "treasure", "Valuable treasure.", Description.NAMETYPE_QUANTITY,
    Description.GENDER_NEUTER);
    
    // quality descriptions
    public static final String[] qualitystrings = {"useless", "pathetic",
    "very poor", "poor", "mediocre", "average", "fair", "good",
    "very good", "excellent", "superb", "brilliant", "divine",
    "perfect"};
    
    public static final int[] qualityvalues = {0, 20, 40, 60, 80, 100, 150,
    200, 400, 800, 1800, 5000, 20000, 100000};
    
    // get wield description for any wield slot
    public static String wieldDescription(int wieldtype) {
        switch (wieldtype) {
            case RPG.WT_MAINHAND :
                return "Right hand";
            case RPG.WT_SECONDHAND :
                return "Left hand";
            case RPG.WT_TWOHANDS :
                return "Both hands";
            case RPG.WT_RIGHTRING :
                return "Right finger";
            case RPG.WT_LEFTRING :
                return "Left finger";
            case RPG.WT_NECK :
                return "Neck";
            case RPG.WT_HANDS :
                return "Worn";
            case RPG.WT_BOOTS :
                return "Feet";
            case RPG.WT_TORSO :
                return "Body";
            case RPG.WT_LEGS :
                return "Worn";
            case RPG.WT_HEAD :
                return "Head";
            case RPG.WT_CLOAK :
                return "Worn";
            case RPG.WT_FULLBODY :
                return "Body";
            case RPG.WT_BRACERS :
                return "Worn";
            case RPG.WT_RANGEDWEAPON :
                return "Ranged weapon";
            case RPG.WT_MISSILE :
                return "Missile";
            case RPG.WT_BELT :
                return "Waist";
            case RPG.WT_BOOKBAG :
            case RPG.WT_FOODSACK :
            case RPG.WT_HOLDING :
            case RPG.WT_INGREDIENTPOUCH :
            case RPG.WT_JEWELRYCASE :
            case RPG.WT_KEYRING :
            case RPG.WT_POTIONCASE :
            case RPG.WT_QUIVER :
            case RPG.WT_RUNEBAG :
            case RPG.WT_SCROLLCASE :
            case RPG.WT_WANDCASE :
                return "Using";
            default :
                return null;
        }
    }
    
    // get a random hit location
    public static int hitLocation() {
        switch (Rand.d(30)) {
            case 1 :
            case 2 :
                return RPG.WT_HEAD;
            case 3 :
                return RPG.WT_HANDS;
            case 4 :
                return RPG.WT_BRACERS;
            case 5 :
                return RPG.WT_NECK;
            case 6 :
            case 7 :
                return RPG.WT_BOOTS;
                
            default :
                switch (Rand.d(5)) {
                    case 1 :
                    case 2 :
                        return RPG.WT_TORSO;
                    case 3 :
                        return RPG.WT_MAINHAND;
                    case 4 :
                        return RPG.WT_SECONDHAND;
                    case 5 :
                        return RPG.WT_LEGS;
                }
        }
        throw new Error("Invalid Hit Location");
    }
    
    // describe damage
    public static String damageDescription(int dam, int max) {
        if (dam <= 0)
            return "no damage";
        if (max <= 0)
            max = 10;
        double d = ((double) dam) / max;
        if (d < 0.2)
            return "minor damage";
        if (d < 0.4)
            return "moderate damage";
        if (d < 0.7)
            return "serious damage";
        return "critical damage";
    }
    
    // item creation routines
    
    public static Thing createShield(int level) {
        return Lib.createType("IsShield",level);
    }
    
    public static Thing createFood(int level) {
        return Food.createFood(level);
    }
    
    public static Thing createWeapon(int level) {
        return Lib.createType("IsWeapon",level);
    }
    
    public static Thing createSword(int level) {
        return Lib.createType("IsSword", level);
    }
    
    public static Thing createLightArmour(int level) {
        // TODO filter light armour only
    	return Lib.createType("IsArmour",level);
    }
    
    public static Thing createArmour(int level) {
        return Lib.createType("IsArmour",level);
    }
    
    public static Thing createArtifact(int level) {
        ArrayList<Thing> al=new ArrayList<Thing>();
        List<Thing> un=instance().uniques;
        
        for (int i=0; i<un.size(); i++) {
        	Thing t=un.get(i);
        	if ((!instance().createdUniques.containsKey(t.name()))&&t.getFlag("IsRandomArtifact")) {
        		if (t.getStat("LevelMin")<=level) {
        			al.add(t);
        		}
        	}
        }
        int count=al.size();
        if (count==0) {
        	Game.warn("No artifact at level "+level);
        	return null;
        }
        Thing art=al.get(Rand.r(count));
        return getArtifact(art.name());
    }
    
    // creates a random item
    // can be added to maps and inventories
    //   level = approximate level of item to create
    // NB: allows for various out-of-depth items to appear
    public static Thing createItem(int level) {
        switch (Rand.d(15)) {
            case 1 :
                return createArmour(level);
            case 2 :
                return Lib.createType("IsWeapon",level);
            case 3 :
                return SpellBook.create(level);
            case 4 :
                return Lib.createType("IsWand",level);
            case 5 :
                return Lib.createType("IsRing",level);
            case 6 :
                return Potion.createPotion();
            case 7 :
            case 8 :
                return Lib.createType("IsFood",level);
            case 9 :
            case 10 :
                return Lib.createType("IsScroll",level);
            case 11 :
                return createArmour(level);
            case 12 :
                return Missile.createMissile(level);
            case 13 :
                return Coin.createRoundedMoney(Rand.d(10 + level * 5)
                * Rand.d(3 + level * 5));
            case 14 :
                return RangedWeapon.createRangedWeapon(level);
            case 15 :
                return Lib.createType("IsSword",level);
        }
        return Food.createFood(level);
    }
    
    public static Thing addNewArtifact(Thing a) {
        Lib.add(a);
        return a;
    }
    
    public static Thing getArtifact(String s) {
    	Map<String, Thing> hm=getCreatedUniques();
    	Thing artifact=hm.get(s);
   		if (artifact==null) {
   			artifact=createThing(Lib.get(s));
   		}
        hm.put(s,artifact);
        return artifact;
    }
    
    
    public static Thing createFoe(int level) {
       	return Lib.createType("IsHostile",level);      	
    }
    
    public static Thing createMonster(int level) {
        return Lib.createType("IsMonster",level+Rand.r(3));
    }
    
    public static Thing createCreature(int level) {
        return Lib.createType("IsMonster",level);
    }
    
    public static Thing createMagicItem(int level) {
        return Lib.createType("IsMagicItem",level);
    }
    
    public static void init() {
        // set up base classes
        initBase();
        
        // create effects, spells and utility objects
        Special.init();
        Effect.init();
        Skill.init();
        Spell.init();
		
        
        // call inidvidual library generation classes
        Aura.init();
		Being.init();
        Special.initClouds();
        Item.init();
        Decoration.init();
        Scenery.init();
        Tile.init();
        Door.init();
        Chest.init();
        Trap.init();
        Gods.init();
        Quest.init();
        
        // init people and monsters
        AI.init();
        Animal.init();
        Monster.init();
        Person.init();
        Artifact.init();
        EndGame.init();
        Tutorial.init();
        GoblinVillage.init();
		Temple.init();
        
        // inits that use random ingredients
        Recipe.init();  
        Spell.updateIngredients();
        
        if (Game.isDebug()) {
        	System.out.println("Library initialisation complete");
        	System.out.println(instance().stats());
        }
    }
    
    public Set<String> getAllPropertyNames() {
    	HashSet<String> hs=new HashSet<>();
    	
    	for (Iterator<Thing> it=all.iterator(); it.hasNext(); ) {
    		BaseObject b=it.next();
    		
    		Map<String,Object> m=b.getCollapsedMap();
    		
    		for (Iterator<String> i=m.keySet().iterator(); i.hasNext(); ) {
    			hs.add(i.next());
    		}
    	}
    	
    	return hs;
    }

    public void set(String name, Object object) {
        lib.put(name, object);
    }
    
    public static void setInstance(Lib newInstance) {
        if (Lib.instance!=newInstance) {
        	Lib.instance = newInstance;
        	newInstance.buildTypeArrays();
        }
    }
}