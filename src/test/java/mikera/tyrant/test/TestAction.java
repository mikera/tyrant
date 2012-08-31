package mikera.tyrant.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import mikera.tyrant.Action;

public class TestAction {
	@Test
    public void testSerialization() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);

        out.writeObject(Action.MOVE_N);
        out.flush();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteArrayInputStream);

        assertSame(Action.MOVE_N, in.readObject());
    }
}
