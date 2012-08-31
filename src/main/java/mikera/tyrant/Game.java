package mikera.tyrant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Static methods for managing the overall game state
 * 
 * Includes useful input functionality
 * 
 * @author Mike
 */

import java.io.*;

import mikera.engine.BaseObject;
import mikera.tyrant.author.MapMaker;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Point;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.Text;
import mikera.util.Maths;

public final class Game extends BaseObject implements Serializable {
	private static final long serialVersionUID = 3544670698288460592L;
    
	// temp: static game instance
	private static Game instance = new Game();
	
	public static transient IMessageHandler messagepanel;
	
	// version number is updated in QuestApp.init()
    public static String VERSION = null;

	// reference to the Hero object
	private Thing hero;
	
	
	private transient InputHandler inputHandler = null;
    
	// The actor field stores which being is currently
	// taking an action. Primary use is to determine
	// whether the hero is responsible for inflicting
	// damage, i.t. whether beasties get angry or not.
	public static Thing actor;

	// Game over flag
	// Set to true during game to terminate main loop
	public static boolean over = true;

	// Interface helper object
	private static Interface userinterface;

    // Flag that shows if save() call is first or no
    private static boolean saveHasBeenCalledAlready = true;

	// Thread for recieveing user input
	public static Thread thread;

	public static int seed() {
		return hero().getStat("Seed");
	}

	public static void loadVersionNumber() {
		if (VERSION!=null) return;
		java.util.Properties props=null;
		
		// load version number
		try {
			InputStream fis = Game.class.getResourceAsStream( "/version.txt" );
			props=new java.util.Properties();
			props.load(fis);
			Game.VERSION=props.getProperty("version");		
		} catch (Exception e) {
			Game.warn("Version number problem!");
			e.printStackTrace();
		}
	}
	
	/**
	 * List of recent messages
	 */
	private ArrayList messageList=new ArrayList();
	private boolean debug = false;
	// toggle for visual effects
	public static boolean visuals = false;
	
    public ArrayList getMessageList() {
        return messageList;
    }
    
	public String messageList() {
		StringBuffer sb=new StringBuffer();
		for (int i=0; i<messageList.size(); i++) {
			sb.append("\n"+(String)messageList.get(i));
		}
		return sb.toString();
	}
	
	/**
	 * Print a general game message
	 * All messages should be routed through here or quotedMessage()
	 */
	public static void message(String s) {
		if (instance().messageStack.isEmpty()) {
			instance().displayMessage(s);
		} else {
			ArrayList al=(ArrayList)(instance().messageStack.peek());
			al.add(s);
		}
	}

    /**
     * Print a general game message, surrounded by quotation marks
     * All messages should be routed through here or message()
     */
    public static void quotedMessage(String s) {
        final String sToAdd = "\"" + s + "\"";
        if (instance().messageStack.isEmpty()) {
            instance().displayMessage(sToAdd);
        } else {
            ArrayList al=(ArrayList)(instance().messageStack.peek());
            al.add(sToAdd);
        }
    }


	/**
	 * Prints a set of messages stored in an ArrayList
	 * This is designed for use with popMessages();
	 * 
	 * @param al ArrayList containing String objects for each message
	 */
	public static void message(ArrayList al) {
		for (int i=0; i<al.size(); i++) {
			message((String)al.get(i));
		}		
	}
	
	/*
	 * Holds messages in a stack so that they can be displayed later
	 */
	public void pushMessages() {
		messageStack.push(new ArrayList());
	}
	
	/*
	 * Pulls a stored set of messages off the stack
	 */
	public ArrayList popMessages() {
		ArrayList top=(ArrayList)messageStack.pop();
		return top;
	}
	
	public void clearMessageList() {
		messageStack=new Stack();
	}
	
	public static char showData(String s) {
		java.awt.Component comp=getQuestapp().getScreen();
		InfoScreen is=new InfoScreen(getQuestapp(),"");
		TextArea ma=new TextArea();
		is.setLayout(new BorderLayout());
		ma.setText(s);
		ma.setBackground(Color.darkGray);
		ma.setForeground(Color.lightGray);
		ma.addKeyListener(getQuestapp().keyadapter);
		is.add(ma);
		getQuestapp().switchScreen(is);
		ma.setCaretPosition(1000000);
		char c=Game.getChar();
		getQuestapp().switchScreen(comp);
		return c;
	}
	
