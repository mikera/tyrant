/*
 * Created on 27-Jun-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package mikera.tyrant;

import mikera.engine.Lib;
import mikera.engine.RPG;
import mikera.engine.Thing;
import mikera.util.Maths;
import mikera.util.Rand;

/**
 * @author Mike
 *
 * Contains routines for the management of damage to items
 * and monsters.
 * 
 */
public class Damage {
	// damage is halved for each 10 levels of resistance
	private static final double resistMultiplier=Math.pow(0.5,0.1);
	
	/**
	 * Calculates the actual amount of modified damage
	 * 
	 * Slight random factor due to rounding
	 * 
	 * Includes all modifiers for armour and resistance
	 * 
	 * @param t The thing to be damages
	 * @param dam The base (unmodified) amount of damage
	 * @param dt The type of damage
	 * @return
	 */
	private static int calculate(Thing t, double damage, String dt) {
	    
		if (damage <= 0)
			return 0;
		double arm = Armour.calcArmour(t, dt);

		damage = (damage * damage) / (arm + damage);
		
		// calculate resistance
		int res = t.getResistance(dt);
		if (res != 0) {
			// Game.warn("Damage.calculate(): Resistance = "+res);
			damage = damage*Math.pow(resistMultiplier,res);
		}

		if (dt.equals("poison")) {
			if (!t.getFlag("IsLiving")) damage=0.0;
		}
		
		if (dt.equals("chill")) {
			if (!t.getFlag("IsLiving")) damage=0.0;
		}
		
		int intDamage=RPG.round(damage);
		
		// item damage is different
		if (t.getFlag("IsItem")) {
			int max=t.getStat("HPSMAX");
			int dl=t.getStat("DamageLevels");
			if (dl>0) {
				if (intDamage<(max/(1+dl))) {
					intDamage=(Rand.r(max/(1+dl))<intDamage) ? max/(1+dl) : 0;
				}
				// no change
			} else {
				intDamage = Rand.r(max) < intDamage ? max : 0;			
			}
		}
		return intDamage;
	}
		
