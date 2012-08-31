/*
 * Created on 19-Jun-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package mikera.tyrant.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mikera.tyrant.EventHandler;
// import mikera.tyrant.Game;
// import mikera.tyrant.Lib;
import mikera.tyrant.Scripts;
import mikera.tyrant.util.Count;
import mikera.tyrant.util.Text;

/**
 * The BaseObject class is the root for all Tyrant game objects
 * 
 * It implements a dynamic, mutable list of properties
 * 
 * @author Mike
 * 
 */
public class BaseObject implements Cloneable, Serializable {

    private static final long serialVersionUID = 6165762084693059838L;
    // properties
    private HashMap<String,Object> local;
    private BaseObject inherited;
    public static boolean GET_SET_DEBUG = false;
    public static boolean GET_OUTPUT_DEBUG = false;    
    public static boolean SET_OUTPUT_DEBUG = false; 
    public static final boolean OPTIMIZE = true; 
    
    /**
     * Counter for debugging
     */
    public static HashMap<String,Count> getCounter=new HashMap<String,Count>();

    public BaseObject() {
        // no properties for default baseobject
    }
    
	@SuppressWarnings("unchecked")
	public BaseObject(HashMap<String,Object> propertiesToCopy, BaseObject parent) {
		if (propertiesToCopy!=null) {
			local=(HashMap<String,Object>)propertiesToCopy.clone();
		}
		inherited=parent;
	}

    /**
     * Clone copies a BaseObject instance, maintaining
     * the same inherited properties
     */
    @SuppressWarnings("unchecked")
	public Object clone() {
        BaseObject o=new BaseObject();
        o.inherited=inherited;
        if (local!=null) {
        	o.local=(HashMap<String,Object>)local.clone();
        }
        
        return o;
    }

    public final boolean equals(Object o) {
    	return this==o;
    }
    
    public BaseObject(BaseObject parent) {
        this.inherited = parent;
    }
    
    public BaseObject(Map<String,Object> data) {
    	for (Iterator<String> it=data.keySet().iterator(); it.hasNext();) {
    		String key=it.next();
    		set(key,data.get(key));
    	}
    }
    
	public void replaceWith(BaseObject t) {
	    if (t.local != null) {
            local = new HashMap<String,Object>(t.local);
        } else {
            local = null;
        }
	    inherited=t.inherited;
	}

    /** 
     * Flattens the most commonly accessed properties for 
     * faster access.
     * 
     * Used in Lib.add();
     *
     */
    public void optimize() {
    	if (OPTIMIZE) {
    		flattenEntry("Number");
            flattenEntry("IsHostile");
            flattenEntry("IsBeing");
            flattenEntry("IsMobile");
            flattenEntry("IsItem");
            flattenEntry("IsViewBlocking");
            flattenEntry("IsVisible");
            flattenEntry("Z");
            flattenEntry("DamageMark");
            flattenEntry("OnAction");
            set("IsOptimized",1);
        }
    }

    /**
     * Flattens a specific key into a top-level value
     * 
     * @param key
     *            Key to flatten
     */
    private void flattenEntry(String key) {
        if (local==null) local=new HashMap<String,Object>();
        local.put(key, get(key));
    }

    public void set(String s, boolean value) {
        set(s, (value ? new Integer(1) : new Integer(0)));
    }

    public void set(String s, int value) {
        set(s, new Integer(value));
    }
    
    public void set(String s, double value) {
        set(s, new Double(value));
    }

    public void setProperties(Map<String,Object> map) {
    	Iterator<String> it=map.keySet().iterator();
    	while (it.hasNext()) {
    		String key=(String)it.next();
    		set(key,map.get(key));
    	}
    }
    
    /**
     * Add a modifier to a "reason" property
     * 
     * @param reason
     *            Modifier effect reason e.g. "WieldedModifiers"
     * @param mod
     */
    public void add(String reason, Modifier mod) {
        Modifier[] mods = (Modifier[]) get(reason);
        if (mods == null) {
            set(reason, new Modifier[]{mod});
            return;
        }
        Modifier[] nmods = new Modifier[mods.length + 1];
        System.arraycopy(mods, 0, nmods, 0, mods.length);
        nmods[mods.length] = mod;
        set(reason, nmods);
    }

    /**
     * Checks whether the Stuff object contains a given key
     * 
     * @param key
     * @return True if the receiver contains the key, false otherwise
     */
    public boolean containsKey(String key) {
        if (local != null && local.containsKey(key)) return true;

        // tail-recursive call could give a mild performance
        // benefit with a good compiler that can spot it!
        if (inherited != null) return inherited.containsKey(key);
        return false;
    }

    /**
     * Sets a property value
     * 
     * @param key The key value to set
     * @param value The new value
     * @return True if local value set, false otherwise
     */
    public boolean set(String key, Object value) {
        boolean didSet = realSet(key, value);
        if(GET_SET_DEBUG&&SET_OUTPUT_DEBUG) {
            System.out.println("set: " + key + " value: " + value + " didSet " + didSet);
        }
        return didSet;
    }

