package mikera.tyrant.test;

import static org.junit.Assert.*;

import org.junit.Test;

import mikera.tyrant.Action;
import mikera.tyrant.ActionMapping;


public class ActionMapping_TC {
  
    @Test
    public void testAction() throws Exception {
        ActionMapping map = new ActionMapping();
        map.addDefaultMappings();
        assertEquals(Action.MOVE_N, map.convertKeyToAction('8'));
        assertEquals(Action.MOVE_E, map.convertKeyToAction('6'));
        assertEquals(Action.MOVE_NOWHERE, map.convertKeyToAction('5'));
        assertEquals(Action.WAIT, map.convertKeyToAction('.'));
    }
}
