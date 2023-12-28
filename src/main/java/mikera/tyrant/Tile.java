// stores information and codes for map tile objects

package mikera.tyrant;

import java.util.ArrayList;
import java.util.HashMap;

import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.util.Rand;

public class Tile {

	// Tile flags
	public static final int TF_BLOCKED = 65536;
	public static final int TF_TRANSPARENT = 131072;
	public static final int TF_DISCOVERED = 262144;
	public static final int TF_ANTIMAGIC = 524288;
	public static final int TF_VISIBLE = 2097152;
	public static final int TF_LIT = 4194304;
	public static final int TF_DIRECTIONBASE = 8388608;
	public static final int TF_DIRECTION = 15 * TF_DIRECTIONBASE;
	public static final int TF_ACTIVE = 16 * TF_DIRECTIONBASE;
	public static final int TF_LOS = 32 * TF_DIRECTIONBASE;

	public static final int TF_TYPEMASK = 65535;

	// public static final Tile WALL=new GameTile(
	// Tile types
	public static final int NOTHING=0;
	public static final int FLOOR = 1;
	public static final int WALL = 2;
	public static final int BRICKWALL = 3;
	public static final int GOLDWALL = 4;
	public static final int STONEFLOOR = 5;
	public static final int STONEWALL = 6;
	public static final int METALFLOOR = 7;
	public static final int METALWALL = 8;
	public static final int CAVEFLOOR = 9;
	public static final int CAVEWALL = 10;
	public static final int ICEFLOOR = 11;
	public static final int ICEWALL = 12;
	public static final int POSHFLOOR = 13;
	public static final int POSHWALL = 14;
	public static final int FORESTFLOOR = 15;
	public static final int TREE = 16;

	public static final int RIVER = 17;
	public static final int GRASS = 18;

	// outdoor tiles
	public static final int PLAINS = 19;
	public static final int FORESTS = 20;
	public static final int HILLS = 21;
	public static final int MOUNTAINS = 22;
	public static final int PLAINSROCK = 23;
	public static final int MOREPLAINS = 24;
	public static final int SEA = 25;

	public static final int STREAM = 26;
	public static final int GUNK = 27;
	public static final int POOL = 28;

	public static final int SWAMP = 29;

	public static final int WOODENWALL = 30;
	public static final int WOODENFLOOR = 31;
	public static final int MOSSFLOOR = 32;
	
	public static final int LAVA = 33;
	public static final int REDWALL = 34;
    public static final int REDFLOOR = 35;

    public static final int MARBLEFLOOR = 36;
    public static final int MUDFLOOR = 37;
    public static final int PARQUETFLOOR = 38;
    
    public static final int LAVASEA = 39;
    
    public static final int VOID=40;

	public static String[] names= null;
	public static int[] images = null;
	public static int[] imagefill = null;
	public static boolean[] filling = null;
	public static int[] borders = null;
	public static int[] movecost = null;
	public static boolean[] active=null;
	public static boolean[] issolid=null;
	public static boolean[] ispassable=null;
	public static int[] mapColours=null;
	
	public static Thing[] tiles;
    private static java.util.Map<String, Integer> tileByName;
    private static java.util.Map<Integer, String> nameById;
	
	public static int getMoveCost(int t) {
		return movecost[t];
	}

	/** 
	 * Gets a tile using the given tile name
	 * @param tileName
	 * @return
	 */
	public static int fromName(String tileName) {
        if(nameById == null)
            createMaps();
        Integer tile = tileByName.get(tileName);
        if(tile != null) return tile.intValue();
		throw new Error("Tile named ["+tileName+"] does not exist");
	}
    
	public static String tileNameFor(int tile) {
        if(tileByName == null)
            createMaps();
        String name = nameById.get(tile);
        if(name != null) return name;
		throw new Error("Tile of type ["+ tile +"] does not exist");
	}

    private static void createMaps() {
        tileByName = new HashMap<>();
        nameById = new HashMap<>();
        for (int i=0; i<names.length; i++) {
            tileByName.put(names[i], i);
            nameById.put(i, names[i]);
        }
    }
	
	public static int getImage(int t) {
		return images[t];
	}
	
	public static String getASCII(int t) {
		return get(t).getString("ASCII");
	}

	public static int getFilledImage(int t) {
		return imagefill[t];
	}
	
	public static boolean isFilling(int t) {
		return filling[t];
	}
	

