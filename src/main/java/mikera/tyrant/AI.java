package mikera.tyrant;

import java.io.Serializable;

import mikera.tyrant.engine.Description;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Script;
import mikera.tyrant.engine.Thing;
import mikera.util.Maths;
import mikera.util.Rand;

/**
 * AI implements the behaviour of monsters and other beings
 * 
 * AI behaviour can be tuned through the properties of these
 * beings.
 * 
 * @author Mike
 */
public class AI implements Serializable {
	private static final long serialVersionUID = 4051043086123415608L;
    // ai state constants
	// Used with RPG.ST_STATE characteristic
	public static final String STATE_HOSTILE = "Attack";
	public static final String STATE_FOLLOWER = "Follow";
	public static final String STATE_INHABITANT = "Wander";

	// notify event constats

	// ATTACKED event
	// ext = side attacked
	// o = attacker (usually Game.actor)
	public static final int EVENT_ATTACKED = 1;
	public static final int EVENT_VISIBLE = 2;
	public static final int EVENT_MORALECHECK = 3;
	public static final int EVENT_FEARCHECK = 4;
	public static final int EVENT_ALARM = 5;
	public static final int EVENT_THEFT = 6;
	
	public static void setNeutral(Thing m) {
		m.set("IsHostile",0);
		m.set("IsNeutral",1);
		m.set("IsInhabitant",1);
	}

	public static boolean isHostile(Thing m, Thing b) {
		// never hostile to self!
		if (b==m) return false;
		
		boolean mhostile=m.getFlag("IsHostile");
		boolean bhostile=b.getFlag("IsHostile");
		if (mhostile&&bhostile) return false;
		
		if (m.isHero() && bhostile) return true;
		if (b.isHero()) {
			if (mhostile) return true;
			if (b.getStat("CH")<0) return true;
		}
		
		if ((m.getFlag("IsInsane"))||(b.getFlag("IsInsane"))) return true;
		
		if (m.name().equals(b.name())) return false;
		
		if (m.getFlag("IsInhabitant")&&b.getFlag("IsInhabitant")) return false;
		
		if (m.getFlag("IsNeutral")||b.getFlag("IsNeutral")) return false;
		
		if ((mhostile)&&(!bhostile)) return true;
		if ((bhostile)&&(!mhostile)) return true;


		return false;
	}

	public static void turnNasty(Thing m) {
		// Could turn townies into nasties
		// but prefer changing entire map state
		// m.setAI(NastyCritterAI.instance);
		if (m.isHero()) {
			throw new Error("AI.turnNasty(): Trying to affect hero!!!");
		}
		m.set("IsHostile",1);
		if (m.getFlag("IsInhabitant")) m.getMap().setAngry(true);
	}

	public static void reTarget(Thing m, Thing f) {
		m.set("CurrentFoe",f);
	}

	public static int notifyAttack(Thing m, Thing attacker) {
		notify(m,AI.EVENT_ATTACKED,0,attacker);
		return 0;
	}
	
	public static int notify(Thing m, int eventtype, int ext, Object o) {
        Thing other = (Thing)o;
		Map map = m.getMap();
		if (map == null)
			return 0;
		// hero doesn't respond!
		if (m.isHero()) return 0;
		if (isHostile(m,Game.hero()))
			return 0;
		
		
		switch (eventtype) {
			case EVENT_ATTACKED :
				if (other.isHero() && (ext == m.getStat(RPG.ST_SIDE))) {
					if (m.getFlag("IsIntelligent")) Game.message(m.getTheName() + " shouts angrily!");
					turnNasty(m);
				}
				return 0;
			case EVENT_VISIBLE :
				if (m.isHostile(Game.hero())) {
					reTarget(m, Game.hero());
					m.getMap().areaNotify(m.x, m.y, 5, AI.EVENT_ALARM, 0, null);
				}
				return 1;

			case EVENT_THEFT :
				if (m.isVisible(Game.hero())&&m.getFlag("IsIntelligent")) {
					if (RPG.test(m.getStat(RPG.ST_IN), 
							other.getStat(RPG.ST_SK))) {
						Game.message(m.getTheName() + " shouts angrily!");
						turnNasty(m);
						map.areaNotify(m.x, m.y, 10, eventtype, 1, o);
					} else {
						Game.message(m.getTheName() + " doesn't notice");
					}
				}
				return 0;
		}
		return 0;
	}
	
	/**
	 * Finds an appropriate foe for a monster to attack
	 * @param m
	 */
	public static Thing findFoe(Thing m) {
		Thing foe=m.getThing("CurrentFoe");
		if (foe!=null) {
			if (foe.place==null) {
				// previous foe has died
				foe=null;
			} else {
				// use foe if we can see it
				if (!m.canSee(foe)) foe=null;
			}
		}
		if ((foe==null)||(Rand.d(12)==1)) {
			Map map=m.getMap();
			if (map==null) {
				foe=null;
			} else {
				foe=map.findNearestFoe(m);
			}
			if (foe!=null) {
				m.set("CurrentFoe",foe);
			}
		}
		
		return foe;
	}
	
