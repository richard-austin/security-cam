package common;

import com.proxy.ILogService;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.*;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;

public class Authentication {
    ILogService logService;
    public Authentication(ILogService logService) {
        this.logService = logService;
    }

    public Header getAuthResponse(String username, String password, String method, String uri, String challenge, HttpContext context) {
        Header authResponse = new BasicHeader("", "");
        try {
            if(challenge != null) {
                final boolean isBasic = challenge.toLowerCase().contains("basic");
                ContextAwareAuthScheme caas = isBasic ? new BasicScheme() : new DigestScheme();
                Header authChallenge = new BasicHeader(AUTH.WWW_AUTH, challenge);

                Credentials cred = new UsernamePasswordCredentials(username, password);

                HttpRequest request = new BasicHttpRequest(method, uri);
                caas.processChallenge(authChallenge);
                authResponse = caas.authenticate(cred, request, context);
            }
        }
        catch(Throwable ex) {
            logService.getCam().error(ex.getClass().getName()+": in getAuthResponse, "+ex.getMessage());
        }
        return authResponse;
    }
}
