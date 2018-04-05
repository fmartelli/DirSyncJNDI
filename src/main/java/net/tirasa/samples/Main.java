/**
 * Copyright (C) 2015 Tirasa (info@tirasa.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.samples;

import com.sun.jndi.ldap.ctl.DirSyncResponseControl;
import java.util.Base64;
import java.util.Date;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;

public class Main {

    private static final String ENDPOINT = "ldaps://11.10.10.4:636";

    private static final String BASE_CONTEXT = "dc=test,dc=tirasa,dc=net";

    private static final String PRINCIPAL = "Administrator@test.tirasa.net";

    private static final String CREDENTIALS = "Password1";

    public static void main(final String[] args) {
        @SuppressWarnings({ "UseOfObsoleteCollectionType", "rawtypes" })
        final java.util.Hashtable env = new java.util.Hashtable();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        //set security credentials, note using simple cleartext authentication
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put("java.naming.ldap.version", "3");
        env.put(Context.SECURITY_PRINCIPAL, PRINCIPAL);
        env.put(Context.SECURITY_CREDENTIALS, CREDENTIALS);
        env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put("java.naming.ldap.factory.socket", "net.tirasa.samples.DummySocketFactory");
        env.put("java.naming.ldap.attributes.binary", "nTSecurityDescriptor objectSID");
        env.put(Context.REFERRAL, "follow");

        //connect to my domain controller
        env.put(Context.PROVIDER_URL, ENDPOINT);

        InitialLdapContext ctx = null;

        try {
            // Create the initial directory context
            ctx = new InitialLdapContext(env, null);

            // -----------------------------------
            // Create search control
            // -----------------------------------
            final SearchControls searchCtls = createDefaultSearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchCtls.setReturningAttributes(null);
            // -----------------------------------

            ctx.setRequestControls(new Control[] { new DirSyncControl() });

            System.out.println("Start time: " + new Date(System.currentTimeMillis()));
            
            // performs filtere search
            final NamingEnumeration<SearchResult> answer = ctx.search(
                    BASE_CONTEXT,
                    "(memberOf=CN=Domain Guests,CN=Users," + BASE_CONTEXT + ")",
                    searchCtls);

            // consumes the reply
            if (answer.hasMoreElements()) {
                while (answer.hasMoreElements()) {
                    System.out.println("Entry: " + answer.nextElement());
                }
            } else {
                System.out.println("Empty result set");
            }
            System.out.println("End time: " + new Date(System.currentTimeMillis()));
            
            final Control[] rspCtls = ctx.getResponseControls();

            // reads the cookie
            if (rspCtls != null) {

                for (Control rspCtl : rspCtls) {
                    if (rspCtl instanceof DirSyncResponseControl) {
                        DirSyncResponseControl dirSyncRspCtl = (DirSyncResponseControl) rspCtl;
                        System.out.println("Cookie: " + Base64.getEncoder().encodeToString(dirSyncRspCtl.getCookie()));
                    }
                }
            }

        } catch (NamingException e) {
            System.err.println("[ERROR] Initializing test context: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ex) {
                    // ignore
                }
            }
        }
    }

    private static SearchControls createDefaultSearchControls() {
        SearchControls result = new SearchControls();
        result.setCountLimit(0);
        result.setDerefLinkFlag(true);
        result.setReturningObjFlag(false);
        result.setTimeLimit(0);
        return result;
    }
}
