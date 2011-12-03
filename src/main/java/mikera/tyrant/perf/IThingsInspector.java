package mikera.tyrant.perf;

import mikera.engine.Thing;


public interface IThingsInspector {
    void inspect(Thing thing);
    void printResults();
    void setup(String[] args);
}
