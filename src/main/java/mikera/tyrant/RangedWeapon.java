package mikera.tyrant;

import mikera.engine.Lib;
import mikera.engine.Map;
import mikera.engine.Point;
import mikera.engine.RPG;
import mikera.engine.Thing;
import mikera.util.Rand;

// Standard armour types
// *still need to implement defence skill, encumberance*
public class RangedWeapon {

	public static final int[] qualitymuls = {0, 20, 40, 60, 80, 100, 120, 140,
			160, 180, 200, 220, 240, 260};
	public static final int[] qualitylevels = {0, -4, -3, -2, -1, 0, 2, 4, 6,
			9, 13, 18, 25, 40};

	public static Thing createRangedWeapon(int level) {
		return Lib.createType("IsRangedWeapon", level);
	}

	public static int fireCost(Thing rw, Thing user) {
		int fc=rw.getStat("FireCost");
		// benefits for archery skill
		return (fc*8)/(8+user.getStat(Skill.ARCHERY));
	}
	
	public static void useRangedWeapon(Thing w, Thing user) {
		if (user.isHero()) {
			// get appropraite missile
			Thing missile = user.getWielded(RPG.WT_MISSILE);
			if (!isValidAmmo(w, missile)) {
				// get valid ammo items
				Object mt = w.get("RangedWeaponType");
				if (mt == null)
					Game.warn(w.name() + " has no RangedWeaponType!");
				Thing[] ms = user.getContents(RPG.ST_MISSILETYPE, mt);

				if (ms.length > 1) {
					missile = Game.selectItem("Select ammunition for your "
							+ w.getName(Game.hero()) + ":", ms);
				} else {
					if (ms.length == 1)
						missile = ms[0];
				}
			}

			// fire away!
			if (missile != null) {
				user.wield(missile, RPG.WT_MISSILE);
				user.incStat("APS",-fireCost(w,user));
				fire(w, user, missile);
			} else {
				Game.message("You have no ammunition for your " + w.getName(Game.hero()));
			}
		}
	}

	// check ammo
	public static boolean isValidAmmo(Thing mw, Thing m) {
		return (m != null)
				&& (m.get("MissileType").equals(mw.get("RangedWeaponType")));
	}

	// fire the weapon
	public static void fire(Thing rangedWeapon, Thing shooter, Thing missile) {
		Thing h = Game.hero();
		Map m = shooter.getMap();
		if (m == null) {
			Game.warn("Trying to shoot " + rangedWeapon.getName(Game.hero()) + " with null shooter");
			return;
		}
		if (shooter == h) {
			if (isValidAmmo(rangedWeapon, missile)) {
				GameScreen gs = Game.getQuestapp().getScreen();

				Point p = gs.getTargetLocation(gs.map.findNearestFoe(h));
				//Do not confuse player with possible false info
				Game.message("");
				if (p != null) {
					missile = missile.remove(1);
					fireAt(rangedWeapon, shooter, missile, gs.map, p.x, p.y);
				}

			} else {
				Game.message("You are unable to use " + missile.getTheName()
						+ " as ammunition for your " + rangedWeapon.getName(Game.hero()));
			}
		} else {
			// perhaps some AI here later
		}
	}

	public static Thing createAmmo(Thing s) {
		return createAmmo(s,s.getStat("Level"));
	}
	
	public static Thing createAmmo(Thing s, int level) {
		String atype = s.getString("RangedWeaponType");
		for (int i=0; i<20; i++) {
			Thing t= Lib.createType("IsMissile",level);
			if (t.getString("MissileType").equals(atype)) return t;
		}
		Game.warn("Can't create ammo for "+s.name()+" (Level "+level+")");
		return null;
	}

	public static void fireAt(Thing rangedWeapon, Thing shooter, Thing missile, Map m,
			int tx, int ty) {
		if (missile != null) {
			shooter.incStat("APS", -(fireCost(rangedWeapon,shooter) * 10)
					/ (10 + shooter.getStat(Skill.ARCHERY)));

			// find where thing hits
			Point p = m.tracePath(shooter.x, shooter.y, tx, ty);
			tx=p.x;
			ty=p.y;
			
			// check for current location
			if ((tx == shooter.x) && (ty == shooter.y)) {
				shooter.dropThing(missile);
				return;
			}

			// calculate ranged skill
			int rsk = (shooter.getStat(RPG.ST_SK) * (3 + shooter
					.getStat(Skill.ARCHERY))) / 3;
			rsk = (rsk * rangedWeapon.getStat("RSKMul") * missile
					.getStat(RPG.ST_RSKMULTIPLIER)) / 10000;
			rsk = rsk + rangedWeapon.getStat("RSKBonus")
					+ missile.getStat(RPG.ST_RSKBONUS);

			// calculate distance and offset
			int dx = tx - shooter.x;
			int dy = ty - shooter.y;
			int dist = (int) Math.sqrt(dx * dx + dy * dy);

			// what are we firing at?
			Thing target = m.getMobile(tx, ty);

			Game.instance().doShot(shooter.x, shooter.y, tx, ty, 100, 150);

			int shotDifficulty = Missile.shotDifficulty(target, dist);

			// get the hit factor
			// 0 = miss
			// 1 = normal hit
			// 2 or above = critical hit
			int factor = RPG.hitFactor(rsk, shotDifficulty,shooter,target);

			// test for hit (including Luck)
			if (target == null) {
				shooter.message("You fire " + missile.getTheName()
						+ " at your target");
				missile.moveTo(m, tx, ty);
			} else if (factor>0) {
				//have scored a hit!
				int rst = (shooter.getStat(RPG.ST_ST) *
						(4 + shooter.getStat(Skill.ARCHERY))) / 4;

				rst = (rst * rangedWeapon.getStat("RSTMul") * missile
						.getStat(RPG.ST_RSTMULTIPLIER)) / 10000;

				rst = rst + rangedWeapon.getStat("RSTBonus")
						+ missile.getStat(RPG.ST_RSTBONUS);

				// shooting message
				if (target.isVisible(Game.hero())) {
					if (shooter.isHero()) {
						switch (factor) {
							case 1 :
								Game.message("You fire and hit "
										+ target.getTheName());
								break;
							case 2 :
								Game
										.message("You fire and score a great hit on "
												+ target.getTheName());
								break;
							default :
								Game
										.message("You fire and score a perfect hit on "
												+ target.getTheName());
								break;
						}
					} else {
						Game.message(shooter.getTheName() + " fires and hits "
								+ target.getTheName());
					}
				}

				// inflict hit effect
				Missile.hit(missile, shooter, target, rst);

				// drop item if missile survives
				missile.moveTo(m, p.x, p.y);
				if (Rand.r(100) >= missile.getStat("MissileRecovery")) {
					missile.die();
				}

				return;
			} else {
				Game.message(missile.getTheName() + " misses "+target.getTheName());
				missile.moveTo(m, tx, ty);
			}
		} else {
			shooter.message("You have no ammunition!");
		}
	}

