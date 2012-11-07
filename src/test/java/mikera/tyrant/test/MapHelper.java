package mikera.tyrant.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import mikera.tyrant.Tile;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Thing;

/**
 * @author Chris Grindstaff chris@gstaff.org
 */
public class MapHelper {
    private static java.util.Map<String, List<String>> symbolToNameMap;
    private static java.util.Map<String, String> attributeMap = new HashMap<>();
    private static java.util.Map<String, List<String>> tileMap;
    private static java.util.Map<String, String> symbolForName;
    private static java.util.Map<String, Object> charByTile;
    private java.util.Map<Character, Thing> seenMarkers = new HashMap<>();
    
    public Map createMap(String mapString, boolean useMarkers) {
        int firstNewline = mapString.indexOf('\n');
        int numberOfNewlines = countMatches(mapString, "\n");
        Map map = new Map(firstNewline, numberOfNewlines + 1);
        
        String[] lines = mapString.split("\n");
        for (int y = 0; y <= numberOfNewlines; y++) {
            char[] chars = lines[y].toCharArray();
            for (int x = 0; x < chars.length; x++) {
                char c = chars[x];
                if(c == '\r') continue;
                map.setTile(x, y, Tile.fromName("floor"));

                String tileName = createTile(c);
                if (tileName != null) {
                    // Set the tile
                    map.setTile(x, y, Tile.fromName(tileName));
                } else {
                    // add a marker or thing
                    Thing toAdd = useMarkers ? createMarker(c) : createThing(c);
                    if(useMarkers && toAdd == null) toAdd = createThing(c);
                    map.addThing(toAdd, x, y);
                }
            }
        }
        
        map.calcVisible(TyrantTestCase.getTestHero(), 20);
        return map;
    }
    
    private Thing createMarker(char c) {
        if(!Character.isLetter(c)) return null;
        Character markerName = new Character(c);
        Thing seen = seenMarkers.get(markerName);
        if(seen != null) return seen;
        Thing marker = Lib.get("marker");
        if(marker == null) {
            marker = Lib.extend("marker", "base thing");
            marker.set("ASCII", "*");
            seenMarkers.put(markerName, marker);
        }
        return marker;
    }

    public Map createMap(String mapString) {
        return createMap(mapString, false);
    }

    private String createTile(char aChar) {
        List<String> things = getTileMap().get("" + aChar);
        if(things == null) {
            return null;
        }
    	return things.get(0);
    }
    
    private Thing createThing(char aChar) {
    	if (aChar=='@') return TyrantTestCase.getTestHero();
        String name = "" + aChar;
        List<String> things = getSymbolToName().get(name);
        if (things == null) {
            throw new Error("Unsure how to handle [" + name + "] in MapHelper");
        }
        return Lib.create(things.get(0));
    }