	/**
	 * Inflicts damage on a Thing
	 * 
	 * @param t The thing to be damages
	 * @param dam The base (unmodified) amount of damage
	 * @param damageType The type of damage
	 * @return
	 */	
	public static int inflict(Thing t, int damageAmount, String damageType) {
		// Game.warn("Damage.inflict: "+dam+" "+dt);
		if (!t.getFlag("IsDestructible")) return 0;
	
		// stop recursive destruction!
		if (t.isDead()) return 0;
		
		// react to a surprise attack
		if ((Game.actor != null) && t.getFlag("IsBeing") &&(!t.isHostile(Game.actor))) {
			t.notify(AI.EVENT_ATTACKED, t.getStat(RPG.ST_SIDE),
					Game.actor);
		}
		
		int dam=Damage.calculate(t,damageAmount,damageType);
		
		if (Combat.DEBUG) Game.warn("- Damage on "+t.name()+": "+damageAmount+" => "+dam+" "+damageType);
		

		
		// impact effects
		if (damageType.equals(RPG.DT_IMPACT)) {
			// confusion chance
			if ((Rand.d(10)==1)&&t.getFlag("IsLiving")&&RPG.test(dam,t.getStat("TG"))) {
				t.addAttribute(Lib.create("confusion"));
			}
			
			// knock-back for items and beings
			Thing a=Game.actor;
			if ((a!=null)&&(t.getFlag("IsItem")||(t.getFlag("IsBeing")&&RPG.test(dam,t.getStat("TG")+10,a,t)))) {
				boolean pushable=true;
				if (t.getFlag("IsShopOwned")) pushable=false;
				
				if (pushable) {
					if (Movement.push(t,RPG.sign(t.x-a.x),RPG.sign(t.y-a.y))) {
						t.message("You are knocked back by the force of the impact");
					}
				}
			}
		}
		
		// break if no damage
		if (dam<=0) return 0;
		
		if (t.isHero()) {
			t.isRunning(false);
		}
		
		
		if (t.handles("OnDamage")) {
			Event e=new Event("Damage");
			e.set("Damage",dam);
			e.set("DamageType",damageType);
			e.set("Actor",Game.actor);
			if (t.handle(e)) return e.getStat("Damage");
			dam=e.getStat("Damage");
		}
		
		// may have been changed by event handler
		// if no damage, want to escape
		if (dam<=0) return 0;
		
		if (damageType.equals("drain")||damageType.equals("shock")) {
			t.incStat("MPS", -dam);
			if (t.getStat("MPS") < 0) {
				t.incStat("HPS", t.getStat("MPS"));
				t.set("MPS", 0);
			}
		} else {
            //CBG See defect http://sourceforge.net/tracker/index.php?func=detail&aid=1093596&group_id=16696&atid=116696
            if ((t.getStat("Number")>1)&&(dam>=t.getStat("HPS"))) {
                /* CBG Check to see if this is a stack item, if so destroy the items in the stack */
                int number=t.getNumber();
                int destroyedCount=RPG.round(damageAmount / (double)dam);
                
            	if (destroyedCount>=number) {
                    // all of the stacked items will be destroyed
                    t.incStat("HPS", -dam);
                } else {
                	// just destroy some of the items
                	t=t.separate(destroyedCount);
                	if (t==null) return 0;
                	t.incStat("HPS", -dam);
                }
            } else {
                t.incStat("HPS", -dam);
            }
		}
		
		if (!t.getFlag("IsFrozen")&&damageType.equals("ice")) {
			if (RPG.test(dam,t.getStat(RPG.ST_TG))) {
				t.message("You are briefly frozen!");
				t.set("IsFrozen",1);
				t.incStat("APS",-200);
			}
		}

		
		if (t.getStat("HPS") <= 0) {
			int peity = t.getStat(RPG.ST_PEITY);
			if (peity > 70) {
				t.set(RPG.ST_PEITY, peity*2-140);
				// dam = t.getStat("HPS")-1; // cheat damage
				t.set("HPS", 1); // survive
				t.message("You feel your luck is running out...");
			} else {
				t.set("KillingDamageType",damageType);
				t.die();
			}
		}

		itemDamage(t,damageAmount,damageType);
		
		// mark for damage inflicted
		int damageMark=0;
		if (damageType.equals("water")) damageMark=0;
		if (damageType.equals("poison")) damageMark=40;
		if (damageType.equals("ice")||damageType.equals("chill")||damageType.equals("acid")) damageMark=20;
		if (damageType.equals("shock")) damageMark=61;
		if (damageType.equals("drain")||damageType.equals("disintegrate")) damageMark=80;
		if ((dam*5)>t.getStat("HPSMAX")) damageMark++;
		if (Game.visuals) {
			Game.instance().doDamageMark(t.x,t.y,damageMark);
		}
		
		return dam;		
	}
   /**
    * Applay the protection of all wields bags for a damage.
    *
    * <b>Always use a copy of the inventory array because
    * protected equipment as set to null in the array.</b>
    **/
   private static void bagProtection(Thing hero,  Thing[] equip, int dam, String damtype) {
      for(int i =0;i<RPG.WT_BAGS.length;i++){
         Thing bag =hero.getWielded(RPG.WT_BAGS[i]);
         if(bag!=null){
            Damage.inflict(bag, dam, damtype);
            if(!bag.isDead()){
            	// now I clear the array of all the items that was in the bags
               for(int j=0;j<equip.length;j++){
                  if (equip[j]!=null){
                     //set to null the protected items and the bag because damage have already been apply
                         if ( equip[j]==bag ||
                         	( ! hero.isWielded(equip[j]) && equip[j].getFlag((String)bag.get("Holds")))){
                        equip[j]=null;
                     }
                  }
               }
            }
         }
      }
   }
	// inflict damage on thing and all sub-items/inventory
	public static void damageInventory(Thing t, int dam, String damtype, int chance) {

		Thing[] inv=t.inv();
      if (t.isHero()){
         inv = new Thing[inv.length];
         System.arraycopy(t.inv(),0, inv,0,inv.length);
         bagProtection(t,inv, dam, damtype);
      }
		for (int i = 0; i < t.invCount(); i++) {
			
			Thing it=inv[i];
			if ((it!=null)&&(Rand.r(100)<chance)&&it.getFlag("IsItem")) {
                int itemsInInventory = t.invCount();
				int res=Damage.inflict(it,dam, damtype);
                if(itemsInInventory > t.invCount())
                    i--;
				if (t.isHero() && res > 0 && it.place != null) {
					Game.message(it.getYourName()+" "+it.is()+" damaged");
				}
			}
		}
	}	
	
	private static void itemDamage(Thing t, int dam, String dt) {
		if (!t.hasInventory()) return;
		
		int idc=0;
		int arm=0;
		if (dt.equals("fire")||dt.equals("ice")) {
			idc+=2;
		}
		
		if (dt.equals("water")) {
			idc+=3;
		}
		
		if (dt.equals("acid")) {
			idc+=5;
		}
		
		if (dt.equals(RPG.DT_DISINTEGRATE)) {
			idc+=20;
		}
		
		idc= Maths.middle(0,idc,100);
		
		if ((idc>0)&&(dam>0)) {
			arm = Armour.calcArmour(t,dt);
			if (arm>0) {
				int denom=arm+dam;
				dam = (dam * dam) / (denom);
			}
			// Game.warn("Inventory damage chance="+idc);
			if (dam>0) damageInventory(t,dam,dt,idc);
		}
	}
	
	public static String describeState(Thing t) {
		double d=t.getStat("HPS")/(double)t.getStat("HPSMAX");
		if (d==1) return "no";
		if (d>0.99) return "minimal";
		if (d>0.9) return "minor";
		if (d>0.7) return "light";	
		if (d>0.5) return "moderate";			
		if (d>0.25) return "heavy";
		if (d>0.0) return "critical";
		return "massive";
	} 

}
