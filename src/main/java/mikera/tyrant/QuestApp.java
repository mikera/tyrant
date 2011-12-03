package mikera.tyrant;

// This is the main Applet class for Tyrant

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Hashtable;

import mikera.engine.Lib;
import mikera.engine.Map;
import mikera.engine.RPG;
import mikera.engine.Thing;
import mikera.util.Rand;

/**
 * The main Tyrant applet class, also used as the root UI component when run as
 * an application
 * 
 * TODO: move game-related code out of QuestApp.
 */
public class QuestApp extends Applet implements Runnable {
	private static final long serialVersionUID = 3257569503247284020L;

	// Images
	public static Image tiles;

	public static Image greytiles;

	public static Image scenery;

	public static Image creatures;

	public static Image items;

	public static Image effects;

	public static Image title;

	public static Image paneltexture;

	public static Hashtable images = new Hashtable();

	public static Font mainfont = new Font("Monospaced", Font.PLAIN, 12);

	public static int charsize;

	public static final Color TEXTCOLOUR = new Color(192, 192, 192);

	public static final Color BACKCOLOUR = new Color(0, 0, 0);

	//	public static final Color panelcolor = new Color(64, 64, 64);
	//	public static final Color panelhighlight = new Color(96, 96, 96);
	//	public static final Color panelshadow = new Color(32, 32, 32);

	public static final Color PANELCOLOUR = new Color(64, 64, 64);

	public static final Color PANELHIGHLIGHT = new Color(120, 80, 20);

	public static final Color PANELSHADOW = new Color(40, 20, 5);

	public static final Color INFOSCREENCOLOUR = new Color(0, 0, 0);

	public static final Color INFOTEXTCOLOUR = new Color(240, 200, 160);
	
	public static final Color INFOTEXTCOLOUR_GRAY = new Color(100, 100, 100);

	//  public static final Color panelcolor=new Color(128,64,32);
	//  public static final Color panelhighlight=new Color(208,80,48);
	//  public static final Color panelshadow=new Color(80,32,16);

	private GameScreen screen;

	private Component mainComponent = null;

	private static QuestApp instance;

	public static boolean isapplet = true;

	public static String gameFileFromCommandLine;

	public static String fileEncoding = System.getProperty("file.encoding");

	public Dimension getPreferredSize() {
		return new Dimension(640 + 55, 480 + 150);
	}

	// stop the applet, freeing all resources used
	public void stop() {
		super.stop();
		setInstance(null);
		Game.messagepanel = null;
		Game.thread = null;
	}

	// image filter object to create greyed tiles
	static class GreyFilter extends RGBImageFilter {
		public GreyFilter() {
			canFilterIndexColorModel = true;
		}

		public int filterRGB(int x, int y, int rgb) {
			return (rgb & 0xff000000)
					| (0x10101 * (((rgb & 0xff0000) >> 18)
							+ ((rgb & 0xff00) >> 10) + ((rgb & 0xff) >> 2)));
		}
	}

	public void init(Runnable runnable) {
		// recreate lib in background

		setInstance(this);
		Game.setQuestapp(this);

		super.init();
		setLayout(new BorderLayout());
		setBackground(Color.black);
		setFont(mainfont);

		// Game.warn("Focus owned by:
		// "+KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());

		// set game in action
		Game.setUserinterface(new Interface());
		Game.thread = new Thread(runnable);
		Game.thread.start();
	}

	// inits the applet, loading all necessary resources
	// also kicks off the actual game thread
	public void init() {
		init(this);
	}

	public QuestApp() {
		super();
	}

