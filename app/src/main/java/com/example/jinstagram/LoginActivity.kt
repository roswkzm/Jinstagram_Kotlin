package com.example.jinstagram

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.lang.Exception
import android.content.pm.PackageManager

import android.content.pm.PackageInfo
import android.util.Base64
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        // 이메일 로그인버튼
        email_login_button.setOnClickListener {
            signinAndSignup()
        }

        //구글 로그인 버튼
        google_sign_in_button.setOnClickListener {
            googleLogin()
        }

        // 페이스북 로그인 버튼
        facebook_login_button.setOnClickListener {
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("761147144043-hvuekmart0pm5ab2h168fn327tn6h85g.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        //printHashKey()
        callbackManager = CallbackManager.Factory.create()
    }

    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    // facebook 로그인할 때 hash Key 값을 확인하는 함
    fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey: String = String(Base64.encode(md.digest(), 0))
                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TAG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TAG", "printHashKey()", e)
        }
    }

    //구글로그인
    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    // facebook Login 버튼을 누르면 수행하는곳
    fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile","email"))

        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.i("facebook_callback", "success")
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Log.i("facebook_callback", "cancel")
                }

                override fun onError(error: FacebookException?) {
                    Log.i("facebook_callback", "error")
                }
            })
    }

    // facebookLogin에서 로그인이 성공적으로 수행했을시
    fun handleFacebookAccessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {
                task ->
            if (task.isSuccessful){
                // Login
                moveMainPage(task.result?.user)
            }else{
                // Error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 엑티비티에 담아 로그인 정보넘기기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data!!)
             if (result!!.isSuccess){
                 var account = result.signInAccount
                 firebaseAuthWithGoogle(account)
             }

        }
    }
    //구글로그인
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken,null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if (task.isSuccessful){
                    // Login
                    moveMainPage(task.result?.user)
                }else{
                    // Error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    //이메일 회원가입
    fun signinAndSignup(){
        try {
            auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
                ?.addOnCompleteListener {
                        task ->
                    if (task.isSuccessful){
                        // Creating a user account
                        moveMainPage(task.result?.user)
                    }else if (task.exception?.message.isNullOrEmpty()){
                        // Show error message
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }else{
                        // Login if you have account
                        signinEmail()
                    }
                }
        }catch (e:Exception){
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요",Toast.LENGTH_SHORT).show()
        }
    }
    // 이메일 로그인
    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener {
                    task ->
                if (task.isSuccessful){
                    // Login
                    moveMainPage(task.result?.user)
                }else{
                    // Error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }
    fun moveMainPage(user:FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}