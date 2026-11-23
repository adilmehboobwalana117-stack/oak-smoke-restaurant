# Google Play Console Checklist — Oak & Smoke (`com.aistudio.oakandsmoke.hsplrk`)

## Why Testers Might Not See or Download the App (Quick Fix Checklist)

If testers get **"App not available"** or **"Item not found"** on Google Play, verify each of these 6 Google Play Console steps:

1. **Use the Tester Opt-In URL First (Not the Public Store URL):**
   - Public Play Store links return *Not Found* until Production is live.
   - Share the **Testing Opt-In URL** with your testers first:
     `https://play.google.com/apps/testing/com.aistudio.oakandsmoke.hsplrk`
   - Testers must open this link while signed into the **exact Google account** added to your tester list, tap **"Become a tester"**, and then tap **"Download it on Google Play"**.
2. **Add Tester Emails & Save Changes:**
   - In Play Console → **Testing → Internal testing** (or **Closed testing**) → **Testers** tab:
     - Check the checkbox next to your Email List (e.g., *Oak & Smoke Testers*).
     - Click **Save** at the bottom right.
3. **Confirm the Release Status is "Available to testers":**
   - In **Internal testing**, a release becomes available within minutes once rolled out (no review delay if the app draft is saved properly).
   - In **Closed testing**, you must click **Publishing overview → Send changes for review** and wait for Google Play to approve the Closed track release before testers can download.
4. **Enable Countries / Regions (Including Pakistan):**
   - In **Closed testing → Countries/regions** (and **Production → Countries/regions**), select **Pakistan** (or **All countries/regions**). If no countries are selected, testers in Pakistan cannot download the app.
5. **Complete Mandatory "App Content" Declarations:**
   - **Privacy Policy:** Host `privacy-policy.html` (e.g., GitHub Pages or Firebase Hosting) and paste the HTTPS URL under **Policy → App content → Privacy policy**.
   - **App Access:** Because Guest Customer Mode works without login, select *"All or some functionality is restricted"* and provide a note that Customers can browse the menu freely as Guest while Google Sign-In + Staff ID/Passcode unlocks Staff/Owner features.
   - **Ads:** Select *"No, my app does not contain ads"*.
   - **Content Rating (IARC):** Complete the short questionnaire (Reference / Food & Drink → Rated Everyone / PEGI 3).
   - **Data Safety:**
     - Collects data: **Yes** (Name, Email, Phone, Address, Approximate/Precise Foreground Location at COD checkout, App interactions/orders).
     - Encrypted in transit: **Yes** (HTTPS/TLS via Firebase).
     - Users can request data deletion: **Yes** (in-app *Profile → Delete My Account & Data* + email `adilmehboobwalana117@gmail.com`).
     - Shared with third parties for ads: **No**.
6. **Upload Play Store Graphics & Screenshots:**
   - Upload the 4 generated 9:16 phone screenshots from `app/src/main/res/drawable/`:
     - `screenshot_digital_menu_*.jpg`
     - `screenshot_cod_checkout_*.jpg`
     - `screenshot_table_booking_*.jpg`
     - `screenshot_owner_dashboard_*.jpg`
   - App Icon (512×512 PNG) and Feature Graphic (1024×500 PNG).

## Code-Level Play Console Compliance Verified
- `applicationId`: `com.aistudio.oakandsmoke.hsplrk`
- `minSdk`: 24 | `targetSdk`: 36 | `compileSdk`: 36
- Location hardware marked `android:required="false"` so tablets and Wi-Fi-only test devices are never excluded by Play Console device filtering.
- Least-privilege `<queries>` declared for Maps, HTTPS browser, Phone dialer, and Share intents (no `QUERY_ALL_PACKAGES`).
- Zero restricted storage, SMS, camera, background location, or `REQUEST_INSTALL_PACKAGES` permissions.
- Monochrome vector notification icon (`ic_notification_flame.xml`).
- In-app Privacy Policy viewer (accessible before and after sign-in) + In-app Account & Data Deletion flow.
