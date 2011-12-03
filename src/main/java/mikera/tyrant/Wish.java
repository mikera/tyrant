/*
 * Created on 19-Jul-2004
 *
 * By Mike Anderson
 */
package mikera.tyrant;

import java.util.Iterator;

import mikera.engine.BaseObject;
import mikera.engine.Lib;
import mikera.engine.Map;
import mikera.engine.RPG;
import mikera.engine.Thing;
import mikera.util.Rand;

/**
 * @author Mike
 *
 * This class implements the wish functionality
 */

public class Wish {

	private static String lastWish = "beefcake";

	public static void doWish() {
		
		String s = Game.getLine("What do you wish for? ");
		s=s.trim();
		if (s.equals("ESC")) {
			Game.message("");
			return;
		}
		if (s.equals(""))
			s = lastWish;
		makeWish(s,100);
		lastWish = s;
	}
	
	/**
	 * Make a wish with a given "level"
	 *
	 * @param s
	 * @param level
	 * @return True if wish granted
	 */
	public static boolean makeWish(String s,int level) {
		Thing h=Game.hero();
		Map map=h.getMap();

		if (Game.isDebug()&&makeDebugWish(s)) return true;	
		
		if (makeSpecialWish(s,level)) return true;
		
		// wishing for skill?
		if (Skill.isSkillIgnoreCase(s)) {
			s=Skill.ensureCase(s);
			int cost=1;
		
			// skillpoint cost
			// double for new skill
			if (h.getFlag(s)||(level>=100)) {
				cost=1;
			} else {
				cost=2;
			}
			
			if ((level<100)&&(h.getStat(RPG.ST_SKILLPOINTS)<cost)) {
				Game.message("You must gain more experience before you can learn the art of "+s);
				return false;
			}
			
			h.incStat(RPG.ST_SKILLPOINTS,-cost);
			Game.message("You feel more knowledgeable about "+s);
			h.incStat(s,1);
			
			return true;
		}
		
		if ((level<100)&&s.equals("wishing well")) {
			Game.message("You hear a booming voice:");
			Game.message("\"Nice try... but no cigar\"");
			s="carrot";
			
		}
		
		// try to create the appropriate thing
		Thing c = Lib.create(s);
		if ((c==null)||c.name().equals("strange rock")) {
			c=Lib.createIgnoreCase(s);
		}
		
		if (c==null) {
			Game.message("You feel that you have asked for the impossible");
			return false;
		}
		

		

		
		if (level<c.getStat("Level")) {
			Game.message("You feel that you have asked for too much");
			return false;
		}
		
		if (level<100) {
			if (c.getFlag("IsArtifact")) {
				Game.message("You hear a booming voice:");
				Game.message("\"Ha Ha. Very funny!\"");
				return false;
			}
			
			if (c.getStat("Frequency")<=0) {
				Game.message("You hear a booming voice:");
				Game.message("\"You'll have to work harder for that!\"");
				return false;
			}
			
			if (c.getStat("Number")>1) {
				c.set("Number",1);
				Game.message("You hear a booming voice:");
				Game.message("\"Don't be greedy!\"");
			}
		}
		
		if (!c.getFlag("IsWishable")&&(level<100)) {
			Game.message("You hear a strange voice in your head:");
			Game.message("\"Doesn't sound like a good idea to me\"");
			return false;
		}
		
		if (c.getFlag("IsAlteration")) {
			Game.warn("alteration: "+c.name());
			Thing[] ws=h.getFlaggedContents(c.getString("AlterationType"));
			for (int i=0; i<ws.length; i++) {
				ws[i].addThing(c.cloneType());
			}
			return true;
		} else if (c.getFlag("IsEffect")) {
			h.addAttribute(c);
			return true;
		} else if (c.getFlag("IsSpell")) {
			if (!Spell.canLearn(h,c.name())) {
				Game.message("You cannot comprehend what you have wished for!");
				return false;
			}
            Spell.learn(h,c.getString("Name"));
            return true;
		}  else {
			if (map.addBlockingThing(c, h.x-1,h.y-1,h.x+1,h.y+1)) {
				Game.message(c.getAName()+" appears");				
			} else {
				Game.message(c.getAName()+" appears momentarily but fades away");				
			}
			return true;
		}
		
	}

