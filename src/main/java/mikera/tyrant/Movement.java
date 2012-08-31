/*
 * Created on 12-Jun-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package mikera.tyrant;

import java.util.Iterator;
import java.util.List;

import mikera.tyrant.engine.Describer;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Point;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.util.Maths;
import mikera.util.Rand;

/**
 * @author Mike
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Movement {
	// try to move
	// return true if some action taken
    public static boolean tryMove(Thing thing, Map map, int tx, int ty) {
//        System.out.println(new MapHelper().mapToString(map));
//        System.out.println(new MapHelper().visitToString(map));
        
    	if (thing.getFlag("IsConfused")&&(Rand.d(3)>1)) {
        	tx=thing.x+Rand.r(3)-1;
        	ty=thing.y+Rand.r(3)-1;
        }
        map.visitPath(thing.x, thing.y);
        boolean moving = false;
        if (map==null) {
        	System.out.println("no map in Movement.tryMove ["+thing.getName(Game.hero())+"]");
        	return false;
        }
        int dx = thing.getStatIfAbsent("RunDirectionX", Integer.MIN_VALUE);
        int dy = thing.getStatIfAbsent("RunDirectionY", Integer.MIN_VALUE);
        if(dx == Integer.MIN_VALUE) {
            dx = RPG.sign(tx - thing.x);
            dy = RPG.sign(ty - thing.y);
        } else {
            tx = thing.x + dx;
            ty = thing.y + dy;
        }
        if (thing.getFlag("IsMobile")) {
        	thing.set("DirectionX", dx);
        	thing.set("DirectionY", dy);
        }
        
        if(thing.isRunning()) {
            moving = tryRunningMove(thing, map, tx, ty, dx, dy);
            return moving;
        }
        moving = canMove(thing, map, tx, ty);
        
        // handle staying still
        if ((dx==0)&&(dy==0)) {
        	// Game.warn(thing.name()+" moves to same square!");
        	doMove(thing, map, tx, ty);
        	return true;
        }
        
        Thing mob = map.getMobile(tx, ty);
        if (mob != null) {
        	if (thing.isHostile(mob)||thing.getFlag("IsConfused")) {
        		Combat.attack(thing, mob);
        		return true;
        	} else if (!moving) {
        		if (tryDisplace(thing,mob)) return true;
        	}
        } else {
        	// TODO: remove inteligent dependancy!
            if (thing.getFlag("IsIntelligent") && tryBump(thing, map, tx, ty)) {
        		// MA: return true since we did something
            	return true;
            }
        }
        
        // avoid warnin squares
        if ((thing!=Game.hero())&&(map.getFlaggedObject(tx,ty,"IsWarning")!=null)) {
        	moving=false;
        }
        
        if (moving) {
            return doMove(thing, map, tx, ty);
        }
        return tryDig(thing,map,tx,ty);
    }
    
	private static final double rt2=Math.sqrt(2);
	
	public static boolean tryDig(Thing b, Map m, int x, int y) {
		if (Tile.isDiggable(m,x,y)&&Tile.dig(b,m,x,y)) {
			return true;
		}
		return false;
	}
	
	public static int calcMoveSpeed(Thing t) {
		int ms=t.getStat(RPG.ST_MOVESPEED);
		int ath=t.getStat(Skill.ATHLETICS);
		if (ath>0) {
			ms+=Maths.min(100,t.getStat("AG"))*ath/4;
		}
		return ms;
	}
	
	public static int moveCost(Map m, Thing t, int tx, int ty) {
		double mcost=m.getMoveCost(tx, ty);

		if (t.getFlag("IsFlying")) mcost=100;

		if (m.getFlag("IsWorldMap")) mcost*=20;
		
		int d=(tx-t.x)*(tx-t.x)+(ty-t.y)*(ty-t.y);
		if (d==2) mcost=mcost*rt2;
		
		
		// base movement cost plus encumberance
		int basecost=t.getStat(RPG.ST_MOVECOST);
		basecost+=t.getStat(RPG.ST_ENCUMBERANCE);
		
		mcost= (mcost*basecost)/ calcMoveSpeed(t);
		
		if (mcost==0) Game.warn(t.getName(Game.hero())+" has zero move cost!");
		return (int)mcost;
	}
	
	public static boolean push(Thing t, int dx, int dy) {
		Map map=t.getMap();
		if ((map==null)||(t.place!=map)) return false;
		int tx=t.x+dx;
		int ty=t.y+dy;
		if (!map.isBlocked(tx,ty)) {
			t.moveTo(map,tx,ty);
			return true;
		}
		return false;
	}
	
	public static void jump(Thing h, int x, int y) {
		Map m=h.getMap();
		
		double sx=h.x;
		double sy=h.y;
		double dx=x-sx;
		double dy=y-sy;
		if ((dx==0.0)&&(dy==0.0)) return;
		int d=0;
		
		if (Math.abs(dx)<Math.abs(dy)) {
			d=(int)Math.abs(y-sy);
			dx=dx/Math.abs(dy);
			dy=(dy>0)?1:-1;
		} else {
			d=(int)Math.abs(x-sx);
			dy=dy/Math.abs(dx);
			dx=(dx>0)?1:-1;
		}
		
		int maxJump=(int)(1.0+Math.sqrt(h.getStat(Skill.ATHLETICS)));
		
		d=Maths.min(d,maxJump);
		
		for (int i=0; i<d; i++) {
			sx+=dx;
			sy+=dy;
			int tx=(int)Math.round(sx);
			int ty=(int)Math.round(sy);
			
			if (canJump(h,m,tx,ty)) {
				h.incStat("APS", -(moveCost(m, h, tx, ty)*2));
				flyTo(h,m,tx,ty);
			} else {
				h.displace();
				return;
			}
		}
		
		
	}

	
	private static boolean tryBump(Thing being, Map m, int tx, int ty) {
		Thing head = m.getObjects(tx, ty);
		while (head != null) {
			if (head.isBlocking()&&head.handles("OnBump")) {
				
				Event e=new Event("Bump");
				e.set("Target",being);
				head.handle(e);
				
				return e.getFlag("ActionTaken");
			}
			head = head.next;
		}
		return false;
	}

	private static boolean tryDisplace(Thing t, Thing m) {
		if (t==m) Game.warn("Trying to displace self!");

		
		if (t.isHero()) {
			// OK	
		} else {
			// monsters only push past if tougher
			if (t.getStat("TG")<m.getStat("TG")) return false;
		}
		
		// cost of displace attempt
		t.incStat("APS",-100);

		boolean success=RPG.test(t.getStat("ST"),m.getStat("ST"));
		
		// never push past hero
		// or other non-displaceable beings
		if (success&&(!m.getFlag("IsDisplaceable"))) {
			success=false;
		}
		
		// try to displace
		if (success) {
			
			Map map=t.getMap();
			
			int fx=t.x; int fy=t.y;
			int tx=m.x; int ty=m.y;
			
			if (Tile.isSensibleMove(t,map,tx,ty)&&Tile.isSensibleMove(m,map,fx,fy)) {
				t.moveTo(map,tx,ty);
				m.moveTo(map,fx,fy);
				t.message("You push "+m.getTheName()+" out of the way");
				return true;
			}
		} 
			
		t.message ("You can't get past "+m.getTheName());
		return false;
	}
	
	/**
	 * Work out whether a move is phyically possible
	 * 
	 * @param t A thing
	 * @param m Destination map
	 * @param tx Destination x-position on map
	 * @param ty Destination y-position on map
	 * @return True if move is possible
	 */
	public static boolean canMove(Thing t, Map m, int tx, int ty) {
		if (m.isObjectBlocked(tx,ty)) return false;
		return canMoveToTile(t,m,tx,ty);
	}
	
	public static boolean canMoveToTile(Thing t, Map m, int tx, int ty) {
		if (Tile.isPassable(t,m,tx,ty)) return true;
		
		return false;
	}
	
	public static boolean canJump(Thing t, Map m, int tx, int ty) {
		if (canMove(t,m,tx,ty)) return true;
		
		if (!m.isBlocked(tx,ty)) return true;
		
		int type=m.getTile(tx,ty);

		if (Tile.isFilling(type)) return false;
		
		Thing[] ts=m.getThings(tx,ty);
		for (int i=0; i<ts.length; i++) {
			Thing tt=ts[i];
			if (tt.getFlag("IsBlocking")&&(!tt.getFlag("IsJumpable"))) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Teleport a Thing to a new map location
	 * 
	 * @param t A Thing
	 * @param m The destination Map
	 * @param tx Destination x-coordinate
	 * @param ty Destination y-coordinate
	 */
	public static void teleport(Thing t, Map m, int tx, int ty) {
		moveTo(t,m,tx,ty);
	}
	
	/**
	 * Move a Thing to a new map location
	 * 
	 * @param t A Thing
	 * @param m The destination Map
	 * @param tx Destination x-coordinate
	 * @param ty Destination y-coordinate
	 */
	public static void moveTo(Thing t, Map m, int tx, int ty) {

		t=m.addThing(t, tx, ty);
		if (t.place!=m) {
			throw new Error("Thing not added!");
		}
		Movement.enterTrigger(t, m, tx, ty, !t.getFlag("IsFlying"));
		
		if (t.isHero()) locationMessage();
	}
	
	/**
	 * Move a Thing to a new map location
	 * 
	 * @param t A Thing
	 * @param m The destination Map
	 * @param tx Destination x-coordinate
	 * @param ty Destination y-coordinate
	 */
	public static void flyTo(Thing t, Map m, int tx, int ty) {

		m.addThing(t, tx, ty);
		Movement.enterTrigger(t, m, tx, ty, false);
		if (t.isHero()) locationMessage();
	}
	
	/**
	 * Process special events that occur 
	 * @param t A Thing
	 * @param m The map where the thing has been placed
	 * @param tx Map x-coordinate
	 * @param ty Map y-coordinate
	 * @param touchFloor Whether the Thing touches the floor
	 */
	private static void enterTrigger(Thing t, Map m, int tx,int ty, boolean touchFloor) {
		Thing [] things=m.getThings(tx,ty);
		boolean handled=false;
		for (int i=0; i<things.length; i++) {
			Thing tt=things[i];
			if ((tt.place==m)&&(tt.handles("OnEnterTrigger"))) {
				Event te=new Event("EnterTrigger");
				te.set("Target",t);
				te.set("TouchFloor",touchFloor);
				handled|=tt.handle(te);
			}
		}
		
		if (!handled) Tile.enterTrigger(t,m,tx,ty,touchFloor);
		
		//if (m.getFlag("IsWorldMap")) {
		//	WorldMap.encounter(m,tx,ty,2000);
		//}
	}
	
	/**
	 * Display messages when hero moves to a new location
	 * 
	 */ 
	private static void locationMessage() {
		Thing h=Game.hero();
		Thing t = h.getMap().getObjects(h.x, h.y);
		while (t != null) {
			if (t.getFlag("IsItem")) {
				Item.tryIdentify(h,t);
				//Give  more informaton when walking over an identified item
				String s = t.getAName();
				if (t.getStat("HPS")<t.getStat("HPSMAX")) 				{	s="damaged "+s;	}
				if (t.getFlag("IsWeapon")&&Item.isIdentified(t)) 	{	s=s+"  "+Weapon.statString(t);	}
				if (t.getFlag("IsArmour")&&Item.isIdentified(t)) 	{ s=s+"  "+Armour.statString(t);	}

				Game.message("There " + (Describer.isPlural(t) ? "are " : "is ")	+ s + " here");
				h.isRunning(false);
			}
			t = t.next;
		}
	}

    private static boolean doMove(Thing thing, Map map, int tx, int ty) {
        map.visitPath(tx, ty);
        moveTo(thing, map, tx, ty);
        thing.incStat("APS", -moveCost(map, thing, tx, ty));
        return true;
    }

    private static boolean tryRunningMove(Thing thing, Map map, int tx, int ty, int dx, int dy) {
        boolean canMove = canMove(thing, map, tx, ty);
        int previousExits = -1;
        int runCount = thing.getStat("RunCount");
        if(runCount != 1) canMove = !interruptsRunning(thing, map, tx, ty);
        if (canMove) {
            previousExits = thing.orthogonalExits(dx, dy).size(); 
            doMove(thing, map, tx, ty);
            if(runCount == 1) return true;
            boolean isStillRunning = previousExits == thing.orthogonalExits(dx, dy).size();
            canMove = isStillRunning;
            if(isStillRunning) return isStillRunning;
        }
        if(runCount == 1)return false;
        if(thing.areSeveralDirectionsNotVisited()) return false;
        if(thing.inARoom()) return false;
        //try to run around corners
        List freedoms = thing.moreExits(dx, dy);
        if(freedoms.isEmpty()) return false;
        for (Iterator iter = freedoms.iterator(); iter.hasNext();) {
            Point placeToMove = (Point) iter.next();
            if(map.getPath(placeToMove.x, placeToMove.y) == 1) continue;
            if(interruptsRunning(thing, map, placeToMove.x, placeToMove.y)) continue;
            int oldX = thing.x;
            int oldY = thing.y;
            // this may change the isRunning flag
            doMove(thing, map, placeToMove.x, placeToMove.y);
            thing.set("RunDirectionX", RPG.sign(placeToMove.x - oldX));
            thing.set("RunDirectionY", RPG.sign(placeToMove.y - oldY));
            return thing.isRunning();
        }
        return false;
    }

    private static boolean interruptsRunning(Thing thing, Map map, int tx, int ty) {
        if(map.isBlocked(tx, ty)) return true;
        Thing next;
        for (next = map.getObjects(tx, ty); next != null; next = next.next) {
            if(thing.isDoorVisible(tx, ty)) return true;
            if(next.get("Message") != null) continue;
            if(next.getFlag("IsMarkerPortal")) continue;
            if(next.getFlag("IsMarker")) continue;
            return true;
        }
        return false;
    }
}