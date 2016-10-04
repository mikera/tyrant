package mikera.tyrant.perf;

import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.RPG;


public class CreateLib implements IWork {
    @Override
	public void run() {
        Lib.instance();
    }

    @Override
	public void setUp() {
        RPG.setRandSeed(0);
        Lib.clear();
    }

    @Override
	public String getMessage() {
        return "";
    }
}