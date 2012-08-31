/*
 * Created on 18-Dec-2004
 *
 * By Mike Anderson
 */
package mikera.tyrant.test;

import mikera.tyrant.Game;

/**
 * @author Mike
 */
public class TestNullHandler extends TyrantTestCase {

    public void testMessages() {
        Game.instance().clearMessageList();
        
        assertEquals(null, lastMessage());
        Game.message("Message One");
        assertEquals("Message One", lastMessage());
        Game.message("Message Two");
        assertEquals("Message Two", lastMessage());
    }
}
