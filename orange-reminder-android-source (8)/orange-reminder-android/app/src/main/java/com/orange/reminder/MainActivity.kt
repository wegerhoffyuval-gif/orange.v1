package com.orange.reminder

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("orange_reminder", MODE_PRIVATE) }
    private val date = Calendar.getInstance()
    private var hour = (date.get(Calendar.HOUR_OF_DAY) + 1) % 24
    private var minute = 0
    private lateinit var selectedDate: TextView
    private lateinit var reminderList: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var offsetSpinner: Spinner
    private lateinit var soundSpinner: Spinner
    private lateinit var themeSpinner: Spinner

    private val offsets = listOf(
        "At the time", "5 minutes before", "10 minutes before",
        "30 minutes before", "1 hour before", "2 hours before", "1 day before"
    )
    private val offsetMillis = listOf(
        0L, 5*60_000L, 10*60_000L, 30*60_000L,
        60*60_000L, 120*60_000L, 24*60*60_000L
    )
    private val sounds = listOf("Default ringtone", "Alarm sound", "Notification sound")
    private val soundKeys = listOf("default", "alarm", "notification")
    private val themes = listOf("Orange", "Ocean", "Forest", "Purple", "Rose")
    private val colors = listOf(
        0xFFFF8A00.toInt(), 0xFF1677D2.toInt(), 0xFF21865B.toInt(),
        0xFF805AD5.toInt(), 0xFFE84A75.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 44)
        }
        buildUi()
    }

    private fun buildUi() {
        val themeIndex = prefs.getInt("theme", 0).coerceIn(colors.indices)
        val accent = colors[themeIndex]
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 24)
            setBackgroundColor(Color.rgb(250, 248, 244))
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(TextView(this).apply {
            text = "🍊 Orange Reminder"
            textSize = 27f
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 6)
        })
        content.addView(TextView(this).apply {
            text = "Your plans, your colors, your time."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 18)
        })

        val calendarButton = button("📅 Choose date from calendar", accent) {
            DatePickerDialog(this, { _, y, m, d ->
                date.set(y, m, d)
                updateDateLabel()
            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
        }
        content.addView(calendarButton, matchWrap())
        selectedDate = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 14)
        }
        content.addView(selectedDate)
        updateDateLabel()

        titleInput = EditText(this).apply {
            hint = "Reminder name (e.g. Homework)"
            textSize = 16f
            setSingleLine(true)
        }
        content.addView(titleInput, matchWrap())
        noteInput = EditText(this).apply {
            hint = "Extra details (optional)"
            textSize = 15f
        }
        content.addView(noteInput, matchWrap())

        content.addView(button("🕒 Choose reminder time", accent) {
            TimePickerDialog(this, { _, h, m ->
                hour = h
                minute = m
                Toast.makeText(this, String.format(Locale.getDefault(), "Time set to %02d:%02d", h, m), Toast.LENGTH_SHORT).show()
            }, hour, minute, true).show()
        }, matchWrap())

        offsetSpinner = spinner(offsets)
        soundSpinner = spinner(sounds)
        themeSpinner = spinner(themes)
        addLabel(content, "Alert me:")
        content.addView(offsetSpinner, matchWrap())
        addLabel(content, "Ringtone:")
        content.addView(soundSpinner, matchWrap())
        addLabel(content, "Calendar color:")
        content.addView(themeSpinner, matchWrap())
        themeSpinner.setSelection(themeIndex)
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("theme", position).apply()
                (content.getChildAt(0) as TextView).setTextColor(colors[position])
                calendarButton.setBackgroundColor(colors[position])
            }
        }

        content.addView(button("＋ Save reminder", accent) { saveReminder() }, matchWrap())
        content.addView(TextView(this).apply {
            text = "YOUR REMINDERS"
            textSize = 13f
            setTextColor(accent)
            setPadding(2, 24, 2, 8)
        })
        reminderList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(reminderList, matchWrap())
        content.addView(button("⚙ Settings & permissions", accent) {
            val options = arrayOf("Open notification settings", "Exact alarm permission", "Google Calendar sync status")
            AlertDialog.Builder(this).setTitle("Settings").setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                    1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } else Toast.makeText(this, "Exact alarm access is not needed on this Android version.", Toast.LENGTH_SHORT).show()
                    2 -> AlertDialog.Builder(this)
                        .setMessage("Google Calendar sync is not connected yet. It needs a Google Cloud project, Calendar API enablement, OAuth sign-in, and testing.")
                        .setPositiveButton("OK", null).show()
                }
            }.show()
        }, matchWrap())
        renderReminders()
    }

    private fun button(label: String, accent: Int, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        setBackgroundColor(accent)
        isAllCaps = false
        setPadding(10, 10, 10, 10)
        setOnClickListener { action() }
    }

    private fun spinner(items: List<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
    }

    private fun addLabel(parent: LinearLayout, value: String) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(2, 8, 0, 0)
        })
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        bottomMargin = 8
    }

    private fun updateDateLabel() {
        selectedDate.text = "Selected: " + SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(date.time)
    }

    private fun saveReminder() {
        val title = titleInput.text.toString().trim()
        if (title.isEmpty()) {
            titleInput.error = "Give your reminder a name"
            return
        }

        val cal = date.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val triggerAt = cal.timeInMillis - offsetMillis[offsetSpinner.selectedItemPosition]
        if (triggerAt <= System.currentTimeMillis()) {
            Toast.makeText(this, "Choose a future reminder time.", Toast.LENGTH_LONG).show()
            return
        }

        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            Toast.makeText(this, "Allow exact alarms, then tap Save reminder again.", Toast.LENGTH_LONG).show()
            return
        }

        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val soundKey = soundKeys[soundSpinner.selectedItemPosition]
        val note = noteInput.text.toString().trim()
        val obj = JSONObject().put("id", id).put("title", title)
            .put("note", note).put("when", cal.timeInMillis)
            .put("offset", offsetSpinner.selectedItemPosition).put("sound", soundKey)
        val all = JSONArray(prefs.getString("reminders", "[]"))
        all.put(obj)
        prefs.edit().putString("reminders", all.toString()).apply()

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("note", note)
            putExtra("sound", soundKey)
        }
        val pending = PendingIntent.getBroadcast(
            this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show()
        titleInput.text.clear()
        noteInput.text.clear()
        renderReminders()
    }

    private fun renderReminders() {
        if (!::reminderList.isInitialized) return
        reminderList.removeAllViews()
        val all = JSONArray(prefs.getString("reminders", "[]"))
        for (i in 0 until all.length()) {
            val obj = all.optJSONObject(i) ?: continue
            val id = obj.optInt("id")
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 8)
                setBackgroundColor(Color.WHITE)
            }
            val whenText = SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault()).format(Date(obj.optLong("when")))
            card.addView(TextView(this).apply {
                text = "🍊 " + obj.optString("title") + "\n" + whenText + " · " + offsets.getOrElse(obj.optInt("offset")) { "At the time" }
                textSize = 15f
                setTextColor(Color.DKGRAY)
            })
            if (obj.optString("note").isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = obj.optString("note")
                    setTextColor(Color.GRAY)
                })
            }
            card.addView(button("Delete reminder", Color.rgb(190, 65, 65)) {
                val pending = PendingIntent.getBroadcast(
                    this@MainActivity, id, Intent(this@MainActivity, ReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pending)
                val updated = JSONArray()
                for (j in 0 until all.length()) {
                    val item = all.optJSONObject(j)
                    if (item?.optInt("id") != id && item != null) updated.put(item)
                }
                prefs.edit().putString("reminders", updated.toString()).apply()
                renderReminders()
            }, matchWrap())
            reminderList.addView(card, matchWrap())
        }
        if (all.length() == 0) {
            reminderList.addView(TextView(this).apply {
                text = "No reminders yet — add your first one above."
                setTextColor(Color.GRAY)
                setPadding(2, 10, 2, 16)
            })
        }
    }
}
