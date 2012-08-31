package mikera.tyrant.test;

import mikera.tyrant.Town;
import mikera.tyrant.engine.Map;

public class TestTown extends TyrantTestCase {
    public void testTownBuilding() throws Exception {
        String mapString = 
            "----------------" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "|..............|" + "\n" +
            "----------------";
        Map map = new MapHelper().createMap(mapString);
        Town.addStandardRoom(map, 1, 1, 5, 5, 3, 5);
        //TODO Finish this testcase
    }
}
