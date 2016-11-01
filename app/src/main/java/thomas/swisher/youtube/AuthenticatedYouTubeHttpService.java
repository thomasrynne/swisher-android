package thomas.swisher.youtube;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOError;
import java.io.IOException;

import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import thomas.swisher.utils.Utils;

/**
 * Gets the auth token and makes http calls, adding the header
 */
public class AuthenticatedYouTubeHttpService {

    private static final String youTubeBase = "https://www.googleapis.com/youtube/v3";

    private final Context baseContext;

    public AuthenticatedYouTubeHttpService(Context baseContext) {
        this.baseContext = baseContext;
    }

    private final OkHttpClient client = new OkHttpClient();

    public void checkToken() throws SecurityException, GoogleAuthException, IOException {
        getTokenForFirstGoogleAccount();
    }

    public Utils.FlatJson read(String url) throws SecurityException, GoogleAuthException, IOException {
        return read(url, 3);
    }

    private Utils.FlatJson read(String url, int retry) throws SecurityException, GoogleAuthException, IOException {
        val token = getTokenForFirstGoogleAccount();
        Request request = new Request.Builder()
                .url(youTubeBase + url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        Response response = client.newCall(request).execute();
        switch (response.code()) {
            case 200:
                return Utils.FlatJson.parse(response.body().string());
            case 401:
                GoogleAuthUtil.clearToken(baseContext, token);
                if (retry > 0) {
                    return read(url, retry - 1);
                } else {
                    Log.e("SWISHER", "Youtube authentication failed: " + response.body().string());
                    throw new IOException("not authenticated: " + response.message());
                }
            default:
                Log.e("SWISHER", "Youtube authentication failed: " + response.body().string());
                Log.i("SWISHER", "PackageName 1: " + baseContext.getApplicationInfo().packageName);
                Log.i("SWISHER", "PackageName 2: " + new Bundle().getString("androidPackageName"));
                throw new IOException("call to url " + url + " failed x + " + response.code());
        }
    }

    private String getTokenForFirstGoogleAccount() throws IOException, GoogleAuthException {
        val account = account();
        return getTokenWithRetry(account, 3);
    }

    private Account account() throws SecurityException {
        AccountManager am = AccountManager.get(baseContext);
        Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        return accounts[0];
    }

    private String getTokenWithRetry(Account account, int retry) throws GoogleAuthException, IOException {
        String scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly";
        try {
            return GoogleAuthUtil.getToken(baseContext, account, scope);
        } catch (IOException e) {
            if (retry > 0) {
                try { Thread.sleep(200); } catch (InterruptedException ie) {}
                return getTokenWithRetry(account, retry - 1);
            } else {
                throw e;
            }
        }
    }
}