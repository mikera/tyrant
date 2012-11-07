package mikera.tyrant.perf;

import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.RPG;


public class CreateLib implements IWork {
    public void run() {
        Lib.instance();
    }

    public void setUp() {
        RPG.setRandSeed(0);
        Lib.clear();
    }

    public String getMessage() {
        return "";
    }
}