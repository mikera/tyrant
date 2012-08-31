//
// Standard Tyrant dungeon
//

package mikera.tyrant;

import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Point;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.TyrantException;
import mikera.util.Maths;
import mikera.util.Rand;


public class Dungeon {

	public static Map createDungeon(int w, int h, int l) {
		Map m=new Map(w,h);
		m.set("Description","Dungeon");
		
		if (l>=6) {
			m.set("MonsterType",RPG.pick(new String[]{"IsMonster","IsMonster","IsBeast","IsGoblinoid","IsBandit","IsInsect","IsUndead","IsMonster","IsDemonic"}));
		}
		
		makeDungeon(m,l);
		
		m.addEntrance("stairs up");

		if (l<50) {
			Point ext = m.findFreeSquare();
			m.setExit(Portal.create("stairs down",m,ext.x,ext.y));
		}
		
		return m;		
	}
	
	public static Map createDarkTowerLevel(int l) {
		Map m=new Map(50,50);
		m.set("Description","Dark Tower");
		m.set("ComplexName","dark tower");
		m.set("DungeonLevel",l);
		m.set("NoNeutrals",1);
		m.setTheme("metal");
		
		makeDungeon(m,18+l);
		
		m.set("WanderingRate",1000);
		
		Point ent = m.findFreeSquare();
		Thing entrance=Portal.create("stairs down","dark tower",l-1);
		m.addThing(entrance,ent.x,ent.y);
		m.setEntrance(entrance);

		Point ext = m.findFreeSquare();
		Thing exit=Portal.create("stairs up","dark tower",l+1);
		m.addThing(exit,ext.x,ext.y);
		m.setExit(exit);

		return m;	
	}
	
	private static String[] dungeonThemeNames={
			"standard",
			"caves",
			"goblins",
			"stone",
			"labyrinthe",
			"sewer",
			"mines",
			"dungeon",
			"deep halls",
	};
	
	private static String[] deepThemeNames={
			"hell",
			"fire",
			"ice",
			"metal"
	};
	
	public static Map createDungeon(int w, int h, String theme, int level) {
		Map m=new Map(w,h);
		m.setTheme(theme);
		makeDungeon(m,level);
		return m;
	}
	
	public static String selectTheme(int level) {
		String[] themes=dungeonThemeNames;
		if ((level>=18)&&(Rand.d(4)==1)) themes=deepThemeNames;
		String s=RPG.pick(themes);
		return s;
	}
	
	
	public static void makeDungeon(Map m, int lev) {
		// randomly choose wall type
		if (m.get("Theme")==null) {
			m.setTheme(selectTheme(lev));
		}
		
		int w=m.getWidth();
		int h=m.getHeight();
		
		m.set("Level", lev);

		makeDungeon(m,0, 0, w - 1, h - 1);

		// fill in the blanks
		m.replaceTiles(0, m.wall());

		// drop in some critters
		for (int i = 0; i < ((w * h) / 400); i++) {
			Point p = m.findFreeSquare();
			addBaddie(m ,p.x, p.y);
		}

	}

	// scan in direction (dx,dy) to find blank square
	public static Point findEdge(Map m,int x, int y, int dx, int dy) {
		while (m.getTile(x, y) != 0) {
			x += dx;
			y += dy;
		}
		if (m.isBlocked(x - dx, y - dy))
			return null;
		return new Point(x, y);
	}

	// add a random trap to specified square
	// trap is invisible by default
	public static void addTrap(Map m,int x, int y) {
		m.addThing(Trap.createTrap(m.getLevel()), x, y);
	}