    private boolean realSet(String key, Object value) {
        if ((local==null)||(local != null && !local.containsKey(key))) {
        	if (inherited != null && inherited.containsKey(key)) {
	            Object parentValue = inherited.get(key);
	            if (parentValue == value) return false;
	            if ((parentValue != null) && parentValue.equals(value)) return false;
        	}
        }
        if (local == null) local = new HashMap<String,Object>();
        local.put(key, value);
        return true;
    }
	
    /**
     * Performs compression by removing duplicates
     * @param hs
     */
	public void compressData(HashMap<Object,Object> hs) {
		if (local!=null) {
			Iterator<String> it=local.keySet().iterator();
			while (it.hasNext()) {
				String k=it.next();
				Object o=local.get(k);
				if (o==null) continue;
				
				Object ho=hs.get(k);
				if (ho==null) {
					hs.put(o,o);
				} else {
					local.put(k,ho);
				}
			}
		}
		
		if (inherited!=null) {
			inherited.compressData(hs);
		}
	}

    public final String getString(String s) {
        return (String) get(s);
    }

    public Thing getThing(String s) {
        return (Thing) get(s);
    }

    public Script getScript(String s) {
        return (Script) get(s);
    }

    public int getStatIfAbsent(String stat, int ifAbsent) {
        Integer value = (Integer) get(stat);
        return value == null ? ifAbsent : value.intValue();
    }

    public int getStat(String s) {
        return getBaseStat(s);
    }

    /**
     * Gets a base, unmodified property value
     * 
     * This should differ from getStat(key) only if modifiers are present
     * 
     * @param key
     * @return
     */
    public int getBaseStat(String key) {
    	Integer i=(Integer)realGet(key);
    	if (i==null) return 0;
        return i.intValue();
    }

    public final boolean getFlag(String key) {
        Integer b = (Integer) get(key);
        return (b == null) ? false : (b.intValue() > 0);
    }
    
    public final double getDouble(String key) {
        Double d = (Double) get(key);
        return (d == null) ? 0.0 : (d.doubleValue());
    }

    public EventHandler getHandler(String s) {
        Object o = get(s);
        return (EventHandler) o;
    }

    public void addHandler(String s, EventHandler eh) {
        EventHandler oeh = getHandler(s);
        if (oeh == null) {
            set(s, eh);
        } else {
            set(s, Scripts.combine(oeh, eh));
        }
    }

    public int incStat(String s, int v) {
        int newValue=getBaseStat(s) + v;
		set(s, newValue);
		return newValue;
    }

    public boolean remove(String key) {
        if (local != null && local.containsKey(key)) {
            return local.remove(key) != null;
        }
        return false;
    }
    
    /**
     * This is the critical method in BaseObject that returns a property value
     * from the properties hash.
     * 
     * Note that this is *overridden* by Thing to implement modifiers, hence all
     * property access should go through get(key) or else modifiers will not
     * work correctly.
     * 
     * @param key
     *            The name of the property value to retreive
     * @return Property value
     */
    public Object get(String key) {
        Object value = realGet(key);
        if(GET_SET_DEBUG) {
        	
        	// access counting
            Count count = (Count) getCounter.get(key);
            if (count == null) {
                count = new Count();
                getCounter.put(key, count);
            }
            count.increment();
        	
        	if (GET_OUTPUT_DEBUG) {
	            StackTraceElement[] stackTrace = new RuntimeException("debug").getStackTrace();
	            String stack = "";
	            for (int i = 0; i < stackTrace.length; i++) {
	                StackTraceElement element = stackTrace[i];
	                String methodName = element.getMethodName().toLowerCase();
	                if(methodName.indexOf("get") >= 0) continue;
	                stack = element.getClassName() + "." + element.getMethodName();
	                break;
	            }
	            System.out.println("get: " + key + " value: " + value + " from " + stack);
        	}
        }
        return value;
    }

    private Object realGet(String key) {
        // check current Stuff if present
        if (local != null) {
            // need to do it this way in case local
            // value is set to null!
            if (local.containsKey(key)) { return local.get(key); }
        }
        // default to base Stuff
        if (inherited != null) return inherited.get(key);
        // nothing found
        return null;
    }

    /**
     * Special case handling to test whether two sets of Stuff are equal while
     * disregarding the special property "Number"
     * 
     * TODO: handle general case where Stuff are equal but top-level Stuff are
     * not
     * 
     * @param that
     *            Stuff to test against
     * @return
     */
    public boolean equalsIgnoreNumber(BaseObject that) {
        // check for equivalent base Stuff
        if (inherited == null) {
            if (!(that.inherited == null)) return false;
        } else {
            if (that.inherited == null) return false;
            if (!inherited.equals(that.inherited)) return false;
        }
        Object name = get("Name");
        Object otherName = that.get("Name");
        if (!name.equals(otherName)) return false;
        if (local != null) {
            Iterator<String> it = local.keySet().iterator();
            while (it.hasNext()) {
                String k = it.next();
                if (k.equals("Number")) continue;
                if (!get(k).equals(that.get(k))) return false;
            }
        }
        if (that.local != null) {
            Iterator<String> it = that.local.keySet().iterator();
            while (it.hasNext()) {
                String k = it.next();
                if (k.equals("Number")) continue;
                if (!that.get(k).equals(get(k))) return false;
            }
        }
        return true;
    }

