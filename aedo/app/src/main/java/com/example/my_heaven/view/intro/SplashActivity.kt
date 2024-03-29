package com.example.my_heaven.view.intro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.example.my_heaven.R
import com.example.my_heaven.api.APIService
import com.example.my_heaven.api.ApiUtils
import com.example.my_heaven.databinding.ActivitySplashBinding
import com.example.my_heaven.model.restapi.base.AppPolicy
import com.example.my_heaven.model.restapi.base.Encrypt
import com.example.my_heaven.model.restapi.base.Policy
import com.example.my_heaven.model.restapi.base.Verification
import com.example.my_heaven.util.`object`.ActivityControlManager
import com.example.my_heaven.util.`object`.Constant
import com.example.my_heaven.util.`object`.Constant.RESULT_TRUE
import com.example.my_heaven.util.alert.CustomDialog
import com.example.my_heaven.util.base.BaseActivity
import com.example.my_heaven.util.base.MyApplication
import com.example.my_heaven.util.log.LLog
import com.example.my_heaven.util.log.LLog.TAG
import com.example.my_heaven.util.network.ResultListener
import com.example.my_heaven.util.root.RootUtil
import com.example.my_heaven.util.style.TextStyle
import com.example.my_heaven.view.login.LoginActivity
import com.example.my_heaven.view.main.MainActivity
import com.getkeepsafe.relinker.BuildConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var apiServices: APIService
    private var devpolicyversion: String? = null
    private var prefs = MyApplication.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apiServices = ApiUtils.apiService
        inStatusBar()
        checknetwork()
