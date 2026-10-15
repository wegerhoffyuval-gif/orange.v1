# 🍊 Orange Reminder

An initial Android reminder app written in Kotlin.

## Features in this starter version
- Choose a date using Android's calendar date picker.
- Give each reminder a title and optional note.
- Pick a reminder time.
- Alert at the selected time or 5 minutes, 10 minutes, 30 minutes, 1 hour, 2 hours, or 1 day before.
- Choose a default ringtone, alarm sound, or notification sound.
- Choose an accent color: Orange, Ocean, Forest, Purple, or Rose.
- View and delete saved reminders.
- GitHub Actions workflow to build a debug APK.

## Build an APK using GitHub Actions
1. Upload these project files to the root of your GitHub repository.
2. Open the repository's **Actions** tab and enable Actions if asked.
3. Select **Build Android APK** and press **Run workflow**, or push a commit to `main`.
4. Open the completed workflow run and download `OrangeReminder-debug-apk`.
5. Extract the ZIP and install `app-debug.apk` on your Android device. Android may ask you to allow installation from that source.

This is a debug APK for testing, not a signed Play Store release.

## Important limitations
- Google Calendar sync is not implemented yet. It needs a Google Cloud project, Calendar API enabled, OAuth consent/client setup, and Google sign-in integration.
- Alarm delivery depends on Android notification and exact-alarm permissions and device battery settings. Test the app on your phone.
- This starter version stores reminders locally on the device. It does not yet reschedule reminders after a device restart.
- Test the build and reminder behavior before relying on it for important events.
