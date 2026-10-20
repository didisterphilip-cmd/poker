# KettleFire 🔥

A kettlebell training web app (installable to the home screen as a PWA). It's built so it can be wrapped as an Android app later.

## Features
- **First-time setup:** account details (name, email), body info (age, sex, weight, height), workouts per week and training days, rest between exercises, and current fitness level.
- **Train:** the next workout with a description, estimated time, rounds, rest and a suggested bell weight, plus an exercise list with how-to notes. A guided player walks through every exercise with rest countdowns, beeps and vibration.
- **Calendar:** the next workout and upcoming training days, plus a streak calendar where completed days are orange with 🔥.
- **Profile:** view and edit your details and plan, or delete your data.

Workout difficulty (rounds and reps) and the suggested bell weight come from the chosen fitness level. Workouts rotate between Swing Foundations, Strength & Press and Fire Conditioning.

## Run locally
No build step is needed:
```
python3 -m http.server 8000
```
Then open http://localhost:8000. The service worker and "Add to Home Screen" need `localhost` or HTTPS.

## Deploy
Any static host works, for example GitHub Pages, Netlify or Vercel. After changing files, bump `VERSION` in `sw.js` so installed apps pick up the update.

## Structure
```
index.html            app shell
css/style.css         dark/red theme
js/storage.js         localStorage persistence + date helpers
js/workouts.js        exercise library + workout generator
js/calendar.js        calendar rendering, schedule & streak logic
js/app.js             onboarding, tabs, workout player
manifest.webmanifest  PWA manifest
sw.js                 offline cache
icons/                app icons
```

## Notes / next steps
- Data is stored **on the device only** (localStorage). There's no password and no sync between devices yet. Adding a backend (e.g. Supabase or Firebase Auth) would give real accounts.
- Android: wrap with Capacitor, or publish as a Trusted Web Activity (Bubblewrap).