	/*
	 * Stack to store messages before they are displayed.
	 * 
	 * This is useful so that you can defer the display of messages
	 * until after the result of several actions has been dtermined
	 * 
	 * Game.pushMessages();
	 * .... Do something complex here, possibly generating many messages
	 * ArrayList al=Game.popMessages();
	 * Game.message("The following interesting things happen:);
	 * Game.message(al);  // now display the messages
	 * 
	 * You can also use this method to suppress messages (use exactly
	 * the same technique, but omit the final line).
	 * 
	 */
	private Stack messageStack=new Stack();
    private boolean lineOfSightDisabled;
    private boolean isDesigner = false;
	
	private void displayMessage(String s) {
		if (s==null) {
			Game.warn("Null message!");
			return;
		}
		s=Text.capitalise(s);
		if (s.equals("")) {
			messagepanel.clear();
		} else {
			// mappanel.repaint();
			if (messagepanel!=null) {
				messagepanel.add(Text.capitalise(s + "\n"));
			}
			int number=1;
			if (messageList.size()>0) {
				String last=(String)messageList.get(messageList.size()-1);
				if (last.startsWith(s)&&last.endsWith(")")&&(last.indexOf("(x")>0)) {
					int st=last.indexOf("(x")+2;
					String n=last.substring(st,last.length()-1);
					try {
						number=Integer.parseInt(n)+1;
					} catch (Exception e) {
						Game.warn("Count problem: "+last);
						number=1;
					}
				} else if (last.equals(s)) {
					number=2;
				}
			}
			
			// add count to repeat messages
			if (number>1) {
				messageList.set(messageList.size()-1,s+" (x"+number+")");
			} else {
				messageList.add(s);
			}
			
			// remove old messages if list is too long
			while (messageList.size()>100) {
				messageList.remove(0);
			}
		}
	}

	public InputHandler createInputHandler() {
		InputHandler ih=new InputHandler() {
			public char getCharacter() {
				return getKeyEvent().getKeyChar();
			}
			
			public KeyEvent getKeyEvent() {
				getUserinterface().getInput();
				return getUserinterface().keyevent;
			}
		};
		return ih;
	}
	
	/**
	 * Wait for an single key press
	 */
	public static KeyEvent getInput() {
		return getInput(true);
	}
		
	public static KeyEvent getInput(boolean redraw) {
		if (redraw&&(getMappanel()!=null)&&(getQuestapp().isGameScreen())) {
			getMappanel().render();
			getMappanel().repaint();
		}
		if ((messagepanel!=null)&&(messagepanel instanceof MessagePanel)) {
			((MessagePanel)messagepanel).repaint();
		}
		
		Game g=Game.instance();
		if (g.inputHandler==null) {
			g.inputHandler=g.createInputHandler();
		}
		
		return g.inputHandler.getKeyEvent();
	}

	/**
	 * Request a line of text from the user
	 *  @param prompt The text prompt for the input
	 */ 
	public static String getLine(String prompt) {
		return getLine(prompt, "");
	}

	/**
	 * Request a line of text from the user
	 * 
	 * @param prompt The text prompt for the input
	 * @param result The existing/default text
	 */ 	
	public static String getLine(String prompt, String result) {
		messagepanel.add(prompt + result);
		messagepanel.getPanel().invalidate(); 
		messagepanel.getPanel().repaint();
		while (true) {
			KeyEvent k = getInput(false);
			char ch = k.getKeyChar();
			if (k.getKeyCode() == KeyEvent.VK_ENTER)
				break;
			if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {
				result="ESC";
				break;
			}
			if (k.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
				// add the character to the input string if typed
				// i.e. don't include SHIFT, ALT etc.
				if (Character.isLetterOrDigit(ch)||(" -.:+'[]()=<>".indexOf(ch)>=0)) {
					result = result + ch;
				}
			} else if (result.length()>0) {
				result = result.substring(0, result.length() - 1);
			}
			messagepanel.clear();
			messagepanel.add(prompt + result);
		}
		Game.message("");
		return result;
	}

	/**
	 * Simulate a key press
	 * Useful for handling equivalent mouse clicks
	 */ 
	public static void simulateKey(char c) {
		if (getUserinterface() != null) {
			KeyEvent k = new KeyEvent(getMappanel(), 0, System.currentTimeMillis(),
					0, 0, 'c');
			k.setKeyChar(c);
			getUserinterface().go(k);
		}
	}

