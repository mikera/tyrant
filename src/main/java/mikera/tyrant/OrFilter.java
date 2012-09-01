package mikera.tyrant;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mikera.tyrant.engine.Thing;


public class OrFilter implements IThingFilter {
    private List<IThingFilter> filters = new LinkedList<>();
    
    public OrFilter(IThingFilter filterA, IThingFilter filterB) {
        addFilter(filterA);
        addFilter(filterB);
    }
    public OrFilter(IThingFilter filterA, IThingFilter filterB, IThingFilter filterC) {
        addFilter(filterA);
        addFilter(filterB);
        addFilter(filterC);
    }
    
    public void addFilter(IThingFilter filter) {
        filters.add(filter);
    }
    
    public boolean accept(Thing thing, String query) {
        for (Iterator<IThingFilter> iter = filters.iterator(); iter.hasNext();) {
            IThingFilter filter = iter.next();
            if(filter.accept(thing, query)) return true;
        }
        return false;
    }
}