    /**
     * Get a single Map of all property pairs
     */
    public Map<String,Object> getCollapsedMap() {
        Map<String,Object> map = null;

        // recursively include baseStuff
        if (inherited != null) map = inherited.getCollapsedMap();

        if (map == null) {
            if (local == null) {
                // Create an empty HashMap
                return new HashMap<String,Object>();
            }
            // Just copy the existing Stuff
            map = new HashMap<String,Object>(local);
        } else {
            // Copy the top level Stuff
            if (local == null) return map;

            Iterator<String> it = local.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                map.put(key, local.get(key));
            }
        }
        return map;
    }
    
    public HashMap<String,Object> getPropertyHashMap() {
        HashMap<String,Object> map = null;

        // recursively include baseStuff
        if (inherited != null) map = inherited.getPropertyHashMap();

        if (map == null) {
            if (local == null) {
                // Create an empty HashMap
                return new HashMap<String,Object>();
            }
            // Just copy the existing Stuff
            map = new HashMap<String,Object>(local);
        } else {
            // Copy the top level Stuff
            if (local == null) return map;

            Iterator<String> it = local.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                map.put(key, local.get(key));
            }
        }
        return map;
    }

    public BaseObject getInherited() {
        return inherited;
    }

    public HashMap<String,Object> getLocal() {
        return local;
    }

    public int size() {
        int baseSize = 0;
        if (inherited != null) baseSize = inherited.size();
        return baseSize + local.size();
    }

    public String report() {
        List<String> al = new ArrayList<String>();
        String text = "";
        BaseObject p = getFlattenedStuff(this);
        if (p.local == null) return text;
        Iterator<String> i = p.local.keySet().iterator();
        while (i.hasNext()) {
            String k = (String) i.next();
            Object o = p.get(k);
            String s=k + " : " + (o == null ? "null" : o.toString());
            s=Text.rightPad(s,50);
            al.add( s + "depth="+getPropertyDepth(k)+"\n");
        }
        Collections.sort(al);
        i = al.iterator();
        while (i.hasNext()) {
            String s = (String) i.next();
            text = text + s;
        }
        return text;
    }
    
    public String reportByValue() {
        List<String> al = new ArrayList<String>();
        String text = "";
        BaseObject p = getFlattenedStuff(this);
        if (p.local == null) return text;
        Iterator<String> i = p.local.keySet().iterator();
        while (i.hasNext()) {
            String k = (String) i.next();
            Object o = p.get(k);
            String s=Text.leftPad((o == null ? "null" : o.toString()),20);
            s=s + " : " + k;
            s=Text.rightPad(s,50)+"\n";
            al.add(s);
        }
        Collections.sort(al);
        i = al.iterator();
        while (i.hasNext()) {
            String s = (String) i.next();
            text = text + s;
        }
        return text;
    }
    
    private int getPropertyDepth(String key) {
    	if ((local!=null)&&local.containsKey(key)) return 1;
    	if (inherited==null) return 0;
    	return inherited.getPropertyDepth(key)+((local==null)?0:1);
    }

    public static BaseObject getFlattenedStuff(BaseObject source) {
        BaseObject destination = new BaseObject();
        flattenInto(source, destination);
        return destination;
    }

    /**
     * Puts all Stuff from a given source into the top level of a set of
     * destination Stuff
     * 
     * @param source
     *            Source of property values
     * @param dest
     *            Destination for property values
     */
    public static void flattenInto(BaseObject source, BaseObject dest) {
        if (source == null) return;

        // recursive call for inherited
        flattenInto(source.inherited, dest);

        if (source.local == null) return;

        // iterate through top-level Stuff
        Iterator<String> it = source.local.keySet().iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            dest.set(s, source.get(s));
        }
    }

    /**
     * Create Stuff with single top-level hash
     * 
     * Useful if chain of base Stuff gets too long
     */
    public static BaseObject getFlattened(BaseObject source) {
        BaseObject destination = new BaseObject();
        flattenInto(source, destination);
        return destination;
    }

    public void flattenProperties() {
    	BaseObject flattened = BaseObject.getFlattened(this);
    	this.local = flattened.local;
    	this.inherited = null;
    }
    
    public String[] findAttributesStartingWith(String toFind) {
        List<String> found = new ArrayList<String>();
        findAttributesStartingWith(toFind, found);
        return (String[]) found.toArray(new String[found.size()]);
    }
    
    public void findAttributesStartingWith(String toFind, List<String> found) {
        if (local != null) {
            for (Iterator<String> iter = getLocal().keySet().iterator(); iter.hasNext();) {
                String attribute = (String) iter.next();
                if (attribute.startsWith(toFind)) found.add(attribute);
            }
        }
        if(inherited != null) {
            inherited.findAttributesStartingWith(toFind, found);
        }
    }
}