	private static void makeDungeon(Map m,int x1, int y1, int x2, int y2) {
		m.fillArea(x1, y1, x2, y2, m.wall());
		m.fillArea(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0);

		// create a central room
		m.fillArea((x1 + x2) / 2 - Rand.d(3), (y1 + y2) / 2 - Rand.d(3), (x1 + x2)
				/ 2 + Rand.d(3), (y1 + y2) / 2 + Rand.d(3), m.floor());

		// map desnity coefficient
		double density=m.getDouble("DungeonDensity");
		if (density==0.0) {
			density=0.1;
			Game.warn("No DungeonDensity!");
		}
		
		// map type
		// String s="cooozzrrrrrtthhhnsskkk";
		String s=m.getString("DungeonDNA");
		if (s==null) {
			s="tttkrrroozhs";
			Game.warn("No DungeonDNA!");
		}
		
		for (int buildloop = 0; buildloop < (m.getWidth()*m.getHeight()*density); buildloop++) {

			// first choose a direction
			Point d=Point.randomDirection4();
			int dx = d.x;
			int dy = d.y;

			// now find a free extension point
			// Point p=findFreeSquare();
			// p=findEdge(p.x,p.y,dx,dy);
			Point p = m.findEdgeSquare(dx, dy, 0);
			if (p == null)
				continue;
			// advance onto blank square
			p.x += dx;
			p.y += dy;
			extendDungeon(m,p.x,p.y,dx,dy,s);
		}
	}
	
	public static boolean extendDungeon(Map m, int x, int y, int dx, int dy, String s) {
		char c=RPG.pick(s);

		// choose new feature to add
		switch (c) {
			case 'c' :
				return makeCorridor(m,x, y, dx, dy);
			case 'o' :
				return makeOvalRoom(m,x, y, dx, dy);
			case 'z' :
				return makeMaze(m,x,y,dx,dy);
			case 'r' :
				return makeRoom(m,x,y,dx,dy);
			case 't' :
				return makeCorridorToRoom(m,x, y, dx, dy);
			case 'h' :
				return makeChamber(m,x, y, dx, dy);
			case 'n' :
				return makeTunnel(m,x, y, dx, dy);
			case 's' :
				return makeSquare(m,x, y, dx, dy);
			case 'k' :
				return makeLinkingCorridor(m,x, y, dx, dy);
			default:
				Game.warn("Dungeon extention '"+c+"' not recognised!");
				return false;
		}
	}

	// make a dungeon door
	public static void makeDoor(Map m,int x, int y) {
		switch (Rand.d(50)) {
			case 1 :
				m.setTile(x, y, m.wall());
				Thing d=Lib.create("secret door");
				d.set("SecretDoorType","[IsMonster]");
				m.addThing(d, x, y);
				break;
			case 2 :
				m.setTile(x, y, m.wall());
				m.addThing(Lib.create("secret door"), x, y);
				break;
			case 3 :
				m.setTile(x, y, m.floor());
				m.addThing(Lib.create("portcullis"), x, y);
				break;
			default :
				m.setTile(x, y, m.floor());
				m.addThing(Lib.createType("IsDoor",m.getLevel()), x, y);
				break;
		}
	}

	private static boolean makeMaze(Map m,int x, int y, int dx, int dy) {
		int s=Rand.d(7)+1;
		int x1=x+dx+(dx-1)*s;
		int x2=x+dx+(dx+1)*s;
		int y1=y+dy+(dy-1)*s;
		int y2=y+dy+(dy+1)*s;
		
		if (!m.isBlank(x1, y1, x2, y2))
			return false;		
		
		Maze.buildInnerMaze(m,x1,y1,x2,y2);
		
		Dungeon.makeDoor(m,x,y);
		m.setTile(x+dx,y+dy,m.floor());
		m.setTile(x+dy,y-dx,m.wall());
		m.setTile(x-dy,y+dx,m.wall());
		
		return true;
	}
	
	private static boolean makeCorridorToRoom(Map m,int x, int y, int dx, int dy) {
		// random dimesions and offset
		int cl=Rand.d(2,10);
		
		// check corridor is clear (3 wide)
		if(!m.isBlank(x-dy,y-dx,x+cl*dx+dy,y+cl*dy+dx)) 
			return false;
		
		// build room
		if (!makeRoom(m,x+cl*dx,y+cl*dy,dx,dy))
			return false;
		
		m.fillArea(x,y,x+cl*dx,y+cl*dy,m.floor());
		makeDoor(m,x,y);
		m.setTile(x+dy,y-dx,m.wall());
		m.setTile(x-dy,y+dx,m.wall());
		
		int j1=RPG.rspread(1,cl-1);
		makeRoom(m,x+j1*dx,y+j1*dy,dy,-dx);
		
		int j2=RPG.rspread(1,cl-1);
		makeRoom(m,x+j2*dx,y+j2*dy,-dy,dx);
		
		return true;
	}
	