	// creates a hero according to specified parameters
	public Thing createHero(boolean prompts) {
		long start = System.currentTimeMillis();
		String race = null;
		String profession = null;

		if (!prompts) {
			race = "human";
			profession = "fighter";
			Game.setDebug(true);
		}

		// get list of races
		String[] raceherostrings = Hero.heroRaces();
		String[] racedescriptions = Hero.heroRaceDescriptions();
		if (race == null) {
			DetailedListScreen ls = new DetailedListScreen(
					"What race are you?", raceherostrings, racedescriptions);
			ls.setForeground(new Color(128, 128, 128));
			ls.setBackground(new Color(0, 0, 0));
			ls.bottomString = "Press a letter key to select your race";
			switchScreen(ls);
			while (true) {
				race = (String) ls.getObject();
				//Game.warn(race);
				if ((race != null) || Game.isDebug())
					break;
			}
		}

		if (race == null) {
			// Debug mode only
			// have escaped, so choose randomly
			race = raceherostrings[Rand.r(raceherostrings.length)];
			String[] herostrings = Hero.heroProfessions(race);
			profession = herostrings[Rand.r(herostrings.length)];
		}

		// get list of possible prfessions
		String[] professionstrings = Hero.heroProfessions(race);
		String[] professiondescriptions = Hero.heroProfessionDescriptions(race);
		if (profession == null) {

			DetailedListScreen ls = new DetailedListScreen(
					"What is your profession?", professionstrings,
					professiondescriptions);
			ls.bottomString = "Press a letter key to select your profession";
			ls.setForeground(new Color(128, 128, 128));
			ls.setBackground(new Color(0, 0, 0));
			switchScreen(ls);

			while (profession == null) {
				profession = (String) ls.getObject();
			}
		}

		Thing h = Hero.createHero(prompts ? null : "QuickTester", race,
				profession);

		// hero name and history display
		String name = "QuickTester";
		if (prompts) {
			// setup screen to get name
			Screen ss = new Screen(this);
			ss.setBackground(new Color(0, 0, 0));
			ss.setLayout(new BorderLayout());
			{
				InfoScreen ts = new InfoScreen(this, h.getString("HeroHistory"));
				ts.setBackground(new Color(0, 0, 0));
				ss.add("Center", ts);
			}
			MessagePanel mp = new MessagePanel(this);
			Game.messagepanel = mp;
			ss.add("South", mp);

			switchScreen(ss);

			name = getHeroName(true);
			if (name == null)
				return null;
		}
		Hero.setHeroName(h, name);

		System.out.println((System.currentTimeMillis() - start)
				+ "ms to createHero");
		return h;
	}

	public boolean isGameScreen() {
		return (screen != null) && (mainComponent == screen);
	}

	// switches to a new screen, discarding the old one
	public void switchScreen(Component s) {
		if (s == null) {
			return;
		}
		if (mainComponent == s) {
			// alreay on correct component!
			s.repaint();
			return;
		}

		setVisible(false);
		removeAll();
		add(s);
		if (s instanceof GameScreen) {
			((GameScreen) s).getMappanel().setPosition(Game.hero().getMap(),
					Game.hero().x, Game.hero().y);
		}
		invalidate();
		validate();

		setVisible(true);
		/*
		 * CBG This is needed to give the focus to the contained screen.
		 * RequestFocusInWindow is preferable to requestFocus.
		 */

		s.requestFocus();
		mainComponent = s;
	}

	public String getHeroName(boolean notNull) {

		//ss.invalidate();
		//ss.validate();
		//ss.repaint();

		// get hero name
		String hname = null;

		Game.message("");
		while ((hname == null) || hname.equals("")) {
			hname = Game.getLine("Enter your name: ");
			if (hname.equals("ESC")) {
				return null;
			}
			if ((!notNull) && ((hname == null) || hname.equals("")))
				return null;
		}
		return hname;
	}

	// this is the actual game thread start
	// it loops for each complete game played
	public void run() {
		while (true) {

			Screen ss = new Screen(this);
			ss.setBackground(new Color(0, 0, 0));
			ss.setLayout(new BorderLayout());
			{
				TitleScreen ts = new TitleScreen(this);
				ts.setBackground(new Color(0, 0, 0));
				ss.add("Center", ts);
			}
			MessagePanel mp = new MessagePanel(this);
			Game.messagepanel = mp;
			ss.add("South", mp);

			switchScreen(ss);

			repaint();

			if (!isapplet && gameFileFromCommandLine != null) {
				Game.message("Loading " + gameFileFromCommandLine
						+ " game file...");
				final String ret = Game.tryToRestore(gameFileFromCommandLine);
				if (ret == null) {
					setupScreen();
					getScreen().mainLoop();
					continue;
				}

				Game.message("Load game failed: " + ret);
				Game.message("Press any key (except Tab) to continue");
				Game.getInput(false); //!!! not very good - this does not
				//recognize Tab key, for instance

			}

			Game.message("");
			Game.message("Welcome to Tyrant. You are playing version "
					+ Game.VERSION + ". Would you like to:");
			Game.message(" [a] Create a new character");
			Game.message(" [b] Load a previously saved game");
			Game.message(" [c] Play in debug mode");
			Game.message(" [d] QuickStart debug mode");
			Game.message(" [e] Edit a map");
			mp.repaint();

			// create lib in background
			Game.asynchronousCreateLib();

			char c = Game.getOption("abcdeQ");

			Game.setDebug(false);
			Game.visuals = true;

			if (c == 'b') {
				if (Game.restore()) {
					setupScreen();
					getScreen().mainLoop();

				}

			} else if (c == 'c') {
				// do hero creation
				Game.create();
				Thing h = createHero(true);
				if (h == null)
					continue;

				Game.setDebug(true);
				setupScreen();
				gameStart();

			} else if (c == 'e') {
				//Designer
				Game.message("");
				Game.message("Launching Designer...");
				mikera.tyrant.author.Designer.main(new String[] { "embedded" });
				continue;

			} else {

				Game.create();
				Thing h = createHero(true);

				if (h == null)
					continue;

				// first display starting info....
				InfoScreen l = new InfoScreen(
						this,
						"                                 Introduction\n"
								+ "\n"
								+ "Times are hard for the humble adventurer. Lawlessness has ravaged the land, and few can afford to pay for your services.\n"
								+ "\n"
								+ "After many weeks of travel, you find yourself in the valley of North Karrain. This region has suffered less badly from the incursions of evil, and you hear that some small towns are still prosperous. Perhaps here you can find a way to make your fortune.\n"
								+ "\n"
								+ "After a long day of travel, you see a small inn to the west. Perhaps this would be a good place to meet some and learn some more about these strange lands.\n"
								+ "\n"
								+ "                           [ Press a key to continue ]\n"
								+ "\n" + "\n" + "\n" + "\n" + "\n");

				l.setForeground(new Color(192, 160, 64));
				l.setBackground(new Color(0, 0, 0));
				switchScreen(l);
				Game.getInput();
				setupScreen();
				gameStart();

				//Debug mode should not start when pressing Enter!!
				//Game.create();
				//Game.setDebug(true);
				//createHero(false);
				//setupScreen();
				//gameStart();
			}
		}
	}

