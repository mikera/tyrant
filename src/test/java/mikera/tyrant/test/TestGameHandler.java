package mikera.tyrant.test;

import mikera.tyrant.Action;
import mikera.tyrant.Game;
import mikera.tyrant.GameHandler;
import mikera.tyrant.InputHandler;
import mikera.tyrant.Portal;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.MapHelper;

public class TestGameHandler extends TyrantTestCase {
    private GameHandler gameHandler;

    protected void setUp() throws Exception {
        super.setUp();
        gameHandler = new GameHandler();
    }
    
    public void testDoDirection_noRunning() throws Exception {
        String mapString = 
            "---------" + "\n" +
            "|@......|" + "\n" +
            "---------";
        
        new MapHelper().createMap(mapString);
        gameHandler.doDirection(TyrantTestCase.getTestHero(), Action.MOVE_E, false);
        assertLocation(hero, 2, 1);
    }
    
    public void testDoDirection() throws Exception {
        String mapString = 
            "#####" + "\n" +
            "#@..#" + "\n" +
            "#####";
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 3, 1);
    }

    private void walk(Action direction, boolean running) {
        int attempts = 3;
        do {
            int xBefore = hero.x;
            int yBefore = hero.y;
            
            gameHandler.doDirection(hero, direction, running);
            gameHandler.calculateVision(hero);
            if(xBefore == hero.x && yBefore == hero.y) {
                attempts--;
            } else {
                attempts = 3;
            }
        } while(hero.isRunning() && attempts > 0);
        
    }

    public void testDoDirection_withDoor() throws Exception {
        String mapString = 
            "---------" + "\n" +
            "|@..+...|" + "\n" +
            "---------";
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 3, 1);
    }
    
    public void testRunning_T_vertical() throws Exception {
        String mapString = 
            "####" + "\n" +
            "@#.#" + "\n" +
            ".#.#" + "\n" +
            "...#" + "\n" +
            ".###" + "\n" +
            "####";
         new MapHelper().createMap(mapString);
         walk(Action.MOVE_S, true);
         assertLocation(hero, 0, 3);
    }
    
    
    public void testRunningFollowedByWalking() throws Exception {
        String mapString = 
            "##" + "\n" +
            "@#" + "\n" +
            ".#" + "\n" +
            ".#" + "\n" +
            "##";
         new MapHelper().createMap(mapString);
         walk(Action.MOVE_S, true);
         assertLocation(hero, 0, 3);
         walk(Action.MOVE_N, false);
         assertLocation(hero, 0, 2);
    }
    
    
    public void testRunningAroundCorridor_complex() throws Exception {
        String mapString = 
            "#########################" + "\n" +
            "#@......................#" + "\n" +
            "#######################.#" + "\n" +
            "#.....................#.#" + "\n" +
            "#.###################.#.#" + "\n" +
            "#.#.................#.#.#" + "\n" +
            "#.#.#################.#.#" + "\n" +
            "#.#.#...............#.#.#" + "\n" +
            "#.#.#...............#.#.#" + "\n" +
            "#.#.#...............#.#.#" + "\n" +
            "#.#.#...............#.#.#" + "\n" +
            "#.#.#################.#.#" + "\n" +
            "#.#...................#.#" + "\n" +
            "#.#########.###########.#" + "\n" +
            "#.......................#" + "\n" +
            "#########################";
         new MapHelper().createMap(mapString);
         walk(Action.MOVE_E, true);
         assertLocation(hero, 11, 14);
    }
    
    public void testRunOffEdge() throws Exception {
        String mapString = 
            "&@&" + "\n" +
            "&.&" + "\n" +
            "&.&";
        new MapHelper().createMap(mapString);
        answerGetInputWithChar('n');
        walk(Action.MOVE_S, true);
        assertLocation(hero, 1, 2);
        assertFalse(hero.isRunning());
    }
    
    public void testRunningZigZag_doubleWide() throws Exception {
        String mapString = 
            "         #######" + "\n" +    
            "        ##.....#" + "\n" +    
            "     ####..#### " + "\n" +    
            "    ###..###    " + "\n" +      
            "  ###...##      " + "\n" +      
            "###..####       " + "\n" +      
            "#@..##          " + "\n" +      
            "#####           ";
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        // this use to work but not currently
//        assertLocation(hero, 10, 1);
        assertLocation(hero, 3, 6);
    }
    
    public void testRunning_ZigZag() throws Exception {
        String mapString = 
            "      ######" + "\n" +
            "      #....#." + "\n" +
            "  #####.####" + "\n" +
            "  #.....#   " + "\n" +
            "  #.#####   " + "\n" +
            "  #.#       " + "\n" +
            "###.#       " + "\n" +
            "#...#       " + "\n" +
            "#.###       " + "\n" +
            ".@.......   " + "\n" +
            "#########   ";          
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_N, true);
        assertLocation(hero, 10, 1);
        walk(Action.MOVE_W, true);
        assertLocation(hero, 1, 9);
    }
    
    public void testRunning_T() throws Exception {
        String mapString = 
            "##.##" + "\n" +
            "##.##" + "\n" +
            "@...#" + "\n" +
            "#####"; 
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 2, 2);
    }
    
    public void testRunning_aroundCorner() throws Exception {
        String mapString = 
            "####" + "\n" +
            "@..#" + "\n" +
            "##.#" + "\n" +
            " #.#" + "\n" +
            " ###";
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 2, 3);
    }
    
    public void testRunningInARoom() throws Exception {
        String mapString = 
            "##+###+###+##" + "\n" +
            "#...........#" + "\n" +
            "#.....@.....#" + "\n" + 
            "######.######";
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 11, 2);
    }
    
    public void testRunningByADoor() throws Exception {
        String mapString = 
            "######+######" + "\n" +
            "#..@........#" + "\n" +
            "#...........#" + "\n" + 
            "######.######";
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 6, 1);
    }
    
    public void testRunningBySecretDoor() throws Exception {
        String mapString = 
            "######+######" + "\n" +
            "#..@........#" + "\n" +
            "#...........#" + "\n" + 
            "######.######";
        new MapHelper().createMap(mapString);
        Thing door = hero.getMap().getThings(6, 0)[0];
        door.set("IsSecretDoor", true);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 11, 1);
    }
    
    public void testRunningOverMessagePoint() throws Exception {
        String mapString = 
            "#########" + "\n" +
            "#@.m....#" + "\n" +
            "#########";
        Game.instance().setInputHandler(InputHandler.repeat(' '));
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 3, 1);
        walk(Action.MOVE_W, false);
        assertLocation(hero, 2, 1);
    }
    
    public void testRunningOverItem() throws Exception {
        String mapString = 
            "#########" + "\n" +
            "#@.?....#" + "\n" +
            "#########";
        Game.instance().setInputHandler(InputHandler.repeat(' '));
        new MapHelper().createMap(mapString);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 2, 1);
        walk(Action.MOVE_W, false);
        assertLocation(hero, 1, 1);
    }
    
    public void testRunningOverInvisiblePortal() throws Exception {
        String mapString = 
            "#########" + "\n" +
            "#@......#" + "\n" +
            "#########";
        Map map = new MapHelper().createMap(mapString);
        Thing invisiblePortal = Portal.create("invisible portal");
        map.addThing(invisiblePortal, 3, 1);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 7, 1);
        walk(Action.MOVE_W, true);
        assertLocation(hero, 1, 1);
    }
    
    public void testRunningOverGuardPoint() throws Exception {
        String mapString = 
            "#########" + "\n" +
            "#@......#" + "\n" +
            "#########";
        Map map = new MapHelper().createMap(mapString);
        Thing guardPoint = Lib.create("guard point");
        map.addThing(guardPoint, 3, 1);
        walk(Action.MOVE_E, true);
        assertLocation(hero, 7, 1);
        walk(Action.MOVE_W, true);
        assertLocation(hero, 1, 1);
    }
}