	public static void init() {
		Thing t;
		t=Lib.extend("base tile","base thing");
		t.set("IsTile",1);
		t.set("IsDestructible",0);
		t.set("IsDiggable",0);
		t.set("ImageSource","Tiles");
		t.set("MoveCost",100);
		t.set("MapColour",0x00303030);
		t.set("LevelMin",1);
		Lib.add(t);
		
		t=Lib.extend("base wall tile","base tile");
		t.set("IsViewBlocking",1);
		t.set("IsBlocking",1);
		t.set("IsWall",1);
		t.set("IsSolid",1);
		t.set("IsTileFilling",1);
		t.set("Image",20);
		t.set("IsDiggable",1);
		t.set("IsJumpable",0);
		t.set("IsPassable",0);
		t.set("DigDifficulty",100);
		t.set("DigTile",CAVEFLOOR);
		t.set("MapColour",0x00808080);
		t.set("ImageFill",1); // image fill bonus
		t.set("ASCII","#");
		Lib.add(t);
		
		t=Lib.extend("base floor tile","base tile");
		t.set("IsViewBlocking",0);
		t.set("IsBlocking",0);
		t.set("IsDiggable",1);
		t.set("IsPassable",1);
		t.set("DigDifficulty",100);
		t.set("DigTile",CAVEFLOOR);
		t.set("Image",0);
		t.set("ASCII",".");
		Lib.add(t);
		
		t=Lib.extend("base outdoor tile","base tile");
		t.set("IsViewBlocking",0);
		t.set("IsBlocking",0);
		t.set("IsDiggable",1);
		t.set("DigDifficulty",100);
		t.set("DigTile",CAVEFLOOR);
		t.set("IsOutdoorTile",1);
		t.set("IsPassable",1);
		t.set("Image",60);
		t.set("ASCII",".");
		Lib.add(t);

		
		// call the generation routines for various tile types
		initSpecialTiles();
		initWalls();
		initSpecialWalls();
		initFloors();
		initWater();
		initLava();
		initOutdoors();

		
		// finally build the lookup arrays
		buildTileArrays();
	}
	
	static {
		buildTileArrays();
	}
	
	private static void buildTileArrays() {
		ArrayList<Thing> al=Lib.instance().getTiles();
		int n=al.size();
		
		// initialise arrays to correct length
		names=new String[n];
		images=new int[n];
		imagefill=new int[n];
		filling=new boolean[n];
		borders=new int[n];
		movecost=new int[n];
		tiles=new Thing[n];
		active=new boolean[n];
		issolid=new boolean[n];
		ispassable=new boolean[n];
		mapColours=new int[n];
		
		// fill array values for each tile
		for (int i=0; i<n; i++) {
			Thing t=al.get(i);
			if (t==null) {
				Game.warn("Null in tile list at position "+i);
				continue;
			}
				
			names[i]=t.name();
			images[i]=t.getStat("Image");
			imagefill[i]=t.getStat("Image")+t.getStat("ImageFill");
			filling[i]=t.getFlag("IsTileFilling");
			borders[i]=t.getFlag("IsTileBordered")?1:0;
			movecost[i]=t.getStat("MoveCost");
			active[i]=t.getFlag("IsActive");
			mapColours[i]=t.getStat("MapColour");
			issolid[i]=t.getFlag("IsSolid");
			ispassable[i]=t.getFlag("IsPassable");
			tiles[i]=t;
		}
	}
	
	private static Thing get(int t) {
		return tiles[t&65535];
	}
	
	public static void action(Map m, int x, int y, int time) {
		int tile=m.getTile(x,y);
		Thing t=get(tile);
		
		if (t.getFlag("IsDamageTile")) {
			Thing[] ts=m.getThings(x,y);
			if (ts.length==0) return;
			
			int amount=t.getStat("Damage");
			amount*=RPG.round(time/100.0);
			if (amount<=0) return;
			String dt=t.getString("DamageType");
			
			for (int i=0; i<ts.length; i++)	{
				Thing thing=ts[i];
				
				if (!thing.getFlag("IsPhysical")) continue;		
				
				if (thing.getFlag("IsFlying")) {
					continue;
				}
				
				Damage.inflict(thing,amount,dt);
			}
		}
	}
	
	public static boolean isDiggable(Map m, int x, int y) {
		if ((x==0)||(y==0)||(x==m.width-1)||(y==m.height-1)) return false;
		int tile=m.getTile(x,y)&65535;
		return isDiggable(get(tile));
	}
	
	public static boolean isSolid(Map m, int x, int y) {
		int tile=m.getTile(x,y);
		return issolid[tile];
	}
	
	public static boolean isPassable(int tile) {
		return ispassable[tile];
	}
	