	private static boolean makeRoom(Map m,int x, int y, int dx, int dy) {
		// random dimesions and offset
		int x1 = x - Rand.d(Maths.abs(dx - 1), 5);
		int y1 = y - Rand.d(Maths.abs(dy - 1), 5);
		int x2 = x + Rand.d(Maths.abs(dx + 1), 5);
		int y2 = y + Rand.d(Maths.abs(dy + 1), 5);

		if (((x2 - x1) < 3) || ((y2 - y1) < 3) || (!m.isBlank(x1, y1, x2, y2)))
			return false;

		// draw the floor
		m.fillArea(x1 + 1, y1 + 1, x2 - 1, y2 - 1, m.floor());

		// make the door
		m.setTile(x, y, m.floor());
		m.addThing(Door.create(m.getLevel()), x, y);
		
		if (Rand.d(2) == 1) {
			// ending room has a solid border
			int walltile=m.wall();
			if (Rand.d(30)==1) {
				walltile=RPG.pick(new int[] {Tile.STONEWALL,Tile.POSHWALL,Tile.METALWALL});
			}
			m.completeArea(x1, y1, x2, y2, walltile);
		
			addEndingRoomFeatures(m,x1 + 1, y1 + 1, x2 - 1, y2 - 1);
			
		} else {
			// make entrance wall
			m.completeArea((dx == 0) ? x1 : x, (dy == 0) ? y1 : y, (dx == 0) ? x2 : x,
					(dy == 0) ? y2 : y, m.wall());
			addRoomFeatures(m,x1 + 1, y1 + 1, x2 - 1, y2 - 1);		
			
		}

		return true;
	}

	// adds spice to room area.... assumes blank floor
	private static void addEndingRoomFeatures(Map m,int x1, int y1, int x2, int y2) {
		switch (Rand.d(50)) {
			case 1: 
				// dungeon shop
				if (!m.getFlag("NoNeutrals")) {
					makeShop(m,x1,y1,x2,y2);
				}
				break;
				
			case 2: 
				// side dungeon
				m.addThing(Portal.create("dungeon"),x1,y1,x2,y2);
				break;
			
			default:
				// as for normal room
				addInternalRoomFeatures(m,x1,y1,x2,y2);
				break;
		}
	}

	// adds spice to room area.... assumes blank floor
	private static void addRoomFeatures(Map m,int x1, int y1, int x2, int y2) {
		int w = x2 - x1 + 1;
		int h = y2 - y1 + 1;
		switch (Rand.d(50)) {
			case 10 :
			case 11 : // central room
				if ((w > 5) && (h > 5)) {
					m.fillArea(x1 + 1, y1 + 1, x2 - 1, y2 - 1, m.wall());
					m.fillArea(x1 + 2, y1 + 2, x2 - 2, y2 - 2, m.floor());
					addRoomFeatures(m,x1 + 2, y1 + 2, x2 - 2, y2 - 2);
					switch (Rand.d(4)) {
						case 1 :
							makeDoor(m,x1 + 1, y1 + 1 + Rand.d(h - 4));
							break;
						case 2 :
							makeDoor(m,x2 - 1, y1 + 1 + Rand.d(h - 4));
							break;
						case 3 :
							makeDoor(m,x1 + 1 + Rand.d(w - 4), y1 + 1);
							break;
						case 4 :
							makeDoor(m,x1 + 1 + Rand.d(w - 4), y2 - 1);
							break;
					}
				}
				break;
			default:
				addInternalRoomFeatures(m,x1,y1,x2,y2);
				break;
		}
	}
	
