/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.android.authenticator.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.xwiki.android.authenticator.Constants;
import org.xwiki.android.authenticator.AppContext;
import org.xwiki.android.authenticator.R;
import org.xwiki.android.authenticator.activities.SignUpActivity;
import org.xwiki.android.authenticator.rest.HttpResponse;
import org.xwiki.android.authenticator.rest.XWikiHttp;
import org.xwiki.android.authenticator.utils.Loger;
import org.xwiki.android.authenticator.utils.StatusBarColorCompat;


/**
 * @version $Id: $
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public static final String AUTHORITY = "org.xwiki.android.authenticator";

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String PARAM_USER_SERVER = "XWIKI_USER_SERVER";
    public final static String PARAM_USER_PASS = "XWIKI_USER_PASS";

    public final static String PARAM_APP_UID = "PARAM_APP_UID";
    public final static String PARAM_APP_PACKAGENAME = "PARAM_APP_PACKAGENAME";

    private final int REQ_SIGNUP = 1;
    private final int REQ_SETTINGS = 2;

    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;
    private String mAuthTokenType;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_login);
        StatusBarColorCompat.compat(this, Color.parseColor("#0077D9"));

        //check if there'is already a user, finish and return, keep only one user.
        mAccountManager = AccountManager.get(getApplicationContext());
        Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
        if (availableAccounts.length > 0) {
            Toast.makeText(this, "The user already exists!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (mAuthTokenType == null)
            mAuthTokenType = Constants.AUTHTOKEN_TYPE_FULL_ACCESS;

        if (accountName != null) {
            ((TextView) findViewById(R.id.accountName)).setText(accountName);
        }
        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    public void handleSignUp(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            finishLogin(data);
        } else if(requestCode == REQ_SETTINGS && resultCode == RESULT_OK){

        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void submit() {
        final String userServer = ((TextView) findViewById(R.id.accountServer)).getText().toString();
        final String userName = ((TextView) findViewById(R.id.accountName)).getText().toString();
        final String userPass = ((TextView) findViewById(R.id.accountPassword)).getText().toString();

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        new AsyncTask<String, Void, Intent>() {
            @Override
            protected Intent doInBackground(String... params) {
                Log.d("xwiki", TAG + "> Started authenticating");
                Bundle data = new Bundle();
                try {
                    Loger.debug(userName + " " + userPass + " " + userServer);
                    HttpResponse response = XWikiHttp.login(userServer, userName, userPass);
                    Loger.debug(response.getHeaders().toString() + response.getResponseCode());
                    int statusCode = response.getResponseCode();
                    if (statusCode < 200 || statusCode > 299) {
                        String msg = "statusCode=" + statusCode + ", response=" + response.getResponseMessage();
                        data.putString(KEY_ERROR_MESSAGE, msg);
                    } else {
                        String authtoken = response.getHeaders().get("Set-Cookie");
                        data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
                        data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                        data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                        data.putString(PARAM_USER_SERVER, userServer);
                        data.putString(PARAM_USER_PASS, userPass);
                    }
                } catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.toString());
                }
                final Intent res = new Intent();
                res.putExtras(data);
                return res;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    Toast.makeText(getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
                } else {
                    finishLogin(intent);
                }
            }
        }.execute();
    }

    private void finishLogin(Intent intent) {
        Log.d("xwiki", TAG + "> finishLogin");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        String accountServer = intent.getStringExtra(PARAM_USER_SERVER);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Log.d("xwiki", TAG + "> finishLogin > addAccountExplicitly" + " " + intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = mAuthTokenType;

            Bundle data = new Bundle();
            data.putString(PARAM_USER_SERVER, accountServer);
            data.putString(PARAM_USER_PASS, accountPassword);

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, data);
            //mAccountManager.setAuthToken(account, authtokenType, authtoken);
            mAccountManager.setUserData(account, AccountManager.KEY_USERDATA, accountName);
            mAccountManager.setUserData(account, AccountManager.KEY_PASSWORD, accountPassword);
            mAccountManager.setUserData(account, AuthenticatorActivity.PARAM_USER_SERVER, accountServer);

            //grant permission if adding user from the third-party app (UID,PackageName);
            String packaName = getIntent().getStringExtra(PARAM_APP_PACKAGENAME);
            int uid = getIntent().getIntExtra(PARAM_APP_UID, 0);
            if (packaName != getPackageName()) {
                AppContext.addAuthorizedApp(uid, packaName);
            }

            ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
            //Bundle params = new Bundle();
            //params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
            //params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
            //params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
            //ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, params, 150);
            //ContentResolver.requestSync(account,ContactsContract.AUTHORITY,params);


        } else {
            Log.d("xwiki", TAG + "> finishLogin > setPassword");
            mAccountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        Log.d("xwiki", TAG + ">" + "finish return");
        finish();
    }

}