package mikera.tyrant.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mikera.tyrant.engine.BaseObject;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.Count;
import mikera.tyrant.util.PrintfFormat;


public class LibInspector {
    class DepthOfAttribute implements IThingsInspector {
        private List<String> rows = new ArrayList<String>();
        private String attribute;

        public void inspect(Thing thing) {
            // Find at what depth the attribute is in a thing
            // A -> B -> C -> root
            // 0    1    2     3
            int totalDepth = depthOf(thing, 0);
            int depthOfAttribute = findDepthOfAttribute(attribute, thing, 0);
            rows.add(thing.getName());
            rows.add("" + depthOfAttribute);
            rows.add("" + totalDepth);
        }

        public void printResults() {
            System.out.println("distance from me | name | total depth > for " + attribute);
            for (Iterator<String> iter = rows.iterator(); iter.hasNext();) {
                String name = iter.next();
                String depthOfAttribute = iter.next();
                String totalDepth = iter.next();
                System.out.println(depthOfAttribute + " | " + name + " | " + totalDepth);
            }
        }

        public void setup(String[] args) {
            attribute = args[0];
        }
    }

    class AttributesForAllThings implements IThingsInspector {
        public void inspect(Thing thing) {
            for(Iterator<String> iterator = thing.getCollapsedMap().keySet().iterator(); iterator.hasNext();) {
                String attribute = iterator.next();
                Count count = attributes.get(attribute);
                if(count == null) {
                    count = new Count();
                    attributes.put(attribute, count);
                }
                count.value++;
            }
        }

        public void printResults() {
            for (Iterator<Entry<String, Count>> iter = attributes.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<String, Count> entry = iter.next();
                System.out.println(numberFormat.sprintf(entry.getValue()) + " " + entry.getKey());
            }
        }

        public void setup(String[] args) {
        	// empty
        }
    }

    private PrintfFormat nameFormat = new PrintfFormat("%-20s");
    protected PrintfFormat numberFormat = new PrintfFormat("%5s");
    private Map<Object, Integer> totalNumberOfAttributes = new HashMap<Object, Integer>();
    protected Map<String, Count> attributes = new HashMap<String, Count>();
    
    public static void main(String[] args) {
        new LibInspector().go(args);
    }

    protected void go(String[] args) {
        Lib library = Lib.instance();
        IThingsInspector inspector = new DepthOfAttribute();
        inspector.setup(args);
        for (Iterator<Thing> iter = library.getAll().iterator(); iter.hasNext();) {
            Thing thing = iter.next();
            inspector.inspect(thing);
        }
        inspector.printResults();
    }

    protected int findDepthOfAttribute(String attribute, BaseObject thing, int depth) {
        if(thing.getLocal() != null && thing.getLocal().containsKey(attribute)) return depth;
        if(thing.getInherited() == null) return -1;
        return findDepthOfAttribute(attribute, thing.getInherited(), depth + 1);
    }

    protected int depthOf(BaseObject thing, int depth) {
        if(thing.getInherited() == null) return depth;
        return depthOf(thing.getInherited(), depth + 1);
    }

    protected List<String> collectPercents(List<String> row, BaseObject thing, int totalNumberOfAttributes) {
        boolean topOfStack = row.isEmpty();
        if(thing == null) {
            if(!row.isEmpty() && row.get(row.size() - 1).equals(" > ")) row.remove(row.size() - 1);
            return row;
        }
        row.add(thing.get("Name") + " ");
        if (topOfStack) 
            row.add("[" + thing.getLocal().size() + "/" + totalNumberOfAttributes + "]");
        else 
            row.add("[" + thing.getLocal().size() + "]");
        row.add(" > ");
        return collectPercents(row, thing.getInherited(), totalNumberOfAttributes);
    }

    protected int total(BaseObject thing) {
        Integer total = totalNumberOfAttributes.get(thing.get("Name"));
        if(total == null) {
            total = new Integer(thing.size());
            totalNumberOfAttributes.put(thing.get("Name"), total);
        }
        return total.intValue();
    }

    protected int collectLocalAttributesAndAncestory(BaseObject thing, List<Object> row, int depth) {
        if(thing == null) {
            if(!row.isEmpty() && row.get(row.size() - 1).equals(" > ")) row.remove(row.size() - 1);
            return depth;
        }
        if(depth == 0) {
            row.add(nameFormat.sprintf(thing.get("Name")));
            row.add(" [");
            List<String> localAttributes = new ArrayList<String>(thing.getLocal().keySet());
            row.add("" + localAttributes.size());
            row.add("]");
        } else {
            row.add(" > ");
            row.add(thing.get("Name"));
        }
        return collectLocalAttributesAndAncestory(thing.getInherited(), row, depth + 1);
    }

    protected void addEachAttribute(List<String> row, List<String> localAttributes) {
        Collections.sort(localAttributes);
        for (Iterator<String> iter = localAttributes.iterator(); iter.hasNext();) {
            String attribute = iter.next();
            row.add(attribute);
            row.add(", ");
        }
        if(!row.isEmpty()) row.remove(row.size() - 1);
    }
}