	// adds spice to room area.... assumes blank floor
	private static void addInternalRoomFeatures(Map m,int x1, int y1, int x2, int y2) {
		int w = x2 - x1 + 1;
		int h = y2 - y1 + 1;
		switch (Rand.d(50)) {
			case 1 :
			case 2 :
				if ((w * h) < 66)
					for (int x = x1; x <= x2; x++)
						for (int y = y1; y <= y2; y++) {
							if (Rand.d(5) == 1)
								addGuard(m, x, y);
						}
				break;

			case 3 :
			case 4 :
				// pillars
				if ((w >= 5) && (h >= 5)) {
					if ((w % 2) == 1)
						for (int lx = 1; lx < w; lx += 2) {
							m.setTile(x1 + lx, y1 + 1, m.wall());
							m.setTile(x1 + lx, y2 - 1, m.wall());
						}
					if ((h % 2) == 1)
						for (int ly = 1; ly < h; ly += 2) {
							m.setTile(x1 + 1, y1 + ly, m.wall());
							m.setTile(x2 - 1, y1 + ly, m.wall());
						}
				}
				break;

			case 5 :
			case 6 :
				// plants
				for (int x = x1; x <= x2; x++)
					for (int y = y1; y <= y2; y++) {
						if (Rand.d(5) == 1)
							m.addThing(Lib.create("plant"), x, y);
					}
				break;

			case 7 :
			case 8 :
				m.addThing(Lib.createItem(0), x1 + Rand.r(w), y1 + Rand.r(h));
				break;

			case 9 : // unfilled central area
				m.fillArea(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0);
				break;

			case 12 : // room with runetraps;
				for (int x = x1; x <= x2; x++)
					for (int y = y1; y <= y2; y++) {
						if (Rand.d(4) == 1)
							m.addThing(RuneTrap.create(), x, y);
					}
				break;

			case 13 :
				m.addThing(Lib.createItem(m.getLevel()), x1 + Rand.r(w), y1 + Rand.r(h));
				m.addThing(Trap.createTrap(m.getLevel()), x1 + Rand.r(w), y1 + Rand.r(h));
				break;

			case 14 :
			case 15 :
			case 16 : // vertically partitioned room
				if (w > 3) {
					int x = x1 + Rand.d(w - 2);
					if (m.isBlank(x, y1 - 1)
							&& m.isBlank(x, y2 + 1)) {
						m.fillArea(x, y1, x, y2, m.wall());
						int y = y1 + Rand.r(h);
						m.addThing(Lib.create("secret passage"), x, y); // opens walltile
						addGuard(m,x + 1, y);
						addGuard(m,x - 1, y);
					}
				}
				break;

			case 17 :
			case 18 :
			case 19 : // horizontally partitioned room
				if (h > 3) {
					int y = y1 + Rand.d(h - 2);
					if (m.isBlank(x1 - 1, y, x1 - 1, y)
							&& m.isBlank(x2 + 1, y, x2 + 1, y)) {
						m.fillArea(x1, y, x2, y, m.wall());
						int x = x1 + Rand.r(w);
						m.addThing(Lib.create("secret passage"), x, y); // opens walltile
						addGuard(m,x, y + 1);
						addGuard(m,x, y - 1);
					}
				}
				break;

			case 20 :
				m.addThing(Lib.createType("IsMonster",m.getLevel()+2), x1 + Rand.r(w), y1 + Rand.r(h));
				m.addThing(SpellBook.create(m.getLevel()), x1 + Rand.r(w), y1 + Rand.r(h));
				break;

			case 21 :
				m.fillArea(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Tile.POOL);
				break;

			case 22 :
			case 23 :
				m.addThing(Food.createFood(m.getLevel()), x1, y1, x2, y2);
				break;

			case 24 :
				// traps and bones
				for (int x = x1; x <= x2; x++)
					for (int y = y1; y <= y2; y++) {
						if (Rand.d(4) == 1)
							m.addThing(Trap.create(), x, y);
					}
				//addThing(new Food("corpse"), x1, y1, x2, y2);
				//addThing(new Food("corpse"), x1, y1, x2, y2);
				break;


			
			default :
				break;
		}
	}
	
