// A Portal is a special object which represents a way to travel either
//   - from one map to another
//   - between distant locations on the same map
//
// use Portal.travel(thing) to send an object, create or Hero to the
// target destination.
//
// Portals will often operate in pairs, e.g. stairs on two different levels
// which link to each other.
//
// If of Portal is not twinned with another Portal, then a secret marker
// is added to the destination location.
//
// Portals also implement functionality for creatures to follow the hero
// through the portal if they are in pursuit.

package mikera.tyrant;

import java.util.HashMap;

import mikera.tyrant.author.MapMaker;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Script;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.Text;
import mikera.util.Maths;
import mikera.util.Rand;

public class Portal  {
    
	@SuppressWarnings("serial")
	private static class PortalAction extends Script {
		@Override
		public boolean handle(Thing t, Event e) {
			Map m=t.getMap();
			
			if (!m.isBlocked(t.x,t.y)) {
				Thing cr=getCritter(t);
				if (cr!=null) {
					m.addThing(cr,t.x,t.y);
				}
			}
			return false;
		}
	}

    public static Thing create() {
        return create("invisible portal");
    }
    
    public static Thing create(String s, Map m, int x, int y) {
        Thing t=create(s);
        t.set("Level",m.getLevel());
        m.addThing(t,x,y);
        return t;
    }
    
    public static Thing create(String s) {
        return Lib.create(s);
    }
    
    public static Thing create(String s,String complexName, int dlevel) {
        Thing p= Lib.create(s);
        p.set("ComplexName",complexName);
        p.set("DestinationLevel",dlevel);
        return p;
    }
    
    public static boolean canTravel(Thing t) {
        return t.isHero();
    }
    
    // go through the portal
    public static void travel(Thing portal,Thing thing) {
        // see if blocked by monsters in pursuit
        if (thing==Game.hero()) {
	        Thing cr=peekCritter(portal);
	        if (cr!=null) {
	        	Game.message("The way is blocked by "+cr.getAName());
	        	return;
	        }
	    }
        
        if (portal.handles("OnTravel")) {
        	Event e=new Event("Travel");
        	e.set("Target",thing);
        	if (portal.handle(e)) return;
        }
        
        // special handling if it is the hero
        if (thing.isHero()) {
            // retrieve current map
            Map curmap = thing.getMap();
            int cx = thing.x;
            int cy = thing.y;
            
            if ((curmap != null)) {
                
                // see if there are any critters going to try to follow
                Thing[] things = curmap.getThings(cx - 10, cy - 10, cx + 10,
                cy + 10);
                // System.out.println(things.length);
                for (int i = 0; i < things.length; i++) {
					Thing t=things[i];
					
                    if (t.getFlag("IsHostile")) {
                        if (t.isVisible(Game.hero())) {
                            thing.addThing(t);
                        }
                    }
					
					// followers
					if (AI.isFollower(t,thing)) {
						thing.addThing(t);
					}
                }
            }
            
            // reverse direction of critters in transit
            for (Thing cr=Portal.getCritter(portal); cr!=null; cr=Portal.getCritter(portal)) {
                thing.addThing(cr);
            }
        }
        
        // move traveller to target map
        Map map=(Map)portal.get("PortalTargetMap");
        if (map==null) {
            ensurePortalDestination(portal);
            map=(Map)portal.get("PortalTargetMap");
            if (map==null) throw new Error("Portal destination not created: "+portal.getName(Game.hero()));
        }
        
        int tx=portal.getStat("PortalTargetX");
        int ty=portal.getStat("PortalTargetY");
        
		Thing targetPortal=map.getFlaggedObject(tx,ty,"IsPortal");
		if ((targetPortal!=null)&&(!targetPortal.isInvisible())) {
			Portal.arrive(targetPortal,thing);
			return;
		}
		
        // add followers to map on arrival
        if (thing.isHero() && !map.getFlag("IsWorldMap")) {
            Thing[] followers= Game.hero().getFlaggedContents("IsBeing");
            for (int i=0; i<followers.length; i++) {
				Thing f=followers[i];
				if (AI.isFollower(f,thing)) {
					f.moveTo(map,tx,ty);
	                f.displace();
				} else {
					map.addThing(f);
				}
                
            }
        }
        
        thing.moveTo(map,tx,ty);
        
        //if (portal.getFlag("ForgetDestination")) {
        //    portal.set("PortalTargetMap",null);
        //}
    }
	