	/**
	 * Handle wishes only available in debug mode
	 * @param s
	 * 
	 * @return
	 */
	private static boolean makeDebugWish(String s) {

		Thing h=Game.hero();
		
		if (s.startsWith("lots of ")) {
			s=s.substring(8);
			Game.message("Yahoo!");
			Map m=h.getMap();
			for (int x=h.x-7; x<=h.x+7; x++) for (int y=h.y-7; y<=h.y+7; y++) {
				if (!m.isBlocked(x,y)) m.addThing(Lib.create(s),x,y);
			}
			return true;
		}
		
		s=s.toLowerCase();
		
		if (s.equals("eth")) {
			h.set("IsEthereal",1);
			return true;
		}	
		
		if (s.equals("tv")) {
			Game.message("You can see through everything!");
			h.set("TrueView",120);
			return true;
		}	
		
		if (s.equals("digging")) {
			Game.message("Do you dig it?");
			h.set("Digging",1);
			return true;
		}	
		
		if (s.equals("skills")) {
			Game.message("You feel seriously multi-talented.");
			String[] ss=Skill.fullList();
			for (int i=0; i<ss.length; i++) {
				h.incStat(ss[i],1);
			}
			return true;
		}	
		
		if (s.equals("error")) {
			throw new Error("Error wished for!");
		}	
		
		
		if (s.equals("victory")) {
			Game.over=true;
			return true;
		}	
		
		if (s.equals("luck")) {
			h.incStat("Luck",50);
			return true;
		}	
		
		if (s.equals("stuff")) {
			Game.message("Whoopee!");
			Map m=h.getMap();
			for (int x=h.x-7; x<=h.x+7; x++) for (int y=h.y-7; y<=h.y+7; y++) {
				if (!m.isBlocked(x,y)) m.addThing(Lib.createItem(h.getLevel()),x,y);
			}
			return true;
		}
		
		if (s.equals("weapons")) {
			Game.message("Let's go to work!");
			Map m=h.getMap();
			for (int x=h.x-7; x<=h.x+7; x++) for (int y=h.y-7; y<=h.y+7; y++) {
				if (!m.isBlocked(x,y)) m.addThing(Lib.createType("IsWeapon",h.getLevel()),x,y);
			}
			return true;
		}

		
		if (s.equals("artifacts")) {
			Game.message("Whoopee!!!");
			Map m=h.getMap();
			for (int x=h.x-5; x<=h.x+5; x++) for (int y=h.y-5; y<=h.y+5; y++) {
				if (!m.isBlocked(x,y)) m.addThing(Lib.createArtifact(Rand.d(50)),x,y);
			}
			return true;
		}
		
		if (s.matches("x\\d+")) {
			int ls=Integer.parseInt(s.substring(1));
			Game.message("You feel masses of ancient knowledge fill your mind");
			while ((h.getLevel())<50&&(h.getLevel()<ls)) {
				Hero.gainExperience(Hero.calcXPRequirement(h.getLevel()+1));
				
			}
			return true;
		}
		
		if (s.equals("x")||s.equals("xp")||s.equals("exp")||s.equals("experience")||s.equals("training")) {
			Game.message("You feel ancient knowledge fill your mind");
			Hero.gainExperience(Hero.calcXPRequirement(Game.hero().getLevel()+1));
			return true;
		}	
        
		if (s.equals("all")) {
			Game.message("Everything in the library added to your inventory");
			Thing hero = Game.hero();
            for (Iterator iter = Lib.instance().getAll().iterator(); iter.hasNext();) {
                BaseObject thingAsProperties = (BaseObject) iter.next();
                String name = (String) thingAsProperties.get("Name");
                if (name.startsWith("base ")) continue;
                if(thingAsProperties.get("IsItem") == null) continue;
                Thing thing = Lib.create(name);
                hero.addThing(thing);
            }
			return true;
		}			
		
        if (s.equals("identify")) {
            Game.message("You know a lot.");
            Thing hero = Game.hero();
            for (int i = 0; i < hero.invCount(); i++) {
                Thing thing = hero.getInventory()[i];
                Item.identify(thing);
            }
            return true;
        }       
		return false;
	}
	