	public static void makeShop(Map m,int x1, int y1, int x2, int y2) {
		Thing shopkeeper = Lib.create("shopkeeper");
		
		String type=RPG.pick(new String[] {"IsStoreItem","IsStoreItem","IsStoreItem","IsScroll","IsPotion","IsMagicItem","IsWeapon","IsArmour","IsShopFood","IsFood"});
		
		for (int x = x1 ; x <= x2; x++) {
			for (int y = y1 ; y <= y2; y++) {
				m.addThing(Town.stockingPoint(shopkeeper,type, m.getLevel()), x, y);
			}
		}

		AI.setGuard(shopkeeper, m, x1, y1, x2, y2);
		shopkeeper.set("IsNeutral",1); // don't fight 
		m.addThing(shopkeeper, (x1 + x2) / 2, (y1 + y2) / 2);
	}

	public static void addBeasties(Map m,int x, int y) {
		Thing b = Lib.createCreature(m.getLevel());
		m.addThing(Lib.create(b.getString("Name")), x, y);
	}

	private static void addGuard(Map m, int x, int y) {
		if (m.isClear(x, y)) {
			Thing b = Lib.createMonster(m.getLevel());
			m.addThing(b, x, y);
		}
	}

	public static void addWandering(Map m) {
		Point p = m.findFreeSquare();
		if (p==null) throw new TyrantException("No free squares available!");
		
		int x = p.x;
		int y = p.y;
		
		if (!m.isVisible(x, y)) {
			Thing t=Dungeon.createFoe(m);
			
			m.addThing(t,x,y);
		}
	}
	
	public static Thing createFoe(Map m) {
		String s=m.getString("MonsterType");
		
		if (s==null) s=("IsHostile");
		String[] ss=s.split(",");
		
		String mt=ss[Rand.r(ss.length)];
		
		int level=m.getLevel();
		
		return Lib.createType(mt,level);
	}

	private static void addBaddie(Map m, int x, int y) {
		int level=m.getLevel();
		
		if (m.isClear(x, y)) {
			Thing b = Dungeon.createFoe(m);
	        
			if (Rand.d(10) == 1) {
				b.addThing(Lib.createItem(level));
			}
			
			m.addThing(b, x, y);
		}
	}

	// oval room, pretty cool
	public static boolean makeOvalRoom(Map m,int x, int y, int dx, int dy) {
		int w = Rand.d(2, 3);
		int h = Rand.d(2, 3);
		int x1 = x + (dx - 1) * w;
		int y1 = y + (dy - 1) * h;
		int x2 = x + (dx + 1) * w;
		int y2 = y + (dy + 1) * h;

		if (!m.isBlank(x1, y1, x2, y2))
			return false;

		int cx = (x1 + x2) / 2;
		int cy = (y1 + y2) / 2;

		for (int lx = x1; lx <= (x1 + w * 2); lx++)
			for (int ly = y1; ly < (y1 + h * 2); ly++) {
				if ((((lx - cx) * (lx - cx) * 100) / (w * w) + ((ly - cy)
						* (ly - cy) * 100)
						/ (h * h)) < 100)
					m.setTile(lx, ly, m.floor());
			}

		m.fillArea(cx, cy, x, y, m.floor());
		return true;
	}

