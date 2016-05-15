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
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.xwiki.android.authenticator.Constants;
import org.xwiki.android.authenticator.AppContext;
import org.xwiki.android.authenticator.R;
import org.xwiki.android.authenticator.activities.SettingSyncViewFlipper;
import org.xwiki.android.authenticator.activities.SettingsActivity;
import org.xwiki.android.authenticator.activities.SettingIpViewFlipper;
import org.xwiki.android.authenticator.activities.SignInViewFlipper;
import org.xwiki.android.authenticator.activities.SignUpActivity;
import org.xwiki.android.authenticator.activities.SignUpStep1ViewFlipper;
import org.xwiki.android.authenticator.activities.SignUpStep2ViewFlipper;
import org.xwiki.android.authenticator.rest.HttpResponse;
import org.xwiki.android.authenticator.rest.XWikiHttp;
import org.xwiki.android.authenticator.utils.SharedPrefsUtil;
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

    public final static String IS_SETTING_SYNC_TYPE = "IS_SETTING_SYNC_TYPE";

    private final int REQ_SIGNUP = 1;
    private final int REQ_SETTINGS = 2;

    private final String TAG = this.getClass().getSimpleName();

    private AccountManager mAccountManager;
    private String mAuthTokenType;

    private ViewFlipper mViewFlipper;

    private Toolbar toolbar;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_login);
        StatusBarColorCompat.compat(this, Color.parseColor("#0077D9"));

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("XWiki Account");

        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        boolean is_set_sync = getIntent().getBooleanExtra(AuthenticatorActivity.IS_SETTING_SYNC_TYPE, true);
        if (is_set_sync) {
            //just set sync
            showViewFlipper(ViewFlipperLayoutId.SETTING_SYNC);
        }else{
            //add new contact
            //check if there'is already a user, finish and return, keep only one user.
            mAccountManager = AccountManager.get(getApplicationContext());
            Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
            if (availableAccounts.length > 0) {
                Toast.makeText(this, "The user already exists!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
            if (mAuthTokenType == null) {
                mAuthTokenType = Constants.AUTHTOKEN_TYPE_FULL_ACCESS;
            }
        }
    }

    public void handleSignUp(View view) {
        String userServer = ((TextView) findViewById(R.id.accountServer)).getText().toString();
        SharedPrefsUtil.putValue(AppContext.getInstance().getApplicationContext(), "requestUrl", userServer);
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivityForResult(intent, REQ_SIGNUP);
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
                    Log.d(TAG, userName + " " + userPass + " " + userServer);
                    HttpResponse response = XWikiHttp.login(userServer, userName, userPass);
                    Log.d(TAG, response.getHeaders().toString() + response.getResponseCode());
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


    private void doPreviousNext(boolean next){
        int id = mViewFlipper.getDisplayedChild();
        switch (id){
            case ViewFlipperLayoutId.SETTING_IP:
                if(settingsIpViewFlipper == null){
                    settingsIpViewFlipper = new SettingIpViewFlipper(this, mViewFlipper.getChildAt(id));
                }
                if(next) {
                    settingsIpViewFlipper.doNext();
                }else{
                    settingsIpViewFlipper.doPrevious();
                }
                break;
            case ViewFlipperLayoutId.SIGN_IN:
                if(signInViewFlipper == null){
                    signInViewFlipper = new SignInViewFlipper(this, mViewFlipper.getChildAt(id));
                }
                if(next) {
                    signInViewFlipper.doNext();
                    //for test...
                    Bundle data = new Bundle();
                    data.putString(AccountManager.KEY_ACCOUNT_NAME, "fitz");
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
                    //data.putString(AccountManager.KEY_AUTHTOKEN, "tokentoken");
                    data.putString(AuthenticatorActivity.PARAM_USER_SERVER, XWikiHttp.getServerRequestUrl());
                    data.putString(AuthenticatorActivity.PARAM_USER_PASS, "leee");
                    Intent intent = new Intent();
                    intent.putExtras(data);
                    finishLogin(intent);
                }else{
                    signInViewFlipper.doPrevious();
                }
                break;
            case ViewFlipperLayoutId.SETTING_SYNC:
                if(settingSyncViewFlipper == null){
                    settingSyncViewFlipper = new SettingSyncViewFlipper(this, mViewFlipper.getChildAt(id));
                }
                if(next) {
                    settingSyncViewFlipper.doNext();
                }else{
                    settingSyncViewFlipper.doPrevious();
                }
                break;
            case ViewFlipperLayoutId.SIGN_UP_STEP1:
                if(signUpStep1ViewFlipper == null){
                    signUpStep1ViewFlipper = new SignUpStep1ViewFlipper(this, mViewFlipper.getChildAt(id));
                }
                if(next) {
                    signUpStep1ViewFlipper.doNext();
                }else{
                    signUpStep1ViewFlipper.doPrevious();
                }
                break;
            case ViewFlipperLayoutId.SIGN_UP_STEP2:
                if(signUpStep2ViewFlipper == null){
                    signUpStep2ViewFlipper = new SignUpStep2ViewFlipper(this, mViewFlipper.getChildAt(id));
                }
                if(next) {
                    signUpStep2ViewFlipper.doNext();
                }else{
                    signUpStep2ViewFlipper.doPrevious();
                }
                break;
            default:
                break;
        }
    }

    private SettingIpViewFlipper settingsIpViewFlipper;
    private SignInViewFlipper signInViewFlipper;
    private SettingSyncViewFlipper settingSyncViewFlipper;
    private SignUpStep1ViewFlipper signUpStep1ViewFlipper;
    private SignUpStep2ViewFlipper signUpStep2ViewFlipper;

    public void doPrevious(View view){
        doPreviousNext(false);
    }

    public void doNext(View view){
        doPreviousNext(true);
    }

    public void setLeftRightButton(String leftButton, String rightButton){
        ((Button) findViewById(R.id.left_button)).setText(leftButton);
        ((Button) findViewById(R.id.right_button)).setText(rightButton);
    }

    public interface ViewFlipperLayoutId{
        int SETTING_IP = 0;
        int SIGN_IN = 1;
        int SETTING_SYNC = 2;
        int SIGN_UP_STEP1 = 3;
        int SIGN_UP_STEP2 = 4;
    }

    public void showViewFlipper(int layoutId){
        mViewFlipper.setDisplayedChild(layoutId);
        switch (layoutId){
            case ViewFlipperLayoutId.SETTING_IP:
                toolbar.setTitle("XWiki Account");
                setLeftRightButton("Sign In", "Sign Up");
                break;
            case ViewFlipperLayoutId.SIGN_IN:
                toolbar.setTitle("Sign In");
                setLeftRightButton("Previous", "Login");
                break;
            case ViewFlipperLayoutId.SETTING_SYNC:
                toolbar.setTitle("Setting Sync");
                setLeftRightButton("Don't Sync", "Complete");
                break;
            case ViewFlipperLayoutId.SIGN_UP_STEP1:
                toolbar.setTitle("Sign Up Step1");
                setLeftRightButton("Previous", "Next");
                break;
            case ViewFlipperLayoutId.SIGN_UP_STEP2:
                toolbar.setTitle("Sign Up Step2");
                setLeftRightButton("Previous", "SignUp");
                break;
        }
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

            //clear all SharedPreferences
            //SharedPrefsUtil.clearAll(AuthenticatorActivity.this);

            //grant permission if adding user from the third-party app (UID,PackageName);
            String packaName = getIntent().getStringExtra(PARAM_APP_PACKAGENAME);
            int uid = getIntent().getIntExtra(PARAM_APP_UID, 0);
            if (packaName != getPackageName()) {
                AppContext.addAuthorizedApp(uid, packaName);
            }

            //ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
            //ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);


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
        //finish();

        /*
        Intent settingsIntent = new Intent(AuthenticatorActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
        //startActivityForResult(settingsIntent, REQ_SETTINGS);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        finish();
        */

        /*
        mViewFlipper.setInAnimation(AuthenticatorActivity.this, R.anim.push_left_in);
        mViewFlipper.setOutAnimation(AuthenticatorActivity.this, R.anim.push_left_out);
        SettingViewFlipper settingViewFlipper = new SettingViewFlipper(AuthenticatorActivity.this, mViewFlipper.getChildAt(1));
        mViewFlipper.showNext();
        */
    }

}
