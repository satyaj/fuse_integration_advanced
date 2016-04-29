package org.jboss.fuse.security.cxf.common;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import java.io.InputStream;
import java.util.Scanner;

public class BaseCXF extends AbstractBusClientServerTestBase {

    protected static String inputStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