	// make a 3*3 special chamber with interesting stuff
	public static boolean makeChamber(Map m,int x, int y, int dx, int dy) {
		int x1 = x + (dx - 1) * 2;
		int y1 = y + (dy - 1) * 2;
		int x2 = x + (dx + 1) * 2;
		int y2 = y + (dy + 1) * 2;
		int cx = x1 + 2;
		int cy = y1 + 2;

		if (!m.isBlank(x1, y1, x2, y2))
			return false;
		
		// make x1,y1,x2,y2 cover inner room area
		x1+=1;
		y1+=1;
		x2-=1;
		y2-=1;

		m.fillArea(x1, y1, x2, y2 , m.floor());
		m.setTile(x, y, m.floor());
		makeDoor(m,x, y);

		boolean continued = false;
		boolean straight=false;
		switch (Rand.d(4)) {
			case 1 :
			case 2 :
				continued = (makeRoom(m,cx + 2 * dx, cy + 2 * dy, dx, dy) || continued);
				break;
			case 3 :
				continued = (makeChamber(m,cx + 2 * dx, cy + 2 * dy, dx, dy) || continued);
				break;
		}
		
		if (continued&&Rand.d(3)==1) {
			straight=true;
		} else {
			if (Rand.d(3) == 1)
				continued = (makeChamber(m,cx + 2 * dy, cy + 2 * dx, dy, dx) || continued);
			if (Rand.d(3) == 1)
				continued = (makeChamber(m,cx - 2 * dy, cy - 2 * dx, -dy, -dx) || continued);
		}
		
		if (continued) {
			if (straight) {
				switch (Rand.d(12)) {
					case 1:
						// narrow passage
						m.fillArea(cx-1,cy-1,cx+1,cy+1,m.wall());
						m.fillArea(cx-dx,cy-dy,cx+dx,cy+dy,m.floor());
						break;
						
					case 2:
						// flanking items
						String type=RPG.pick(new String[] {"menhir","gravestone","bone","stone bench"});
						m.addThing(type,cx+dy,cy-dx);
						m.addThing(type,cx-dy,cy+dx);
						break;
				}				
			} else {
				switch (Rand.d(8)) {
				
					case 1:
						m.addThing(Trap.create(), cx-dx, cy-dy);
						m.addThing(Chest.create(m.getLevel()), cx, cy);
						break;
					case 2:
						m.setTile(cx,cy,m.wall());
						m.addThing(Secret.hide(Lib.createItem(m.getLevel())),cx-1,cy-1,cx+1,cy+1);
						break;
					case 3:
						m.addThing("menhir",cx,cy);
						break;
					case 4:
						m.addThing("fountain",cx,cy);
						break;
				}
			}
		} else {
			// we have an ending chamber, so add something interesting
			switch (Rand.d(30)) {
				case 1 :
					m.addThing(Trap.create(), cx, cy);
					m.addThing(Chest.create(m.getLevel()), cx + dx, cy + dy);
					break;
				case 2 :
					m.addThing(RuneTrap.create(), cx, cy);
					m.addThing(RuneTrap.create(), cx + dy, cy - dx);
					m.addThing(RuneTrap.create(), cx - dy, cy + dx);
					m.addThing(Chest.create(m.getLevel()), cx + dx, cy + dy);
					break;
				case 3:
					// central item with pit
					m.addThing(Lib.createType("IsMagicItem",m.getLevel()),cx-dx,cy-dy);
					m.addThing(Chest.create(m.getLevel()), cx, cy);
					break;
				case 4:
					m.setTile(cx,cy,Tile.RIVER);
					break;
				case 5:
					m.setTile(cx,cy,m.wall());
					m.addThing(Secret.hide(Lib.createItem(m.getLevel())),cx-1,cy-1,cx+1,cy+1);
					break;
				case 6:
					m.addThing("[IsGravestone]",cx+dx,cy+dy);
					
					if (Rand.d(3)==1) {
						Thing npc=Lib.createType("IsIntelligent",m.getLevel()+6);
						if (!m.getFlag("NoNeutrals")) {
							AI.setNeutral(npc);
						}
						m.addThing(npc,cx,cy);
					}
					
					if (Rand.d(3)==1) {
						m.addThing("[IsGravestone]",cx+dx-dy,cy+dy+dx);
						m.addThing("[IsGravestone]",cx+dx+dy,cy+dy-dx);
						
					}
					break;
				case 7:
					Thing npc=Lib.createType("IsIntelligent",m.getLevel());
					AI.setGuard(npc,m,x1,y1,x2,y2);
					m.addThing(npc,cx,cy);
					if (!m.getFlag("NoNeutrals")) {
						AI.setNeutral(npc);
					}
					m.addThing(Lib.createItem(m.getLevel()),x1,y1,x2,y2).set("IsOwned",1);
					m.addThing(Lib.createItem(m.getLevel()),x1,y1,x2,y2).set("IsOwned",1);
					if (Rand.d(2)==1) {
						m.addThing(Lib.createItem(m.getLevel()),x1,y1,x2,y2).set("IsShopOwned",1);
						m.addThing(Lib.createItem(m.getLevel()),x1,y1,x2,y2).set("IsShopOwned",1);
					}
					break;
				case 8: 
					m.addThing("pit trap",cx-dx,cy-dy);
					break;
				case 9: {
					Thing t=Lib.createType("IsMonster",m.getLevel());
					for (int lx=x1; x<=x2; x++) {
						for (int ly=y1; y<=y2; y++) {
							m.addThing(t.cloneType(),lx,ly);
						}
					}
					break;
				}
				case 10:
					for (int lx=x1; x<=x2; x++) {
						for (int ly=y1; y<=y2; y++) {
							Thing t=Lib.createType("IsMonster",m.getLevel()+3);
							m.addThing(t,lx,ly);
							m.addThing(Coin.createLevelMoney(m.getLevel()));
						}
					}
					break;
				case 11:
					for (int lx=x1; x<=x2; x++) {
						for (int ly=y1; y<=y2; y++) {
							Thing t=Lib.createType("IsFood",m.getLevel());
							m.addThing(t,lx,ly);
						}
					}
					break;
				case 12: 
					m.addThing("[IsAltar]",cx+dx,cy+dy);
					break;
				case 13: 
					m.addThing("fountain",cx+dx,cy+dy);
					break;
					
					
					
				default:
					addInternalRoomFeatures(m,x1,y1,x2,y2);
					break;
			}
		}

		m.completeArea(x1-1, y1-1, x2+1, y2+1, m.wall());

		return true;
	}

