# 準時團 OnTime - 預防遲到智慧型手機應用程式

## 專案簡介
「準時團 OnTime」是一款專為預防朋友聚會遲到而設計的 Android 原生應用程式 (Kotlin + XML Layouts)。
App 透過起點與集合地點、交通工具 (機車/大眾運輸/步行/汽車/腳踏車) 選擇、串接 Google Maps / Places / Routes API 計算真實路程與出門時間，搭配背景提醒、未抵達加強提醒與約定帳本，達到預防遲到的目的。

---

## 🔑 Google Cloud Console 設定與金鑰指南 (API 金鑰安全設定)

為了在正式環境啟用真實路程與地圖服務，請在 **Google Cloud Console** 完成以下設定：

### 1. 啟用必要的 APIs
進入 Google Cloud Console 的 **APIs & Services -> Library**，搜尋並**啟用**以下服務：
- **Maps SDK for Android**（顯示地圖與座標標記）
- **Places API / Places SDK for Android**（輸入地址/店名自動完成搜尋 Autocomplete）
- **Routes API** (或 Directions API)（計算車程距離、時間與大眾運輸班次估算）

### 2. 建立與限制 API 金鑰 (API Key Restrictions)
1. 前往 **APIs & Services -> Credentials**，點擊 **Create Credentials -> API Key**。
2. **Application restrictions (應用程式限制)**：
   - 選擇 **Android apps**。
   - 點擊 **Add an item**，輸入您的 Package Name：`com.example.myapplicationproject`
   - 輸入您開發環境的 **SHA-1 憑證指紋** (可在 Android Studio 右側 Gradle -> `app` -> `Tasks` -> `android` -> `signingReport` 取得)。
3. **API restrictions (API 限制)**：
   - 選擇 **Restrict key**，僅勾選 Maps SDK for Android, Places API, Routes API。
4. **預算通知與配額 (Budget & Quota Alerts)**：
   - 前往 **Billing -> Budgets & alerts**，建立預算警報（如設定每月 $0 或 $10 觸發 Email 提醒），避免產生非預期費用。

### 3. 在專案中填入金鑰 (local.properties)
在專案根目錄的 `local.properties` 檔案中新增（勿提交至 Git）：
```properties
MAPS_API_KEY=AIzaSy...您的真實金鑰...
```
或在 `AndroidManifest.xml` 中填入：
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="您的真實金鑰" />
```
*備註：當未填入金鑰或網路連線異常時，App 會自動切換至「尚未設定 Google 路線服務 (手動備用模式)」，確保所有計算與提醒流程依然 100% 可測試執行，絕不閃退。*

---

## 🚀 多人連線模式與 Firebase Emulator

1. **進入 backend 資料夾**：
   ```bash
   cd firebase-backend
   ```
2. **啟動 Emulator Suite**：
   ```bash
   firebase emulators:start
   ```
   - **Auth**: `localhost:9099`
   - **Firestore**: `localhost:8080`
   - **Functions**: `localhost:5001` (含邀請碼後端驗證 `verifyInviteCode`)

---

## 📚 課堂技術對照表

| 講義/技術主題 | 對應程式檔案 | 實作說明 |
| :--- | :--- | :--- |
| **findViewById / 事件監聽** | [HomeFragment.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/HomeFragment.kt), [RouteSettingFragment.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/RouteSettingFragment.kt) | 實作點擊事件、RadioGroup 交通工具切換、TextWatcher 驗證 |
| **Fragment & ViewPager2** | [MainActivity.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/MainActivity.kt) | 承載四大主要分頁，保留單一 Activity 導頁架構 |
| **RecyclerView & Adapter** | [GroupsAdapter.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/GroupsAdapter.kt), [RecordsAdapter.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/RecordsAdapter.kt) | 展示團體與約定帳本清單，支援項目點擊與處置對話框 |
| **SQLiteOpenHelper** | [DatabaseHelper.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/db/DatabaseHelper.kt) | 本機資料庫初始化 (非同步執行，僅在資料庫為空時初始化，防止重複覆蓋) |
| **AlarmManager & Notification** | [ReminderManager.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/utils/ReminderManager.kt), [ReminderReceiver.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/receiver/ReminderReceiver.kt) | 排程提醒、未抵達加強提醒、通知按鈕 (我已抵達/延後/停止) 與短震動 |
| **ViewModel & Coroutines** | [RouteViewModel.kt](file:///D:/android%20studio/MyApplicationproject/app/src/main/java/com/example/myapplicationproject/viewmodels/RouteViewModel.kt) | 處理路線查詢，切換交通方式時透過 Job 取消舊的非同步過時請求 |
