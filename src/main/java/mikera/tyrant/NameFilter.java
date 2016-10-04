package mikera.tyrant;

import mikera.tyrant.engine.Describer;
import mikera.tyrant.engine.Description;
import mikera.tyrant.engine.Thing;


public class NameFilter implements IThingFilter {
    @Override
	public boolean accept(Thing thing, String query) {
        if(query == null) return true;
        query = query.trim();
        String name = Describer.describe(null, thing, Description.ARTICLE_NONE);
        return name.indexOf(query) >= 0;
    }
}