	// make a 5*5 special chamber with interesting stuff
	public static boolean makeSquare(Map m, int x, int y, int dx, int dy) {
		int x1 = x + (dx - 1) * 3;
		int y1 = y + (dy - 1) * 3;
		int x2 = x + (dx + 1) * 3;
		int y2 = y + (dy + 1) * 3;
		int cx = x1 + 3;
		int cy = y1 + 3;

		if (!m.isBlank(x1, y1, x2, y2))
			return false;

		m.fillArea(x1 + 1, y1 + 1, x2 - 1, y2 - 1, m.floor());
		m.setTile(x, y, m.floor());
		makeDoor(m,x, y);

		boolean continued = false;

		// room ahead
		switch (Rand.d(4)) {
			case 1 :
			case 2 :
				continued = (makeRoom(m,cx + 3 * dx, cy + 3 * dy, dx, dy) || continued);
				break;
			case 3 :
				continued = (makeChamber(m,cx + 3 * dx, cy + 3 * dy, dx, dy) || continued);
				break;
		}

		// side rooms
		if (Rand.d(2) == 1) {
			if (Rand.d(2) == 1)
				continued = (makeChamber(m,cx + 3 * dy, cy + 3 * dx, dy, dx) || continued);
			else
				continued = (makeRoom(m,cx + 3 * dy, cy + 3 * dx, dy, dx) || continued);
		}
		if (Rand.d(2) == 1) {
			if (Rand.d(2) == 1)
				continued = (makeChamber(m,cx - 3 * dy, cy - 3 * dx, -dy, -dx) || continued);
			else
				continued = (makeRoom(m,cx - 3 * dy, cy - 3 * dx, -dy, -dx) || continued);
		}

		if (continued) {
			switch (Rand.d(8)) {
				case 1 :
					m.addThing(Chest.create(m.getLevel()), cx, cy);
					m.addThing(Trap.create(), cx + dx, cy + dy);
					break;
				case 2 :
					addBeasties(m,cx, cy);
					break;
				case 3 :
					m.addThing(Fire.create(5), cx + 1, cy + 1);
					m.addThing(Fire.create(5), cx - 1, cy + 1);
					m.addThing(Fire.create(5), cx + 1, cy - 1);
					m.addThing(Fire.create(5), cx - 1, cy - 1);
					break;
				case 4:
				case 5:
					m.fillArea(x1+1,y1+1,x2-1,y2-1,m.wall());
					if (!m.isBlank(cx-3,cy)) m.fillArea(cx,cy,cx-3,cy,m.floor());
					if (!m.isBlank(cx+3,cy)) m.fillArea(cx,cy,cx+3,cy,m.floor());
					if (!m.isBlank(cx,cy-3)) m.fillArea(cx,cy,cx,cy-3,m.floor());
					if (!m.isBlank(cx,cy+3)) m.fillArea(cx,cy,cx,cy+3,m.floor());
					break;
				default :
					addInternalRoomFeatures(m,x1 + 1, y1 + 1, x2 - 1, y2 - 1);
					break;
			}
		} else {
			// we have an ending chamber, so add something really interesting
			switch (Rand.d(8)) {
				case 1 :
					m.addThing(Lib.createCreature(m.getLevel() + 3), cx, cy);
					m.addThing(Chest.create(m.getLevel() + 3), cx + dx, cy + dy);
					break;
					
				case 2: 
					if (!m.getFlag("NoNeutrals")) {
						makeShop(m,cx-2,cy-2,cx+2,cy+2);
					}
					break;
					
				case 3: 
					m.addThing(Fire.create(5), cx +dx, cy +dy);
					Thing npc1=Lib.createType("IsIntelligent",m.getLevel()+2);
					if (!m.getFlag("NoNeutrals")) {
						AI.setNeutral(npc1);
					}
					m.addThing(npc1,cx,cy);
					AI.setGuard(npc1,m,cx-2,cy-2,cx+2,cy+2);
					Thing npc2=Lib.createType("IsIntelligent",m.getLevel()+2);
					if (!m.getFlag("NoNeutrals")) {
						AI.setNeutral(npc2);
					}
					m.addThing(npc2,cx-dx,cy-dy);
					AI.setGuard(npc2,m,cx-2,cy-2,cx+2,cy+2);
					break;
			}
		}

		m.completeArea(x1, y1, x2, y2, m.wall());

		return true;
	}