	public void setupScreen() {
		if (getScreen() == null) {
			setScreen(new GameScreen(this));
		} else {
			//only need to reset the messages,
			// otherwise we will start to
			// leak memory/threads
			Game.messagepanel = getScreen().messagepanel;
			Game.messagepanel.clear();
		}
		switchScreen(getScreen());
	}

	private void gameStart(Map map, int entranceX, int entranceY) {

		Game.enterMap(map, entranceX, entranceY);

		// run the game
		getScreen().mainLoop();
	}

	public void gameStart() {
		Game.setQuestapp(this);
		Thing h = Game.hero();
		if (h == null)
			throw new Error("Hero not created");
		Game.instance().initialize(h);

		Map world = Game.instance().createWorld();

		Quest.addQuest(h, Quest.createVisitMapQuest("Vist a town", "town"));

		Thing port = world.find("tutorial inn");
		Map tm = Portal.getTargetMap(port);
		gameStart(tm, tm.getEntrance().x, tm.getEntrance().y);
	}

	private String getDeathString(Thing h) {
		if (h.getStat("HPS") <= 0) {
			Thing t = h.getThing("Killer");
			if (t == null) {
				return "Killed by divine power";
			}
			t.remove();

			String killer = t.getAName();
			if (t.getFlag("IsEffect"))
				killer = t.name();

			if (killer.equals("you"))
				killer = "stupidity";
			return "Killed by " + killer;
		}

		return "Defeated The Tyrant";
	}

