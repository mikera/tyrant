package mikera.tyrant.test;

import mikera.tyrant.util.Text;

public class Text_TC extends TyrantTestCase {

    public void testCamelizeString() {
        assertEquals("DeanOfAdmissions",Text.camelizeString("dean of admissions"));
        assertEquals("Dean",Text.camelizeString("dean"));
        assertEquals("123Four",Text.camelizeString("123 four"));
    }

}