	// make a random twisty tunnel
	public static boolean makeTunnel(Map m,int x, int y, int dx, int dy) {
		if ((m.getTile(x, y) == 0) && m.isValid(x, y)) {
			int ndx = dx;
			int ndy = dy;
			m.setTile(x, y, m.floor());
			if (Rand.d(3) == 1) {
				ndx = -dy;
				ndy = dx;
			}
			if (Rand.d(4) == 1) {
				ndx = dy;
				ndy = -dx;
			}
			makeTunnel(m,x + ndx, y + ndy, ndx, ndy);
			return true;
		}
		return false;
	}

	/** 
	 * Make a corridor which links two areas
	 */
	public static boolean makeLinkingCorridor(Map m,int x, int y, int dx, int dy) {
		int cx=x;
		int cy=y;
		for (int i=Rand.d(4,10); i>0; i--) {
			cx+=dx;
			cy+=dy;
			if (!m.isBlank(cx,cy)) {
				if (m.isBlocked(cx,cy)) return false;
				break;
			}
			
		}
		
		if (!m.isBlank(cx,cy)) {
			m.fillArea(x,y,cx-dx,cy-dy,m.floor());
			return true;
		}
		return false;
	}
	
	// make a long corridor
	public static boolean makeCorridor(Map m,int x, int y, int dx, int dy) {
		int l = Rand.d(2, 8);

		if (!m.isBlank(x, y, x + dx * l, y + dy * l))
			return false;

		for (int i = 0; i < l; i++) {
			m.setTile(x + i * dx, y + i * dy, m.floor());
		}

		// add a door if there is space
		if ((l > 4) && (Rand.d(2) == 1)) {
			if (m.completeTile(x + dy, y + dx, m.wall())
					& m.completeTile(x - dy, y - dx, m.wall())) {
				makeDoor(m,x, y);
			}
		}

		// try adding a room to end
		if ((l > 3)) {
			makeRoom(m,x + dx * l, y + dy * l, dx, dy);
		}

		if (Rand.d(100) == 1)
			addTrap(m,x + Rand.r(l) * dx, y + Rand.r(l) * dy);

		return true;
	}
}