	public static boolean tryMove(Thing m, int nx, int ny) {
		Map map=m.getMap();
		if (!Tile.isSensibleMove(m,map,nx,ny)) return false;
		return Movement.tryMove(m, map, nx, ny);
	}

	// creature attacks towards target tx,ty
	public static void doAttack(Thing m, int tx, int ty) {
		Map map = m.getMap();
		if (map == null)
			return;
		if (Rand.d(100) <= m.getStat(RPG.ST_CASTCHANCE)) {
			if (doCasterAction(m))
				return;
		}

		/*
		 * 
		 if (visible && (Rand.d(3) == 1)
				&& (m.getWielded(RPG.WT_MISSILE) != null)) {
			AI.doCritterAction(m);
			return;
		}
		*/

		int dx = RPG.sign(tx - m.x);
		int dy = RPG.sign(ty - m.y);

		// run away?
		if ((Rand.r(100)<m.getStat("RetreatChance"))||(Being.feelsFear(m)
				&& ((m.getStat(RPG.ST_HPS) * 3) <= m.getStat(RPG.ST_HPSMAX)))) {
			if (tryMove(m, m.x - dx, m.y - dy))
				return;
			if (tryMove(m, m.x - RPG.sign(dx - dy), m.y - RPG.sign(dy + dx)))
				return;
			if (tryMove(m, m.x - RPG.sign(dx + dy), m.y - RPG.sign(dy - dx)))
				return;
		}

		if (tryMove(m, m.x + dx, m.y + dy))
			return;
		if (tryMove(m, m.x + RPG.sign(dx - dy), m.y + RPG.sign(dy + dx)))
			return;
		if (tryMove(m, m.x + RPG.sign(dx + dy), m.y + RPG.sign(dy - dx)))
			return;

		if (Rand.d(2) == 1)
			dx = Rand.r(3) - 1;
		if (Rand.d(2) == 1)
			dy = Rand.r(3) - 1;

		if (!tryMove(m, m.x + dx, m.y + dy)) {
			// blocked
			m.incStat("APS", -30);
			m.set("DirectionX", 0);
			m.set("DirectionY", 0);
		}
	}

	public static class AIScript extends Script {
		private static final long serialVersionUID = 3257562910623609394L;

        public boolean handle(Thing m, Event e) {
			int time=e.getStat("Time");
			
			// bail out if no time to perform action
			if (time<=0) return false;
			
			int aps=m.getStat("APS")+(time*m.getStat("Speed")/100);
			m.set("APS",aps);
			
			if (m.getFlag("IsBeing") && (aps>0)) {
				// max of 10 actions 
				for (int i=0; i<10; i++) {
					doAction(m);
					aps=m.getStat("APS");
					if (aps<=0) break;
				}
			}
			
			if (aps>0) {
				// discard unused APS
				aps=0; 
				// Game.warn("APS discard ("+aps+"): "+m.name());
			}
			m.set("APS",aps);
			
			Being.recover(m,time);
			
			return false;
		}
	}
	
	public static boolean doAction(Thing m) {
		Game.actor=m;
		
		String state = m.getString("AIMode");
		Map map = m.getMap();
		if ((map == null)||!(m.place instanceof Map)) {
			m.set("APS",0);
			return true;
		}
		boolean canSeeHero = m.canSee(Game.hero());

		if ((!m.getFlag("IsHostile"))&&(m.getFlag("IsInhabitant"))&&map.getFlag("IsHostile")) {
			m.set("IsHostile",1);
		}
		
		if (state==null) {
			state="Wander";
			m.set("AIMode", state);
		}
		
		// do interesting actions if visible
		if (canSeeHero) {
			// try spell caster action
			if (m.getFlag(Skill.CASTING)&&doCasterAction(m)) return true;
		}
		
		if (state.equals("Evade")) {
			// Game.warn("Critter action...");
			return doCritterAction(m);
		} else if (state.equals("Guard")) {
			// Game.warn("Guarding...");
			return doGuardAction(m);
		} else if (state.equals("Attack")) {
			Thing f = findFoe(m);
			

			if (f==null) {
				m.set("AIMode","Wander");
				return true;
			}
			
			// try ranged attack
			if ((Rand.d(3)==1)&&doRangedAttack(m,f)) return true;			
			
			// TODO: some way of alerting unaware allies?
			// map.areaNotify(m.x, m.y, 6, AI.EVENT_ALARM, 0, null);
			doAttack(m,f.x,f.y);
			return true;
		} else if (state.equals("Wander")) {
			Thing f = findFoe(m);
				
			if (f != null) {
				// attack the foe
				if (Rand.d(2)==1) m.set(RPG.ST_AIMODE, "Attack");
				doAttack(m, f.x, f.y);				
			} else if ((Rand.d(4) == 1) && (m.getFlag("IsIntelligent"))) {
				// pick up and use stuff
				doPickup(m);
			} else {
				int dx=m.getStat("DirectionX");
				int dy=m.getStat("DirectionY");
				if (((dx==0)&&(dy==0))||(Rand.d(2)==1)) {
					if (Rand.d(3)==1) dx=Rand.r(3)-1;
					if (Rand.d(3)==1) dy=Rand.r(3)-1;
				}
				// make a random move
				doAttack(m, m.x+dx,m.y+dy);
			}
			return true;
		} else if (state.equals("Follow")) {
			return doFollowAction(m);
		}
		return false;
	}
	
