package org.jboss.fuse.security.service;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestPwdCallback implements CallbackHandler {

    private static Map<String, String> passwords = new HashMap<String, String>();

    static {
        passwords.put("myalias", "myAliasPassword");
        passwords.put("alice", "alicePassword");
        passwords.put("username", "myAliasPassword");
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];

            String pass = passwords.get(pc.getIdentifier());
            if (pass != null) {
                pc.setPassword(pass);
            }
        }
    }
}