	public static boolean isPassable(Thing b, Map m, int x, int y) {
		if ((x<0)||(y<0)||(x>=m.width)||(y>=m.height)) return false;
		int tile=m.getTile(x,y);
		Thing t=get(tile);
		
		if (t.getFlag("IsPassable")) return true;
		
		// can't move to unpassable edge tile
		if ((x==0)||(y==0)||(x==m.width-1)||(y==m.height-1)) return false;

		
		if (issolid[tile]) {
			if (b.getFlag("IsEthereal")) return true;
			if (b.getFlag("IsFlying")) {
				// Game.warn("flying isPassable check");
				if (t.getFlag("IsJumpable")) return true;
			}
			return false;
		}
		
		if (b.getFlag("IsFlying")) return true;
		
		// special tile cases
		switch (tile) {
			case Tile.SEA:
				Thing[] bs=b.getFlaggedContents("IsBoat");
				return (bs!=null)&&(bs.length>0);
			case Tile.MOUNTAINS:
				return b.getFlag(Skill.CLIMBING);
		}
		
		return true;
	}
	
	public static boolean isSensibleMove(Thing b, Map m, int x, int y) {
		if ((x<0)||(y<0)||(x>=m.width)||(y>=m.height)) return false;
		int tile=m.getTile(x,y);
		Thing t=get(tile);
		
		if (t.getFlag("IsPassable")) return true;
		
		if (issolid[tile]) {
			if (b.getFlag("IsEthereal")) return true;
			if (b.getFlag("IsFlying")) {
				if (t.getFlag("IsJumpable")) return true;
			}
			return false;
		}
		
		if (b.getFlag("IsFlying")) return true;
		
		// special tile cases
		switch (tile) {
			case Tile.SEA:
				Thing[] bs=b.getFlaggedContents("IsBoat");
				return (bs!=null)&&(bs.length>0);
			case Tile.MOUNTAINS:
				return b.getFlag(Skill.CLIMBING);
		}
		
		return false;
	}
	
	private static boolean isDiggable(Thing t) {
		return t.getFlag("IsDiggable");
	}
	
	public static int digAbility() {
		return 100;
	}
	
	public static int digCost(Thing b, Thing w) {
		int mining=b.getStat(Skill.MINING);
		if (b.getFlag("Digging")) return 100;
		if (mining<=0) return 0;
		if ((w!=null)&&w.getFlag("IsDiggingTool")) {
			return w.getStat("DigCost")/mining;
		}
		return 0;
	}
	
	public static void kick(Thing b, Map m, int x, int y) {
		int tile=m.getTile(x,y);
		Thing t=get(tile);
		
		if (t.getFlag("IsWall")) {
			b.message("You kick the wall - ouch!");
		} else if (t.getFlag("IsWaterTile")) {
			b.message("Your attempt to kick the water only succeeds in getting you wet!");
		} else {
			b.message("You kick thin air");
		}
	}
	
	public static boolean dig(Thing b, Map m, int x, int y) {
		int tile=m.getTile(x,y);
		Thing w=b.getWielded(RPG.WT_MAINHAND);
		Thing t=get(tile);
		
		if (isDiggable(m,x,y)) {
			int cost=digCost(b,w);
			if (cost<=0) return false;
			int hard=t.getStat("DigDifficulty");
			if (Rand.r(hard)<digAbility()) {
				if (w!=null) Damage.inflict(w,w.getStat("DigDamage"),RPG.DT_SPECIAL);
				b.incStat("APS",-cost);
				b.message("You dig through the "+t.name());
				Tile.dig(m,x,y);
				return true;
			} 
			
			b.message("You dig furiously at the "+t.name());
			return false;
		}
		
		b.message("You are unable to dig through "+t.getTheName());
		return false;
	}

	public static boolean dig(Map m, int x, int y) {
		if (!isDiggable(m,x,y)) return false;
		Thing t=get(m.getTile(x,y));
		m.setTile(x,y,t.getStat("DigTile"));
		return true;
	}
	private static void addTile(int tile, Thing t) {
		t.set("TileValue",tile);
		tile=tile&65535;
		ArrayList<Thing> al=Lib.instance().getTiles();
		while (al.size()<=tile) al.add(null);
		if (al.get(tile)!=null) throw new Error("Tile arraylist already filled at position "+tile);
		al.set(tile,t);
		
		int tileMask=0;
		if (!t.getFlag("IsViewBlocking")) tileMask|=Tile.TF_TRANSPARENT;
		if (t.getFlag("IsBlocking")) tileMask|=Tile.TF_BLOCKED;
		if (t.getFlag("IsActive")) tileMask|=Tile.TF_ACTIVE;
		t.set("TileMask",tileMask);
		
		Lib.add(t);
	}
	
