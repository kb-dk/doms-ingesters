/**
 * 
 */
package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.net.URL;

/**
 * @author tsh
 * 
 */
public class DOMSLoginInfo {

    private final String login;
    private final String password;

    private final URL domsWSAPIUrl;

    public DOMSLoginInfo(URL domsWSAPIUrl, String login, String password) {
	this.login = login;
	this.password = password;
	this.domsWSAPIUrl = domsWSAPIUrl;
    }

    /**
     * @return the login
     */
    public String getLogin() {
	return login;
    }

    /**
     * @return the password
     */
    public String getPassword() {
	return password;
    }

    /**
     * @return the domsWSAPIUrl
     */
    public URL getDomsWSAPIUrl() {
	return domsWSAPIUrl;
    }
}
