package mikera.tyrant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mikera.tyrant.engine.BaseObject;
import mikera.tyrant.author.MapMaker;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Point;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.Count;
import mikera.tyrant.util.Text;
import mikera.util.Rand;

public class GameScreen extends Screen {
 	private static final long serialVersionUID = 3907207143852421428L;

	private MapPanel mappanel;
	public MessagePanel messagepanel;
	public StatusPanel statuspanel;
    protected GameHandler gameHandler = new GameHandler();
	public Map map;

    private InventoryScreen inventoryScreen;
    private List<IActionHandler> actionHandlers;
    
    public void addActionHandler(IActionHandler actionHandler) {
        if(actionHandlers == null) actionHandlers = new LinkedList<>();
        actionHandlers.add(actionHandler);
    }
    
    public void setGameHandler(GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }
	
    public GameHandler getGameHandler() {
        return gameHandler;
    }
    
	// this is the main game loop
	// it catches any exceptions for stability
	// and lets the game continue
	//
	// very important that endTurn() gets called after the player moves
	// this ensures that the rest of the map stays up to date
	//
	// use Game.hero here - it might change due to save/load
	public void mainLoop() {
		map=Game.hero().getMap();
		endTurn();

		Game.over=false;
        KeyEvent k = null;
        Action action = null;
        
        while (!Game.over) {
        	try {
        		// get the hero each turn - this could change!
        		Thing h=Game.hero();
        		
        		if(!Game.instance().lineOfSightDisabled())
                    getMappanel().viewPosition(map, h.x, h.y);
                k = getUserInput(k);
                action = convertEventToAction(k);
                if(tryTick(h, action, k.isShiftDown())) {
                    ensureHerosNotDead();
	            }
	        } catch (Throwable e) {
	            e.printStackTrace();
	        }
		}
		questapp.gameOver();
	}

    /**
     * Answer true if the action is executed, false otherwise.
     */
    public boolean tryTick(Thing thing, Action action, boolean isShiftDown) {
        if (action == null) return false;
        performAction(thing, action, isShiftDown);

        endTurn();
        return true;
    }

    private void ensureHerosNotDead() {
//      ensure we are back on the GameScreen
        questapp.switchScreen(this);
        // has the hero died?
        if ((Game.hero().place==null)||Game.hero().getStat("HPS") <= 0) {
            Game.message("You have died.....");
            Game.over=true;
        }
    }

    public Action convertEventToAction(KeyEvent keyEvent) {
        if(rejectEvent(keyEvent)) return null;
        return gameHandler.actionFor(keyEvent);
    }

    private KeyEvent getUserInput(KeyEvent keyEvent) {
        Game.instance().clearMessageList();
        // get key, or repeat if running
        if(Game.hero().isRunning()) return keyEvent;
        return Game.getInput();
    }
    
	public GameScreen(QuestApp q) {
		super(q);

		if (q==null) return;
		q.setScreen(this);
		
		setForeground(Color.white);
		setBackground(Color.black);

		//set the default layout for the main screen
		setLayout(new BorderLayout());

		// Add the message panel
		messagepanel = new MessagePanel(q);
		add(messagepanel, "South");

		mappanel=new MapPanel(this);
		add(mappanel, "Center");
		
		// status panel and map container
		Panel cp=new Panel();
		cp.setLayout(new BorderLayout());
		add("East", cp);
		
		statuspanel = new StatusPanel();
		cp.add("Center", statuspanel);

		levelMap = new LevelMapPanel();
		cp.add(levelMap, "South");
		
		// create our world!!
		/*
		 * Map m=new Town(51,51); enterMap(m,40,20); Map f=new
		 * DeepForest(71,71); m.entrance=new Portal(0);
		 * m.entrance.setDestination(f,f.entrance.x,f.entrance.y);
		 * f.entrance.setDestination(m,40,24);
		 */


		setFont(QuestApp.mainfont);
	}

