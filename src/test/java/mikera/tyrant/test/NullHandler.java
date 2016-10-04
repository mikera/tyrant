package mikera.tyrant.test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mikera.tyrant.Game;
import mikera.tyrant.IMessageHandler;
import mikera.tyrant.TPanel;


public class NullHandler implements IMessageHandler {
    public List<String> messages;
    @Override
	public void clear() {
        messages.clear();
    }

    @Override
	public void add(String s) {
        messages.add(s);
    }

    @Override
	public void add(String s, Color c) {
        add(s);
    }

    @Override
	public TPanel getPanel() {
        return null;
    }

    public static List<String> installNullMessageHandler() {
    	List<String> messages=new ArrayList<>();
        installNullMessageHandler(messages);
        return messages;
    }

    public static void installNullMessageHandler(List<String> messages) {
        NullHandler aMessageHandler = new NullHandler();
        aMessageHandler.messages = messages;
        Game.messagepanel = aMessageHandler;
        Game.instance().getMessageList().clear();
    }
}