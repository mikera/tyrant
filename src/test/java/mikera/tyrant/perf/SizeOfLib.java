package mikera.tyrant.perf;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.RPG;


public class SizeOfLib implements IWork {
    private int size;

    @Override
	public void run() {
        Lib.instance();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(out);
            objectOut.writeObject(Lib.instance().getAll());
            size = out.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
	public void setUp() {
        RPG.setRandSeed(0);
        Lib.clear();
    }

    @Override
	public String getMessage() {
        return "" + size + " bytes";
    }
}