    public void performAction(Thing thing, Action action, boolean isShiftDown) {
        Game.actor = thing;
        Game.message("");
		
        if(actionHandlers != null) {
            for (Iterator<IActionHandler> iter = actionHandlers.iterator(); iter.hasNext();) {
                IActionHandler actionHandler = iter.next();
                if(actionHandler.handleAction(thing, action, isShiftDown)) return;
            }
        }
        
        // movement
        if (action == Action.WAIT) doWait(thing);
        else if (action.isMovementKey()) gameHandler.doDirection(thing, action, isShiftDown);
        else if (action == Action.EXIT) doExit(thing);

        // information
        else if (action == Action.HELP) doHelp();
        else if (action == Action.SHOW_QUESTS) doShowQuests();
        else if (action == Action.INVENTORY) doInventory(thing);
        else if (action == Action.MESSAGES) doMessages();
        else if (action == Action.VIEW_STATS) doViewStats(thing);

        // display
        else if (action == Action.ZOOM_OUT) doZoom(25);
        else if (action == Action.ZOOM_IN) doZoom(-25);  

        
        // save and load games
        else if (action == Action.SAVE_GAME) {
            if (Game.save() < 0) { 
                Game.message("Save game failed."); 
            } 
        }
        
        // quit , TODO:need to test how this works in applet mode
        else if ( action == Action.QUIT_GAME ){
          System.exit(0);
        }

        else if (action == Action.LOAD_GAME) {
            Game.restore();
            return;
        }
        else if (action == Action.DEBUG) doDebugKeys(thing);

        // dom't perform game actions if on world map
        else if (thing.getMap().getFlag("IsWorldMap")) {
        	thing.message("You must enter an area by pressing \"x\" first");
        	return;
        }
        
//       game actions
        else if (action == Action.APPLY_SKILL) doApplySkill(thing);
        else if (action == Action.CHAT) doChat(thing);
        else if (action == Action.DROP) doDrop(thing,false);
        else if (action == Action.DROP_EXTENDED) doDrop(thing,true);
        else if (action == Action.EAT) doEat(thing);
        else if (action == Action.FIRE) doFire(thing);
        else if (action == Action.GIVE) doGive(thing);
        else if (action == Action.JUMP) doJump(thing);
        else if (action == Action.KICK) doKick(thing);
        else if (action == Action.LOOK) doLook();
        else if (action == Action.OPEN) doOpen(thing);
        else if (action == Action.PICKUP) doPickup(thing,false);
        else if (action == Action.PICKUP_EXTENDED) doPickup(thing,true);
        else if (action == Action.QUAFF) doQuaff(thing);
        else if (action == Action.READ) doRead(thing);
        else if (action == Action.SEARCH) doSearch(thing);
        else if (action == Action.THROW) doThrow(thing);
        else if (action == Action.USE) doUse(thing);
        else if (action == Action.WIELD) doWield(thing);
        else if (action == Action.PRAY || action == Action.PRAY2) doPray(thing, action);
        else if (action == Action.ZAP) doZap(thing, true);
        else if (action == Action.ZAP_AGAIN) doZap(thing, false);
        else if (Game.isDebug() && action == Action.SELECT_TILE) {
            Thing t = Game.selectItem("Select a tile:", Tile.tiles);
            if (t!=null) {
                getMappanel().currentTile=t.getStat("TileValue");
            }
        }


    }

     /*
      * BUG Fix for
      * http://sourceforge.net/tracker/index.php?func=detail&aid=1088187&group_id=16696&atid=116696
      * Ignore Alt keypresses, we may need to add more of these for other
      * platforms.
      */
    private boolean rejectEvent(KeyEvent keyEvent) {
        return (keyEvent.getModifiers() | InputEvent.ALT_DOWN_MASK) > 0 && keyEvent.getKeyCode() == 18;
    }
   

    public InventoryScreen getInventoryScreen() {
        if(inventoryScreen == null) {
            inventoryScreen = new InventoryScreen(); 
        }
        return inventoryScreen;
    }

	public Point getSpellTargetLocation(Thing h, Thing s) {
		Map map=h.getMap();
		Thing f = (s.getStat("SpellUsage") == Spell.SPELL_OFFENCE)
		? map.findNearestFoe(h)
		: null;
		if ((f != null) && (!map.isVisible(f.x, f.y))) f = null;
		
		if (f==null) {
			if (s.getStat("SpellUsage") == Spell.SPELL_OFFENCE) {
				// aim at square in front of caster
				Point sp=new Point(h.x+h.getStat("DirectionX"),h.y+h.getStat("DirectionY"));
				return getTargetLocation(map,sp);
			}
			return getTargetLocation(h);
		}
        return getTargetLocation(f);
	}
	
	public Point getTargetLocation() {
		return getTargetLocation(Game.hero());
	}
	