	private static boolean makeSpecialWish(String s,int level) {
		s=s.toLowerCase();
		
		Thing h=Game.hero();

		String stat=null;
		if (s.equalsIgnoreCase("sk")||s.equals("skill")) stat="SK";
		if (s.equalsIgnoreCase("st")||s.equals("strength")) stat="ST";
		if (s.equalsIgnoreCase("ag")||s.equals("agility")) stat="AG";
		if (s.equalsIgnoreCase("tg")||s.equals("toughness")) stat="TG";
		if (s.equalsIgnoreCase("in")||s.equals("intelligence")) stat="IN";
		if (s.equalsIgnoreCase("wp")||s.equals("willpower")) stat="WP";
		if (s.equalsIgnoreCase("ch")||s.equals("charisma")) stat="CH";
		if (s.equalsIgnoreCase("cr")||s.equals("craft")) stat="CR";
		if (stat!=null) {
			Game.message("You feel your talents increasing");
			h.incStat(stat,Rand.d(3));
			return true;
		}
		
		if (s.equals("immortality")) {
			Game.message("You feel that you will be remembered for all time");
			// TODO: add player to Tyrant hall of fame!
			return true;
		}	
		
		if (s.equals("mortality")) {
			Game.message("You feel rather vulnerable");
			h.set("IsImmortal",-100);
			return true;
		}	
		
		if (s.equals("friend")||s.equals("friends")) {
			Game.message("And you shall have a friend!");
			Thing f=Lib.create("[IsBeing]",h.getLevel());
			
			h.getMap().addThing(f,h.x-1,h.y-1,h.x+1,h.y+1);
			AI.setFollower(f,h);
			return true;
		}	
		
		if (s.equals("death")) {
			Game.message("You feel your spirit being released from your body");
			h.set("IsImmortal",-100);
			h.die();
			return true;
		}	
		
		if (s.equals("hunger")||s.equals("hungry")) {
			Game.message("You suddenly feel hungry");
			h.set("Hunger",h.getStat(RPG.ST_HUNGERTHRESHOLD)+1);
			return true;
		}	
		

		if (s.equals("map")) {
			Game.message("You know this place like the back of your hand");
			LevelMap.reveal(h.getMap());
			return true;
		}
		
		if (s.equals("charging")) {
			Game.message("You feel a great surge of power");
			Spell.rechargeSpells(h,1000000);
			return true;
		}
		
		if (s.equals("foes")) {
			Game.message("Let battle commence");
			Map m=h.getMap();
			for (int x=h.x-7; x<=h.x+7; x++) for (int y=h.y-7; y<=h.y+7; y++) {
				if (!m.isBlocked(x,y)) m.addThing(Lib.create("[IsMonster]"),x,y);
			}
			return true;
		}
		

		if (s.equals("action")) {
			Game.message("You feel very excited");
			Map m=h.getMap();
			m.incStat("Level",6);
			m.incStat("WanderingRate",m.width*m.height);
			return true;
		}


		if (s.equals("identification")||s.equals("id")||s.equals("ident")) {
			Game.message("You feel suddenly knowledgable about your posessions");
			Thing[] is=h.getFlaggedContents("IsItem");
			for (int i=0; i<is.length; i++) {
				Item.fullIdentify(is[i]);
			}
			return true;
		}	
		
		if (s.equals("speed")) {
			h.incStat("MoveSpeed",Rand.d(20));
			Game.message("You feel like running very fast");
			return true;
		}
		
		if (s.equals("aggression")||s.equals("ferocity")) {
			h.incStat("AttackSpeed",Rand.d(15));
			Game.message("You feel more aggressive");
			return true;
		}
		
		if (s.equals("recharging")) {
			Game.message("You feel a surge of power around your posessions");
			Thing[] is=h.getFlaggedContents("IsWand");
			for (int i=0; i<is.length; i++) {
				is[i].incStat("Charges",5);
			}
			return true;
		}	
		
		if (s.equals("cash")||s.equals("money")||s.equals("wealth")) {
			Game.message("Your purse feels heavier");
			Coin.addMoney(h,10*level*Rand.d(2,300));
			return true;
		}			
		

		if (s.equals("heal")||s.equals("hps")) {
			Game.message("Your body is surrounded by healing energies");
			h.set("HPS",h.getStat("HPSMAX"));
			return true;
		}
		
		if (s.equals("health")||s.equals("cure")||s.equals("curing")||s.equals("cure poison")) {
			Game.message("Your body is surrounded by curing energies");
			Poison.cure(h,1000000);
			return true;
		}		

		return false;
	}
}
