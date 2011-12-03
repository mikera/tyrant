package mikera.tyrant;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;


public class ActionMapping {
    private Map mappings = new HashMap();
    
    public Action convertKeyToAction(char key) {
        Action action = (Action) mappings.get(new Character(key));
        return action == null ? Action.UNKNOWN : action;
    }
    
    public void clear() {
        mappings.clear();
    }
    
    public void map(char aChar, Action action) {
        mappings.put(new Character(aChar), action);
    }
    
    public void addDefaultMappings() {
        addDefaultMovementActions();
        
        mappings.put("KeyEvent" + KeyEvent.VK_ENTER, Action.OK);
        mappings.put("KeyEvent" + KeyEvent.VK_ESCAPE, Action.CANCEL);

        mappings.put("KeyEvent" + KeyEvent.VK_F1, Action.HELP);
        mappings.put("KeyEvent" + KeyEvent.VK_F2, Action.ZOOM_OUT);
        mappings.put("KeyEvent" + KeyEvent.VK_F3, Action.ZOOM_IN);
        mappings.put("KeyEvent" + KeyEvent.VK_F5, Action.DEBUG);

        mappings.put(new Character('a'), Action.APPLY_SKILL);
        mappings.put(new Character('c'), Action.CHAT);
        mappings.put(new Character('d'), Action.DROP);
        mappings.put(new Character('D'), Action.DROP_EXTENDED);
        mappings.put(new Character('e'), Action.EAT);
        mappings.put(new Character('f'), Action.FIRE);
        mappings.put(new Character('g'), Action.GIVE);
        mappings.put(new Character('i'), Action.INVENTORY);
        mappings.put(new Character('j'), Action.JUMP);
        mappings.put(new Character('k'), Action.KICK);
        mappings.put(new Character('l'), Action.LOOK);
        mappings.put(new Character('m'), Action.MESSAGES);
        mappings.put(new Character('o'), Action.OPEN);
        mappings.put(new Character('p'), Action.PICKUP);
        mappings.put(new Character(','), Action.PICKUP_EXTENDED);
        mappings.put(new Character('P'), Action.PICKUP_EXTENDED);
        mappings.put(new Character('q'), Action.QUAFF);
        mappings.put(new Character('r'), Action.READ);
        mappings.put(new Character('s'), Action.SEARCH);
        mappings.put(new Character('t'), Action.THROW);
        mappings.put(new Character('u'), Action.USE);
        mappings.put(new Character('v'), Action.VIEW_STATS);
        mappings.put(new Character('@'), Action.VIEW_STATS);
        mappings.put(new Character('w'), Action.WIELD);
        mappings.put(new Character('x'), Action.EXIT);
        mappings.put(new Character('<'), Action.EXIT);
        mappings.put(new Character('>'), Action.EXIT);
        mappings.put(new Character('_'), Action.PRAY);
        mappings.put(new Character('y'), Action.PRAY2);
        mappings.put(new Character('z'), Action.ZAP);
        mappings.put(new Character('`'), Action.ZAP_AGAIN);
        mappings.put(new Character('?'), Action.HELP);
        mappings.put(new Character(';'), Action.SELECT_TILE);
        mappings.put(new Character(' '), Action.WAIT);
        mappings.put(new Character('.'), Action.WAIT);
        mappings.put(new Character('-'), Action.SAVE_GAME);
        mappings.put(new Character('+'), Action.LOAD_GAME);
        mappings.put(new Character('='), Action.LOAD_GAME);
        mappings.put(new Character('#'), Action.SHOW_QUESTS);
        mappings.put(new Character(':'), Action.DEBUG);
        mappings.put(new Character('('), Action.ZOOM_OUT);
        mappings.put(new Character(')'), Action.ZOOM_IN);
        
        mappings.put("Control" + KeyEvent.VK_L , Action.LOAD_GAME);
        mappings.put("Control" + KeyEvent.VK_S , Action.SAVE_GAME);
        mappings.put("Control" + KeyEvent.VK_X , Action.QUIT_GAME);
    }

    public void addDefaultMovementActions() {
        //Movement
        mappings.put(new Character('1'), Action.MOVE_SW);
        mappings.put("KeyEvent" + KeyEvent.VK_END, Action.MOVE_SW);
        mappings.put(new Character('2'), Action.MOVE_S);
        mappings.put("KeyEvent" + KeyEvent.VK_DOWN, Action.MOVE_S);
        mappings.put(new Character('3'), Action.MOVE_SE);
        mappings.put("KeyEvent" + KeyEvent.VK_PAGE_DOWN, Action.MOVE_SE);
        mappings.put(new Character('4'), Action.MOVE_W);
        mappings.put("KeyEvent" + KeyEvent.VK_LEFT, Action.MOVE_W);
        mappings.put(new Character('5'), Action.MOVE_NOWHERE);
        mappings.put(new Character('.'), Action.MOVE_NOWHERE);
        mappings.put(new Character('6'), Action.MOVE_E);
        mappings.put("KeyEvent" + KeyEvent.VK_RIGHT, Action.MOVE_E);
        mappings.put(new Character('7'), Action.MOVE_NW);
        mappings.put("KeyEvent" + KeyEvent.VK_HOME, Action.MOVE_NW);
        mappings.put(new Character('8'), Action.MOVE_N);
        mappings.put("KeyEvent" + KeyEvent.VK_UP, Action.MOVE_N);
        mappings.put(new Character('9'), Action.MOVE_NE);
        mappings.put("KeyEvent" + KeyEvent.VK_PAGE_UP, Action.MOVE_NE);
    }
    
    public void addRougeLikeMappings() {
        //Movement
        mappings.put(new Character('b'), Action.MOVE_SW);
        mappings.put(new Character('j'), Action.MOVE_S);
        mappings.put(new Character('n'), Action.MOVE_SE);
        mappings.put(new Character('h'), Action.MOVE_W);
        mappings.put(new Character('.'), Action.MOVE_NOWHERE);
        mappings.put(new Character('l'), Action.MOVE_E);
        mappings.put(new Character('y'), Action.MOVE_NW);
        mappings.put(new Character('k'), Action.MOVE_N);
        mappings.put(new Character('u'), Action.MOVE_NE);
    }

    public Action actionFor(KeyEvent keyEvent) {
        char keyChar = keyEvent.getKeyChar();
        if(keyChar == KeyEvent.CHAR_UNDEFINED) {
            return (Action) mappings.get("KeyEvent" + keyEvent.getKeyCode());
        }
        //Make sure we can control-something events
        if( keyEvent.isControlDown() ){
          //Not sure the puritain JDK will like this
          //Sun should sue those Eclipse guys
          return (Action) mappings.get("Control" + keyEvent.getKeyCode() );
        }
        return (Action) mappings.get(new Character(keyChar));
    }
}