	public static void infoScreen(String s) {
		Screen old = getQuestapp().getScreen();
		InfoScreen is = new InfoScreen(getQuestapp(), s);
		getQuestapp().switchScreen(is);
		Game.getInput();
		getQuestapp().switchScreen(old);
	}
	
	public static void scrollTextScreen(String s) {
		scrollTextScreen(s,false);
	}
		
	public static void scrollTextScreen(String s, boolean showEnd) {

		QuestApp questapp=Game.getQuestapp();
		Screen old = questapp.getScreen();
		InfoScreen is=new InfoScreen(questapp,"");
		TextArea ma=new TextArea();
		is.setLayout(new BorderLayout());

		ma.setText( s );
		ma.setBackground(Color.darkGray);
		ma.setForeground(Color.lightGray);
		ma.addKeyListener(questapp.keyadapter);
		is.add(ma);
		questapp.switchScreen(is);
		
		if (showEnd) {
			ma.setCaretPosition(1000000);
		}
		Game.getInput();
		questapp.switchScreen(old);
	}

	public static void viewMap(Map m) {
        if(getQuestapp().getScreen() == null) getQuestapp().setupScreen();
		GameScreen gs = getQuestapp().getScreen();

		gs.map = m;
		gs.getMappanel().map = m;
		
	}
	
	// transport to location of particular map
	public static void enterMap(Map m, int tx, int ty) {
		viewMap(m);
		m.addThing(Game.hero(), tx, ty);
		Game.message(m.getEnterMessage());

		// update highest reached level if necessary
		if (hero().getStat(RPG.ST_SCORE_BESTLEVEL) < m.getLevel()) {
			hero().set(RPG.ST_SCORE_BESTLEVEL, m.getLevel());
		}
		
		// Game.hero.set("APS",0);
	}
	
    /**
     * Gets the map sorage HashMap
     * 
     * @return
     */
    public HashMap getMapStore() {
    	HashMap h=(HashMap)Game.instance().get("MapStore");
    	if (h==null) {
    		h=new HashMap();
    		Game.instance().set("MapStore",h);
    	}
    	return h;
    }
    
    private HashMap getMapObjectStore() {
    	HashMap h=(HashMap)Game.instance().get("MapObjectStore");
    	if (h==null) {
    		h=new HashMap();
    		Game.instance().set("MapObjectStore",h);
    	}
    	return h;
    }
    
    private ArrayList getMapObjectList(String mapName) {
    	HashMap h=getMapObjectStore();
    	ArrayList al=(ArrayList)h.get(mapName);
    	if (al==null) {
    		al=new ArrayList();
    		h.put(mapName,al);
    	}
    	return al;
    }
    
    /**
     * Adds a thing to a map, storing it in a temporary queue 
     * if the map is not yet created
     * 
     * @param t The thing to add
     * @param mapName The map name
     */
    public void addMapObject(Thing t, String mapName) {
    	addMapObject(t,mapName,0,0);
    }

    
    public void addMapObject(Thing t, String mapName, int x, int y) {
    	t.remove();
    	t.x=x;
    	t.y=y;
    	
    	Map map = (Map)getMapStore().get(mapName);
    	if (map==null) {
    		ArrayList al=getMapObjectList(mapName);
    		al.add(t);
    	} else {
    		addMapObject(t,map);
    	}
    }
    
    private void addMapObject(Thing t, Map map) {
    	if ((t.x==0)&&(t.y==0)) {
    		map.addThing(t);
    	} else {
    		map.addThing(t,t.x,t.y);
    	}
    }
    
    public void addMapObjects(Map map) {
    	ArrayList obs=getMapObjectList(map.getString("HashName"));
    	for (Iterator it=obs.iterator(); it.hasNext(); ) {
    		Thing t=(Thing)it.next();
    		addMapObject(t,map);
    	}
    	obs.clear();
    }
	
	public Map createWorld() {
		set("MapStore",null);
		return Portal.getMap("karrain",1,0);
	}
	
	public void compressAllData() {
		HashMap hs=new HashMap();
		
		HashMap store=getMapStore();
		Set keySet=getMapStore().keySet();
		
		for (Iterator it=keySet.iterator(); it.hasNext();) {
			Map m=(Map)store.get(it.next());
			compressMapData(hs,m);
		}
	}
	
