package mikera.tyrant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import mikera.tyrant.engine.Map;
import mikera.tyrant.engine.RPG;
import mikera.tyrant.engine.StringList;
import mikera.tyrant.engine.Thing;
import mikera.tyrant.util.Text;

public class StatusPanel extends TPanel {
	private static final long serialVersionUID = 3905800885761095223L;
    public static final int boxborder = 1;
	public static final Color powercolor = new Color(0, 128, 60);
	public static int charwidth=0;
	public static int charheight=0;
	public static int charmaxascent=0;
	
	public StatusPanel() {
		super(Game.getQuestapp());
		setBackground(QuestApp.PANELCOLOUR);
		
	}

	public Dimension getPreferredSize() {
		return new Dimension(208, 272);
	}

	public void paint(Graphics g) {
		super.paint(g);

		FontMetrics met = g.getFontMetrics(g.getFont());
		charwidth = met.charWidth(' ');
		charheight = met.getMaxAscent() + met.getMaxDescent();
		charmaxascent = met.getMaxAscent();
		
		Thing h = Game.hero();
		Map m = h.getMap();

		Rectangle bounds = getBounds();
		int width = bounds.width;
		// int height = bounds.height;

		// draw energy bars
		int hp = h.getStat(RPG.ST_HPS);
		int hpm = h.getStat(RPG.ST_HPSMAX);
		float hel = ((float) hp) / hpm;

		if (hel < 0)
			hel = 0;
		if (hel > 1)
			hel = 1;
		
		Color healthcolor=new Color(
			((int)(255-hel*192)),
			((int)(hel*160)),
			((int)(100-hel*100)));
		
		paintLabel(g, "Health: " + hp + "/" + hpm, 10, 13);
		paintBar(g, 10 , 26, width - 20 , 16,
				healthcolor, Color.black, hel);

//TODO : one day we will print power , mp amd mpm again		
//		int mp = h.getStat(RPG.ST_MPS);
//		int mpm = h.getStat(RPG.ST_MPSMAX);		
//		float pow = ((float) mp) / mpm;

		/*
		paintLabel(g, "Power:  " + mp + "/" + mpm, 10, 44);
		if (pow < 0)
			pow = 0;
		if (pow > 1)
			pow = 1;
		paintBar(g, 10 + 16 * charwidth, 36, width - 20 - 16 * charwidth, 16,
				powercolor, Color.black, pow);
*/
		
		// List stats
		int yPos = 36+charheight;
		
		paintStats(g,10,yPos);
		yPos += 6*charheight;

		// experience
		paintLabel(g, "Exp: "+h.getStat(RPG.ST_EXP)+" / "+Hero.calcXPRequirement(h.getStat("Level")+1),10, yPos);
		yPos += charheight;
		
		// fate
		paintLabel(g, "Piety: "+h.getStat(RPG.ST_PEITY),10, yPos);
		yPos += charheight;
		

		
		// Location string
		{
			yPos += charheight;
			paintLabel(g, (m == null) ? "Six Feet Under" : m.getDescription(), 10, yPos);
			yPos += charheight;
			
			if ((m!=null)&&Game.isDebug()) {
				String levelName=m.name();
				paintLabel(g, "["+levelName+"] lv="+m.getLevel(), 10, yPos);
				yPos += charheight;
			}
			yPos += charheight;
		}
		
		// list current attributes
		{
			Thing[] atts = h.getFlaggedContents("IsEffect");
			StringList sl = new StringList();
			for (int i = 0; i < atts.length; i++) {
				sl.add(Text.capitalise(atts[i].getString("EffectName")));
			}

			Thing w1 = h.getWielded(RPG.WT_MAINHAND);
			Thing w2 = h.getWielded(RPG.WT_SECONDHAND);
			if ((w1!=null)&&Item.isDamaged(w1)) sl.add("Damaged weapon");
			if ((w2!=null)&&Item.isDamaged(w2)) sl.add("Damaged weapon");
			if ((w1!=null)&&(!w1.getFlag("IsWeapon"))) w1=null;
			if ((w2!=null)&&(!w2.getFlag("IsWeapon"))) w2=null;
			if ((w1 == null)&&(w2==null))
				sl.add("Unarmed");

			Thing[] fs=h.getFlaggedContents("IsBeing");
			
			int followers = 0;
			int pursuers=0;
			for (int i=0; i<fs.length; i++) {
				if (fs[i].isHostile(h)) {
					pursuers++;
				} else {
					followers++;
				}
			}
			if (followers > 0) {
				sl.add(Integer.toString(followers)
						+ ((followers > 1) ? " companions" : " companion"));
			}
			if (pursuers > 0) {
				sl.add(Integer.toString(pursuers)
						+ ((pursuers > 1) ? " enemies in pursuit" : " enemy in pusuit"));
			}
			
			
			if (Hero.hasHungerString(h)) {
				String hunger=Text.capitalise(Hero.hungerString(h));
				sl.add(hunger);
			}
			
			sl = sl.compress();
			sl = sl.compact(21,", ");
			for (int i = 0; i < sl.getCount(); i++) {
				paintLabel(g, sl.getString(i), 20, yPos);
				yPos += charheight;
			}
		}
		

	}

	private static String[] stats1=new String[] {"SK","ST","AG","TG","Level"};
	private static String[] stats2=new String[] {"IN","WP","CH","CR","Luck"};
	public void paintStats(Graphics g,int x, int y) {
		for (int i=0; i<stats1.length; i++) {
			paintStat(g,stats1[i],x,y+i*charheight);
		}
		
		for (int i=0; i<stats2.length; i++) {
			paintStat(g,stats2[i],x+100,y+i*charheight);
		}
	}
	
	public void paintStat(Graphics g,String s, int x, int y) {
		paintLabel(g,s+": "+Game.hero().getStat(s),x,y);
	}
		
	public static void paintBar(Graphics g, int x, int y, int w, int h,
			Color f, Color b, float amount) {
		if (amount > 1)
			amount = 1;
		int hh=h/4;
		g.setColor(f);
		g.fillRect(x, y, (int) (w * amount), h);
		g.setColor(f.brighter());
		g.fillRect(x, y, (int) (w * amount), hh);
		g.setColor(f.darker());
		g.fillRect(x, y+3*hh, (int) (w * amount), h-3*hh);
		g.setColor(b);
		g.fillRect(x + (int) (w * amount), y, (int) (w * (1 - amount)), h);
		paintBox(g, x, y, w, h, false);
	}

	public static int paintLabel(Graphics g, String s, int x, int y) {
		g.setColor(QuestApp.INFOTEXTCOLOUR);

		g.drawString(s, x, y + charmaxascent - charheight / 2);

		return charwidth * s.length();
	}

	// paint a boxed area, raised or lowered
	public static void paintBox(Graphics g, int x, int y, int w, int h,
			boolean raised) {
		if (raised)
			g.setColor(QuestApp.PANELHIGHLIGHT);
		else
			g.setColor(QuestApp.PANELSHADOW);

		g.fillRect(x, y, w, boxborder);
		g.fillRect(x, y, boxborder, h);

		if (!raised)
			g.setColor(QuestApp.PANELHIGHLIGHT);
		else
			g.setColor(QuestApp.PANELSHADOW);

		g.fillRect(x + 1, y + h - boxborder, w - 1, boxborder);
		g.fillRect(x + w - boxborder, y + 1, boxborder, h - 1);
	}
}