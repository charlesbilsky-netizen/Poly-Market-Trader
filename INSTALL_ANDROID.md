# 📲 Install PolyTrader on Android — 3 easy steps

The app ships as **one universal APK** (a single file that works on every
modern Android phone — no Play Store needed).

**Permanent download link** (this is also what the in-app QR code points to):

```
https://github.com/charlesbilsky-netizen/poly-market-trader/releases/latest/download/PolyTrader.apk
```

## The 3 steps

1. **📸 Scan or tap** — point your phone camera at the QR code (or open the
   link above in your phone browser). The download starts right away.
   If Chrome says *"this file might be harmful"*, tap **Download anyway** —
   browsers say this for every APK outside the Play Store.
2. **⬇️ Open the file** — pull down your notifications and tap
   `PolyTrader.apk`. If Android asks, allow installs from your browser:
   **Settings → Allow from this source** (one-time toggle).
3. **✅ Install** — tap **Install**. If Google Play Protect pops up, tap
   **More details → Install anyway**. Done — open PolyTrader! 🎉

## Why these warnings appear (and why they're OK here)

Any app installed outside the Play Store ("sideloaded") triggers the same
generic warnings. This APK is built from the source code in this repository
by GitHub Actions and signed with the repository's sideload key — you can
audit every line that goes into it.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| "App not installed" / "package appears to be corrupted" | You have an older PolyTrader/PolyTrade Finder build signed with a different key. Uninstall the old app once, then install this one. Future updates install cleanly. |
| Download opens as text or fails | Use the link above directly in Chrome — don't preview the file in a chat app first. |
| Nothing happens when tapping the APK | Open your **Files** app → **Downloads** → tap `PolyTrader.apk`. |

## Updating

Install the new APK over the old one — no uninstall needed. Every build is
signed with the same committed sideload key, so Android accepts it as a
normal update. (This key is for direct installs only; a Play Store release
would use a fresh private key.)
