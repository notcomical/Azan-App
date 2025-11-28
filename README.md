# Azan Alarm App

Never miss a prayer again! This Android app automatically sets alarms for all five daily prayers based on your location, ensuring you're always notified when it's time to pray.

## Features

- **Automatic Prayer Times** - Just turn on your location and the app fetches accurate prayer times for your area
- **Five Daily Prayers** - Get alarms for Fajr, Dhuhr, Asr, Maghrib, and Isha
- **Custom Tahajjud Time** - Set your own time for Tahajjud prayer (night prayer)
- **Personalized Alarm Sound** - Choose your favorite ringtone or Azan audio from your phone
- **Individual Prayer Control** - Enable or disable alarms for specific prayers with a simple toggle
- **24-hour or 12-hour Format** - Display prayer times in your preferred format
- **Dark Mode** - Easy on the eyes during night prayers
- **Auto-Restart After Reboot** - Your alarms automatically reschedule when your phone restarts
- **Beautiful Interface** - Clean, modern design that's easy to use

## Installation

### Download from GitHub

1. Go to the [Releases](../../releases) page of this repository
2. Download the `.apk` file
3. On your Android phone, go to **Settings** → **Security** (or **Privacy**)
4. Enable **Install from Unknown Sources** (or **Install Unknown Apps** for Android 8+)
5. Open the downloaded APK file from your Downloads folder
6. Tap **Install** and follow the prompts
7. Once installed, you can disable "Unknown Sources" if you wish

### Build from Source

If you prefer to build the app yourself:

1. Clone this repository:
   ```
   git clone https://github.com/n0tcomical/AzanApp.git
   ```
2. Open the project in Android Studio
3. Let Gradle sync complete
4. Connect your Android device or start an emulator
5. Click **Run** → **Run 'app'**

## How to Use

1. **First Time Setup**
   - Open the app and grant location permission when asked
   - Grant notification permission (for Android 13 and above)
   - The app will automatically detect your location

2. **Fetch Prayer Times**
   - Tap the refresh icon in the menu or pull down to refresh
   - The app will fetch today's prayer times based on your location
   - All five prayer alarms will be automatically scheduled

3. **Customize Your Experience**
   - **Toggle Alarms**: Use the switches next to each prayer to enable or disable specific alarms
   - **Custom Tahajjud Time**: Tap the menu → "Set Tahajjud Time" to choose your preferred time
   - **Change Alarm Sound**: Tap the menu → "Select Alarm Sound" and pick any audio file from your phone
   - **Switch Time Format**: Tap the menu → "24-Hour Format" to toggle between 12-hour and 24-hour display
   - **Enable Dark Mode**: Tap the menu → "Dark Mode" for a darker theme

4. **Daily Use**
   - Prayer times update automatically based on your location
   - Pull down to refresh if you've traveled to a new location
   - Your alarms will go off at the right time, even if your phone is locked

## Requirements

- Android 7.0 (Nougat) or higher
- Location services enabled
- Internet connection (to fetch prayer times)

## Permissions Explained

The app needs these permissions to work properly:

- **Location** - To determine your current location and fetch accurate prayer times
- **Notifications** - To alert you when it's time for prayer
- **Exact Alarms** - To ensure alarms go off at the precise prayer time
- **Audio/Media** - To let you select custom alarm sounds from your phone
- **Boot Completed** - To automatically reschedule alarms after your phone restarts

## Prayer Time Calculation

This app uses the **University of Islamic Sciences, Karachi** calculation method by default, fetching data from the trusted [Aladhan Prayer Times API](https://aladhan.com/prayer-times-api).

## TechStack

- **Kotlin** - Modern Android development language
- **Retrofit** - For API communication
- **Coroutines** - For smooth, non-blocking operations
- **Google Play Services** - For accurate location detection
- **Material Design** - For a beautiful, intuitive interface

## License

This project is open source and available for educational and personal use.

---

**May this app help you stay connected to your prayers. Jazakallahu Khairan!**
   