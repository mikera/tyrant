package mikera.tyrant.perf;

import mikera.engine.BaseObject;
import mikera.engine.Lib;
import mikera.engine.Map;
import mikera.engine.RPG;
import mikera.engine.Thing;
import mikera.tyrant.Action;
import mikera.tyrant.Game;
import mikera.tyrant.GameScreen;
import mikera.tyrant.Hero;
import mikera.tyrant.QuestApp;
import mikera.tyrant.test.MapHelper;
import mikera.tyrant.test.NullHandler;
import mikera.tyrant.test.TyrantTestCase;


public class MoreKilling implements IWork {
    private Thing hero;
    private Map map;
    private GameScreen gameScreen;

    public MoreKilling() {
    	// empty constructor
    }
    
    public void run() {
        boolean originalGetSet = BaseObject.GET_SET_DEBUG;
        try {
            while (hero.x < (map.getWidth() - 2)) {
                // BaseObject.GET_SET_DEBUG = true;
                gameScreen.tryTick(hero, Action.MOVE_E, false);
                // BaseObject.GET_SET_DEBUG = false;
            }
        } finally {
            BaseObject.GET_SET_DEBUG = originalGetSet;
        }    
    }

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
            "##.............................#" + "\n" +
            "#..............................#" + "\n" +
            "################################";
        
        map = new MapHelper().createMap(mapString);
        for (int x = hero.x; x < map.getWidth(); x++) {
            if (!map.isBlocked(x, 1)) {
                map.addThing(Lib.create("[IsMonster]"), x, 1);
                map.addThing(Lib.create("[IsItem]"), x, 1);
                map.addThing(Lib.create("menhir"), x, 2);
                map.addThing(Lib.create("[IsMonster]"), x, 3);
                map.addThing(Lib.create("[IsItem]"), x, 3);
            }
        }
        hero.set("IsImmortal", true);
        gameScreen = new GameScreen(new QuestApp());
        gameScreen.map = map;
    }
    
    public String getMessage() {
        return "";
    }
}
