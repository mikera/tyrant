/**
 * 
 */

package mikera.engine;

import java.util.*;

import mikera.tyrant.AI;
import mikera.tyrant.Being;
import mikera.tyrant.Combat;
import mikera.tyrant.Event;
import mikera.tyrant.EventHandler;
import mikera.tyrant.Game;
import mikera.tyrant.Item;
import mikera.tyrant.Movement;
import mikera.tyrant.util.Text;
import mikera.util.Rand;

/**
 * The Thing class represents all objects that can appear
 * on a Tyrant map. Thing supports an arbitrary, mutable 
 * list of properties inherited from BaseObject.
 * 
 * It also implements an inventory, which contains other Things
 * 
 * It also has the ability to be affected by property modifiers
 * 
 * Note that Thing is final for performance reasons (good JVMs 
 * should be able to optimise all the small accessor methods)
 * 
 * @author Mike
 */
public final class Thing extends BaseObject implements
			Description,
			ThingOwner {


	private static final long serialVersionUID = 2056412474365358346L;

    /**
	 * Next Thing in linked list for map squares
	 */ 
	public Thing next;
	
	/**
	 * Place where this thing can be found
	 * Generally either:
	 * - Another Thing (item in inventory)
	 * - A Map 
	 */
	public ThingOwner place;

	/**
	 * x-position of thing
	 * 
	 * Precise definition dependant on the owner
	 * e.g. if place is an instanc of Map
	 * Then x is the column number of the Thing's location
	 */
	public int x;

	/**
	 * y-position of thing
	 * 
	 * Precise definition dependant on the owner
	 * e.g. if place is an instanc of Map
	 * Then y is the column number of the Thing's location
	 */
	public int y;

	/**
	 * @author Mike
	 * Stores an array of things contained or owned by the current thing
	 * Used for:
	 * - Items in inventory
	 * - Game effects
	 */ 
	private Thing[] inv;
	
	/** 
	 * Count of active items in the Thing's inv array
	 */
	private int invcount;
    
	/**
	 * Optional list of modifiers that are affecting this thing
	 */
	private HashMap modifiers=null;


	//Z ordering constanrs
	public static final int Z_ELSEWHERE = -10;
	public static final int Z_FLOOR = 0;
	public static final int Z_ONFLOOR = 5;
	public static final int Z_ITEM = 20;
	public static final int Z_MOBILE = 40;
	public static final int Z_OVERHEAD = 60;
	public static final int Z_SYSTEM = 80;

	public Thing() {
		super();
	}
	
	public Thing(BaseObject baseObject) {
		super(baseObject);
	}
	
	public Thing(HashMap propertiesToCopy, BaseObject parent) {
		super(propertiesToCopy,parent);
	}
	
	public Thing(Thing t) {
		super(t);
	}

	public String toString() {
		return getString("Name");
	}
	
	public int invCount() {
		return invcount;
	}
	
	public ThingOwner owner() {
		return place;
	}
	
	/**
	 * Calculate total weight of inventory items
	 */ 
	public int getInventoryWeight() {
		int result = 0;
		for (int i = 0; i < invcount; i++) {
			result = result + inv[i].getWeight();
		}
		return result;
	}

	/**
	 * Get the complete contents list for a thing
	 * i.e. all items contained by the current Thing
	 */ 
	public Thing[] getItems() {
		sortItems();
		if (invcount == 0)
			return new Thing[0];
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (t.getFlag("IsItem")) {
				temp[tempcount] = t;
				tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0)
			System.arraycopy(temp, 0, result, 0, tempcount);
		return result;
	}
	
    public int countItems(String name) {
		int c=0;
		for (int i=0; i<invcount; i++) {
			if (name.equals(inv[i].name())) {
				c+=inv[i].getStat("Number");
			}
		}
		return c;
	}
	
	public int countIdentifiedItems(String name) {
		int c=0;
		for (int i=0; i<invcount; i++) {
			Thing t=inv[i];
			if (name.equals(inv[i].name())&&Item.isIdentified(t)) {
				c+=inv[i].getStat("Number");
			}
		}
		return c;
	}
	
	/**
	 * Removes a number of items from the Thing, up to a specified
	 * number.
	 * 
	 * @param name The name of the type of items to remove
	 * @param num The maximum number to remove
	 * @return The actual number removed
	 */
	public int removeItems(String name, int num) {
		int c=0;
		for (int i=0; i<invcount; i++) {
			Thing t=inv[i];
			if (name.equals(t.name())) {
				int snum=t.getStat("Number");
				if (snum>=num) {
					c+=num;
					t.remove(num);
					break;
				}
                t.remove();
                num-=snum;
                c+=snum;
                i--; // step back one since stack is removed
			}
		}
		return c;
	}

	/**
	 * Gets an inventory item with a particular name
	 * 
	 * Returns null if not found
	 * 
	 * @author Mike
	 */ 
	public Thing getContents(String name) {
		for (int i = 0; i < invcount; i++) {
			if ((inv[i].name().equals(name)))
				return inv[i];
		}
		return null;
	}

	public Thing getWeapon(int wt) {
		for (int i = 0; i < invcount; i++) {
			if (inv[i].y == wt)
				return inv[i];
		}
		return null;
	}

	public Thing[] getUsableContents() {
		if (invcount == 0)
			return null;
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (t.getFlag("IsUsable")||t.handles("OnUse")) {
					temp[tempcount] = t;
					tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0)
			System.arraycopy(temp, 0, result, 0, tempcount);
		return result;
	}

	// get all contents with a given stat value
	public Thing[] getFlaggedContents(String stat) {
		if (invcount == 0)
			return new Thing[0];
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (t.getStat(stat) > 0) {
				temp[tempcount] = t;
				tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0)
			System.arraycopy(temp, 0, result, 0, tempcount);
		return result;
	}

    /**
	 * Get a list of the contents that match a specific type from a thing
	 * e.g. all money items owned by the current Thing
	 */ 
    public Thing[] getFlaggedItems(String flag) {
        // This is an alternate implementation of getFlaggedContents
        // It is here for performance testing purposes and will probably go away soon.
        // Do not use this method in your code -Rick
        if (invcount == 0) {
            return null;
        }
        int count = 0;
        Thing t = null;
        for (int i = 0; i < invcount; i++) {
            t = inv[i];
            if (t.getFlag(flag)) {
                count++;
            }
        }
        if (count > 0) {
            int insertionIndex = 0;
            Thing[] result = new Thing[count];
            for (int i = 0; i < invcount; i++) {
                t = inv[i];
                if (t.getFlag(flag)) {
                    result[insertionIndex] = t;
                    insertionIndex++;
                }
            }
            return result;
        }
        return null;
    }

	// get all contents with a given stat value
	public Thing[] getContents(String stat, Object val) {
		if (invcount == 0)
			return null;
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (val.equals(t.get(stat))) {
				temp[tempcount] = t;
				tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0)
			System.arraycopy(temp, 0, result, 0, tempcount);
		return result;
	}

	// get all contents that can be worn/wielded with 'w'
	public Thing[] getWieldableContents() {
		if (invcount == 0)
			return null;
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (t.getFlag("WieldType")) {
				temp[tempcount] =  t;
				tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0) {
			System.arraycopy(temp, 0, result, 0, tempcount);
		}
		return result;
	}

	// time effect - pass to active sub-items
	public void inventoryAction(Event ae) {
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			t.action(ae);
		}
	}

	public Thing getThing(int i) {
		return ((i >= 0) & (i < invcount)) ? inv[i] : null;
	}

	public int getUsage(int i) {
		return ((i >= 0) & (i < invcount)) ? inv[i].y : RPG.WT_ERROR;
	}

	public void ensureSize(int s) {
		if ((inv!=null)&&(s <= inv.length))	return;
		int invsize = s + 5;
		Thing[] newinv = new Thing[invsize];
		if (inv != null) {
			System.arraycopy(inv, 0, newinv, 0, invcount);
		}
		inv = newinv;
	}

	// all removes go through here eventually....
	// part of ThingOwner interface
	public void removeThing(Thing thing) {
		int pos = thing.x;
		if (inv == null)
			throw new Error("Empty Inventory bug!");
		if (thing.place != this)
			throw new Error("Thing in wrong place!");
		if (inv[pos] != thing)
			throw new Error("Thing in wrong position!");
		if (pos < (invcount - 1))
			System.arraycopy(inv, pos + 1, inv, pos, invcount - pos - 1);
		
		// not using anymore, remove modifiers
		setUsage(thing,RPG.WT_NONE); 
		thing.unApplyModifiers("CarriedModifiers",this);
		
		inv[invcount - 1] = null;
		thing.place = null;
		thing.next = null;
		invcount = invcount - 1;
		for (int i = 0; i < invcount; i++)
			inv[i].x = i; // new positions
	}

	public void swapItems(int a, int b) {
		if ((a >= invcount) || (b >= invcount) || (a < 0) || (b < 0))
			return;
		Thing ti = inv[a];
		inv[a] = inv[b];
		inv[b] = ti;
		inv[a].x = a;
		inv[b].x = b;
	}
	
	public void sortItems() {
		if (inv==null) return;
		Arrays.sort(inv,new Comparator() {
			public int compare(Object a, Object b) {
				Thing ta=(Thing)a;
				Thing tb=(Thing)b;
				
				if (tb==null) return -1;
				if (ta==null) return 1;
				
				return tb.y-ta.y;
			}
		});
		for (int i=0; i<inv.length; i++) {
			if (inv[i]!=null) inv[i].x=i;
		}
	}

	public Thing addThingWithStacking(Thing thing) {
		// see if we can stack with existing items
		if (thing.getFlag("IsItem")) {
			for (int i = 0; i < invcount; i++) {
				Thing t=inv[i];
				if ((t.y>0)&&(t.y!=RPG.WT_MISSILE)) continue;
				if (thing.stackWith(t)) return t;
			}
		}	
		
		if (thing.place!=this) return addThing(thing);
		return thing;
	}
	
	public Thing separate(int n) {
		int number=getStat("Number");
		if (n>=number) return this;
		if (n<=0) return null;
		
		incStat("Number",-n);
		
		Thing t = (Thing) this.clone();
		t.set("Number", n);
		if (place instanceof Thing) {
			t=((Thing)place).addThing(t);
		} else if (place instanceof Map) {
			t=((Map)place).addThing(t,x,y);
		}
		return t;
	}
	
	public Thing restack() {
		if (place instanceof Thing) {
			Thing p=(Thing)place;
			return p.addThingWithStacking(this);			
		}
		return this;
	}
	
	public Thing addThing(Thing thing) {
		if (thing == null) return null;
		thing.remove();

		// see if we can stack with existing items
		if (thing.getFlag("NoStack")) {
			String cancel=thing.getString("CancelEffect");

			for (int i = 0; i < invcount; i++) {
				String n=inv[i].name();
				if (thing.name().equals(n)) return null;
				
				// if cancel effect in place, remove thing
				// note: need i-- as inv array is changed
				if (n.equals(cancel)) inv[i--].remove();
			}
		}		
		
		ensureSize(invcount + 1);
		inv[invcount] = thing;
		thing.place = this;
		thing.next = null; // don't care about list
		thing.x = invcount; // might be useful to know slot!
		setUsage(thing, RPG.WT_NONE); // item not in use
		invcount = invcount + 1;
		thing.applyModifiers("CarriedModifiers",this);
		
		return thing;
	}

	public boolean clearUsage(int wt) {
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			int it = t.y;
			if ((it == wt) && (t.getFlag("IsItem"))) {
				
				if (!t.getFlag("IsCursed")) {
					// update usage, change modifiers if needed
					message("You remove your "+t.getName(Game.hero()));
					setUsage(t, RPG.WT_NONE);
					t.restack();
				} else {
					message("You are unable to remove your " + t.getName(Game.hero())
							+ "!");
					return false;
				}
			}
		}
		return true;
	}	
	
	//generic thing properties
	public boolean isMobile() {
		return getFlag("IsMobile");
	}
    
    public boolean isRunning() {
        return getFlag("IsRunMode");
    }
    
	public boolean isBlocking() {
		return getFlag("IsBlocking");
	}

	public Thing holder() {
		return (place instanceof Thing)?(Thing)place:null;
	}

	
	public boolean handle(Event e) {
		EventHandler eh=getHandler(e.handlerName());
		if (eh!=null) {
			if (eh.handle(this,e)) return true;
		}
		return false;
	}
	
	public boolean handles(String handlerName) {
		Game.assertTrue(handlerName.startsWith("On"));
        return containsKey(handlerName);
	}
	
	public void action(Event ae) {	
		inventoryAction(ae);

		// set thing as actor
		Game.actor=this;

		if (handle(ae)) return;
	}

	public void multiplyStat(String s, double v) {
		set(s, RPG.round(getBaseStat(s) * v));
	}

	// object state functions
	public boolean isIdentified() {
		return Item.isIdentified(this);
	}

	public int getWeight() {
		return getStat("ItemWeight") * getStat("Number");
	}

	public int getNumber() {
		return getBaseStat("Number");
	}

	public int getImage() {
		return getStat("Image");
	}
	
	public int getZ() {
		return getStat("Z");
	}

	public void setPersonality(Script t) {
		set("OnChat",t);
	}

	// add attribute and activate
	public void addAttribute(Thing a) {
		addThing(a);
		
		// message and set usage if add successful
		// (may not work due to stacking constraints)
		if (a.place==this) {
			if (isHero()) {
				String mes=a.getString("AttributeAddMessage");
				if (mes!=null) message(mes);
			}
			setUsage(a,RPG.WT_EFFECT);
		}
	}

	public ThingOwner getPlace() {
		return place;
	}
	
	public Thing getParent() {
		if (place instanceof Thing) {
			return (Thing) place;
		}
        return null;
	}

	// Description interface
	public String getName(int number, int article) {
		return Describer.describe(Game.hero(), this, article, number);
	}

	public String getPronoun(int number, int acase) {
		return Describer.getPronoun(number, acase, getNameType(), getGender());
	}

	// return nametype and gender for a generic thing
	// override as necessary.
	public int getNameType() {
		return getStat("NameType");
	}
	public int getGender() {
		return getStat("Gender");
	}

	public String getDescriptionText() {
		String s = (String) get("Description");
		return (s != null) ? s : Text.capitalise(getName(Game.hero()) + ".");
	}

	public Description getDescription() {
		return this;
	}
	
	/**
	 * Returns true if this thing is the current hero
	 * 
	 * Needs to be fast - called very often!
	 * 
	 * @return
	 */
    public final boolean isHero() {
        return this == Game.hero();
    }
    
	public String is() {
		return isHero() || getNumber()>1 ? "are" : "is";
	}
	
	public String verb(String v) {
		return isHero() || getNumber()>1 ? v : v+"s";
	}
	
    public String getName(Thing person, int quantity) {
        return Describer.describe(person, this, Description.ARTICLE_NONE, quantity);
    }

	public String name() {
		return getString("Name");
	}
	
	public void message(String m) {
		if (isHero()) Game.message(m);
	}
	
	public String getName() {
		return getName(Game.hero());
	}
	public String getAName() {
		return getAName(Game.hero());
	}
	public String getTheName() {
		return getTheName(Game.hero());
	}
	public String getYourName() {
		return getYourName(Game.hero());
	}
	public String getName(Thing person) {
		return Describer.describe(person, this, Description.ARTICLE_NONE);
	}
	public String getAName(Thing person) {
		return Describer.describe(person, this, Description.ARTICLE_INDEFINITE);
	}
	public String getTheName(Thing person) {
		return Describer.describe(person, this, Description.ARTICLE_DEFINITE);
	}
	public String getYourName(Thing person) {
		return Describer.describe(person, this, Description.ARTICLE_POSSESIVE);
	}
	public String getFullName(Thing person) {
		return Describer.describe(person, this);
	}
	
	// return describing adjectives if applicable
	public String getAdjectives() {
		String adjective= getString("Adjective");
		String adj=adjective;
		if (adj==null) {
			adj="";
		} else {
			adj=adj+" ";
		}
		
		if (getFlag("IsItem")) {
			if (getFlag("IsStatusKnown")) {
				if (getFlag("IsCursed")) {
					adj+="cursed ";
				} else if (getFlag("IsBlessed")) {
					adj+="blessed ";
				}
			}
			if ((adjective==null)&&getFlag("IsRunic")) adj+="runic ";
		}
		return adj.equals("")?null:adj;
	}

	public String getSingularName() {
		if (!Item.isIdentified(this)) {
			String uname=getString("UName");
			if (uname!=null) return uname;
		} 
		return getString("Name");
	}
	
	public String getPluralName() {
		if (!Item.isIdentified(this)) {
			String uname=(String) get("UNamePlural");
			if (uname!=null) return uname;
			uname=getString("UName");
			if (uname!=null) return uname+"s";
		} 
		String pn =getString("NamePlural");
		return (pn != null) ? pn : (getString("Name") + "s");
	}

	public int getQuality() {
		return getStat("Quality");
	}

	public final Map getMap() {
		return (place == null) ? null : place.getMap();
	}

	public Thing[] inv() {
		return getInventory();
	}
	
	public boolean hasInventory() {
		return inv!=null;
	}
	
	public final int getMapX() {
		if (place instanceof Map)
			return x;
		return (place == null) ? -1 : ((Thing) place).getMapX();
	}

	// can thing "see" specified map location?
	public boolean canSee(Map m, int tx, int ty) {
		int vr=Being.calcViewRange(this);
		if (vr<=0) return false;
		if (vr*vr< ((x-tx)*(x-tx)+(y-ty)*(y-ty))) return false;
		if ((m == null) || (m != place))
			return false;
		return m.isLOS(x, y, tx, ty);
	}

	// can thing see other thing?
	public boolean canSee(Thing t) {
		if (t.place != place)
			return false;
		
		// fast path for seeing if hero is visible
		if (t.isHero()) {
			return ((Map)place).isHeroLOS(x,y);
		}
		
		// fast path if we are the hero
		if (isHero()) {
			return ((Map)place).isVisible(t.x,t.y);
		}

		return canSee(getMap(), t.x, t.y);
	}
	
	/**
	 * Calculate LOS on current map for this thing
	 */ 
    public void calculateVision() {
        if(Game.instance().lineOfSightDisabled()) return;
    	int viewrange = Being.calcViewRange(this);
    	Map map=getMap();
        if (map!=null) {
        	map.calcVisible(this, viewrange);
        }
    }

	public final int getMapY() {
		if (place instanceof Map)
			return y;
		return (place == null) ? -1 : ((Thing) place).getMapY();
	}

	public void remove() {
		if (place != null)
			place.removeThing(this);
	}

	public Thing remove(int n) {
		if (n < 1)
			throw new Error("Thing.remove(): can't remove "+n+" objects");
		int number = getNumber();
		if (number <= 0)
			throw new Error("Thing.remove(): stack contains "+number+" objects");
		if (n == number) {
			remove();
			return this;
		} else if (n<number) {
			Thing t = (Thing) this.clone();
			
			set("Number", number - n);
			t.set("Number", n);
			return t;
		} else {
			throw new Error("Thing: tring to remove more than entire stack!");
		}
	}
	
	public void replaceWith(String s) {
		this.replaceWith(Lib.create(s));
	}
	
	public void die() {
		Combat.die(this);
	}

	// move to square, landing on floor
	public void moveTo(Map m, int tx, int ty) {
		Movement.moveTo(this,m,tx,ty);
	}

	/**
	 * Move an object randomly to an adjacent, unblocked square.
	 * 
	 * @return True if object was moved from original place
	 */ 
	public boolean displace() {
		if (!(place instanceof Map)) return false;
		boolean blocking=isBlocking();
		Map m = (Map) place;
		
		// try random move first	
		if (true) { 
			int nx = x + 2*Rand.r(2) - 1;
		
			int ny = y + 2*Rand.r(2) - 1;
			if (Rand.d(2)==1) {
				if (Rand.d(2)==1) {
					nx=x;
				} else {
					ny=y;
				}
			}
	        if (!m.isTileBlocked(nx, ny)&&(!(blocking && m.isBlocked(nx, ny)))) {
	        	moveTo(m, nx, ny);
	        	return true;
	        }
		}
		
        // count possible moves
		int choices=0;
		for (int nx=x-1; nx<=x+1; nx++) {
			for (int ny=y-1; ny<=y+1; ny++) {
				if ((nx==x)&&(ny==y)) continue;
	            if (!m.isTileBlocked(nx, ny)&&(!(blocking && m.isBlocked(nx, ny)))) {
	            	choices++;
	            }
			}
		}
		
		// select a possible move randomly
		if (choices==0) return false;
		choices=Rand.d(choices);
		for (int nx=x-1; nx<=x+1; nx++) {
			for (int ny=y-1; ny<=y+1; ny++) {
				if ((nx==x)&&(ny==y)) continue;
	            if (!m.isTileBlocked(nx, ny)&&(!(isBlocking() && m.isBlocked(nx, ny)))) {
	            	choices--;
	            	if (choices<=0) {
	                	moveTo(m, nx, ny);
	                	return true;
	            	}
	            }
			}
		}
		return false;
	}

	public boolean isWithin(ThingOwner thing) {
		for (Thing head = this; head != null; head = (Thing) head.place) {
			if (head.place == thing)
				return true;
			if (!(head.place instanceof Thing))
				return false;
		}
		return false;
	}

	public boolean isTransparent() {
		return !getFlag("IsViewBlocking");
	}

	// returns true f the Thing is damaged
	// used for Items mainly
	public boolean isDamaged() {
		return getFlag("Damage");
	}

	// Message to be given if Thing is visible to the player
	public void visibleMessage(String s) {
		if ((isVisible(Game.hero())) && (!isInvisible())) {
			Game.message(s);
		}
	}

	// returns true if item can currently be seen
	// doesn't account for invisible items,
	//   i.e. assumes hero can see through all invisibilities
	public boolean isVisible(Thing toThing) {
		if (place instanceof Map) {
			// can see it if it is in a visible square
			return toThing.canSee(this);
		}
        // can see it if we are carrying it!
        return toThing.isHero() && rootThing() == toThing;
	}

	public Thing rootThing() {
		return (place instanceof Thing)?((Thing)place).rootThing():this;
	}
	
	// returns true if the item should be displayed
	public boolean isInvisible() {
		return getFlag("IsInvisible");
	}

	// clone method. Creates displaced copy of this
	public Object clone() {
			Thing t = new Thing(super.getLocal(),super.getInherited());
			t.place = null;
			t.next = null;

			if (inv != null) {
				t.inv = (Thing[]) inv.clone();
				for (int x = 0; x < invcount; x++) {
					t.inv[x] = (Thing) inv[x].clone();
					t.inv[x].place = t;
				}
			}
			
			return t;
	}

	// check if stacking possible
	public boolean canStackWith(Thing other) {
		if (this==other) return false;
		if (!other.getFlag("IsItem")) return false;
		if (other==this) throw new Error("Can't do canStackWith(self)!!");
		if (!equalsIgnoreNumber(other)) return false;
		if (!sameContents(other)) return false;
		return true;
	}
	
	public boolean sameContents(Thing other) {
		if ((invcount==0)&&(other.invcount==0)) return true;
		if (invcount!=other.invcount) return false;
		int n=invcount;
		for (int i=0; i<n; i++) {
			if (!inv[i].name().equals(other.inv[i].name())) {
				return false;
			}
		}
		
		return true;
	}

    public boolean stackWith(Thing thing) {
		if (!canStackWith(thing)) return false;
		thing.incStat("Number",getStat("Number"));
		remove();
	    return true;
	}
	
	public int getResistance(String dt) {
		return getStat("RES:"+dt);
	}
	
	public int getLevel() {
		return getStat("Level");
	}

	// checks if being has item in possession
	public boolean hasItem(String s) {
		for (int i = 0; i < invcount; i++) {
			if ((inv[i].name().equals(s)))
				return true;
		}
		return false;
	}
	
	public Thing getItem(String s) {
		for (int i = 0; i < invcount; i++) {
			if ((inv[i].name().equals(s)))
				return inv[i];
		}
		return null;
	}
   /**
    * checks if a thing is wielded
    * @return the fact
    */
   public boolean isWielded(Thing item) {
      boolean result=false;
      for (int i = 0; i < invcount; i++) {
         Thing t = inv[i];
         if (t.getFlag("IsItem")&&(t.y > 0) && t==item ) {
            return true;
         }
      }
      return result;
   }

	/**
	 * Returns all wielded items/effects
	 * @return Array of wielded items
	 */
	public Thing[] getWielded() {
		if (invcount == 0)
			return new Thing[0];
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (t.y > 0) {
				temp[tempcount] = t;
				tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0)
			System.arraycopy(temp, 0, result, 0, tempcount);
		return result;
	}
	
	/**
	 * Returns all wielded items/effects
	 * @return Array of wielded items
	 */
	public Thing[] getWieldedItems() {
		if (invcount == 0)
			return new Thing[0];
		Thing[] temp = new Thing[invcount];
		int tempcount = 0;
		for (int i = 0; i < invcount; i++) {
			Thing t = inv[i];
			if (t.getFlag("IsItem")&&(t.y > 0)) {
				temp[tempcount] = t;
				tempcount++;
			}
		}
		Thing[] result = new Thing[tempcount];
		if (tempcount > 0)
			System.arraycopy(temp, 0, result, 0, tempcount);
		return result;
	}
	
	// return the wielded / worn item in particular location
	public Thing getWielded(int wt) {
		Thing w = getWeapon(wt);

		if (w == null) {
			if (wt == RPG.WT_MAINHAND)
				w = getWeapon(RPG.WT_TWOHANDS);
			if ((wt == RPG.WT_TORSO) || (wt == RPG.WT_LEGS))
				w = getWeapon(RPG.WT_FULLBODY);
		}
		return w;
	}
	
	private static final Thing[] noThings={};
	public Thing[] getInventory() {
		return (inv==null)?noThings:inv;
	}



	public void dropThing(Thing thing) {
		thing.remove();
		if (place instanceof Map) {
			((Map) place).addThing(thing, x, y);
		}
	}
	
	
	// wields the item in appropriate slot
	// unwields other items to allow this
	public boolean wield(Thing t) {
		return wield(t,t.getStat("WieldType"));
	}
	
	public boolean wield(Thing t, int wt) {
		// already there?
		if (t.y==wt) return true;
		
		if (!clearUsage(wt))
			return false;

		
		
		// remove other items if cannot be wielded together
		// remeber to avoid quick boolean evaluation!
		if (wt == RPG.WT_TWOHANDS) {
			if (!clearUsage(RPG.WT_MAINHAND)
					| !clearUsage(RPG.WT_SECONDHAND))
				return false;
		}
		if (wt == RPG.WT_MAINHAND) {
			if (!clearUsage(RPG.WT_TWOHANDS))
				return false;
		}
		if (wt == RPG.WT_SECONDHAND) {
			if (!clearUsage(RPG.WT_TWOHANDS))
				return false;
		}

		if (wt == RPG.WT_FULLBODY) {
			if (!clearUsage(RPG.WT_TORSO) | !clearUsage(RPG.WT_LEGS))
				return false;
		}
		if (wt == RPG.WT_TORSO) {
			if (!clearUsage(RPG.WT_FULLBODY))
				return false;
		}
		if (wt == RPG.WT_LEGS) {
			if (!clearUsage(RPG.WT_FULLBODY))
				return false;
		}

		// TODO check logic
		int num=t.getNumber();
		if ((num>1)&&(wt!=RPG.WT_MISSILE)) {
			// remove excess items
			Thing tt=t.separate(num-1);
			addThing(tt); // don't stack
		}
		
		setUsage(t,wt);
		
		if (t.getFlag("IsArtifact")) Item.identify(t);
		
		return true;
	}

	// change usage, also change relevant modifiers
	public void setUsage(Thing target, int wt) {
		if (target.place!=this) throw new Error(target.getName(Game.hero())+" not in inventory ");
		if (target.y==wt) return;
		
		target.unApplyModifiers("WieldedModifiers",this);
		target.unApplyModifiers("EffectModifiers",this);
		target.y=wt;
		if ((wt>0)&&(wt!=RPG.WT_EFFECT)) target.applyModifiers("WieldedModifiers",this);
		if (wt==RPG.WT_EFFECT) target.applyModifiers("EffectModifiers",this);
	}
	
	private static final Modifier[] emptyModifiers=new Modifier[] {};
	
	/**
	 * Gets modifiers caused by this Thing 
	 * with a given reason.
	 * 
	 * @param reason
	 * @return
	 */
	private Modifier[] getModifiers(String reason) {
		Modifier[] mods=(Modifier[])get(reason);
		return (mods==null)?emptyModifiers:mods;
	}
	
	/** 
	 * Apply modifiers from this thing to a given target
	 * 
	 * @param reason Modifier reason e.g. "WieldedModifiers"
	 * @param target Target Thing
	 */
	public void applyModifiers(String reason, Thing target) {
		Modifier[] mods=getModifiers(reason);
		for (int i=0; i<mods.length; i++) {
			target.applyModifier(Modifier.create(mods[i],this,reason));
		}
	}
	
	public void applyModifiers(Modifier[] mods, String reason, Thing target) {
		for (int i=0; i<mods.length; i++) {
			target.applyModifier(Modifier.create(mods[i],this,reason));
		}
	}
	
	/** 
	 * Unapply modifiers applied from this thing from a given target
	 * 
	 * @param reason Modifier reason e.g. "WieldedModifiers"
	 * @param target Target Thing
	 */
	public void unApplyModifiers(String reason, Thing target) {
		Modifier[] mods=getModifiers(reason);
		for (int i=0; i<mods.length; i++) {
			String stat=mods[i].getStat();
			target.removeModifiersWithSource(this,reason,stat);
		}
	}
	
	// remove modifiers for a given stat, source and reason
	private void removeModifiersWithSource(Thing source, String reason, String stat) {
		ArrayList al=getStatModifiers(stat);
		if (al!=null) for (int i=al.size()-1; i>=0; i--) {
			Modifier m=(Modifier)al.get(i);
			if ((m.getSource()==source)&&(m.getReason().equals(reason))) {
				removeModifier(al,i);
			}
			if (al.size()==0) {
				setStatModifiers(stat,null);
			}
		}
	}
	
	private void removeModifier(ArrayList al, int i) {
		Modifier m=(Modifier)al.remove(i);
		String st=m.getString("RemoveMessage");
		if (st!=null) message(st);
	}
	
	public void removeAllModifiers(String reason) {
		HashMap hm=getModifierList();
		if (hm==null) return;
		
		Iterator it=hm.values().iterator();
		while (it.hasNext()) {
			ArrayList al=(ArrayList)it.next();
			if (al!=null) for (int i=al.size()-1; i>=0; i--) {
				Modifier m=(Modifier)al.get(i);
				if (m.getReason().equals(reason)) {
					removeModifier(al,i);
				}
				
			}
			if (al.size()==0) {
				it.remove();
			}
		}
	}
	
	public boolean isHostile(Thing t) {
	  if( this.isHero() ){
		  return AI.isHostile(t, this);
	  }
		return AI.isHostile(this, t);
	}

	public Thing cloneType() {
		return Lib.create(getString("Name"));
	}
	
	public int notify(int eventtype, int ext, Object o) {
		return AI.notify(this, eventtype, ext, o);
	}

	public void give(Thing giver, Thing gift) {
		if (handles("OnGift")) {
			Event e=new Event("Gift");
			e.set("Gift",gift);
			e.set("Giver",giver);
			handle(e);
		} else {
			giver.message(getTheName() + " does not seem interested");
		}
	}
	
	public boolean isDead() {
		return (getStat("HPS")<=0);
	}	
	
	private HashMap getModifierList() {
		return modifiers;
	}
	
	private void setModifierList(HashMap hm) {
		modifiers=hm;
	}
	
	public boolean isModified() {
		return modifiers!=null;
	}
	
	private ArrayList getStatModifiers(String s) {
		HashMap hm=getModifierList();
		if (hm==null) return null;
		return (ArrayList)hm.get(s);
	}
	
	private void setStatModifiers(String s, ArrayList al) {
		HashMap hm=getModifierList();
		
		if (hm==null) {
			if (al==null) return;
			hm=new HashMap();
			setModifierList(hm);
		}
		
		if (al!=null) {
			hm.put(s,al);
		} else {
			hm.remove(s);
			if (hm.size()==0) {
				setModifierList(null);
			}
		}
	}
	
	public void add(String reason, Modifier mod) {
		super.add(reason,mod);
		
		if (reason.equals("CarriedModifiers")&&(place instanceof Thing)) {
			getParent().applyModifier(Modifier.create(mod,this,reason));
		}
		
		if (reason.equals("SelfModifiers")) {
			this.applyModifier(Modifier.create(mod,this,reason));
		}
	}
	
	/*
	 * All additions of modifiers to this thing go through here
	 */
	private void applyModifier(Modifier m) {
		String s=m.getStat();
		ArrayList al=getStatModifiers(s);
		if (al==null) {
			al=new ArrayList();
			setStatModifiers(s,al);
		}
		al.add(m);
		String st=m.getString("ApplyMessage");
		if (st!=null) message(st);
		Collections.sort(al,Modifier.sorter);
	}
	
	public Object getModified(String s, int pos) {
		ArrayList al=getStatModifiers(s);
		if ((al!=null)&&(pos<al.size())) {
			
			// get the value from the next modifier
			Modifier m=(Modifier)al.get(pos);
			return m.calculate(this,s,pos+1);
		}
		// no modifiers left, so use properties
		return super.get(s);
	}	
	
	public int getStat(String key) {
		Integer i=(Integer)get(key);
		if (i==null) return 0;
		return i.intValue();
	}
	
	public Object get(String key) {
		if (modifiers!=null) {
			return getModified(key,0);
		}
        return super.get(key);
	}
	
	public String report() {
		String s = super.report();
		if (modifiers!=null) {
			s=s+"\n";
			s=s+"Modified Values:\n";
			Iterator it=modifiers.keySet().iterator();
			while (it.hasNext()) {
				String p=(String)it.next();
				s=s+p+" = "+get(p)+"\n";
			}
		}
		return s;
	}

    public List orthogonalExits(int directionX, int directionY) {
        Map map = getMap();
        if (map == null) return Collections.EMPTY_LIST;
        List exits = new ArrayList();
        if (directionX == 1 || directionX == -1) {
            if (!map.isBlocked(x, y - 1) || isDoorVisible(x, y - 1)) exits.add(new Point(x, y - 1));
            if (!map.isBlocked(x, y + 1) || isDoorVisible(x, y + 1)) exits.add(new Point(x, y + 1));
        } else if (directionY == 1 || directionY == -1) {
            if (!map.isBlocked(x - 1, y) || isDoorVisible(x - 1, y)) exits.add(new Point(x - 1, y));
            if (!map.isBlocked(x + 1, y) || isDoorVisible(x + 1, y)) exits.add(new Point(x + 1, y));
        }
        return exits;
    }
    
    public boolean isDoorVisible(int x, int y) {
        Thing[] things = getMap().getThings(x, y);
        for (int i = 0; i < things.length; i++) {
            Thing thing = things[i];
            if(thing.getFlag("IsSecretDoor") || thing.getFlag("IsSecretPassage")) continue;
            if(thing.getFlag("IsDoor")) return true;
        }
        return false;
    }

    public List moreExits(int directionX, int directionY) {
        Map map = getMap();
        if (map == null) return Collections.EMPTY_LIST;
        List exits = orthogonalExits(directionX, directionY);
        if (directionX == 1 || directionX == -1) {
             if(!map.isBlocked(x + directionX, y - 1)  || isDoorVisible(x + directionX, y - 1)) exits.add(new Point(x + directionX, y - 1));
             if(!map.isBlocked(x + directionX, y) || isDoorVisible(x + directionX, y)) exits.add(new Point(x + directionX, y));
             if(!map.isBlocked(x + directionX, y + 1) || isDoorVisible(x + directionX, y + 1)) exits.add(new Point(x + directionX, y + 1));
        } else if (directionY == 1 || directionY == -1) {
             if(!map.isBlocked(x + directionX, y - 1) || isDoorVisible(x + directionX, y - 1)) exits.add(new Point(x + directionX, y - 1));
             if(!map.isBlocked(x + directionX, y) || isDoorVisible(x + directionX, y)) exits.add(new Point(x + directionX, y));
             if(!map.isBlocked(x + directionX, y + 1) || isDoorVisible(x + directionX, y + 1)) exits.add(new Point(x + directionX, y + 1));
        }
        return exits;
    }

    public boolean areSeveralDirectionsNotVisited() {
        Map map = getMap();
        int[] xDelta = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        int[] yDelta = new int[]{-1, -1, -1, 0, 1, 1, 1, 0};
        List notVisited = new ArrayList();
        for (int i = 0; i < xDelta.length; i++) {
            if(map.isTileBlocked(x + xDelta[i], y + yDelta[i])) continue;
            int pathValue = map.getPath(x + xDelta[i], y + yDelta[i]);
            if(pathValue == 0) {
                notVisited.add(new Point(xDelta[i], yDelta[i]));
            }
        }
        if(notVisited.size() < 2) return false;
        int signX = -2;
        int signY = -2;
        boolean xsMatch, ysMatch;
        for (Iterator iter = notVisited.iterator(); iter.hasNext();) {
            Point point = (Point) iter.next();
            if(signX == -2) {
                signX = point.x;
            }
            if(signY == -2) {
                signY = point.y;
            }
            xsMatch = point.x != 0 && point.x == signX;
            ysMatch = point.y != 0 && point.y == signY;
            if(!xsMatch && !ysMatch) return true;
        }
        return false;
    }
    
    /*
     * These are the regions mentioned below:
     */