	public static void setStats(Thing t, int rskm, int rskb, int rstm, int rstb) {
		t.set("RSKMul", rskm);
		t.set("RSKBonus", rskb);
		t.set("RSTMul", rstm);
		t.set("RSTBonus", rstb);
	}

	public static void init() {
		Thing t = Lib.extend("base ranged weapon", "base item");
		t.set("Image", 120);
		t.set("IsRangedWeapon", 1);
		t.set("LevelMin", 1);
		t.set("RangedWeaponType", "arrow");
		t.set("Range",10);
		t.set("WieldType", RPG.WT_RANGEDWEAPON);
		t.set("HPS", 30);
		t.set("FireCost",200);
		t.set("ItemWeight", 4000);
		t.set("ValueBase", 100);
		t.set("Frequency", 100);
		t.set("DamageLevels",1);
		t.set("ASCII",")");
		Lib.add(t);

		initBows();
		initSlings();
	}

	private static void initSlings() {
		Thing t = Lib.extend("sling", "base ranged weapon");
		t.set("Image", 120);
		t.set("LevelMin", 3);
		t.set("IsSling",1);
		t.set("RangedWeaponType", "bullet");
		t.set("HPS", 8);
		t.set("ItemWeight", 800);
		t.set("ValueBase", 30);
		t.set("Frequency", 70);
		setStats(t, 60, -2, 60, 1);
		Lib.add(t);

		t = Lib.extend("makeshift sling", "sling");
		t.set("UName", "sling");
		t.set("LevelMin", 1);
		t.set("HPS", 3);
		t.set("ItemWeight", 500);
		t.set("Frequency", 30);
		setStats(t, 40, -3, 40, 0);
		Lib.add(t);
	}

	private static void initBows() {
		Thing t = Lib.extend("base bow", "base ranged weapon");
		t.set("Image", 122);
		t.set("RangedWeaponType", "arrow");
		t.set("LevelMin", 6);
		t.set("IsBow", 1);
		t.set("HPS", 14);
		t.set("ItemWeight", 2000);
		t.set("Frequency", 70);
		t.set("ValueBase",300);
		setStats(t, 70, 0, 12, 7);
		Lib.add(t);

		t = Lib.extend("short bow", "base bow");
		t.set("ItemWeight", 1500);
		setStats(t, 60, 0, 10, 4);
		t.set("LevelMin",1);
		Lib.add(t);
		
		t = Lib.extend("bow", "base bow");
		t.set("LevelMin",3);
		Lib.add(t);
		
		t = Lib.extend("bow of swift shooting", "base bow");
		t.set("UName","bow");
		t.multiplyStat("FireCost",0.5);
		t.set("LevelMin",10);
		Lib.add(t);

		t = Lib.extend("longbow", "base bow");
		t.set("ItemWeight", 3000);
		setStats(t, 80, 0, 15, 12);
		t.set("LevelMin",9);
		Lib.add(t);
		
		t = Lib.extend("elven longbow", "base bow");
		t.set("UName","longbow");
		t.set("ItemWeight", 2500);
		setStats(t, 100, 0, 15, 15);
		t.multiplyStat("FireCost",0.7);
		t.set("LevelMin",15);
		Lib.add(t);
		
		t = Lib.extend("black longbow", "longbow");
		t.set("ItemWeight", 3500);
		setStats(t, 80, 0, 16, 15);
		t.set("LevelMin",13);
		t.set("IsCursed",1);
		Lib.add(t);
		
		t = Lib.extend("longbow of power", "longbow");
		t.set("UName","longbow");
		t.set("ItemWeight", 3200);
		setStats(t, 70, 0, 15, 20);
		t.set("LevelMin",11);
		t.set("Frequency",30);
		Lib.add(t);
		
		t = Lib.extend("longbow of accuracy", "longbow");
		t.set("UName","longbow");
		t.set("ItemWeight", 2800);
		setStats(t, 160, 0, 15, 12);
		t.set("Frequency",30);
		t.set("LevelMin",13);
		Lib.add(t);
		
		t = Lib.extend("great bow", "base bow");
		t.set("ItemWeight", 5000);
		setStats(t, 70, 0, 20, 15);
		t.multiplyStat("FireCost",1.2);
		t.set("LevelMin",8);
		Lib.add(t);

	}
}