//        moveActivity()
    }

    // 네트워크 체크
    private fun checknetwork() {
        LLog.e("1. 네트워크 확인")
        if (!isNetworkAvailable) {
            alert?.showDialog(
                getString(R.string.error_network_connectivity)) {
                finishAffinity()
            }?.cancelable(false)
            return
        }
        checkLoot()
        checkVerification()
    }

    // 2. 루팅 확인
    private fun checkLoot() {
        LLog.e("2. 루팅 확인")
        if (!BuildConfig.DEBUG && RootUtil.isDeviceRooted) {
            alert?.showDialog(
                getString(R.string.warning_rooting)) {
                finishAffinity()
            }?.cancelable(false)
            return
        }
    }

    // 3. 검증 API 호출키 및 Hash키 검증
    private fun checkVerification() {
        LLog.e("3. 검증 API 호출키 및 Hash키 검증")
        requestVerificationJson(object : ResultListener {
            override fun onSuccess() {
                checkPolicyVersion()
            }
        })
    }

    // 4. 정책 버전 비교 및 저장
    private fun checkPolicyVersion() {
        LLog.e("4. 정책 버전 비교 및 저장")
        requestPolicy(object : ResultListener {
            override fun onSuccess() {
                enablecheck()
            }
        })
    }

    private fun requestVerificationJson(listener: ResultListener) {
        LLog.e("APP HASH: $hash")
        val vercall: Call<Verification> = apiServices.getVerification("qwer")
        vercall.enqueue(object : Callback<Verification> {
            override fun onResponse(call: Call<Verification>, response: Response<Verification>) {
                val result = response.body()
                if (response.isSuccessful && result != null) {
                    val encrypt = Encrypt()
                    encrypt.key.toString()
                    encrypt.iv.toString()
                    result.result
                    result.encrypt
                    result.app_token
                    result.policy_ver
                    if (result.result == RESULT_TRUE) {
                        devpolicyversion = result.policy_ver.toString()
                        getinformation(result, listener)
                    }
                    else {
                        Log.d(TAG,"Vertification result ERROR -> ${result.result.equals("true")}")
                        alert?.showDialog(
                            getString(R.string.warning_repackaging)) {
                            finishAffinity()
                        }?.cancelable(false)
                    }
                }
                else {
                    finish()
                }
            }
            override fun onFailure(call: Call<Verification>, t: Throwable) {
                Log.d(TAG, "loadVerAPI error -> $t")
                finishAffinity()
            }
        })
    }

    private fun getinformation(result: Verification?, networkListener: ResultListener) {
        var app_token: String? = result?.app_token.toString()
        val aes_iv: String? = Encrypt().iv
        val aes_key: String? = Encrypt().key
        if (app_token != null) {
            app_token = app_token.substring(8)
        }
        prefs.myapptoken = app_token
        prefs.myeniv = aes_iv
        prefs.myenkey = aes_key
        prefs.mylangcode = "LANG_0001"
        Log.d(TAG,"Prefs AppToken SUCCESS -> ${prefs.myapptoken}")
        Log.d(TAG,"Prefs MyEnIv SUCCESS -> ${prefs.myeniv}")
        Log.d(TAG,"Prefs MyEnKey SUCCESS -> ${prefs.myenkey}")
        networkListener.onSuccess()
    }

    private fun requestPolicy(listener: ResultListener) {
        val vercall: Call<AppPolicy> = apiServices.getPolicy("qwer")
        vercall.enqueue(object : Callback<AppPolicy> {
            override fun onResponse(call: Call<AppPolicy>, response: Response<AppPolicy>) {
                val result = response.body()
                if (response.isSuccessful && result != null) {
                    Log.d(TAG,"Policy response SUCCESS -> $result")
                    moveLogin()
                }
                else {
                    Log.d(TAG,"Policy response ERROR -> $result")
                    finish()
                }
            }
            override fun onFailure(call: Call<AppPolicy>, t: Throwable) {
                Log.d(TAG, "Policy error -> $t")
                finishAffinity()
            }
        })
    }

    private fun enablecheck() {
        val useYn = realm.where(Policy::class.java).equalTo("id","APP_ENABLE_YN").findFirst()
        val popupText = realm.where(Policy::class.java).equalTo("id","APP_ENABLE_CONTENT").findFirst()
        if (useYn != null) {
            if (useYn.value.equals("Y")) {
                alert!!.showDialog(if (popupText != null) popupText.value else getString(R.string.service_disable_error)) {
                    finishAffinity()
                }!!.cancelable(false)
                return
            }
        } else {
            alert!!.showDialog(getString(R.string.error_policy_update_db)) { v -> finishAffinity() }!!.cancelable(false)
            return
        }
        emergencyPopup()
        checkAppVersion()
    }

    // App 버전 체크
    private fun checkAppVersion() {
        val versionCode: Policy? =
            realm.where(Policy::class.java).equalTo("id", "APP_VER_ANDROID").findFirst()
        if (versionCode != null) {
            val refrenceCode: Int = versionCode.value!!.toInt()
            var currentCode: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentCode = try {
                    packageManager.getPackageInfo(
                        this.packageName,
                        PackageManager.GET_ACTIVITIES
                    ).longVersionCode
                        .toInt()
                } catch (e: PackageManager.NameNotFoundException) {
                    -1
                }
            } else {
                val manager = this.packageManager
                val info: PackageInfo
                try {
                    info = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
                    currentCode = info.versionCode
                } catch (e: PackageManager.NameNotFoundException) {
                    currentCode = -1
                }
            }
            if (currentCode < refrenceCode) {
                showAppUpdate()
                return
            }
        } else {
            alert!!.showDialog(getString(R.string.error_policy_update_db)) { v -> finishAffinity() }!!
                .cancelable(false)
            return
        }
        emergencyPopup()
    }

    private fun showAppUpdate() {
        val upadateDialog = CustomDialog(this)
        upadateDialog.text(getString(R.string.need_update_app))
            ?.positive(getString(R.string.update)) { v ->
                val appPackageName =
                    packageName
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$appPackageName")
                        )
                    )
                    finish()
                } catch (anfe: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                        )
                    )
                    finish()
                }
            }
    }

    // 긴급공지 체크
    private fun emergencyPopup() {
        val popupTime: Policy? =
            realm.where(Policy::class.java).equalTo("id","POPUP_TIME_END").findFirst()
        val popupContent: Policy? =
            realm.where(Policy::class.java).equalTo("id", "POPUP_CONTENT").findFirst()
        val popupEnable: Policy? =
            realm.where(Policy::class.java).equalTo("id", "POPUP_ENABLE_YN").findFirst()

        if (popupEnable != null) {
            if ("Y" == popupEnable.value && TextStyle.compareDateAvailable(
                    popupTime!!.value!!,
                    "yyyyMMddHHmmss"
                ) && !prefs.getStr(Constant.PREF_EMERGENCY_NOTICE_NOT_SHOW, "").equals(popupTime.value)
            ) {
                val dialog = CustomDialog(this)
                if (popupContent != null) {
                    dialog.text(popupContent.value)
                        ?.positive(getString(R.string.ok)) {
                            dialog.dismiss()
                        }
                        ?.negative(getString(R.string.not_show_again)) {
                            dialog.dismiss()
                            prefs.setStr(Constant.PREF_EMERGENCY_NOTICE_NOT_SHOW, popupTime.value)
                        }!!.cancelable(false).show()
                }
                return
            }
        }
    }

    private fun moveActivity() {
        ActivityControlManager.delayRun({
            moveAndFinishActivity(MainActivity::class.java)
            overridePendingTransition(0, 0)
        }, Constant.SPLASH_WAIT)
    }

    private fun moveLogin() {
        ActivityControlManager.delayRun({
            moveAndFinishActivity(LoginActivity::class.java) },
            Constant.SPLASH_WAIT
        )
    }
}