	/**
	 * Call when a thing arrives at a destination portal
	 * @param portal
	 * @param t
	 */
	public static void arrive(Thing portal, Thing t) {
		if (t.isHero()) {
			Map map=portal.getMap();
			int tx=portal.getMapX();
			int ty=portal.getMapY();
			Movement.moveTo(t,map,tx,ty);
			
			if (!map.getFlag("IsWorldMap")) {
	            Thing[] followers= Game.hero().getFlaggedContents("IsBeing");
	            for (int i=0; i<followers.length; i++) {
					Thing f=followers[i];
					portal.addThing(f);
	            }
			}
			
		} else {
			portal.addThing(t);
		}
	}
    
    public static Map getTargetMap(Thing p) {
    	return ensurePortalDestination(p);
    }
    
    public static Map ensurePortalDestination(Thing p) {
    	Map map=(Map)p.get("PortalTargetMap");
    	if (map!=null) return map;
    	
    	String complexName=destinationComplex(p);
        int dlevel=destinationLevel(p);
        
		int version=0;
		if (p.getFlag("NewMap")) {
			version=1;
			HashMap<String, Map> hm=Game.instance().getMapStore();
			while (hm.containsKey(getHashName(complexName,dlevel,version))) {
				version++;
			}
		}
		
       	map=getMap(complexName,dlevel,version);
        
        if (map==null) {
        	throw new Error("Can't ensure portal destination ["+complexName+":"+dlevel+"] for ["+p.name()+"]");
        }
        
        makeLink(p,map);
        
        return map;
    }
    
    public static String destinationComplex(Thing p) {
    	String complexName=p.getString("ComplexName");
        
        if ((complexName==null)&&p.getFlag("IsRoutePortal")) {
        	// route portals are named for their target complex
        	return p.name();
        }
        
        if ((complexName==null)&&p.getFlag("IsPlacePortal")) {
        	// route portals are named for their target complex
        	return p.name();
        }
    	
        if (complexName==null) {
        	complexName=p.getMap().getString("ComplexName");
        }
        
    	return complexName;
    }
    
    private static String getHashName(String complexName, int dlevel,int version) {
		String hashName=complexName+":"+Integer.toString(dlevel);
	        
		if (version>0) hashName+=":"+version;
		
		return hashName;
	}
	
    /**
     * Gets a specified map, creating it if not yet created
     * 
     * @param complexName Dungeon complex
     * @param dlevel Level of dungeon complex
     * @return
     */
    public static Map getMap(String complexName, int dlevel,int version) {
    	
        String hashName=getHashName(complexName,dlevel,version);
		
        HashMap<String, Map> maps=Game.instance().getMapStore();
        
        Map map=maps.get(hashName);
        if (map==null) {
        	// create map and add it to store
        	map=createMap(complexName,dlevel);
        	maps.put(hashName,map);
        }
		
       	map.set("DungeonLevel",dlevel);
       	map.set("ComplexName",complexName);
       	map.set("DungeonVersion",version);
       	map.set("HashName",hashName);
        return map;
    }
    
    public static Map loadFromFile(String name) {
    	MapMaker m=new MapMaker();
    	Map map=m.create(Text.loadFromFile(name), false);
    	
    	if (map.getFlag("IsWorldMap")) return map;
    	
    	Thing[] eps=map.getObjects(0,0,map.width-1,map.height-1,"IsEntrancePoint");
    	if (eps.length>0) {
    		Thing ep=eps[Rand.r(eps.length)];
    		Thing ent=Portal.create(ep.getString("PortalName"));
    		map.addThing(ent,ep.x,ep.y);
    		map.setEntrance(ent);
    	} else {
    		Game.warn("No entrance for map "+name);
    	}
    	return map;
    }
    
