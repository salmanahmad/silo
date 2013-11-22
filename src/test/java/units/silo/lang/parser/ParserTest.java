/*
 *
 *  Copyright 2012 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */


import org.junit.*;
import java.util.*;

import silo.util.Helper;
import silo.lang.compiler.*;
import silo.lang.compiler.grammar.*;
import silo.lang.nodes.*;

public class ParserTest {

    @Test
    public void testEmpty() {
        Parser parser = new Parser();
        parser.parse("");
        parser.parse("\n\n\n");
        parser.parse("\n\n  \n \n");
    }
}
