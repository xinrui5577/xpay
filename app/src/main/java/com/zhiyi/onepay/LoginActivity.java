/**
 *  个人收款 https://gitee.com/DaLianZhiYiKeJi/xpay
 *  大连致一科技有限公司
 * */

package com.zhiyi.onepay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.bugly.Bugly;
import com.zhiyi.onepay.util.AppUtil;
import com.zhiyi.onepay.util.DBManager;
import com.zhiyi.onepay.util.RequestUtils;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {


    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };


    // UI references.
    private EditText mAppIdView;
    private EditText mTokenView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView helpView;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mAppIdView = findViewById(R.id.app_id);
        mTokenView = findViewById(R.id.token);
        helpView = findViewById(R.id.sign_in_help_text);
        mTokenView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button getToken = findViewById(R.id.btn_getToken);
        getToken.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                act_GetToken(v);
            }
        });

        Button mEmailSignInButton = findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        Bugly.init(getApplicationContext(), "ff0995764e", false);

        dbManager = new DBManager(this);
        String appid = readData(AppConst.KeyAppId);
        if(!TextUtils.isEmpty(appid)){
            mAppIdView.setText(appid);
            AppConst.AppId = Integer.parseInt(appid);
            String token = readData(AppConst.KeyToken);
            if(!TextUtils.isEmpty(token) && !"123456".equals(token)){
                mTokenView.setText(token);
                attemptLogin();
            }
        }else{
            createAppId();
        }
    }


    private void createAppId() {
        String appUnid = AppUtil.getUniqueId(this);
        if(appUnid!=null){
            showProgress(true);
            Handler.Callback callback= new Handler.Callback(){
                @Override
                public boolean handleMessage(Message message) {
                    try {

                        JSONObject json = new JSONObject(message.obj.toString());
                        if(json.getInt("code")==0){
                            JSONObject data = json.getJSONObject("data");
                            String appId = data.getString("appid");
                            AppConst.AppId = Integer.parseInt(appId);
                            mAppIdView.setText(appId);
                            dbManager.setConfig(AppConst.KeyAppId,appId);
                            helpView.setText(data.getString("help"));
                        }else{
                            Toast.makeText(LoginActivity.this, json.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    }catch (JSONException je){
                        Toast.makeText(LoginActivity.this, je.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    showProgress(false);
                    return false;
                }
            };
            Handler idHander = new Handler(callback);
            String sign= AppUtil.toMD5("zhiyikeji"+appUnid);
            RequestUtils.getRequest(AppConst.HostUrl+"person/api/getAppId/unid/"+appUnid+"/sign/"+sign,idHander);
        }
    }

    private String readData(String name){
        return dbManager.getConfig(name);
    }

    public void act_GetToken(View view) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(AppConst.HostUrl+"person/index/getToken/appid/"+AppConst.AppId));//Url 就是你要打开的网址
        intent.setAction(Intent.ACTION_VIEW);
        this.startActivity(intent); //启动浏览器
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mTokenView.setError(null);

        // Store values at the time of the login attempt.
        String token = mTokenView.getText().toString();
        AppConst.Token = token;
        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(token)) {
            focusView = mTokenView;
            cancel = true;
        }
        String appid = ""+AppConst.AppId;

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            RequestUtils.getRequest(AppConst.HostUrl+"person/api/login/appid/"+appid+"/token/"+token,callback);
        }
    }

    private Handler callback = new Handler(
            new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    showProgress(false);
                    try {

                        JSONObject json = new JSONObject(message.obj.toString());
                        if(json.getInt("code")==0){
                            AppConst.Secret = json.getString("data");
                            dbManager.setConfig(AppConst.KeyToken,AppConst.Token);
                            dbManager.setConfig(AppConst.KeySecret,AppConst.Secret);
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        }else{
                            Toast.makeText(LoginActivity.this, json.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                    }catch (JSONException je){
                        Toast.makeText(LoginActivity.this, je.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            }
    );



    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

}