    private static Map createMap(String name, int dlevel) {
        Map map=null;
		if (name.equals("karrain")) {
            map=WorldMap.createWorld();
        } else if (name.equals("town")) {
            map=Town.createTown(80,48);
        } else if (name.equals("caves")) {
            map=Caves.createCaves(65,65,dlevel);
        } else if (name.equals("ruin")) {
            map=DeepForest.create(71,71);
        } else if (name.equals("pit")) {
            map=Pit.create(dlevel);
        } else if (name.equals("wood temple")) {
            map=new Map(71,71);
            DeepForest.makeWoodTemple(map);
        } else if (name.equals("goblin village")) {
            map=GoblinVillage.makeGoblinVillage();
        } else if (name.equals("graveyard")) {
        	map=new Map(71,71);
        	Graveyard.makeGraveyard(map);
        } else if (name.equals("Old Nyck's hut")) {
        	map=Town.buildNyckMap();
                          
        } else if (name.equals("dark forest")) {
            map=DeepForest.makeDarkForest(dlevel);
        } else if (name.equals("tutorial inn")) {
    		map=Tutorial.buildTutorialMap();

        } else if (name.equals("tutorial cellar")) {
    		map=Tutorial.buildCellar();
        } else if (name.equals("tutorial loft")) {
    		map=Tutorial.buildLoft();

        } else if (name.equals("dark tower")) {
        	switch (dlevel) {
            	default:
                    map=Dungeon.createDarkTowerLevel(dlevel);
            		break;
            		
        		case 10: 
        			map=EndGame.getFinalMap();
        			break;
        	}
        } else if (name.equals("old dungeon")) {
        	switch (dlevel) {
            	default:
                    map=Dungeon.createDungeon(60,40,dlevel);
            		break;
            		
        		case 10: 
        			map=Caves.createBigCave(50,50,10);
        			break;
        	}
        } else if (name.equals("mysterious dungeon")) {
        	switch (dlevel) {
            	default:
                    map=Dungeon.createDungeon(60,40,dlevel+3);
            		break;
            		
        		case 15: 
        			map=Dungeon.createDungeon(70,25,dlevel+3);
        			map.getExit().remove();
        			map.setExit(null);
        			
        			Thing c=Chest.create(15);
        			c.addThing(Lib.create("potion of literacy"));
        			map.addThing(c);
        			
            		break;
        	}
        }  else if (name.equals("deep dungeon")) {
            map=Dungeon.createDungeon(60,40,dlevel+5);
            map.set("Description","Deep Dungeon");
        	
        } else if (name.equals("old catacombs")) {
        	switch (dlevel) {
            	default:
                    map=Dungeon.createDungeon(60,40,6+dlevel*3);
            		map.set("Description","Old Catacombs");
            		
            		// key to escape the catacombs
            		// make it indestructible just in case!
            		if (dlevel==3) {
            			Thing t=Lib.create("skull key");
            			t.set("IsDestructible",0);
            			map.addThing(t);
            		}
            		
            		break;
            		
        		case 4: 
        			map=Pit.createArtifactVault("IsUndead",15);
        			break;
        	}
        } else if (name.equals("dungeon")) {
        	Thing dg=Lib.getLibraryInstance("dungeon");
        	int level=dg.getStat("LastLevel");
        	level=Maths.min(50,level+1);
        	dg.set("LastLevel",level);
        	map=Dungeon.createDungeon(60,60,level);
        	map.getExit().remove();
        	map.setExit(null);
		} else {
			Thing p=Lib.getLibraryInstance(name);
			if (p==null) {
	        	throw new Error("Unknown portal ["+name+"]");
			}
			
			String mapName=p.getString("MapFileName");
			if (mapName==null) {
	        	throw new Error("No portal destination ["+name+"]");
			}
			
			Game.warn("Loading map ["+mapName+"]");
			 map=loadFromFile(mapName);
        }   
    	
		// zero time action event to do map set-up
		map.action(Event.createActionEvent(0));

        return map;

    }
    
    /**
     * Gets the destination level of a given portal
     * 
     * @param p
     * @return
     */
    public static int destinationLevel(Thing p) {
        int dl=p.getStat("DestinationLevel");
        if (dl>0) return dl;
        
        Map currentMap=p.getMap();
        if (currentMap!=null) {
        	return currentMap.getStat("DungeonLevel")+p.getStat("DestinationLevelIncrement");
        }
        
        return 1;
    }
    
    // add a critter to portal monster list
    // give to hero if a follower and no map
    public static void addCritter(Thing p, Thing c) {
        if (p.place != null) {
            p.addThing(c);
        } else {
            Thing leader=(Thing)c.get("Leader");
            if (leader != null) {
                // add to leader's inventory
                leader.addThing(c);
            }
        }
    }
    
