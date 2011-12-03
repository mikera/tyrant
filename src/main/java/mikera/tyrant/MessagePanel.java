//
// Panel for displaying game status messages at the botton of the screen
//

package mikera.tyrant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class MessagePanel extends TPanel implements IMessageHandler {
	//private String text="";

	//	public MessagePanel() {
	//	  super("",0,0,SCROLLBARS_VERTICAL_ONLY);
	//	  setEditable(false);
	//	  setForeground(QuestApp.textcolor);
	//	  setBackground(QuestApp.backcolor);
	//	  setFont(QuestApp.mainfont);
	//	  Game.messagepanel=this;
	//	}

	private static final long serialVersionUID = 3258416114332807730L;
    //private TextArea textzone = new TTextArea("",0,0,TextArea.SCROLLBARS_VERTICAL_ONLY);
	private TextZone textzone = new TextZone();

	public MessagePanel(QuestApp q) {
		super(q);
		setLayout(new BorderLayout());
		textzone.setBackground(QuestApp.PANELCOLOUR);
		textzone.setForeground(QuestApp.INFOTEXTCOLOUR);

		add("Center", textzone);
		Game.messagepanel = this;
	}

	private void setText(String s) {
		textzone.setText(s);
	}

	private String getText() {
		return textzone.getText();
	}

	public void clear() {
		textzone.setText("");
	}
	
	public void add(String s) {
		add(s,Color.lightGray);

	}
	
	public void add(String s, Color c ) {	
		String t = getText();
		if (t.length() > 2000) {
			setText(t.substring(t.length() - 2000, t.length()));
		}

		String newtext = getText() + s;
		setText(newtext);

		//    try {
		//      setCaretPosition(newtext.length()-1);
		//    } catch (Exception e) {}
	}

	//public void append(String s){
	//  text=text+s;
	//  repaint();
	//}

	//public void setText(String s) {
	//	text=s;
	//	repaint();
	//}

	public Dimension getPreferredSize() {
		return new Dimension(500, 120);
	}

    public TPanel getPanel() {
        return this;
    }
}