package com.aa2test2

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.os.Build
import android.os.IBinder
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.flipper.ReactNativeFlipper
import com.facebook.soloader.SoLoader

import com.governikus.ausweisapp2.IAusweisApp2Sdk
import com.governikus.ausweisapp2.IAusweisApp2SdkCallback

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              // Packages that cannot be autolinked yet can be added manually here, for example:
              // add(MyReactNativePackage())
            }

        override fun getJSMainModuleName(): String = "index"

        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }

  override val reactHost: ReactHost
    get() = getDefaultReactHost(this.applicationContext, reactNativeHost)

  private var sdkConnection: ServiceConnection? = null
  private var sdkSessionId: String? = null

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, false)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      // If you opted-in for the New Architecture, we load the native entry point for this app.
      load()
    }
    ReactNativeFlipper.initializeFlipper(this, reactNativeHost.reactInstanceManager)

    val packageName = applicationContext.packageName
    val serviceIntent = Intent("com.governikus.ausweisapp2.START_SERVICE")
      .setPackage(packageName)

    val sdkCallback = object : IAusweisApp2SdkCallback.Stub() {
      override fun sessionIdGenerated(sessionId: String, isSecureSessionId: Boolean) {
        this@MainApplication.sdkSessionId = sessionId
      }

      override fun receive(messageJson: String) {
        print("message: $messageJson")
      }

      override fun sdkDisconnected() {
        print("disconnected")
        sdkSessionId = null
      }
    }

    sdkConnection = object : ServiceConnection {
      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        try {
          val sdk = IAusweisApp2Sdk.Stub.asInterface(service)
          sdk?.connectSdk(sdkCallback)
          print("connected")
        } catch (e: Exception) {
          print("error connecting service: $e")
        }
      }

      override fun onServiceDisconnected(className: ComponentName) {
        // Needed for ServiceConnection Interface
      }
    }.also {
      try {
        this.bindService(serviceIntent, it, Context.BIND_AUTO_CREATE)
      } catch (e: Exception) {
        print("error binding service $e")
      }
    }
  }
}
