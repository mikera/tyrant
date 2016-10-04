package mikera.tyrant.perf;

import mikera.engine.BaseObject;
import mikera.tyrant.Action;
import mikera.tyrant.Game;
import mikera.tyrant.GameScreen;
import mikera.tyrant.Hero;
import mikera.tyrant.QuestApp;
import mikera.tyrant.engine.Lib;
import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.test.MapHelper;
import mikera.tyrant.test.NullHandler;
import mikera.tyrant.test.TyrantTestCase;


public class KillAllBaddies implements IWork {
    private Thing hero;
    private Map map;
    private GameScreen gameScreen;

    public KillAllBaddies() {
    	// empty
    }
    
    @Override
	public void run() {
        boolean originalGetSet = BaseObject.GET_SET_DEBUG;
//        int ticks = -1;
        try {
            while (monstersAreLeft(map)) {
//                ticks++;
//                    if(ticks % 10 == 0) {
//                        System.err.println(map);
//                    }
//                    BaseObject.GET_SET_DEBUG = true;
                Action direction = hero.x == map.getWidth() - 2 ? Action.MOVE_W : Action.MOVE_E;
                gameScreen.tryTick(hero, direction, false);
                BaseObject.GET_SET_DEBUG = false;
            }
        } finally {
            BaseObject.GET_SET_DEBUG = originalGetSet;
        }
//        System.out.println("ticks " + ticks);
//            LibInspector libInspector = new LibInspector();
//            libInspector.go(new String[] {"IsHostile"});
//            libInspector.go(new String[] {"IsMobile"});
    }

    @Override
	public void setUp() {
        RPG.setRandSeed(0);
        Lib.clear();
        hero = Hero.createHero("bob", "human", "fighter");
        TyrantTestCase.setTestHero(hero);
        NullHandler.installNullMessageHandler();
        Game.setUserinterface(null);
        
        String mapString = 
            "################################" + "\n" +
            "#@.............................#" + "\n" +
            "################################";
        
        map = new MapHelper().createMap(mapString);
        for (int x = hero.x; x < map.getWidth(); x++) {
            if (!map.isBlocked(x, 1)) map.addThing(Lib.create("[IsMonster]"), x, 1);
        }
        hero.set("IsImmortal", true);
        gameScreen = new GameScreen(new QuestApp());
        gameScreen.map = map;
    }
    
    private boolean monstersAreLeft(mikera.tyrant.engine.Map map) {
        for (int i = 0; i < map.getThings().length; i++) {
            Thing thing = map.getThings()[i];
            if(thing.getFlag("IsHostile")) return true;
        }
        return false;
    }
    
    @Override
	public String getMessage() {
        return "";
    }
}
