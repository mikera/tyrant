//
// Graphical component to display an area of text
//

package mikera.tyrant;

import java.awt.*;

import mikera.tyrant.engine.RPG;
import mikera.tyrant.util.Text;
import mikera.util.Maths;

public class TextZone extends Component {
	private static final long serialVersionUID = 3256443603391033401L;
    public String text;
	public Font font;
	public static int linelength;

	private int border = 5;

	public TextZone() {
		this("");
	}

	public TextZone(String t) {
		text = t;
		font = QuestApp.mainfont;
		setFont(font);
	}

	public void setText(String s) {
		text = new String(s);
	}

	public String getText() {
		return text;
	}

	public void paint(Graphics g) {
		FontMetrics met = g.getFontMetrics(g.getFont());

		g.setColor(QuestApp.INFOTEXTCOLOUR);

		Rectangle r = getBounds();
		int charsize = met.charWidth(' ');

		
		int y;
		int lineinc;
		{
			linelength = (r.width - border * 2) / charsize;
			y = border + met.getMaxAscent();
			lineinc = met.getMaxAscent() + met.getMaxDescent();
		}

		String[] st = Text.separateString(text, '\n');
		int height=0;
		
		for (int i = 0; i < st.length; i++) {
			String s = st[i];

			String[] lines = Text.wrapString(s, linelength);

			for (int l = 0; l < lines.length; l++) {
				height+=lineinc;
			}
		}
		
		int scroll=Maths.max(0,height-getHeight());
		
		for (int i = 0; i < st.length; i++) {
			String s = st[i];

			String[] lines = Text.wrapString(s, linelength);

			for (int l = 0; l < lines.length; l++) {
				String line = lines[l];

				g.drawString(line, border, y-scroll);
				y += lineinc;
			}
		}
	}

}