    public static Thing peekCritter(Thing p) {
    	return p.getThing(0);
    }
    
    public static Thing getCritter(Thing p) {
        Thing c = p.getThing(0);
        if (c==null) return null;
        c.remove();
        return c;
    }
    
    // link portal to entrance of given map
    private static void makeLink(Thing portal, Map map) {
        if (portal.getMap()==null) {
            throw new Error("Bad portal link attempted");
        }
        Thing target=map.getEntrance();
        if (target == null) {
        	
        	// look up a corresponding portal
        	String fromMapName=portal.getMap().getString("ComplexName");
        	if (fromMapName!=null) {
        		target=map.find(fromMapName);
        	}
        	
            if (target==null) {
            	throw new Error("Target map ["+map.getString("Description")+"] of portal ["+portal.name()+"] has no entrance caled ["+fromMapName+"] in makeLink");
            }
        	/*Game.warn("No entrance for map!");
            Game.warn("Creating temporary stairs");
        	
            Thing ent=Lib.create("stairs up");
            map.addThing(ent);
            map.setEntrance(ent);*/
        }
       	Portal.setDestination(portal, map, target.getMapX(), target.getMapY());
        Portal.setDestination(target, portal.getMap(), portal.x, portal.y);
    }
    
    // Portal may introduce new creatures to dungeon
    // or do other stuff based on time elapsed
        /*
         * TODO reinstate this functionality
         public void action(int time) {
                Map m = getMap();
         
                // bring creatures out of list
                if (critters != null) {
                        int cx = x + Rand.r(3) - 1;
                        int cy = y + Rand.r(3) - 1;
                        if (!m.isBlocked(cx, cy)) {
                                m.addThing(critters, cx, cy);
                        }
                }
        }
         */
    
    public static void setDestination(Thing p, Map m, int tx, int ty) {
        p.set("PortalTargetMap", m);
        p.set("PortalTargetX", tx);
        p.set("PortalTargetY", ty);
    }
    
    public static void init() {
        Thing t=Lib.extend("base portal","base scenery");
        t.set("IsPortal",1);
        t.set("IsBlocking",0);
        t.set("IsDestructible",0);
        t.set("ImageSource","Scenery");
        t.set("ASCII","*");
        t.set("MapColour",0x0000FFFF);
        t.set("LevelMin",1);
        t.addHandler("OnAction",new PortalAction());
        Lib.add(t);
        
        // invisible portal is really just a marker
        t=Lib.extend("invisible portal","base portal");
        t.set("IsInvisible",1);
        t.set("ImageSource","Effects");
        t.set("Image",145);
        t.set("IsPortal",0); // not travelable
        t.set("IsMarkerPortal", 1);
        t.set("IsScenery",0);
        Lib.add(t);
        
        initPortals();
        initRoutePortals();
        initPlaces();
    }
    
    public static void initPortals() {
        Thing t;
        
        t=Lib.extend("base stairs","base portal");
        t.set("IsStairs",1);
        t.set("IsBlocking",0);
        t.set("IsInvisible",0);
        Lib.add(t);
        
        t=Lib.extend("stairs up","base stairs");
        t.set("Image",0);
        t.set("DestinationLevelIncrement",-1);
        Lib.add(t);
        
        t=Lib.extend("pit","base portal");
        t.set("ComplexName","pit");
        t.set("NewMap",1);
        t.set("Image",41);
        Lib.add(t);
        
 
        t=Lib.extend("stairs down","base stairs");
        t.set("Image",1);
        t.set("DestinationLevelIncrement",1);
        Lib.add(t);
        
        t=Lib.extend("ladder up","base stairs");
        t.set("DestinationLevelIncrement",-1);
        t.set("Image",2);
        Lib.add(t);
        
        t=Lib.extend("ladder down","base stairs");
        t.set("DestinationLevelIncrement",1);
        t.set("Image",3);
        Lib.add(t);
        
        t=Lib.extend("infinite portal","base stairs");
        t.set("Image",1);
        t.set("DestinationLevelIncrement",1);
        Lib.add(t);
    }
    