	public void gameOver() {
		Wish.makeWish("identification", 100);
		Game.message("");

		Thing h = Game.hero();

		String outcome = getDeathString(h);

		String story = null;

		getScreen().getMappanel().repaint();

		String hresult = "No high score available in debug mode";

		int sc = h.getStat("Score");
		String score = Integer.toString(sc);
		String level = Integer.toString(h.getLevel());
		String seed = Integer.toString(h.getStat("Seed"));
		String name = h.getString("HeroName");
		String profession = h.getString("Profession");
		String race = h.getString("Race");

		try {
			String urldeath = URLEncoder.encode(outcome, fileEncoding);
			String urlname = URLEncoder.encode(name, fileEncoding);

			String check = Integer.toString((sc + name.length()
					* profession.length() * race.length()) ^ 12345678);
			String st = "&name=" + urlname + "&race=" + race + "&profession="
					+ profession + "&level=" + level + "&score=" + score
					+ "&check=" + check + "&version=" + Game.VERSION + "&seed="
					+ seed + "&death=" + urldeath;

			String url = "http://tyrant.sourceforge.net/logscore.php?client=tyrant"
					+ st;

			Game.warn((Game.isDebug() ? "NOT " : "") + "Sending data:");
			Game.warn(st);

			if (!Game.isDebug()) {
				URL u = new URL(url);
				InputStream s = u.openStream();

				String returnstring = "";
				int b = s.read();
				while (b >= 0) {
					returnstring = returnstring + (char) b;
					b = s.read();
				}

				int ok = returnstring.indexOf("OK:");
				if (ok >= 0) {
					hresult = "High score logged.\n";
					hresult += "You are in position "
							+ returnstring.substring(ok + 3).trim();
				} else {
					hresult = "Failed to log high score";
					Game.warn(returnstring);
				}
			}
		} catch (Exception e) {
			Game.warn(e.getMessage());
			hresult = "High score feature not available";
		}

		if ((!h.isDead())) {
			story = "You have defeated The Tyrant!\n"
					+ "\n"
					+ "Having saved the world from such malevolent evil, you are crowned as the new Emperor of Daedor, greatly beloved by all the people of the Earth.\n"
					+ "\n"
					+ "You rule an Empire of peace and prosperity, and enjoy a long and happy life.\n"
					+ "\n" + "Hurrah for Emperor " + h.getString("HeroName")
					+ "!!\n";

			if (Game.isDebug()) {
				story = "You have defeated The Tyrant in Debug Mode.\n" + "\n"
						+ "Now go and do it the hard way....\n";
			}

		} else {
			story = "\n"
					+ "It's all over...... "
					+ outcome
					+ "\n"
					+ "\n"
					+ "You have failed in your adventures and died a hideous death.\n"
					+ "\n" + "You reached level " + level + "\n"
					+ "Your score is " + score + "\n" + "\n" + hresult + "\n";
		}

		Game.message("GAME OVER - " + outcome);

		Game.message("Would you like to see your final posessions? (y/n)");

		char c = Game.getOption("yn");

		if (c == 'y') {
			Game.selectItem("Your final posessions:", h);
		}

		// display the final story
		Game.scrollTextScreen(story);
		
		
		
		// display the final story
		String killData=Hero.reportKillData();
		Game.scrollTextScreen(killData);

		Game.over = true;

		Lib.clear();

		// recreate lib in background
		Game.asynchronousCreateLib();
	}

	public KeyAdapter keyhandler = null;

	// All keypresses get directed here.....
	public final KeyAdapter keyadapter = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			//Game.warn("Focus owned by:
			// "+KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
			//Game.warn(""+e.getKeyChar());

			// call the currently registered keyhandler
			if (keyhandler != null) {
				keyhandler.keyPressed(e);
			} else {
				Game.getUserinterface().go(e);
			}
		}
	};

	public void destroy() {
		removeAll();
	}

	// loads an image from wherever possible
	// ideally, from the .jar resource bundle
	// not sure if all of this is necessary
	// but try to cover all possible environments
	public static Image getImage(String filename) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();

		Image image = null;

		try {
			URL imageURL = QuestApp.class.getResource(filename);
			if (imageURL != null) {
				image = toolkit.getImage(imageURL);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return image;
	}

	/**
	 * @param screen
	 *            The screen to set.
	 */
	public void setScreen(GameScreen screen) {
		this.screen = screen;
	}

	/**
	 * @return Returns the screen.
	 */
	public GameScreen getScreen() {
		return screen;
	}

	public static void setInstance(QuestApp instance) {
		QuestApp.instance = instance;
	}

	public static QuestApp getInstance() {
		if (instance == null) {
			instance = new QuestApp();
		}
		return instance;
	}

	static {
		Applet applet = new Applet();

		tiles = getImage("/images/tiles32.png");
		scenery = getImage("/images/scenery32.png");
		creatures = getImage("/images/creature32.png");
		items = getImage("/images/items32.png");
		effects = getImage("/images/effects32.png");

		title = getImage("/images/title.png");
		paneltexture = getImage("/images/texture3.png");

		// store images in source hashtable
		images.put("Tiles", tiles);
		images.put("Scenery", scenery);
		images.put("Creatures", creatures);
		images.put("Items", items);
		images.put("Effects", effects);

		// Create mediatracker for the images
		MediaTracker mediaTracker = new MediaTracker(applet);
		mediaTracker.addImage(tiles, 1);
		mediaTracker.addImage(scenery, 1);
		mediaTracker.addImage(creatures, 1);
		mediaTracker.addImage(items, 1);
		mediaTracker.addImage(effects, 1);
		mediaTracker.addImage(title, 1);
		mediaTracker.addImage(paneltexture, 1);

		// create grey-filtered background tiles
		ImageFilter imf = new GreyFilter();
		greytiles = applet.createImage(new FilteredImageSource(tiles
				.getSource(), imf));

		// Wait for images to load
		try {
			mediaTracker.waitForID(1);
		} catch (Exception e) {
			System.out.println("Error loading images.");
			e.printStackTrace();
		}
	}
}