	private void compressMapData(HashMap hs, Map m) {
		Thing[] ts=m.getThings();
		for (int i=0; i<ts.length; i++) {
			ts[i].compressData(hs);
		}
	}
	
	public static void assertTrue(boolean condition) {
		if (!condition) {
			try {
				throw new AssertionError();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	// has same effect as pressing stipulated direction key
	public static void simulateDirection(int dx, int dy) {
		switch (dy) {
			case -1 :
				switch (dx) {
					case 1 :
						simulateKey('9');
						return;
					case 0 :
						simulateKey('8');
						return;
					case -1 :
						simulateKey('7');
						return;
				}
			case 0 :
				switch (dx) {
					case 1 :
						simulateKey('6');
						return;
					case 0 :
						simulateKey('5');
						return;
					case -1 :
						simulateKey('4');
						return;
				}
			case 1 :
				switch (dx) {
					case 1 :
						simulateKey('3');
						return;
					case 0 :
						simulateKey('2');
						return;
					case -1 :
						simulateKey('1');
						return;
				}
		}

		return;
	}

	// Waits for user to select one of specified keys
	// returns: key pressed (from list)
	//      or: 'q' if ESC is pressed
	//      or: 'e' if ENTER is pressed
	public static char getOption(String s) {
		while (true) {
			KeyEvent k = getInput();
			
			char c = GameScreen.getKey(k);
			if (s.indexOf(c) >= 0)
				return c;
			if (k.getKeyCode() == KeyEvent.VK_ESCAPE)
				return 'q';
			if (k.getKeyCode() == KeyEvent.VK_ENTER)
				return 'e';
		}
	}
	
	public static char getChar() {
        KeyEvent k=getInput();
        return k.getKeyChar();
	}
	
	/**
	 * Get a number  
	 * 
	 * @param prompt
	 * @param max
	 * @return Number entered, max if too high or 0 if too low or ESC is pressed
	 */
	public static int getNumber(String prompt, int max) {
		String line=getLine(prompt);
		try {
			if (line.equals("ESC")) return 0;
			if (line.equals("")||line.equals("all")) return max;
			int r= Integer.parseInt(line);
			r=Maths.middle(0,r,max);
			return r;
		} catch(Exception e) {
			Game.warn("Invalid number in Game.getNumber(...)");
		}
		return 0;
	}

	public static void warn(String s) {
		if (Game.isDebug()) System.out.println(s);
	}

	/**
	 * Temporary access method for Game.instance
	 * @return
	 */
	public static Game instance() {
		return instance;
	}
	
	/**
	 * Returns the current level (hero's level)
	 * @return Difficulty level
	 */
	public static int level() {
		//Map m=hero.getMap();
		//if (m!=null) return m.getLevel();
		return hero() == null ? 1 : hero().getLevel();
	}
	
	/**
	 * Choose a direction, given as a Point offset
	 */ 
	public static Point getDirection() {
		while (true) {
			KeyEvent e = Game.getInput();

			char k = Character.toLowerCase(e.getKeyChar());
			int i = e.getKeyCode();

			// handle key conversions
			if (i == KeyEvent.VK_UP)
				k = '8';
			if (i == KeyEvent.VK_DOWN)
				k = '2';
			if (i == KeyEvent.VK_LEFT)
				k = '4';
			if (i == KeyEvent.VK_RIGHT)
				k = '6';
			if (i == KeyEvent.VK_HOME)
				k = '7';
			if (i == KeyEvent.VK_END)
				k = '1';
			if (i == KeyEvent.VK_PAGE_UP)
				k = '9';
			if (i == KeyEvent.VK_PAGE_DOWN)
				k = '3';
			if (i == KeyEvent.VK_ESCAPE)
				k = 'q';
			if (k == 'q') {
				return null;
			}
            Point direction = getQuestapp().getScreen().gameHandler.convertKeyToDirection(k);
            if(direction != null) return direction;
        }
	}

	/**
	 * 
	 * Gets player to select a string from given list
	 * Calls inventory-style screen
	 * Restores original screen before returning
	 */
	public static String selectString(String message, String[] strings) {
		Screen old = getQuestapp().getScreen();
		ListScreen ls = new ListScreen(message, strings);
		getQuestapp().switchScreen(ls);
		String ret = (String) ls.getObject();
		getQuestapp().switchScreen(old);
		return ret;
	}
	
	public static String selectString(String message, ArrayList strings) {
		return selectString(message,strings,strings);
	}
	
	public static String selectString(String message, ArrayList strings, ArrayList results) {
		String[] ss=new String[strings.size()];
		for (int i=0; i<ss.length; i++) {
			ss[i]=(String)strings.get(i);
		}
		int i=strings.indexOf(selectString(message,ss));
		return (i>=0) ? (String)results.get(i) : null;
	}

	/**
	 * Gets player to select an item from given list
	 */ 
	public static Thing selectItem(String message, Thing[] things) {
		return selectItem(message,things,false);
	}
	
	/**
	 * Gets player to select an item from given list
	 */ 
	public static Thing selectItem(String message, Thing[] things, boolean rememberFilter) {
		Item.tryIdentify(Game.hero(),things);
		Screen old = getQuestapp().getScreen();
		InventoryScreen is = getQuestapp().getScreen().getInventoryScreen();
		is.setUp(message,null,things);
		if (!rememberFilter) {
			is.inventoryPanel.clearFilter();
		}
		getQuestapp().switchScreen(is);
		Thing ret = is.getObject();
		getQuestapp().switchScreen(old);
		return ret;
	}
	
	/**
	 * Allow player to select an item to sell
	 * 
	 * Used by shopkeeper (chat)
	 * 
	 * @param message Message to display at top of inventory
	 * @param seller Seller of the items, i.e. the hero
	 * @param buyer Buyer of the items, i.e. the shopkeeper
	 * @return
	 */
	public static Thing selectSaleItem(String message, Thing seller, Thing buyer) {
		Thing[] things=seller.getItems();
		Item.tryIdentify(seller,things);
		Screen old = getQuestapp().getScreen();
		InventoryScreen is = getQuestapp().getScreen().getInventoryScreen();
		is.setUp(message,buyer,things);
		getQuestapp().switchScreen(is);	
		Thing ret = is.getObject();
		//System.out.println( ret );
		getQuestapp().switchScreen(old);
		return ret;
	}
	
    public static int selectSaleNumber(String message, Thing seller, Thing buyer, int max) {
        Thing[] things = seller.getItems();
        Item.tryIdentify(seller,things);
        InventoryScreen is = Game.getQuestapp().getScreen().getInventoryScreen();
        is.setUp(message,buyer,things);
        Game.getQuestapp().switchScreen(is);
        String line = is.getLine();
        try {
            if (line.equals("ESC")) {
                return 0;
            }
            if (line.equals("") || line.equals("all")) {
                return max;
            }
            int r = Integer.parseInt(line);
            r = Maths.middle(0,r,max);
            return r;
        } catch(Exception e) {
            Game.warn("Invalid number in Game.getNumber(...)");
        }
        return 0;
    }

	public static Thing selectItem(String message, Thing owner) {
		return selectItem(message,owner.getItems());
	}
	
	
	// animates a shot from (x1,y1) to (x2,y2)
	public void doShot(int x1, int y1, int x2, int y2, int c, double speed) {
		Animation a1=Animation.shot(x1,y1,x2,y2,c,speed);
		Animation a2=Animation.spark(x2,y2,c);
		
		getMappanel().addAnimation(Animation.sequence(a1,a2));
		getMappanel().repaint();
	}
	
	// animates a shot from (x1,y1) to (x2,y2)
	public void doBreath(int x1, int y1, int x2, int y2, int c, double speed) {
		for (int i=0; i<20; i++) {
			Animation a0=Animation.delay(i*50);
			Animation a1=Animation.spray(x1,y1,x2,y2,c,speed);
			getMappanel().addAnimation(Animation.sequence(a0,a1));
		}
		getMappanel().repaint();
	}
	
	// animates a shot from (x1,y1) to (x2,y2)
	public void doSpellShot(int x1, int y1, int x2, int y2, int c, double speed, int r) {
		Animation a1=Animation.shot(x1,y1,x2,y2,c,speed);
		Animation a2;
		if (r==0) {
			a2=Animation.spark(x2,y2,c);
		} else {
			a2=Animation.explosion(x2,y2,c,r);
		}
		getMappanel().addAnimation(Animation.sequence(a1,a2));
		getMappanel().repaint();
	}
	
	public void doDamageMark(int tx, int ty, int c) {
		getMappanel().addAnimation(Animation.hit(tx,ty,c));
	}

	// makes an explosion of the specified style and radius
	public void doExplosion(int x, int y, int c, int r) {
		if (r<=0) {
			doSpark(x,y,c);
			return;
		}
		getMappanel().addAnimation(Animation.explosion(x,y,c,r));	
	}

	public void doSpark(int x, int y, int c) {
		getMappanel().addAnimation(Animation.spark(x,y,c));
	}
	


	public static boolean saveMap(Map m) {
		try {
	        String filename = "map.xml";
	        FileDialog fd = new FileDialog(new Frame(),
	        		"Save Map", FileDialog.SAVE);
	        fd.setFile(filename);
	        fd.setVisible(true);
	       
	        if (fd.getFile() != null) {
	            filename = fd.getFile();
	        } else { 
	            // cancel
	            return false;
	        } 
	        
	        FileOutputStream f = new FileOutputStream(filename);
	        PrintWriter pw=new PrintWriter(f);
	        String mapXML=m.getLevelXML();
	        pw.write(mapXML);
	        Game.warn(mapXML);
	        pw.flush();
	        f.close();
	        
	        Game.message("Map saved - "+filename);
	        
	        return true;
		} catch (Exception e) {
			Game.message("Error encountered while saving the map");
            if (QuestApp.isapplet)
            {
			    Game.message("This may be due to your browser security restrictions");
			    Game.message("If so, run the web start or downloaded application version instead");
            }
			e.printStackTrace();
			return false;
		}
		
	}
	
	// save game to local ZIP file
	/** 
	* Save game to local ZIP file&#1102; 
	* @return <0, when the saving failed. 0, when user refused to save the  
	* game. >0, when the saving succeded. 
	*/ 
//	 save game to local ZIP file 
	public static int save() {
		try {
			String filename = "tyrant.sav";
			FileDialog fd = new FileDialog(new Frame(), "Save Game",
					FileDialog.SAVE);
			fd.setFile(filename);
			fd.setVisible(true);

			if (fd.getFile() != null) {
				filename = fd.getDirectory() + fd.getFile();
			} else {
				// return zero on cancel
				return 0;
			}

			FileOutputStream f = new FileOutputStream(filename);
			ZipOutputStream z = new ZipOutputStream(f);

			z.putNextEntry(new ZipEntry("data.xml"));

			if (!save(new ObjectOutputStream(z))) {
				throw new Error("Save game failed");
			} 
				
			Game.message("Game saved - " + filename);
			z.closeEntry();
			z.close();

			
            if (saveHasBeenCalledAlready)
			    Game.message("Please note that you can only restore the game with the same version of Tyrant (v"+VERSION+").");
            saveHasBeenCalledAlready = false;
		} catch (Exception e) {
			Game.message("Error while saving: "+e.toString());
            if (QuestApp.isapplet)
            {
			    Game.message("This may be due to your browser security restrictions");
			    Game.message("If so, run the web start or downloaded application version instead");
            }
			System.out.println(e);
			return -1;
		}
		return 1;
	} 

	public Map loadMap(String path) {
		try {
			InputStream inStream=getClass().getResourceAsStream(path);
			
			StringBuffer contents = new StringBuffer();
	        String line = null;
	        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
	        while((line = reader.readLine()) != null) {
	            contents.append(line);
	            contents.append(MapMaker.NL);
	        }
	        Map map = new mikera.tyrant.author.MapMaker().create(contents.toString(), false);
	        
	        return map;
		} catch (Throwable x) {
			x.printStackTrace();
			return null;
		}
	}

    /**
     * Tries to restore game from file specified in the argument.
     * @return null when the restoring was OK; otherwise string with description
     * of the problem.
     */
    public static String tryToRestore(final String filename) {
        String ret = null;
        try {
            FileInputStream f = new FileInputStream(filename);
			ZipInputStream z = new ZipInputStream(f);
			z.getNextEntry();
			if (!restore(new ObjectInputStream(z))) {
                ret = "Cannot load " + filename + " game file";
			}
			z.closeEntry();
			z.close();
        } catch (Exception e) {
            System.out.println(e);
			if (e.toString() == null)
                ret = "Unknown problem"; //I hope situation when "e.toString() == null"
                //is not possible; but just in case
            else
                ret = e.toString();
		}
        return ret;
    }

    // restore game from local zip file
	public static boolean restore() {
        String ret;
		try {
            String filename = "tyrant.sav";
            FileDialog fd = new FileDialog(new Frame(),
            		"Load Game", FileDialog.LOAD);
            fd.setFile(filename);
            fd.setVisible(true);

            if (fd.getFile() != null) {
                filename = fd.getDirectory() + fd.getFile();
            } else {
                // cancel
                return false;
            }

            ret = tryToRestore(filename);
            if (ret == null)
                return true;
		} catch (Exception e) {
            ret = e.toString();
			System.out.println(e);
		}
        Game.message("Load game failed: " + ret);
        if (QuestApp.isapplet) {
            Game.message("This may be due to browser security restrictions");
            Game.message("If so, run the downloaded application version instead");
        }
		return false;
	}

	// creates a pseudo-random number based on:
	//  1. The seed vale
	//  2. The hero instance
	//  3. The max value
	public static int hash(int seed, int max) {
		return (seed ^ hero().getStat("Seed")) % max;
	}

	/**
	 * Perform important initialisation of static fields
	 * 
	 * @param h
	 */
	public void initialize(Thing h) {
		hero=h;
		
		if (h==null) throw new Error("Null hero in Game.initialize()");
		
		Lib library=(Lib)get("Library");
		if (library!=null) {
			Lib.setInstance(library);
		} else {
			library=Lib.instance();
			if (library==null) {
				throw new Error("No library in Game.initialize()");
			}
			set("Library",library);
		}
		
				
	}
	
	
	public static void create() {
		instance=new Game();
	}
	
	
	public static boolean save(ObjectOutputStream o) {
		try {
			Game g=Game.instance();
			g.compressAllData();
			
			o.writeObject(Game.instance());
			o.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


	public static boolean restore(ObjectInputStream i) {
		try {
			Lib.clear();
			
			instance = (Game)i.readObject();
			
			// do post-load initialisation of data structures
			instance.initialize(instance.hero);
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	// called when something is kiled or destroyed
	public static void registerDeath(Thing t) {
		Being.registerKill(Game.actor,t);
	}

	/**
	 * @param hero The hero to set.
	 */
	public void setHero(Thing hero) {
		Game.instance().hero = hero;
		hero.set("Game",instance());
	}

	/**
	 * @return Returns the hero.
	 */
	public static Thing hero() {
		return instance().hero;
	}

    public char getCharacter() {
        KeyEvent k = getInput();    
        return  GameScreen.getKey(k);
    }
    
    public void setInputHandler(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    public boolean lineOfSightDisabled() {
        return lineOfSightDisabled;
    }
    
    public void isLineOfSightDisabled(boolean los) {
        lineOfSightDisabled = los;
    }

	/**
	 * @return Returns the current mappanel.
	 */
	public static MapPanel getMappanel() {
		QuestApp q=Game.getQuestapp();
		if (q==null) return null;
		GameScreen gs=q.getScreen();
		if (gs==null) return null;
		return gs.getMappanel();
	}

	/**
	 * @param debug The debug to set.
	 */
	public static void setDebug(boolean debug) {
		Game.instance().debug = debug;
	}

	/**
	 * @return Returns the debug.
	 */
	public static boolean isDebug() {
		return Game.instance().debug;
	}

	/**
	 * @param questapp The questapp to set.
	 */
	public static void setQuestapp(QuestApp questapp) {
		QuestApp.setInstance(questapp);
	}

	/**
	 * @return Returns the questapp.
	 */
	public static QuestApp getQuestapp() {
		return QuestApp.getInstance();
	}

	/**
	 * @param userinterface The userinterface to set.
	 */
	public static void setUserinterface(Interface userinterface) {
		Game.userinterface = userinterface;
	}

	/**
	 * @return Returns the userinterface.
	 */
	public static Interface getUserinterface() {
		return userinterface;
	}

    public void setDesignerMode(boolean isDesigner) {
        this.isDesigner = isDesigner; 
    }
    
    public boolean isDesigner() {
        return isDesigner;
    }
    
    public static void asynchronousCreateLib() {
		new Thread(new Runnable() {
			public void run() {
				Lib.instance();
			}
		}).start();
	}

	static {
    	loadVersionNumber();
    	
    }
    
}