	// turn creature into a follower
	public static void setFollower(Thing t, Thing leader) {
		t.set("Leader",leader);
		if (leader.isHero()) {
			t.set("AIMode", AI.STATE_FOLLOWER);
		}
		t.set("IsHostile",leader.getStat("IsHostile"));
		t.set("IsNeutral",leader.getStat("IsNeutral"));
		t.set(RPG.ST_SIDE, leader.getStat(RPG.ST_SIDE));
	}
	
	public static boolean isFollower(Thing t, Thing leader) {
		return (t.get("Leader")==leader);
	}
	
	private static boolean doFollowAction(Thing m) {
		
		Thing l=m.getThing("Leader");
		if ((l==null)||l.isDead()) {
			m.set("AIMode","Wander");
			m.set("APS",0);
			return true;
		}
		
		int ld=Math.abs(m.x-l.x)+Math.abs(m.y-l.y);
		
		Thing f=findFoe(l);
		if (f!=null) {
			int fd=Math.abs(f.x-m.x)+Math.abs(f.y-m.y);
			if ((fd<=2)||(ld<=fd)) {
				doAttack(m,f.x,f.y);
				return true;
			}
		}
		if ((f!=null)||Rand.d(6)<ld) {
			doAttack(m,l.x,l.y);
			return true;
		}
		
		return doRandomWalk(m);
	}
	
	private static boolean doRandomWalk(Thing m) {
		// wander randomly
		int dx = Rand.r(3) - 1;
		int dy = Rand.r(3) - 1;
		int nx = m.x + dx;
		int ny = m.y + dy;
		if (!tryMove(m, nx, ny))
			m.incStat("APS", -100);

		// why not pick up some items?
		if ((Rand.d(10) == 1) && (m.getStat(RPG.ST_CR) >= 5)) {
			doPickup(m);
		}
		return true;
	}
	
	private static void doPickup(Thing m) {
		Map map=m.getMap();
		Thing[] stuff = map.getThings(m.x, m.y);
		boolean found = false;
		for (int i = 0; i < stuff.length; i++) {
			Thing st=stuff[i];
			if (st.getFlag("IsItem")&&(!Item.isOwned(st))) {
				Item.pickup(m,st);
				found = true;
			}
		}
		if (found) {
			Being.utiliseItems(m);	
		}
	}
	
	public static boolean doCritterAction(Thing m) {
		Thing h = Game.hero();
		Map map = m.getMap();

		/*
		// sniff for food - this is a slow algorithm
		// so only do occasionally or if visible
		if (m.isVisible() || (Rand.d(40) == 1)) {
			int dis = 4; //search distance
			Thing[] search = map.getThings(m.x - dis, m.y - dis, m.x + dis, m.y
					+ dis);
			for (int i = 0; i < search.length; i++) {
				Thing t = search[i];
				if (t.getFlag("IsFood")) {
					m.set(RPG.ST_TARGETX, t.x);
					m.set(RPG.ST_TARGETY, t.y);
				}
			}
			// m.incStat("APS",-50);
		}
		*/
		
		Thing f=findFoe(m);
		if ((f!=null)&&(f.getStat("HPS")<m.getStat("HPS"))) {
			doAttack(m,f.x,f.y);
		}

		int d = RPG.distSquared(h.x, h.y, m.x, m.y);

		int dx;
		int dy;

		dx = Rand.r(3) - 1;
		dy = Rand.r(3) - 1;

		if (Rand.d(10) > d)
			dx = -RPG.sign(h.x - m.x);
		if (Rand.d(10) > d )
			dy = -RPG.sign(h.y - m.y);

		int nx = m.x + dx;
		int ny = m.y + dy;

		return Movement.tryMove(m,map,nx,ny);
	}

