package com.example.timil.sensorproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.example.timil.sensorproject.database.StepDB
import com.example.timil.sensorproject.entities.Step
import com.example.timil.sensorproject.fragments.*
import org.jetbrains.anko.doAsync
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val THEME_PREF = "pref_theme_settings"

class MainActivity : AppCompatActivity(), SensorEventListener, MapFragment.MapFragmentTrophyClickListener, AugmentedTrophyFragment.AugmentedFragmentTrophyClickListener {

    private val homeFragment = HomeFragment()
    private val statisticsFragment = StatisticsFragment()
    private val mapFragment = MapFragment()
    private var pref: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private lateinit var sm: SensorManager
    private var sStepDetector: Sensor? = null

    private val date = LocalDateTime.now()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val formattedDate = date.format(formatter)

    override fun onCreate(savedInstanceState: Bundle?) {
        // bind shared preference values the first time app is created
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        val themePreference = getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        val useTheme = themePreference.getString(THEME_PREF, "N/A")

        Log.d("DBG", "Using theme: "+useTheme)
        when(useTheme){
            "AppTheme" -> setTheme(R.style.AppTheme)
            "AppBlueTheme" -> setTheme(R.style.AppBlueTheme)
            "AppGreenTheme" -> setTheme(R.style.AppGreenTheme)
            "AppRedTheme" -> setTheme(R.style.AppRedTheme)
            "N/A" -> setTheme(R.style.AppTheme)
        }

        super.onCreate(savedInstanceState)

        if ((/*Build.VERSION.SDK_INT >= 23 &&*/
                        ContextCompat.checkSelfPermission(this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),0
            )
        }

        createNotificationChannel()
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sStepDetector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        // check there is step_detector sensor in the used device
        // if sensor exists, register listener and add observer
        // else inform user there is no sensor needed
        if (sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null ) {
            sm.registerListener(this, sStepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        }
        else {
            val snack = Snackbar.make(container, "You don't have required sensor(STEP_DETECTOR) in your phone!", Snackbar.LENGTH_LONG)
            snack.setAction("CLOSE", {})
            snack.setActionTextColor(Color.WHITE)
            snack.show()
        }

        setContentView(R.layout.activity_main)

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        //Log.d("DBG", pref!!.getString("pref_settings", "N/A"))

        listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->

            // check if key value already exists
            if (key == THEME_PREF) {
                Log.d("DBG", prefs.getString(key, "N/A") + key)
                val editor = getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE).edit()
                editor.putString(key, prefs.getString(key, "N/A"))
                editor.apply()

                val intent = intent
                finish()

                startActivity(intent)
            }
            //setTheme(R.style.AppGreenTheme)
            //recreate()


        }
        pref!!.registerOnSharedPreferenceChangeListener(listener)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        //swsupportFragmentManager.beginTransaction().add(R.id.fragment_container, mapFragment).commit()

        //supportFragmentManager.beginTransaction().hide(mapFragment).commit()

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, homeFragment).commit()

    }

    override fun onDestroy() {
        super.onDestroy()
        pref!!.unregisterOnSharedPreferenceChangeListener(listener)
        sm.unregisterListener(this, sStepDetector)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // do stuff
    }

    override fun onSensorChanged(event: SensorEvent) {
        doAsync {
            if (getSteps(formattedDate) == pref!!.getString("pref_goal", "N/A").toInt()*100) {
                val notification = NotificationCompat.Builder(this@MainActivity, "Channel_id")
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle("Nice Job!")
                        .setContentText("You reached your step goal of the day")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()
                NotificationManagerCompat.from(this@MainActivity).notify(1, notification)
            }
            saveSteps(formattedDate, getSteps(formattedDate) + 1)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater

        inflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.settings -> {
                //Log.d("DBG", "Clicked settings")
                val settingsFragment = SettingsFragment()
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, settingsFragment).addToBackStack(null).commit()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTrophyClick(id: Long, latitude: Double, longitude: Double) {

        val arFragment = AugmentedTrophyFragment()

        val bundle = Bundle()
        bundle.putInt("x", getScreenCenter().x)
        bundle.putInt("y", getScreenCenter().y)
        bundle.putLong("id", id)
        bundle.putDouble("latitude", latitude)
        bundle.putDouble("longitude", longitude)
        arFragment.arguments = bundle

        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, arFragment).addToBackStack(null).commit()
    }

    override fun onARTrophyClick() {
        supportFragmentManager.popBackStack()
        val snack = Snackbar.make(container, "Trophy collected! Added +500 points to high score!", Snackbar.LENGTH_LONG)
        snack.setAction("CLOSE", {})
        snack.setActionTextColor(Color.WHITE)
        snack.show()
    }

    private fun saveSteps(sid: String, steps: Int){
        StepDB.get(this).stepDao().insert(Step(sid, steps))
    }

    private fun getSteps(date: String): Int{
        return when (StepDB.get(this).stepDao().getSteps(date)?.steps != null){
            true -> StepDB.get(this).stepDao().getSteps(date)!!.steps
            false -> 0
        }
    }

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Name"
            val description = "Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("Channel_id", name, importance)
            channel.description = description
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, homeFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, statisticsFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, mapFragment).commit()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

}
