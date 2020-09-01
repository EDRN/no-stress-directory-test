/**
 * No stress
 */

package gov.nasa.jpl.cancer.directory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.NamingEnumeration;
import javax.naming.AuthenticationException;


public final class Main {
    private final static String _contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
    private final static String ADMIN_DN = "uid=admin,ou=system";

    private static DirContext getLDAPContext(String dn, String password, String uri) throws Throwable {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, _contextFactory);
        env.put(Context.PROVIDER_URL, uri);
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put("com.sun.jndi.ldap.connect.timeout", "30000");  // 30 seconds
        env.put("com.sun.jndi.ldap.read.timeout", "60000");    // 1 minute
        return new InitialDirContext(env);
    }

    private static void imitateLabCAS(String uri, String password) throws Throwable {
        // This imitates UserServiceLdapImpl.getValidUser
        String dn = null;
        DirContext context = getLDAPContext(ADMIN_DN, password, uri);
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> response = context.search("", "(uid=kelly)", controls);
        if (response.hasMore()) {
            SearchResult result = (SearchResult) response.next();
            dn = result.getNameInNamespace();
        }
        response.close();
        context.close();

        // This imitates UserServiceLdapImpl.validateUserCredentials
        try {
            context = getLDAPContext(dn, "x", uri);
            context.close();
        } catch (javax.naming.AuthenticationException ex) {
            // Expected
        } catch (Exception ex) {
            // Not expected!
            throw ex;
        }
    }

    public static void main(String[] argv) throws Throwable {
        String uri = null;
        String password = null;

        if (argv.length < 1 || argv.length > 2) {
            System.err.println("Usage: {old|new} [frequency]");
            System.exit(1);
        }

        int frequency = 10;

        if (argv[0].equals("old")) {
            uri = Constants.OLD_URI;
            password = Constants.OLD_PASSWORD;
        } else {
            uri = Constants.NEW_URI;
            password = Constants.NEW_PASSWORD;
        }
        if (argv.length == 2)
            frequency = Integer.parseInt(argv[1]);

        System.err.println("✍️ Using uri «" + uri + "» with pw «" + password + "»");

        int count = 0;
        for (;;) {
            count += 1;
            imitateLabCAS(uri, password);
            if (count % frequency == 0) System.err.println("☑️ Tried " + count + " times so far …");
        }
    }
}
