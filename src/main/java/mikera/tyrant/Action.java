package mikera.tyrant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class Action implements Serializable {
    private static final long serialVersionUID = 1L;

    private static int nextId = 0;
    
    //Movement
    public static final Action UNKNOWN = new Action("Unknown");
    public static final Action MOVE_N = new Action("N");
    public static final Action MOVE_NE = new Action("NE");
    public static final Action MOVE_E = new Action("E");
    public static final Action MOVE_SE = new Action("SE");
    public static final Action MOVE_S = new Action("S");
    public static final Action MOVE_SW = new Action("SW");
    public static final Action MOVE_W = new Action("W");
    public static final Action MOVE_NW = new Action("NW");
    public static final Action MOVE_NOWHERE = new Action(".");

    //Actions
    public static final Action APPLY_SKILL = new Action("apply skill");
    public static final Action CHAT = new Action("chat");
    public static final Action DROP = new Action("drop");
    public static final Action EAT = new Action("eat");
    public static final Action FIRE = new Action("fire");
    public static final Action GIVE = new Action("give");
    public static final Action INVENTORY = new Action("inventory");
    public static final Action JUMP = new Action("jump");
    public static final Action KICK = new Action("kick");
    public static final Action LOOK = new Action("look");
    public static final Action MESSAGES = new Action("messages");
    public static final Action OPEN = new Action("open");
    public static final Action PICKUP = new Action("pickup");
    public static final Action QUAFF = new Action("quaff");
    public static final Action READ = new Action("read");
    public static final Action SEARCH = new Action("search");
    public static final Action THROW = new Action("throw");
    public static final Action USE = new Action("use");
    public static final Action VIEW_STATS = new Action("view_stats");
    public static final Action WIELD = new Action("wield");
    public static final Action WAIT = new Action("wait");
    public static final Action EXIT = new Action("exit");
    public static final Action PRAY = new Action("pray");
    public static final Action PRAY2 = new Action("pray2");
    public static final Action ZAP = new Action("zap");
    public static final Action ZAP_AGAIN = new Action("zap_again");
    public static final Action HELP = new Action("help");
    public static final Action SELECT_TILE = new Action("select_tile");
    public static final Action SAVE_GAME = new Action("save");
    public static final Action LOAD_GAME = new Action("load");
    public static final Action QUIT_GAME = new Action("quit");
    public static final Action SHOW_QUESTS = new Action("show_quests");
    public static final Action DEBUG = new Action("debug");
    public static final Action ZOOM_OUT = new Action("zoom");
    public static final Action ZOOM_IN = new Action("zoom");

    public static final Action OK = new Action("ok");
    public static final Action CANCEL = new Action("cancel");
    
    public static final Action DROP_EXTENDED = new Action("drop_extended");
    public static final Action PICKUP_EXTENDED = new Action("pickup_extended");

    private String name;
    private int id;
    
    private static Map<Integer, Action> allById;
    
    public Action(String name) {
        this(name, nextId++);
    }
    
    public Action(String name, int id) {
        this.name = name;
        this.id = id;
        getAllById().put(new Integer(id), this);
    }
    
    private Map<Integer, Action> getAllById() {
        if(allById == null)
            allById = new HashMap<>();
        return allById;
    }

    @Override
	public String toString() {
        return name;
    }
    
    public Object readResolve() {
        return allById.get(new Integer(id));
    }

    public boolean isMovementKey() {
        return 
        this == Action.MOVE_E || this == Action.MOVE_N ||
        this == Action.MOVE_NE || this == Action.MOVE_NW ||
        this == Action.MOVE_S || this == Action.MOVE_SE ||
        this == Action.MOVE_SW || this == Action.MOVE_W;
    }
}