	public static boolean doRangedAttack(Thing m, Thing f) {
		Thing rw=m.getWielded(RPG.WT_RANGEDWEAPON);
		Thing ms=m.getWielded(RPG.WT_MISSILE);
		
		//Game.warn("thinking! "+rw+" and "+ms);
		if ((rw!=null)&&(RangedWeapon.isValidAmmo(rw,ms))) {
			//Game.warn("shooting!");
			RangedWeapon.fireAt(rw,m,ms.remove(1),f.getMap(),f.x,f.y);
			Being.utiliseItems(m);
			return true;
		} else if (ms!=null) {
			// throw a missile
			if (Combat.DEBUG) Game.warn(m.name()+" throwing "+ms.name());
			Missile.throwAt(ms.remove(1),m,f.getMap(),f.x,f.y);
			Being.utiliseItems(m);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean doCasterAction(Thing m) {
		Thing[] ar = m.getFlaggedContents("IsSpell");
		int c = ar.length;
		//m.incStat("APS", - 10); // pause to think

		if (c <= 0)
			return false;

		if (m.isVisible(Game.hero()) || (Rand.d(10) == 1)) {
			int i = Rand.r(c);
			Thing a = ar[i];
			return Spell.castAI(m,a);
		}
		return false;
	}

	public static void setGuard(Thing t,Map m,int x1,int y1) {
		setGuard(t,m,x1-1,y1-1,x1+1,y1+1);
	}
	
	public static void setGuard(Thing t,Thing gp) {
		t.set("GuardPoint",gp);
		t.set("AIMode","Guard");
		gp.set("Guard",t);
	}

	public static void setGuard(Thing t,Map m,int x1,int y1,int x2,int y2) {
		Thing gp=t.getThing("GuardPoint");
		if (gp==null) {
			gp=Lib.create("guard point");
			t.set("GuardPoint",gp);
		}
		gp.set("Guard",t);
		gp.set("GuardRadius",Maths.min(Math.abs(x2-x1),Math.abs(y2-y1))/2);
		m.addThing(gp,(x1+x2)/2,(y1+y2)/2);
		t.set("AIMode","Guard");
	}

	public static void name(Thing t, String name) {
		t.set("Name",name);
		t.set("NameType",Description.NAMETYPE_PROPER);
		t.set("IsNamed",1);
		t.set("IsUnique",1);
		t.set("Frequency",0);
	}
	
	public static boolean doGuardAction(Thing m) {
		Map map = m.getMap();
		if (map == null) {
			return false;
		}
		
		Thing gp=m.getThing("GuardPoint");
		if (gp==null) {
			gp=map.getNamedObject(m.x,m.y,"guard point");
			
			if (gp==null) {
				Game.warn(m.name()+" has no guard point!");
				return false;
			} 
			m.set("GuardPoint",gp);
			
		}
		int gr=gp.getStat("GuardRadius");
		
		int x1=gp.x-gr;
		int y1=gp.y-gr;
		int x2=gp.x+gr;
		int y2=gp.y+gr;

		int tx = gp.x;
		int ty = gp.y;



		Thing f = findFoe(m);
		if ((f != null) && RPG.distSquared(m.x, m.y, f.x, f.y) <= 50) {
			doAttack(m, f.x, f.y);
		} else if ((m.x <= x1) || (m.x >= x2) || (m.y <= y1) || (m.y >= y2)) {
			// outside area, need to get back in!
			doAttack(m, tx, ty);
		} else {
			boolean changedirection = (Rand.d(6) == 1)
					|| ((m.getStat("DirectionX") == 0) && (m
							.getStat("DirectionY") == 0));
			if (changedirection)
				m.set("DirectionX", Rand.r(3) - 1);
			if (changedirection)
				m.set("DirectionY", Rand.r(3) - 1);
			doAttack(m, m.x + m.getStat("DirectionX"),
					m.y + m.getStat("DirectionY"));
		}
		return true;
	}
	
	public static boolean doFriendlyAction(Thing m) {
		int tx = m.getStat(RPG.ST_TARGETX);
		int ty = m.getStat(RPG.ST_TARGETY);

		Map map = m.getMap();
		
		if (map.isAngry()&&m.getFlag("IsIntelligent")) m.set("IsHostile",1);

		int dx = Rand.r(3) - 1;
		int dy = Rand.r(3) - 1;

		if ((tx > 0) && (ty > 0)) {
			if (Rand.d(2) == 1)
				dx = RPG.sign(tx - m.x);
			if (Rand.d(2) == 1)
				dy = RPG.sign(ty - m.y);
		}

		int nx = m.x + dx;
		int ny = m.y + dy;

		if (!map.isBlocked(nx, ny)) {
			m.moveTo(map, nx, ny);
		} else {
			// don't do anything. we are nice!!!
		}
		m.incStat("APS", - 10000 / m.getStat(RPG.ST_MOVESPEED));
		return true;
	}	
	
	public static void init() {
		// nothing here yet
		

	}
}