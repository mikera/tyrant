package mikera.tyrant;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import mikera.tyrant.util.Text;

public class InfoScreen extends Screen {
	private static final long serialVersionUID = 3256727281736168249L;
    public String text;
	public Font font;

	private int border = 20;

	public InfoScreen(QuestApp q, String t) {
		super(q);
		
		text = t;
		font = QuestApp.mainfont;
		setForeground(QuestApp.INFOTEXTCOLOUR);
		setBackground(QuestApp.INFOSCREENCOLOUR);
		setFont(font);
	}

	public void activate() {
		// questapp.setKeyHandler(keyhandler);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		FontMetrics met = g.getFontMetrics(g.getFont());

		Rectangle r = getBounds();
		int charsize = met.charWidth(' ');

		int linelength;
		int y;
		int lineinc;
		{
			linelength = (r.width - border * 2) / charsize;
			y = border + met.getMaxAscent();
			lineinc = met.getMaxAscent() + met.getMaxDescent();
		}

		String[] st = Text.separateString(text, '\n');

		for (int i = 0; i < st.length; i++) {
			String s = st[i];

			String[] lines = Text.wrapString(s, linelength);

			for (int l = 0; l < lines.length; l++) {
				String line = lines[l];

				g.setColor(QuestApp.INFOTEXTCOLOUR);
				g.drawString(line, border, y);
				y += lineinc;
			}
		}
	}
}