package common;

import com.proxy.ILogService;

import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Objects;

public class Authentication extends HeaderProcessing {
    public Authentication(ILogService logService) {
        super(logService);
    }

    public String createAuthHeader(ByteBuffer request, ByteBuffer response, String userName, String password) {
        String retVal = "";
        String authenticate = getHeader(response, "WWW-Authenticate");
        if(Objects.equals(authenticate, ""))
            return authenticate;
        else {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                final String realm = getHeaderPart(authenticate, "Digest realm=");
                final String nonce = getHeaderPart(authenticate, "nonce=");
                final String algorithm = getHeaderPart(authenticate, "algorithm=");
                final String method = getRTSPMethod(request);
                final String uri = getRTSPUri(request);
                if(Objects.equals(algorithm, "MD5")) {
                    final byte[] HA1 = md.digest((userName + ":" + realm + ":" + password).getBytes());
                    final byte[] HA2 = md.digest((method+":"+uri).getBytes());
                    final byte[] resp = md.digest((new String(HA1)+":"+nonce+":"+new String(HA2)).getBytes());
                    retVal = "Authorization: Digest username="+userName+", algorithm="+algorithm+
                            ", realm="+realm+", nonce="+nonce+", uri="+uri+", response="+new String(resp);
                }
            }
            catch(Exception ex) {
                logService.getCam().error(ex.getClass().getName()+" in createAuthHeader: "+ex.getMessage());
            }
        }
        return retVal;
    }

    String getHeaderPart(@NotNull String header, @NotNull String name) {
        String retVal = "";

        final int idxOfName = header.indexOf(name);
        if(idxOfName != -1) {
            final int startOfPart = header.indexOf('"', idxOfName) + 1;
            if(startOfPart > 0) {
                final int endOfPart = header.indexOf('"', startOfPart);
                if(endOfPart != -1)
                    retVal = header.substring(startOfPart, endOfPart);
            }
        }
        return retVal;
    }
}
