package mikera.tyrant;

import java.awt.Color;

public interface IMessageHandler {
    void clear();
    void add(String s);
    void add(String s, Color c);
    
    /**
     * If headless this may answer null.
     */
    TPanel getPanel();
}