	public static int getMask(int tile) {
		if (tile>tiles.length) return 0;
		Thing t=get(tile);
		return t.getStat("TileMask");
	}
	
	private static void initSpecialTiles() {
		Thing t;
		
		t=Lib.extend("nothing","base tile");
		t.set("IsPassable",1);
		t.set("ASCII", " ");
		t.set("Image",33);
		addTile(NOTHING,t);
		
		t=Lib.extend("void","base tile");
		t.set("IsPassable",0);
		t.set("ASCII", " ");
		t.set("IsActive",1);
		t.set("IsDamageTile",1);
		t.set("DamageType","disintegrate");
		t.set("Damage",400);
		t.set("Image",33);
		addTile(VOID,t);
	}
	private static void initWalls() {
		Thing t;
		t=Lib.extend("wall","base wall tile");
		t.set("Image",20);
		addTile(WALL,t);
        
		t=Lib.extend("brick wall","wall");
		t.set("Image",20);
		addTile(BRICKWALL,t);
		
        t=Lib.extend("gold wall","wall");
		t.set("Image",78);
		addTile(GOLDWALL,t);
		
		t=Lib.extend("stone wall","base wall tile");
		t.set("Image",22);
		addTile(STONEWALL,t);
		
		t=Lib.extend("metal wall","base wall tile");
		t.set("IsDiggable",0);
		t.set("Image",24);
		addTile(METALWALL,t);
		
		t=Lib.extend("cave wall","base wall tile");
		t.set("Image",26);
		t.set("IsDiggable",1);
		addTile(CAVEWALL,t);
		
		t=Lib.extend("ice wall","base wall tile");
		t.set("Image",28);
		addTile(ICEWALL,t);
		
		t=Lib.extend("posh wall","base wall tile");
		t.set("Image",30);
		addTile(POSHWALL,t);
		
		t=Lib.extend("wooden wall","base wall tile");
		t.set("Image",38);
		addTile(WOODENWALL,t);
		
		t=Lib.extend("red wall","base wall tile");
		t.set("Image",36);
		addTile(REDWALL,t);
	}
	
	private static void initSpecialWalls() {
		Thing t;
		t=Lib.extend("tree wall","base wall tile");
		t.set("Image",32);
		t.set("ImageFill",0);
		t.set("IsDiggable",0);
		addTile(TREE,t);
	}

	public static int getMapColour(int tile) {
		return mapColours[tile];
	}
	
	public static void enterTrigger(Thing t, Map m, int tx,int ty, boolean touchFloor) {
		int type=m.getTile(tx,ty);
		
		if ((t.x!=tx)||(t.y!=ty)) {
			throw new Error("Tile.enterTrigger wrong place!");
		}
		
		switch (type) {
			case SEA: case RIVER: 
				if (touchFloor) {
					if (!t.getFlag(Skill.SWIMMING)) {
						t.message("You struggle in the water");
						Damage.inflict(t,3,RPG.DT_WATER);
					}
				}
				break;
		}
	}	
	
	private static void initFloors() {
		Thing t;
		
		t=Lib.extend("grass","base floor tile");
		t.set("Image",60);
		t.set("MapColour",0x00305030);
		addTile(GRASS,t);
		
		t=Lib.extend("floor","base floor tile");
		t.set("Image",0);
		addTile(FLOOR,t);
		
		t=Lib.extend("stone floor","base floor tile");
		t.set("Image",14);
		addTile(STONEFLOOR,t);
		
        t=Lib.extend("marble floor","base floor tile");
        t.set("Image",164);
        t.set("IsDiggable",0);
        addTile(MARBLEFLOOR,t);

        t=Lib.extend("mud floor","base floor tile");
        t.set("Image",162);
        addTile(MUDFLOOR,t);

		t=Lib.extend("metal floor","base floor tile");
		t.set("Image",4);
        t.set("IsDiggable",0);
		addTile(METALFLOOR,t);
		
		t=Lib.extend("cave floor","base floor tile");
		t.set("Image",6);
		addTile(CAVEFLOOR,t);
		
		t=Lib.extend("ice floor","base floor tile");
		t.set("Image",8);
		addTile(ICEFLOOR,t);
		
		t=Lib.extend("posh floor","base floor tile");
		t.set("Image",10);
		addTile(POSHFLOOR,t);
		
		t=Lib.extend("wooden floor","base floor tile");
		t.set("Image",18);
		addTile(WOODENFLOOR,t);
		
        t=Lib.extend("parquet floor","base floor tile");
        t.set("Image",165);
        addTile(PARQUETFLOOR,t);

		t=Lib.extend("forest floor","base floor tile");
		t.set("Image",12);
		addTile(FORESTFLOOR,t);
		
		t=Lib.extend("moss floor","base floor tile");
		t.set("Image",13);
		t.set("MoveCost",130);
		addTile(MOSSFLOOR,t);
		
		t=Lib.extend("red floor","base floor tile");
		t.set("Image",16);
		t.set("MoveCost",100);
		addTile(REDFLOOR,t);
		
	}
	
