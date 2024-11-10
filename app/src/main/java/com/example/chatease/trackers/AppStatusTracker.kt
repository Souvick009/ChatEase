package com.example.chatease.trackers

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.example.chatease.activities.SignInActivity
import com.example.chatease.activities.SignUpActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.atomic.AtomicInteger

class AppStatusTracker : Application(), Application.ActivityLifecycleCallbacks {

    private lateinit var rtDB: FirebaseDatabase
    private var currentUser: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private var activityCount = AtomicInteger(0)
    private var lastStatus: String? = null
    private var isActivityChangingConfiguration = false
    private var notificationService: NotificationService? = null
    private var serviceConnection: ServiceConnection? = null
    private var serviceBound = false

    override fun onCreate() {
        super.onCreate()
        rtDB = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as NotificationService.NotificationBinder
                notificationService = binder.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d("SERVICE", "Service disconnected")
                serviceBound = false
                notificationService = null
            }
        }
        registerAuthListener()
        setupConnectionListener()
        registerActivityLifecycleCallbacks(this)
    }

    private fun registerAuthListener() {
        auth.addAuthStateListener { authStatus ->
            currentUser = authStatus.currentUser
            if (currentUser == null)
                updateStatus("Offline")
        }
    }

    private fun setupConnectionListener() {
        currentUser?.let { user ->
            rtDB.getReference(".info/connected")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val connected = snapshot.getValue(Boolean::class.java) ?: false
                        if (connected && currentUser != null) {
                            setupOnlineStatusWithOnDisconnect()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        }
    }

    fun setupOnlineStatusWithOnDisconnect() {
        currentUser?.let { user ->
            val userRef = rtDB.getReference("users").child(user.uid)
            userRef.child("status").onDisconnect().setValue("Offline")
            userRef.child("lastHeartBeat").onDisconnect().setValue(ServerValue.TIMESTAMP)

            updateStatus("Online")
        }

    }

    override fun onActivityStarted(activity: Activity) {
        currentUser?.let {
            if (!isActivityChangingConfiguration && activity !is SignInActivity && activity !is SignUpActivity) {
                if (activityCount.incrementAndGet() == 1) {
                    // The service will start here as the app is in foreground
                    Log.d("SERVICE BINDING ACTIVITY",activity.localClassName)

                    startNotificationService(activity)

                    updateStatus("Online")
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        currentUser?.let {
            isActivityChangingConfiguration = activity.isChangingConfigurations
            if (!isActivityChangingConfiguration && activity !is SignInActivity && activity !is SignUpActivity) {
                if (activityCount.decrementAndGet() == 0) {
                    // The service will stop here as the app is in background
                    Log.d("SERVICE BINDING ACTIVITY",activity.localClassName)
                    stopNotificationService(activity)
                    updateStatus("Offline")
                }
            }
        }
    }

    private fun startNotificationService(activity: Activity) {
        Log.d("START", "STARTING")
        if (!serviceBound) {
            val intent = Intent(activity, NotificationService::class.java)
            serviceConnection?.let {
                startService(intent)
                activity.bindService(intent, it, Context.BIND_AUTO_CREATE)
                serviceBound = true
                Log.d("START", "STARTED")
            }
        }
    }

    private fun stopNotificationService(activity: Activity) {
        Log.d("STOP", "STOPPING")
        if (serviceBound) {
            val intent = Intent(activity, NotificationService::class.java)
            serviceConnection?.let {
                stopService(intent)
                try {
                    activity.unbindService(it)
                } catch (e: Exception) {
                    Log.e("ERROR","NO ERROR: CHINTA KI KOI BAAT NAHI HAI")
                }

                serviceBound = false
                Log.d("STOP", "STOPPED")
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
        auth.removeAuthStateListener { registerAuthListener() }
    }

    private fun updateStatus(status: String) {
        currentUser?.let { user ->
            if (status != lastStatus) {
                lastStatus = status
                rtDB.getReference("users").child(user.uid)
                    .updateChildren(
                        mapOf(
                            "status" to status,
                            "lastHeartBeat" to ServerValue.TIMESTAMP
                        )
                    )
            }
        }
    }
}