# Firebase Setup

The Android module is prepared for Firebase Authentication, Cloud Firestore, and Firebase Cloud Messaging.

1. Create or select a Firebase project.
2. Register the Android application with package name `com.anees.adventurehopper`.
3. Enable Anonymous Authentication.
4. Create a Cloud Firestore database.
5. Download `google-services.json` into `app/`.
6. Deploy `firestore.rules` with the Firebase CLI or Firebase console.
7. Configure server-side notification delivery separately if push notifications should be sent when a request is created.

The Google Services Gradle plugin is applied automatically when `app/google-services.json` exists. Until then, the app compiles but shows a Hebrew Firebase setup error instead of claiming that marketplace data exists.