    private static java.util.Map<String, List<String>> getSymbolToName() {
        if(symbolToNameMap == null) {
            symbolToNameMap = new HashMap<>();
            symbolForName = new HashMap<>();
            
            addMapping(symbolToNameMap, "@", "you");
            addMapping(symbolToNameMap, "=", "plain ring");
            addMapping(symbolToNameMap, ">", "stairs up, ladder up");
            addMapping(symbolToNameMap, "*", "town, ruin, graveyard, goblin village, dark tower, dark forest, caves, tutorial inn");
            addMapping(symbolToNameMap, "<", "stairs down");
            addMapping(symbolToNameMap, "+", "door, invincible portcullis, portcullis");
            addMapping(symbolToNameMap, "`", "large rock, stone");
            addMapping(symbolToNameMap, "%", "+IsFood(beefcake)");
            addMapping(symbolToNameMap, "!", "+IsPotion(potion of strength)");
            addMapping(symbolToNameMap, "^", "+IsTrap(Spark trap)");
            addMapping(symbolToNameMap, "#", "tree, bush, stone bench, invisible portal, water barrel, NS table, table, inn sign, rat cave");
            addMapping(symbolToNameMap, "}", "fire, well");
            addMapping(symbolToNameMap, "_", "stone altar");
            addMapping(symbolToNameMap, ")", "arrow, bone, leather cap, stone knife");
            addMapping(symbolToNameMap, "K", "inn loft key");
            addMapping(symbolToNameMap, "$", "chest");
            addMapping(symbolToNameMap, "?", "xxx scroll");
            
            addMapping(symbolToNameMap, "x", "small yellow bug, cockroach, insect");
            addMapping(symbolToNameMap, "S", "demon snake, grass snake");
            addMapping(symbolToNameMap, "r", "field mouse, small rat");
            addMapping(symbolToNameMap, "k", "kobold");
            addMapping(symbolToNameMap, "L", "leprechaun");
            
            addMapping(symbolToNameMap, "|", "stool, potted flower");
            addMapping(symbolToNameMap, "m", "message point, guard point");
            addMapping(symbolToNameMap, "/", "stick");
            
            for (Iterator<Entry<String, List<String>>> iter = symbolToNameMap.entrySet().iterator(); iter.hasNext();) {
                Entry<String, List<String>> entry = iter.next();
                List<String> names = entry.getValue();
                for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
                    String name = iterator.next();
                    symbolForName.put(name, entry.getKey());
                }
            }
        }
        return symbolToNameMap;
    }
    
    private static void addMapping(java.util.Map<String, List<String>> tileMap2, String symbol, String names) {
        if(names.startsWith("+")) {
            int firstPareen = names.indexOf('(');
            String attribute = names.substring(1, firstPareen);
            String prototypical = names.substring(firstPareen + 1, names.indexOf(')'));
            attributeMap.put(attribute, symbol);
            names = prototypical;
        }
        String[] splits = names.split(",");
        for (int i = 0; i < splits.length; i++) {
            String item = splits[i].trim();
            List<String> items = tileMap2.get(symbol);
            if(items == null) {
                items = new ArrayList<>();
                tileMap2.put(symbol, items);
            }
            items.add(item);
        }
    }

    private static java.util.Map<String, List<String>> getTileMap() {
        if(tileMap == null) {
        	tileMap = new HashMap<>();
            charByTile = new HashMap<>();
            addMapping(tileMap, "-", "wall");
            addMapping(tileMap, "#", "wall");
            addMapping(tileMap, " ", "nothing");
            addMapping(tileMap, "|", "wall");
            addMapping(tileMap, ".", "floor, plains, grass, forests");
            addMapping(tileMap, "~", "river, swamps, sea");
            addMapping(tileMap, "&", "mountains, hills");
            
            for (Iterator<java.util.Map.Entry<String,List<String>>> iter = tileMap.entrySet().iterator(); iter.hasNext();) {
                java.util.Map.Entry<String,List<String>> entry = iter.next();
                List<String> names = entry.getValue();
                for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
                    String name = iterator.next();
                    charByTile.put(name, entry.getKey());
                }
            }
        }
        return tileMap;
    }

    public int countMatches(String string, String sub) {
        if (string == null || string.length() == 0 || sub == null || sub.length() == 0) return 0;
        int count = 0;
        int index = 0;
        while ((index = string.indexOf(sub, index)) != -1) {
            count++;
            index += sub.length();
        }
        return count;
    }

    public String mapToString(Map map) {
        StringBuffer buffer = new StringBuffer();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Thing[] things = map.getThings(x, y);
                if(things != null && things.length > 0) {
                    Thing thing = things[0];
                    for (int i = 0; i < things.length; i++) {
                        thing = things[i];
                        if(thing.getFlag("IsMessagePoint")) continue;
                    }
                    buffer.append(thing.getString("ASCII"));
                } else {
                    buffer.append(tileFor(map, x, y));
                }
            }
            buffer.append("\n");
        }
        if(buffer.length() > 0) buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }
    
    public String visitToString(Map map) {
        StringBuffer buffer = new StringBuffer();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++)
                buffer.append(map.getPath(x, y));
            buffer.append("\n");
        }
        if(buffer.length() > 0) buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    private String tileFor(Map map, int x, int y) {
        int tile = map.getTile(x, y);
        return Tile.getASCII(tile);
    }
    public java.util.Map<Character, Thing> getSeenMarkers() {
        return seenMarkers;
    }
}