	public Point getTargetLocation(Thing a) {
		if (a==null) a=Game.hero();
		return getTargetLocation(a.getMap(),new Point(a.x,a.y));
	}

	
	// get location, initially place crosshairs at start
	public Point getTargetLocation(Map m,Point start) {
		if (start == null) return getTargetLocation();
		
		getMappanel().setCursor(start.x, start.y);
		getMappanel().viewPosition(m,start.x, start.y);
		
		// initial look
		doLookPoint( new Point( getMappanel().curx, getMappanel().cury ) );
		
		//get interesting stuff to see
		//Note that the hero is incidentally also seen
		//So there should be no worries of an empty list
		List<Point> stuff = map.findStuff( Game.hero() , Map.FILTER_ITEM + Map.FILTER_MONSTER );
		int stuffIndex = 0;
		
		// repaint the status panel
		statuspanel.repaint();
        //TODO : get 'x' and 'l' working
		while (true) {
			KeyEvent e = Game.getInput();
			if (e == null)
				continue;

			char k = Character.toLowerCase(e.getKeyChar());

			int i = e.getKeyCode();

			// handle key conversions
			switch(i) {
				case KeyEvent.VK_UP:
					k = '8'; break;
				case KeyEvent.VK_DOWN:
					k = '2'; break;
				case KeyEvent.VK_LEFT:
					k = '4'; break;
				case KeyEvent.VK_RIGHT:
					k = '6'; break;
				case KeyEvent.VK_HOME:
					k = '7'; break;
				case KeyEvent.VK_END:
					k = '1'; break;
				case KeyEvent.VK_PAGE_UP:
					k = '9'; break;
				case KeyEvent.VK_PAGE_DOWN:
					k = '3'; break;
				case KeyEvent.VK_ESCAPE:
					k = 'q'; break;
			}
			
			int dx = 0;
			int dy = 0;
			switch(k) {
				case '*':
				case 'x':
				case 'l':
					if( stuffIndex >= stuff.size()) stuffIndex = 0;
					Point p = stuff.get( stuffIndex );
					stuffIndex++;
					if( p == null ) p = start;
					dx = p.x - getMappanel().curx;
					dy = p.y - getMappanel().cury;
					break;
				case '8': 
					dx = 0;
					dy = -1;
					break;
				case '2':
					dx = 0;
					dy = 1;
					break;
				case '4':
					dx = -1;
					dy = 0;
					break;
				case '6':
					dx = 1;
					dy = 0;
					break;
				case '7':
					dx = -1;
					dy = -1;
					break;
				case '9':
					dx = 1;
					dy = -1;
					break;
				case '1':
					dx = -1;
					dy = 1;
					break;
				case '3':
					dx = 1;
					dy = 1;
					break;
				case 'q':
					getMappanel().clearCursor();
					return null;
				default:
					getMappanel().clearCursor();
					return new Point(getMappanel().curx, getMappanel().cury);		
			}
			getMappanel().setCursor(getMappanel().curx + dx, getMappanel().cury + dy);
			getMappanel().viewPosition(m,getMappanel().curx, getMappanel().cury);
			doLookPoint( new Point( getMappanel().curx, getMappanel().cury ) );
		}
	}

	public void castSpell(Thing h, Thing s) {
		if (s == null)
			return;
		
		Map map=h.getMap();

		switch (s.getStat("SpellTarget")) {
			case Spell.TARGET_SELF :
				Spell.castAtSelf(s, h);
				break;
			case Spell.TARGET_DIRECTION : {	
				Game.message("Select Direction:");
				Point p=Game.getDirection();
				if (p!=null) Spell.castInDirection(s, h,p.x,p.y);
				break;
			}
			case Spell.TARGET_LOCATION :
				Thing f = (s.getStat("SpellUsage") == Spell.SPELL_OFFENCE)
						? map.findNearestFoe(h)
						: null;
				if ((f != null) && (!map.isVisible(f.x, f.y)))
					f = null;
				Point p = getSpellTargetLocation(h,s);
				//Do not confuse player with possible false info
				Game.message("");
				if (p != null) {
					// don't fire offensive spell at self by accident
					if ((p.x == h.x) && (p.y == h.y)
							&& (s.getStat("SpellUsage") == Spell.SPELL_OFFENCE)) {
						Game
								.message("Are you sure you want to target yourself? (y/n)");
						char opt = Game.getOption("yn");
						if (opt == 'n')
							break;
					}

					if (map.isVisible(p.x, p.y)) {
						Spell.castAtLocation(s, h, map, p.x, p.y);
					} else {
						Game.message("You cannot see to focus your power");
					}
				}
				break;
			case Spell.TARGET_ITEM :
				Thing t = Game.selectItem("Select an item:",h.getItems());
				questapp.switchScreen(this);
				if (t != null) {
					Spell.castAtObject(s, h, t);
				}
				break;
		}
	}

	
	public void endTurn() {
		// make sure hero is not responsible for these actions
		Game.actor=null;
		
		Thing h=Game.hero();
		
		while (h.getStat("APS")<0) {
			int apsElapsed = -h.getStat("APS");

			// advance game time and perform all actions
			int timeElapsed=apsElapsed*100/h.getStat("Speed");
			Time.advance(timeElapsed);
			
			h.incStat("APS",apsElapsed);
		}

		// special calculation for encumberance
		Being.calcEncumberance(h);
		
		// other updates
		h.set("IsFrozen",0);

		if (h.getMap() != map) {
			// Game.warn("Map switch");
			map = h.getMap();
			if (map == null)
				return;
			Game.enterMap(map, h.x, h.y);
		}
		
		gameHandler.calculateVision(h);
		
        if (levelMap != null) {
            levelMap.repaint();
            statuspanel.repaint();
        }
	}
	