	private static void initOutdoors() {
		Thing t;
		
		t=Lib.extend("plains","base outdoor tile");
		t.set("Image",60);
		t.set("IsOutdoorTile",1);
		t.set("MapColour",0x00208030);
		addTile(PLAINS,t);
		
		t=Lib.extend("forests","base outdoor tile");
		t.set("Image",61);
		t.set("MoveCost",130);
		t.set("IsViewBlocking",1);
		t.set("MapColour",0x00105020);
		addTile(FORESTS,t);
		
		t=Lib.extend("hills","base outdoor tile");
		t.set("Image",62);
		t.set("MoveCost",150);
		t.set("ASCII","^");
		t.set("MapColour",0x00404040);
		addTile(HILLS,t);
		
		t=Lib.extend("mountains","base outdoor tile");
		t.set("Image",63);
		t.set("IsPassable",0);
		t.set("IsBlocking",1);
		t.set("IsViewBlocking",1);
		t.set("MoveCost",300);
		t.set("ASCII","&");
		t.set("MapColour",0x00606060);
		addTile(MOUNTAINS,t);
		
		t=Lib.extend("rocky plains","base outdoor tile");
		t.set("Image",60);
		t.set("MoveCost",130);
		addTile(PLAINSROCK,t);
		
		t=Lib.extend("open plains","base outdoor tile");
		t.set("Image",60);
		addTile(MOREPLAINS,t);
		
		t=Lib.extend("swamps","base outdoor tile");
		t.set("Image",203);
		t.set("ASCII","-");
		t.set("MapColour",0x00504030);
		addTile(SWAMP,t);
		
		t=Lib.extend("sea","base water tile");
		t.set("Image",66);
		t.set("IsOutdoorTile",1);
		t.set("IsBlocking",1);
		t.set("MoveCost",300);
		t.set("MapColour",0x00103060);
		addTile(SEA,t);
		
		t=Lib.extend("lava sea","lava");
		t.set("IsOutdoorTile",1);
		t.set("IsBlocking",1);
		t.set("MoveCost",500);
		addTile(LAVASEA,t);
	}
	
	private static void initWater() {
		Thing t;
		
		t=Lib.extend("base water tile","base tile");
		t.set("IsWaterTile",1);
		t.set("IsViewBlocking",0);
		t.set("IsBlocking",0);
		t.set("Image",0);
		t.set("IsTileBordered",1);
		t.set("IsPassable",0);
		t.set("Image",181);
		t.set("ASCII","~");
		t.set("MapColour",0x00406080);
		Lib.add(t);
		
		t=Lib.extend("river","base water tile");
		t.set("IsBlocking",1);
		t.set("IsActive",1);
		t.set("IsDamageTile",1);
		t.set("Damage",1);
		t.set("DamageType","water");
		t.set("MoveCost",250);
		addTile(RIVER,t);
		
		t=Lib.extend("stream","base water tile");
		t.set("MoveCost",150);
		t.set("IsPassable",1);
		addTile(STREAM,t);
		
		t=Lib.extend("gunk","base water tile");
		t.set("Image",202);
		t.set("IsPassable",1);
		t.set("MapColour",0x00304020);
		addTile(GUNK,t);
		
		t=Lib.extend("pool","base water tile");
		addTile(POOL,t);
	}
	
	private static void initLava() {
		Thing t=Lib.extend("lava","base water tile");
		t.set("Image",47);
		t.set("IsBlocking",0);
		t.set("IsLavaTile",1);
		t.set("IsActive",1);
		t.set("IsDamageTile",1);
		t.set("DamageType","fire");
		t.set("Damage",400);
		t.set("IsPassable",0);
		t.set("MapColour",0x00907030);
		t.set("MoveCost",250);
		addTile(LAVA,t);
		
	}

}