package mikera.tyrant.test;

import mikera.tyrant.Event;
import mikera.tyrant.Scripts;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.MapHelper;

public class TestScenery extends TyrantTestCase {

    public void testGenerateInPlaceScript() {
        String mapString = 
            // 0123
              "----" + "\n" +
              "|.@|" + "\n" +
              "----";
        final Thing patch = Lib.create("carrot");
        patch.addHandler("OnAction",Scripts.generatorInPlace("raspberry",100));
        Map map = new MapHelper().createMap(mapString);
        map.addThing(patch,1,1);
        Thing[] things = map.getThings(1,1);
        assertEquals(1,things.length); // our carrot
        // wait long enough to ensure the time * generation rate >= 1000000
        patch.handle(Event.createActionEvent(10000));
        things = map.getThings(1,1);
        assertEquals(2,things.length); // a carrot and a raspberry
    }
}