	public static char getKey(KeyEvent e) {
		char k = Character.toLowerCase(e.getKeyChar());

		// handle key conversions
		if (e.getKeyCode() == KeyEvent.VK_UP)
			k = '8';
		if (e.getKeyCode() == KeyEvent.VK_DOWN)
			k = '2';
		if (e.getKeyCode() == KeyEvent.VK_LEFT)
			k = '4';
		if (e.getKeyCode() == KeyEvent.VK_RIGHT)
			k = '6';
		if (e.getKeyCode() == KeyEvent.VK_HOME)
			k = '7';
		if (e.getKeyCode() == KeyEvent.VK_END)
			k = '1';
		if (e.getKeyCode() == KeyEvent.VK_PAGE_UP)
			k = '9';
		if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)
			k = '3';
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			k = 'Q';
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
			k = 'Q';
		if (e.getKeyCode() == KeyEvent.VK_F1)
			k = '?';
		if (e.getKeyCode() == KeyEvent.VK_F2)
			k = '(';
		if (e.getKeyCode() == KeyEvent.VK_F3)
			k = ')';
		if (e.getKeyCode() == KeyEvent.VK_F4)
			k = '*';
		if (e.getKeyCode() == KeyEvent.VK_F5)
			k = ':';
		if (e.getKeyCode() == KeyEvent.VK_TAB)
			k = '\t';
		return k;
	}

	public void doZoom(int factor) {
		getMappanel().zoomfactor += factor;
		if (getMappanel().zoomfactor < 25)
			getMappanel().zoomfactor = 25;
		if (getMappanel().zoomfactor>800) getMappanel().zoomfactor=800;
		Game.warn("Zooming.... (" + getMappanel().zoomfactor + "%)");
		getMappanel().repaint();
	}

	private LevelMapPanel levelMap=null;
	
	 //TODO : as in Angband, the wizard commands should really be in a separate class
	 //Docu : keys taken : 
	 //a : create artefact
	 //c : teleport
	 //d : generate dungeon with specific dna
	 //l : magic look
	 //x : load map from file
	 //p : portals
	 //q : quit
	 //t : change tile player is standing one
	 //g : load existing map
	 //s : save
	 //m : show report on spells
	 //z : turn of debug mode
	 //y : create random object ?
	 //b : do random thing to help detect bugs ;)
	
    private void doDebugKeys(Thing h) {
		if (!Game.isDebug())
			return;
		String command = Game.getLine("What is your bidding? ");
		if (command.equals("")||command.equals("ESC")) {
			Game.message("");
			return;
		}
		char ch = command.charAt(0);
		command = command.substring(1);
		
		if( ch == 'b' ){
		  Thing nt = Lib.create( "potion of ethereality", 100 );
		  map.addThing(nt, h.x , h.y );
		  
		}

		if (ch == 'a') {
			try { // create an artifact
				int n = Integer.parseInt(command);
				map.addThing(Lib.createArtifact(n), h.x - 1, h.y - 1,
						h.x + 1, h.y + 1);
			} catch (Exception exception) {
				return;
			}
		}

		if (ch == 'c') {
			Point p = getTargetLocation();
			Thing ts=map.getFlaggedObject(p.x,p.y,"IsMobile");
			if (ts!=null) Game.instance().setHero(ts);
		}
		
		if (ch == 'd') {
			String s=Game.getLine("Enter dungeon DNA: ");

			Map map=new Map(50,50);
			map.setTheme(Dungeon.selectTheme(h.getLevel()));
			map.set("DungeonDNA",s);
			Dungeon.makeDungeon(map,h.getLevel());
			
			map.addThing(h);
			Wish.makeWish("map",100);
		}
		
		if ((ch == 'l')) {
			Game.message("Magic Look: select location");
			Point p = getTargetLocation();
			if (p != null) {
				for (Thing t = map.getObjects(p.x, p.y); t != null; t = t.next) {
					doMagicLook(t);
				}
				questapp.switchScreen(this);
			}
		}
		
		if ((ch == 'x')) {
			Game.message("Load map: ");
            MapMaker mapMaker = new MapMaker();
            Map newMap = mapMaker.promptAndLoad();
            if(newMap == null) return;
            Thing stairsUp = Portal.create("stairs up");
            newMap.set("Description", "bob");
            // Thing stairsDown = Portal.create("stairs down");
            if(newMap.containsKey("EntranceX")) {
                int entranceX = newMap.getStat("EntranceX");
                int entranceY = newMap.getStat("EntranceY");
                newMap.addThing(stairsUp, entranceX, entranceY);
                newMap.setEntrance(stairsUp);
            }
            newMap.addThing(h);
		}
		
		if (ch == 'p') {
			Thing[] portals = map.getObjects(0, 0, map.getWidth() - 1,
					map.getHeight() - 1, "IsPortal");
			if (portals.length > 0) {
				int r = Rand.r(portals.length);
				map.addThing(h, portals[r].x, portals[r].y);
			}
		}

		if (ch == 'q') {
			Game.message("Are you sure you want to quit this game (y/n)");
			if (Game.getOption("yn") == 'y')
				h.set("HPS", -10);
		}		
		
		if (ch == 't') {
			String s=Game.getLine("Enter tile number: ");
			getMappanel().currentTile=Tile.tiles[Integer.parseInt(s)].getStat("TileValue");
		}	
		
		if (ch == 'g') {
			String s=Game.getLine("Enter target map: ");
			String[] ss=s.split(":");
			String complex=ss[0];
			int lev=(ss.length==2)?Integer.parseInt(ss[1]):1;
			
			Thing p=Portal.create("stairs down");
			p.set("ComplexName",complex);
			p.set("DestinationLevel",lev);
			map.addThing(p,h.x,h.y);
			Portal.travel(p,h);
		}	
		
		if (ch == 's') {
			Game.saveMap(map);
		}		
		
		if (ch == 'm') {
			Game.showData(Spell.spellReport());
		}	
        
        if (ch == 'z') {
            BaseObject.GET_SET_DEBUG = !BaseObject.GET_SET_DEBUG;
            Game.message("BaseObject.GET_SET_DEBUG = " + BaseObject.GET_SET_DEBUG);
        }
        
        if (ch == 'y') {
        	java.util.HashMap<String, Count> gc=BaseObject.getCounter;
        	Thing rt=new Thing(new BaseObject(gc,null));
        	Game.showData(rt.reportByValue());
        }
	}

	public void doWait(Thing h) {
		h.incStat("APS", -50);
	}

	private String lastSkill=null;
	
	public void doApplySkill(Thing h) {
		ArrayList<String> al=Skill.getList(h);
		if (lastSkill!=null) {
			al.remove(lastSkill);
			al.add(0,lastSkill);
		}
		String[] sks = al.toArray(new String[al.size()]);
		String a = Game.selectString("Your skills",sks);
		
		if (a != null) {
			lastSkill=a;
			a=Skill.trim(a);
						
			if (a.equals(Skill.THROWING)) {
				doThrow(h);
			} else if (a.equals(Skill.ARCHERY)) {
				doFire(h);
			} else if (a.equals(Skill.CASTING)) {
				doZap(h,true);
			} else {
				Skill.apply(h,a);
			}
		}
	}
	
	public void doIntroduction( Thing person ){
	  String intro = (String)person.get("Introduction");
	  if(intro!=null){
	    Game.message( intro );
	    person.set("Introduction" , null);
	  }		 
	}

	public void doChat(Thing h) {
		Thing person = null;
		if (map.countNearby("IsIntelligent",h.x, h.y, 1) > 2) {
			Game.message("Chat: select direction");
			Point p = Game.getDirection();
			person = map
					.getFlaggedObject(h.x + p.x, h.y + p.y, "IsIntelligent");
		} else {
			try {
				person = map.getNearby("IsIntelligent",h.x, h.y, 1);
			} catch (Exception anyex) {
				anyex.printStackTrace();
			}
		}

		if (person == null || person.equals(Game.hero())){
		  if(!doOpen( h )){
		    Game.message("You mumble to yourself");
		  }
			return;
		}	
		if (AI.isHostile(person,h)) {
			Game.message(person.getTheName()+" is attacking you!");
			return;
		}
		
		if (person.handles("OnChat")) {
      doIntroduction( person );
			Event e=new Event("Chat");
			e.set("Target",h);
			person.handle(e);
		} else if (person.equals(Game.hero())) {
			Game.message("You mumble to yourself");
		} else if (person.getFlag("IsIntelligent")) {
		  doIntroduction( person );
		  Game.message("You chat with " + person.getTheName() 	+ " for some time");

		} else {
			Game.message("You can't talk to " + person.getTheName());
		}
		h.incStat("APS", -200);

	}

	public void doDrop(Thing h, boolean ext) {
		if (ext) {
			Thing o = Game.selectItem("Select item to drop:", h.getItems());
			while (o != null) {
				Being.tryDrop(h,o);
				Thing[] its=h.getItems();
				if (its.length==0) break;
				o = Game.selectItem("Select item to drop:", its,true);
			}	
		} else {
			Thing o = Game.selectItem("Select item to drop:", h.getItems());
			if (o != null) {
				Being.tryDrop(h,o);
			}
		}
	}

	public void doEat(Thing h) {
		Thing o = Game.selectItem("Select item to eat:", h
				.getFlaggedContents("IsEdible"));
		questapp.switchScreen(this);
		if (o != null)
			Food.eat(h, o);
	}

	public void doFire(Thing h) {
		Thing w = h.getWielded(RPG.WT_RANGEDWEAPON);
		Thing m=h.getWielded(RPG.WT_MISSILE);
		
		if ((w==null)&&(m!=null)&&m.getFlag("IsThrowingWeapon")) {
			doThrow(h,m);
			return;
		}
		
		if (w == null) {
			Thing[] rws = h.getFlaggedContents("IsRangedWeapon");
			if (rws.length > 0) {
				w = Game.selectItem("Select a ranged weapon:", rws);
			}
		}

		if (w != null) {
			RangedWeapon.useRangedWeapon(w, h);
		} else {
			if (m!=null) {
				doThrow(h,m);
			} else {
				Game.message("You must first find an appropriate missile weapon");
			}

		}
	}

	public void doGive(Thing h) {

		// select mobile to give to
		Thing mobile = null;
		if (map.countNearby("IsGiftReceiver", h.x, h.y, 1) > 1) {
			Game.message("Give: select direction");
			Point p = Game.getDirection();
			mobile = map.getFlaggedObject(h.x + p.x, h.y + p.y, "IsGiftReceiver");
		} else {
			mobile = map.getNearby("IsGiftReceiver",h.x, h.y, 1);
		}

		if ((mobile != null) && (!h.isHostile(mobile))) {
			Thing gift = Game.selectItem("Select item to give:", h
					.getItems());
			if (gift != null) {

				// can't give a cursed item
				if ((gift.y > 0) && (!h.clearUsage(gift.y)))
					return;

				int total=gift.getStat("Number");
				if (total > 1) {
					int n=Game.getNumber("Give how many (Enter=All)? ",total);
					if (n>0) {
						gift = gift.separate(n);
					}
				}				
				
				mobile.give(h, gift);
				
				// restack nicely if not taken
				if (gift.place==h) gift.restack();
				
				h.incStat("APS", -100); // make it quick
			}
		} else {
			Game.message("There is nobody to receive your gift");
		}

	}

	private void doInventory(Thing h) {
		Thing t=Game.selectItem("Your inventory:  (choose a letter to inspect an item)",h.getItems());
		
		if (t!=null) {
			Game.infoScreen(Item.inspect(t));
		}
	}
	
	private void doJump(Thing h) {
		Game.message("Jump: select location");
		Point p = getTargetLocation();
		if (p != null) {		
			Movement.jump(h,p.x,p.y);
		
		}
	}

	private void doKick(Thing h) {
		Game.message("Kick: select direction");
		Point p = Game.getDirection();
		if ((p != null) && ((p.x != 0) || (p.y != 0))) {
			messagepanel.clear();
			Combat.kick(h,p.x, p.y);
		} else {
			Game.message("");
		}
	}

	private void doLook() {
		Game.message("Look: select location");
		Point p = getTargetLocation();
		doLookPoint( p );
	}
	
	//You can pass null Points ( pun intended )
	//to this method
	private void doLookPoint( Point p ){
		if (p != null) {
			messagepanel.clear();
			Game.message((Game.isDebug()) ? ("You see: " + (map.getTile(p.x,
					p.y) & 65535)) : "You see:");
			if (map.isVisible(p.x, p.y)) {
				Thing t = map.getObjects(p.x, p.y);
				while ((t != null) && t.isVisible(Game.hero())) {
					String st = "";
					st = st + t.getDescription().getDescriptionText();
					if (t.getFlag("IsBeing")) {
						st = st + " (" + Text.capitalise(Damage.describeState( t )) + " damage)";
					}
					if (t.isHostile(Game.hero())) {
						st = st + " (Hostile) ";
					}
					Game.message(st);
					t = t.next;
				}
			}
		}		
	}

	private void doMagicLook(Thing t) {
		char c=Game.showData(t.report());
		
		if (c=='e') doEdit(t);
	}
	
	private void doEdit(Thing t) {
		String s=Game.getLine("Edit property for "+t.getName(Game.hero())+":");
		
		if (s.equals("")||s.equals("ESC")) {
			Game.message("");
			return;
		}
		
		String[] ss=s.split("=");
		try {
			int val=Integer.parseInt(ss[1]);
			t.set(ss[0],val);
		} catch (Exception x) {
			t.set(ss[0],ss[1]);
		}
	}
	
	private void doMessages() {
		//TODO add this as method to Text clas ?
		String[] st = Text.separateString( Game.instance().messageList(), '\n');
        String wrappedText = "";	
        //TextZone is the expert on the wrapping length of the line
        //and it uses the Graphics context for that, we know that at
        //this stage of the game, this variable has been set...
        int wrapLength = TextZone.linelength==0?80:TextZone.linelength;
		for (int i = 0; i < st.length; i++) {
			String[] lines = Text.wrapString( st[i], wrapLength );
			for (int l = 0; l < lines.length; l++) {
				wrappedText = wrappedText.concat( lines[l] ).concat("\n"); 
			}
		} 
		Game.scrollTextScreen(wrappedText,true);
	}
	
	//Open/close ( toggle ) openable things such as
	//doors, chests, portcullises etc.
	//returns a boolean, because really, if you try to 'c'hat
	//with a door, you might just as well open/close it ;)
	private boolean doOpen(Thing h) {
	  Thing t = null;
		if (map.countNearby("IsOpenable",h.x, h.y, 1) > 2) {	  
		  Game.message("Select direction");
		  Point p = Game.getDirection();
		  if ((p != null) && ((p.x != 0) || (p.y != 0))) {
			  messagepanel.clear();
			  t = map.getFlaggedObject(h.x + p.x, h.y + p.y, "IsOpenable");
		  }
		}else{
			try {
				t = map.getNearby("IsOpenable",h.x, h.y, 1);
			} catch (Exception anyex) {
				anyex.printStackTrace();
			}
		}  
  	if (t != null) {
				Door.useDoor(h, t);
				h.incStat("APS", -Being.actionCost(h));
				return true;
  	}
  	return false;
	}

	private void doPickup(Thing h, boolean ext) {
		
		if (h.getInventoryWeight() >= Being.maxCarryingWeight(h)) {
			Game.message("You cannot carry any more!");
			return;
		}
		
		Thing[] th = map.getObjects(h.x, h.y, h.x, h.y, "IsItem");
		boolean all=false;
		
		if (ext&&(th.length>1)) {
			while (th.length>0) {
				Thing t=Game.selectItem("Select items to pick up:",th);
				if (t==null) {
					break;
				}
				
				Being.tryPickup(h,t);
				th = map.getObjects(h.x, h.y, h.x, h.y, "IsItem");
			}
		} else {
			for (int i = 0; i < th.length; i++) {
				Thing t = th[i];
	
				if ((!all)&&(th.length > 1)) {
                    char c;
					if (i == (th.length - 1)) {
                        //-we are about to select last item
                        Game.message("Pick up " + th[i].getTheName() + "? (Y/N)");
                        c = Game.getOption("yn,p");
                    } else
                    {
                        Game.message("Pick up " + th[i].getTheName() + "? (Y/N/All)");
                        c = Game.getOption("yn,pa");
                    }
					if (c=='n') continue;
					if (c=='q') {
						Game.message("");
						break;
					}
					if (c=='a') all=true;
				}
	
				Being.tryPickup(h,t);
			}
		}

	}

	/**
	 * Called when the player presses "q" to quaff a potion
	 * 
	 * @param h The hero
	 */
	private void doQuaff(Thing h) {
		Thing o = Game.selectItem("Select item to quaff:", h
				.getFlaggedContents("IsDrinkable"));
		questapp.switchScreen(this);
		if (o != null) {
			Potion.drink(h, o);
		}

	}

	private void doRead(Thing h) {
		int skill=h.getStat(Skill.LITERACY);
		if (skill>0) {
			Thing o = Game.selectItem("Select item to read:", h
					.getFlaggedContents("IsReadable"));
			questapp.switchScreen(this);
			if (o != null) {
				// cost depends on reading ability
				h.incStat("APS", -600/skill);
	
				o=o.separate(1);
				h.message("You read "+o.getTheName());
				if (o.handles("OnRead")) {
					Event e=new Event("Read");
					e.set("Reader",h);
					e.set("Target",h);
					o.handle(e);
				}		
				if (o.place==h) o.restack();
			}
		} else {
			h.message("You can't read!");
		}
	}

	private void doSearch(Thing h) {
		Game.message("Searching...");
		h.incStat("APS",- 200);
		Secret.searchAround();
	}

	private void doThrow(Thing h) {
		Thing o = Game.selectItem("Throw Item:", h.getItems());
		questapp.switchScreen(this);
		doThrow(h,o);
	}

		
	private void doThrow(Thing h, Thing o) {
		if (o != null) {
			// get wield position
			int wt=o.y;
			
			o=o.separate(1);
			

			// can't throw cursed worn item
			if ((o.y > 0) && (!h.clearUsage(o.y)))
				return;


			
			// get initial target
			Thing f = map.findNearestFoe(h);
			if ((f != null) && (!map.isVisible(f.x, f.y)))
				f = null;
			// get user target selection
			Point p = getTargetLocation(f);

			if (p != null) {
				o = o.remove(1);
				Being.throwThing(h, o, p.x, p.y);
			}
			
			if (o.place==h) {
				o=o.restack();
				h.setUsage(o,wt);
			}
			
		}
	}

	private void doUse(Thing h) {
		Thing o = Game.selectItem("Use Item:", h
				.getUsableContents());
		questapp.switchScreen(this);
		if (o != null) {
			o=o.separate(1);
			Item.use(h, o);
			o.restack();
		}
	}
	
	/**
	 * Caled when the player presses "v" to view stats
	 * 
	 * @param h
	 */
	private void doViewStats(Thing h) {
		CharacterScreen ls = new CharacterScreen(h);
		questapp.switchScreen(ls);
		Game.getInput();
		questapp.switchScreen(this);
	}	
	
	private void doWield(Thing h) {
		Thing o = Game.selectItem("Wield/wear which item?", h
				.getWieldableContents());
		
		if (o != null) {
			Thing item = o;
			boolean wielded=false;
			final int wt = item.getStat("WieldType");
			
			if (item.y==wt) {
				if (h.clearUsage(wt)) {
					Game.message("You are no longer using "+item.getTheName());
				} 
				return;
			}

			if ((wt == RPG.WT_LEFTRING) || (wt == RPG.WT_RIGHTRING)) {
				Game.message("Which finger? (r/l)");
				char c = Game.getOption("lr");
				if (c == 'r') {
					if (h.wield(item, RPG.WT_RIGHTRING)) {
						Game.message("You put " + item.getTheName()
								+ " on your right finger");
						wielded=true;
					}
				} else if (c == 'l') {
					if (h.wield(item, RPG.WT_LEFTRING)) {
						Game.message("You put " + item.getTheName()
								+ " on your left finger");
						wielded=true;
					}
				} else
					messagepanel.clear();
			}

			else if ((wt == RPG.WT_MAINHAND) || (wt == RPG.WT_SECONDHAND)) {
				Game.message("Which hand? (r/l) [" + h.inHandMessage() + "]");
				char c = Game.getOption("lr");
				if (c == 'r') {
					if (h.wield(item, RPG.WT_MAINHAND)) {
						Game.message("You wield " + item.getTheName()
								+ " in your right hand");
						wielded=true;
					}
				} else if (c == 'l') {
					if (h.wield(item, RPG.WT_SECONDHAND)) {
						Game.message("You wield " + item.getTheName()
								+ " in your left hand");
						wielded=true;
					}
				} else
					messagepanel.clear();
			}

			else if ((wt == RPG.WT_TWOHANDS)) {
				if (h.wield(item, RPG.WT_TWOHANDS)) {
					Game.message("You wield " + item.getTheName()
							+ " in both hands");
					wielded=true;
				}
			}

			else if ((wt == RPG.WT_RANGEDWEAPON)) {
				if (h.wield(item, wt)) {
					Game.message("You wield " + item.getTheName()
							+ " as your ranged weapon");
					wielded=true;
				}
			}

			else if ((wt == RPG.WT_MISSILE)) {
				if (h.wield(item, wt)) {
					Game.message("You prepare to fire "
									+ item.getTheName());
					wielded=true;
				}
			}

			else {
				if (h.wield(item, wt)) {
					Game.message("You are now wearing "
									+ item.getTheName());
					wielded=true;
				}
				
			}
			
			if (wielded &&item.getFlag("IsCursed")) {
				h.message(item.getTheName()+" glows black for a second");
				item.set("IsStatusKnown",1);
			}
			
			h.incStat("APS",-Being.actionCost(h));
		}
	}
	
	private void doExit(Thing h) {
		h.incStat("APS",-100);
		map.exitMap(h.x, h.y);
	
	}
	
	private void doPray(Thing h, Action action) {
		
		if (Game.isDebug() && action == Action.PRAY2) {
			Wish.doWish();
		} else if (action==Action.PRAY2) {
			// pray 2 only used for debug mode
			return;
		} else {
			
			Gods.pray(h);

		}	
	}
	
	private static Thing lastSpell=null;

	/**
	 * Called when the player tries to cast a spell
	 * 
	 * @param h The hero
	 */
	private void doZap(Thing h,boolean select) {
		if ((h.getStat(Skill.CASTING)<=0)&&(h.getStat(Skill.HOLYMAGIC)<=0)) {
			h.message("You are unable to cast magic spells");
			return;
		}
		
		Thing s=lastSpell;
		if (select||(s==null)) {
			s=Game.selectItem("Select spell to cast:",h.getFlaggedContents("IsSpell"));
		} 
		if (s != null) {
			if (Spell.canCast(h,s)||Game.isDebug()) {
				Spell.castCost(h,s);
				
				h.message("You cast " + s.getName(Game.hero()));

				castSpell(h,s);
				Spell.train(h, s);
			} else {
				Game
						.message("You have insufficient power to cast "
								+ s.getName(Game.hero()));
			}
			lastSpell=s;
		}		
	}
	

	private void doShowQuests() {
		// TODO: fix this
		ArrayList<Thing> quests = Quest.getQuests();
		String s = "Your quests:\n\n";
		for (int i = 0; i < quests.size(); i++) {
			s += Quest.getQuestText(quests.get(i)) + "\n";
			
		}
		s += "[Press space to continue]";
		Game.infoScreen(s);
		return;		
	}

	
	private void doHelp() {
		questapp.switchScreen(new InfoScreen(Game.getQuestapp(),"Key Commands:\n"
				+ "  a = apply skill                       ? = help\n"
				+ "  c = chat to somebody                  = = load game\n"
				+ "  d = drop item                         - = save game\n"
				+ "  e = eat item                          # = view quests\n" 
				+ "  f = fire ranged weapon                ( = zoom map in\n"
				+ "  g = give item                         ) = zoom map out\n"
				// + "  h = help\n"
				+ "  i = inspect inventory\n"
				+ "  j = jump\n"
				+ "  k = kick something\n" 
				+ "  l = look\n"
				+ "  m = message log\n"
				+ "  o = open / close door\n" 
				+ "  p = pick up item\n"
				+ "  , = pick up item (extended)\n"
				+ "  q = quaff potion\n"
				+ "  r = read book or scroll\n"
				+ "  s = search area\n" 
				+ "  t = throw / shoot missile\n"
				+ "  u = use item\n" 
				+ "  v = view hero statistics\n"
				+ "  w = wield weapon / wear armour\n"
				+ "  x = exit area / climb staircase\n"
				+ "  _ = pray for divine aid\n" 
				+ "  z = cast spell\n"
				+ "\n"));
		Game.getInput();
		questapp.switchScreen(this);		
		
	}

	/**
	 * @param mappanel The mappanel to set.
	 */
	public void setMappanel(MapPanel mappanel) {
		this.mappanel = mappanel;
	}

	/**
	 * @return Returns the mappanel.
	 */
	public MapPanel getMappanel() {
		return mappanel;
	}
    
    public LevelMapPanel getLevelMap() {
        return levelMap;
    }
}