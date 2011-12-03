package mikera.tyrant;

import java.awt.event.KeyEvent;

import mikera.engine.Map;
import mikera.engine.Point;
import mikera.engine.Thing;


public class GameHandler {
    private ActionMapping actionMapping;
    
    public GameHandler() {
        actionMapping = new ActionMapping();
        actionMapping.addDefaultMappings();
//        actionMapping.addRougeLikeMappings();
    }
    
    public void doDirection(Thing thing, Action action, boolean isShiftDown) {
        Map map = thing.getMap();
        Point direction = convertActionToDirection(action);

        // handle movement commands
        if (map.getTile(thing.x + direction.x, thing.y + direction.y) != 0) {
            if (isShiftDown) {
                if (thing.getStat("DirectionX") != direction.x || thing.getStat("DirectionY") != direction.y || !thing.isRunning()) {
                    /* This is needed for running around corners. The DirectionX changes when you run around a corner
                     * but the key sent doesn't so direction.x is stale. Only clear the path when RunDirectionX is reset.
                     */
                    if(thing.getStat("RunDirectionX") == Integer.MIN_VALUE) {
                        map.clearPath();    
                    }
                }
                thing.isRunning(true);
            }
            boolean moved = Movement.tryMove(thing, map, thing.x + direction.x, thing.y + direction.y);
            if (!moved) {
                thing.isRunning(false);
            }
        } else if (map.canExit()) {
            thing.isRunning(false); // end running
            Game.message("Exit area? (y/n)");
            char c = Game.getOption("yn123456789");
            Game.messagepanel.clear();
            if ((thing.x == 0) && ("147".indexOf(c) >= 0)) c = 'y';
            if ((thing.y == 0) && ("789".indexOf(c) >= 0)) c = 'y';
            if ((thing.x == (map.getWidth() - 1)) && ("369".indexOf(c) >= 0)) c = 'y';
            if ((thing.y == (map.getWidth() - 1)) && ("123".indexOf(c) >= 0)) c = 'y';

            if (c == 'y') {
                map.exitMap(thing.x, thing.y);
            }
        } else if(map.getFlag("IsWorldMap")) {
            if(isShiftDown) {
                boolean atEdge = (thing.x == 0 && direction.x == -1) ||
                    (thing.x == map.getWidth() - 1 && direction.x == 1) ||
                    (thing.y == 0 && direction.y == -1) ||
                    (thing.y == map.getHeight() - 1 && direction.y == 1);
                thing.isRunning(!atEdge);
            }
        }
    }
    
    public void calculateVision(Thing thing) {
       thing.calculateVision();
    }
    
    public Action actionFor(KeyEvent keyEvent) {
        return actionMapping.actionFor(keyEvent);
    }
    
    public Point convertKeyToDirection(char k) {
        Action action = actionMapping.convertKeyToAction(k);
        return convertActionToDirection(action);
    }

    public static Point convertActionToDirection(Action action) {
        Point direction = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
        if (action == Action.MOVE_N) {
            direction.x = 0;
            direction.y = -1;
        }
        if (action == Action.MOVE_S) {
            direction.x = 0;
            direction.y = 1;
        }
        if (action == Action.MOVE_W) {
            direction.x = -1;
            direction.y = 0;
        }
        if (action == Action.MOVE_E) {
            direction.x = 1;
            direction.y = 0;
        }
        if (action == Action.MOVE_NW) {
            direction.x = -1;
            direction.y = -1;
        }
        if (action == Action.MOVE_NE) {
            direction.x = 1;
            direction.y = -1;
        }
        if (action == Action.MOVE_SW) {
            direction.x = -1;
            direction.y = 1;
        }
        if (action == Action.MOVE_SE) {
            direction.x = 1;
            direction.y = 1;
        }
        if (action == Action.MOVE_NOWHERE) {
            direction.x = 0;
            direction.y = 0;
        }
        return direction.x == Integer.MIN_VALUE ? null : direction;
    }
    
    public void setActionMapping(ActionMapping actionMapping) {
        this.actionMapping = actionMapping;
    }
}
