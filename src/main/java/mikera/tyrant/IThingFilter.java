package mikera.tyrant;

import mikera.engine.Thing;


public interface IThingFilter {
    boolean accept(Thing thing, String query);
}