    public static void initRoutePortals() {
        Thing t;
        
        // route portal is invisible but travellable
        t=Lib.extend("route portal","base portal");
        t.set("IsInvisible",1);
        t.set("IsRoutePortal",1);
        t.set("ImageSource","Effects");
        t.set("Image",145);
        t.set("IsScenery",0);
        Lib.add(t);
        
        
        t=Lib.extend("annenvale","route portal");
        t.set("MapFileName","/maps/world/annenvale.txt");
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("karrain","route portal");
        t.set("MapFileName","/maps/world/karrain.txt");
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("wilds","route portal");
        t.set("MapFileName","/maps/world/wilds.txt");
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("doom","route portal");
        t.set("MapFileName","/maps/world/doom.txt");
        t.set("DestinationLevel",1);
        Lib.add(t);
    }
        
    
    public static void initPlaces() {
        Thing t;
        
        t=Lib.extend("place portal","base portal");
        t.set("IsPlacePortal",1);
        Lib.add(t);
        
        t=Lib.extend("tutorial inn","place portal");
        t.set("ComplexName","tutorial inn");
        t.set("Image",310);
        Lib.add(t);
	
        t=Lib.extend("tutorial cellar","place portal");
        t.set("ComplexName","tutorial cellar");
        t.set("Image",4);
        Lib.add(t);
		
        t=Lib.extend("tutorial loft","place portal");
        t.set("ComplexName","tutorial loft");
        t.set("Image",2);
        Lib.add(t);
        
        t=Lib.extend("Old Nyck's hut","place portal");
        t.set("Image",309);
        t.set("ComplexName","icy hut");
        Lib.add(t);
        
        t=Lib.extend("town","place portal");
        t.set("Image",305);
        t.set("NewMap",1);
        t.set("ComplexName","town");
        Lib.add(t);
        
        t=Lib.extend("graveyard","place portal");
        t.set("Image",220);
        t.set("ComplexName","graveyard");
        Lib.add(t);
        
        t=Lib.extend("ruin","place portal");
        t.set("DestinationLevel",1);
        t.set("ComplexName","ruin");
        t.set("Image",302);
        Lib.add(t);
        
        t=Lib.extend("caves","place portal");
        t.set("ComplexName","caves");
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("grotto","place portal");
        t.set("MapFileName","/maps/special/grotto.txt");
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
		
        t=Lib.extend("dwarftown","place portal");
        t.set("MapFileName","/maps/special/dwarftown.txt");
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("firetop","place portal");
        t.set("MapFileName","/maps/special/firetop.txt");
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("yanthrall temple","place portal");
        t.set("MapFileName","/maps/temples/yanthrall.txt");
        t.set("Image",300);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("mysterious dungeon","place portal");
        t.set("ComplexName","mysterious dungeon");
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("deep dungeon","place portal");
        t.set("ComplexName","deep dungeon");
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("dungeon","place portal");
        t.set("ComplexName","dungeon");
        t.set("NewMap",1);
        t.set("Image",303);
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("bandit caves","place portal");
        t.set("ComplexName","bandit caves");
        t.set("Image",303);
        t.set("DestinationLevel",7);
        Lib.add(t);
        
        t=Lib.extend("goblin village","place portal");
        t.set("ComplexName","goblin village");
        t.set("DestinationLevel",1);
        t.set("Image",304);
        Lib.add(t);
        
        t=Lib.extend("old dungeon","stairs down");
        t.set("ComplexName","old dungeon");
        t.set("DestinationLevel",1);
        Lib.add(t);
        
        t=Lib.extend("dark forest","place portal");
        t.set("ComplexName","dark forest");
        t.set("DestinationLevel",10);
        t.set("Image",307);
        Lib.add(t);
        
        t=Lib.extend("wood temple","place portal");
        t.set("ComplexName","wood temple");
        t.set("DestinationLevel",10);
        t.set("Image",303);
        Lib.add(t);
        
        t=Lib.extend("dark tower","place portal");
        t.set("ComplexName","dark tower");
        t.set("DestinationLevel",1);
        t.set("Image",308);
        Lib.add(t);
        
        //public static void setDestination(Thing p, Map m, int tx, int ty)
        t=Lib.extend("traveler portal","place portal");
        t.set("Image",20);
        Lib.add(t);        
        
    }
}