//  AFFB
//  E..G
//  E..G
//  DHHC 
    private List allExits(boolean countAlreadyVisited) {
        int[] xDelta = {};
        int[] yDelta = {};
        Map map = getMap();
        if(map == null) return Collections.EMPTY_LIST;
        if (x == 0) {
            if (y == 0) {
                // region A
                xDelta = new int[]{1, 1, 0};
                yDelta = new int[]{0, 1, 1};
            } else if (y == map.getHeight() - 1) {
                // region D
                xDelta = new int[]{0, 1, 1};
                yDelta = new int[]{-1, -1, 0};
            } else {
                // region E
                xDelta = new int[]{0, 1, 1, 1, 0};
                yDelta = new int[]{-1, -1, 0, 1, 1};
            }
        } else if (y == 0) {
            if (x == map.getWidth() - 1) {
                // region B
                xDelta = new int[]{-1, -1, 0};
                yDelta = new int[]{0, 1, 1};
            } else {
                // region F
                xDelta = new int[]{-1, -1, 0, 1, 1};
                yDelta = new int[]{0, 1, 1, 1, 0};
            }
        } else if (x == map.getWidth() - 1) {
            if (y == map.getHeight() - 1) {
                // region C
                xDelta = new int[]{-1, -1, 0};
                yDelta = new int[]{0, -1, -1};
            } else {
                // region G
                xDelta = new int[]{0, -1, -1, -1, 0};
                yDelta = new int[]{-1, -1, 0, 1, 1};
            }
        } else if (y == map.getHeight() - 1) {
            // region H
            xDelta = new int[]{-1, -1, 0, 1, 1};
            yDelta = new int[]{0, -1, -1, -1, 0};
        } else {
            // middle
            xDelta = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
            yDelta = new int[]{-1, -1, -1, 0, 1, 1, 1, 0};
        }
        List freedoms = new ArrayList();
        for (int i = 0; i < xDelta.length; i++) {
            if (!map.isBlocked(x + xDelta[i], y + yDelta[i])) {
                int newX = x + xDelta[i];
                int newY = y + yDelta[i];
                if(!countAlreadyVisited && map.getPath(newX, newY) == 1) continue;
                if(Movement.canMove(this, map, newX, newY))
                    freedoms.add(new Point(x + xDelta[i], y + yDelta[i]));
            }
        }
        return freedoms;
    }
    
    public boolean inARoom() {
        Map map = getMap();
        if(map == null) return false;
        return allExits(true).size() >= 3;
    }
    
    public void isRunning(boolean isRunning) {
        set("IsRunMode", isRunning);
        if(isRunning) {
            incStat("RunCount", 1);
        } else {
            set("RunCount", 0);
            set("RunDirectionX", Integer.MIN_VALUE);
            set("RunDirectionY", Integer.MIN_VALUE);
        }
    }

    //MAINHAND == r
    public String inHandMessage() {
        Thing bothHands = getWielded(RPG.WT_TWOHANDS);
        if(bothHands != null) return bothHands.getName() + " in both hands";
        StringBuffer inHandMessage = new StringBuffer();
        Thing inHand = getWielded(RPG.WT_MAINHAND);
        Thing otherHand = getWielded(RPG.WT_SECONDHAND);
        inHandMessage.append(inHand == null ? "" : " " + inHand.getName() + " in right hand");
        if(otherHand != null) {
            if(inHandMessage.length() > 0) inHandMessage.append(", ");
            inHandMessage.append(otherHand.getName());
            inHandMessage.append(" in left hand");
        }
        if(inHand == null && otherHand == null) return "nothing in hand";
        return inHandMessage.toString().trim();
    }
}