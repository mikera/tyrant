package mikera.tyrant;

import mikera.tyrant.engine.Thing;


public interface IThingFilter {
    boolean accept(Thing thing, String query);
}
