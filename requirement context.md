Here’s a single, copy-paste brief you can give to Codex in VS Code so it has enough context to scaffold the repo and guide you step-by-step.

---

**Prompt for Codex (paste this in your VS Code chat):**

You are my repo-setup assistant. I’m on macOS using Android Studio and VS Code. I want a **$0 Android push-notification POC** using **Firebase Cloud Messaging (FCM)**—no Play Store publishing. Target device: **Google Pixel** (Android 13+). Language: **Kotlin**. Build: **Gradle (KTS)**. Also include a tiny **sender** so I can trigger pushes from my Mac (Node.js or Python—your choice, pick the simpler).

### Goals

1. Android app that:

   * requests notification permission on Android 13+,
   * obtains and logs the **FCM registration token**,
   * receives foreground/background notifications,
   * displays payload data in-app (simple log/Toast/UI text),
   * handles **data-only** messages via `FirebaseMessagingService`.

2. Minimal sender tool that:

   * uses **FCM HTTP v1**,
   * authenticates with a **Google Cloud service account JSON**,
   * sends both **notification** and **data** payload examples to a given token.

### Constraints

* Keep everything minimal; prioritize clarity over features.
* No paid services. Use Firebase free tier.
* I will add my own `google-services.json` later; include a placeholder and instructions.
* The project must build/run on **macOS** with Android Studio.
* Use **Kotlin**, **Gradle KTS**, and the **Firebase BoM**.

### Deliverables

1. **Repo structure** (create all files with initial content):

```
android-fcm-poc/
  README.md
  .gitignore
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/example/poc/MainActivity.kt
    src/main/java/com/example/poc/AppMessagingService.kt
    src/main/res/layout/activity_main.xml
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  tools/sender/   (choose Node.js OR Python)
    README.md
    package.json  (if Node) OR requirements.txt (if Python)
    send.js       (Node)   OR send.py         (Python)
  docs/
    fcm.http.json.examples
  google-services.json.example
```

2. **Android app specifics**

* Add Firebase dependencies via BoM in `app/build.gradle.kts`:

  * `com.google.firebase:firebase-messaging`
* Apply `com.google.gms.google-services`.
* Request `POST_NOTIFICATIONS` permission at runtime for Android 13+.
* `MainActivity`:

  * button “Get Token” → fetch and log token (`Log.d`), show it on screen.
  * text area/log view for last received payload (title/body/data).
* `AppMessagingService`:

  * override `onMessageReceived` to log and broadcast the payload to the activity (e.g., via `LocalBroadcastManager` or a simple `MutableLiveData`).
  * override `onNewToken` to log and update UI.
* `AndroidManifest.xml`:

  * declare internet permission,
  * declare `AppMessagingService` with the right intent filter,
  * set a default notification channel via metadata or create it at runtime.
* `google-services.json.example`:

  * clearly comment which fields I will replace (project_number, project_id, mobilesdk_app_id, package_name).

3. **Sender tool**

* Choose **Node.js** (preferred) or **Python**—whichever is shortest to get running.
* Provide scripts to:

  * obtain an OAuth2 **access token** from the service account JSON,
  * send a **notification** message (title/body),
  * send a **data-only** message (e.g., `{"action":"PING","ts":"<unix>"}`),
  * configurable target: single token via CLI arg or `.env`.
* Include example payload files and commands in `tools/sender/README.md`.

4. **README.md** (top-level) with exact steps:

* Prereqs (Android Studio, Java 17, Node/Python).
* Firebase Console steps (create project, add Android app, download `google-services.json`, place into `app/`).
* Running the app on a Pixel (USB debugging) and using the **emulator**.
* How to view the FCM token (logcat + on-screen).
* How to run the sender:

  * install deps,
  * export `GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json`,
  * run a command to send both notification and data messages.
* Troubleshooting section:

  * Android 13+ permission,
  * notification channels/importance,
  * battery optimization,
  * token rotation (`onNewToken`),
  * ensuring the Firebase project and package name match.

5. **Acceptance checklist** (include at end of README):

* [ ] App compiles and installs on a Pixel (Android 13+).
* [ ] Tapping **Get Token** shows an FCM token on-screen and in logcat.
* [ ] Sender can push a **notification** message and it appears as a system notification.
* [ ] Sender can push a **data-only** message and `AppMessagingService` logs it, then UI displays it.
* [ ] Works on emulator and physical device.

### Notes for you (Codex)

* Fill in boilerplate content where reasonable (package name `com.example.poc`).
* Put TODO comments where I must paste real project values.
* Keep code idiomatic (Kotlin) and concise.
* Prefer **Node.js** sender using `google-auth-library` for brevity.
* Provide sample `npm` scripts: `send:notification`, `send:data`, with token and title/body as args.

When you’re ready, start by generating the full repo tree and initial file contents, then print:

1. the file tree,
2. any **commands** I need to run (Android Studio import, Gradle sync, Node/Python install, sender commands),
3. reminders about where to place `google-services.json` and the service account JSON.

---

If you want, I can also paste a quick follow-up “one-shot” Codex message that asks it to **fill in** any placeholders with my real Firebase values once I have them.
