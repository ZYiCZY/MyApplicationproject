AGENTS.md — 準時出門助手（給 Gemini agent 的專案規則）

1. 這個 App 是什麼（唯一的產品定義）
「準時出門助手」：幫常遲到的個人（大學生、帶小孩的家長）準時出門的 Android App。 它不是多人聚會 App、不是揪團 App、不是更多鬧鐘。 核心想法：用使用者自己實際被記錄到的出門耗時，倒推「起身時間」和「出門時間」， 並在對的時間只提醒一次對的動作。

場域：嘉義（嘉義大學蘭潭校區、嘉義市區）。預設地點、範例資料都用嘉義，不要用台北。

2. 功能範圍（MoSCoW，嚴格遵守）
Must（先做完這些，其他都不准碰）

M1 行程設定：目的地、抵達時間、交通方式（預設機車）。
M2 倒推時間：分三段顯示「準備｜路上｜最後一哩（停車、走到教室）」各幾分鐘， 並顯示「上次實際花了幾分鐘」。
M3 自動記錄實際耗時：用 Geofencing（住處、目的地兩個圍欄）自動記錄 「離開住處 → 抵達目的地」的時間，不需要使用者手動按。 累積 5 筆後，用個人中位數取代預設值（TimeCalculator.calculatePersonalizedSuggestion 已有）。
M4 「現在起身」出門信號：出門時間到時跳出全螢幕畫面（full-screen intent）， 文字是動作句，例如「現在起身，拿安全帽」，不是「8 點有課」。 需「滑動確認已起身」才能關閉；最多延後 1 次、5 分鐘。專屬通知頻道與鈴聲。
M5 前一晚必帶物品提醒：21:00 提醒，清單最多 3 項。
每個行程最多 2 次提醒（前一晚 + 出門時間）。

Should（Must 全部完成且測過才做）

S1 預計抵達通知：判斷會晚到時跳出卡片，按確認後用 Android 分享 Intent 傳到 LINE。 三種範本（公司／朋友／老師），訊息不得含「抱歉」「又」等自責字眼。 一律使用者按確認才傳，絕不自動傳。
S2 雨天加時間：前一晚查氣象預報，有雨就加個人雨天差值並提醒建議出門時間。

Could（之後再說）：孩子模式倒數動畫、正向「本週準時天數」（預設關閉）、家人共享。

Won't（禁止實作，看到就刪）

罰款、扣分、懲罰、申訴、遲到排行榜、「你這週遲到 N 次」這類負面統計。
群組、邀請碼、多人帳號、Firebase 同步、讓別人追蹤位置。
自動代發訊息。
更多鬧鐘、解題鬧鐘、重複轟炸式「加強提醒」。

3. 技術規則
語言：Kotlin + XML Layout。單一 Activity + Fragment。不要改成 Compose。
不要新增 Firebase、Room 以外的資料庫套件。資料存在本機 SQLite（現有 DatabaseHelper）。
不要升級或更改 gradle/libs.versions.toml 裡已有的版本、AGP、compileSdk，除非我明確要求。
本專案用 AGP 9（內建 Kotlin），不要在 app/build.gradle.kts 加 kotlin-android plugin。
新增套件時，先加到 libs.versions.toml，再在 app/build.gradle.kts 用 libs.xxx 引用。
權限：Android 13+ 必須在執行時請求 POST_NOTIFICATIONS； 精確鬧鐘要先檢查 canScheduleExactAlarms()； Geofencing 需要 ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION（分兩步請求）； 全螢幕信號需要 USE_FULL_SCREEN_INTENT。每個權限被拒絕時都要有說明畫面，不能閃退。
API 金鑰不得寫死在 AndroidManifest.xml，用 local.properties + manifestPlaceholders。
不准造假：沒有真的呼叫 API，就不能顯示「Google Routes API（即時路況）」。 沒有金鑰時顯示「估算值」並讓使用者手動輸入路上分鐘數。
不要在主執行緒存取資料庫或網路。
不要插入假的示範資料（demo data）到正式資料庫。

4. 工作方式（非常重要）
一次只做一個小任務。 每次修改不超過 3 個檔案；超過就先列計畫問我。
動手前先用 3–5 行說明：要改哪些檔案、為什麼。
改完後一定要編譯（Build > Make Project），有錯就自己修到通過再回報。
回報時列出：改了哪些檔案、怎麼在模擬器上測試這個功能。
不要刪除或改寫跟本次任務無關的程式碼；不要「順便重構」。
不確定需求時，回頭看本檔第 2 節；本檔沒寫的，先問我，不要自己發明功能。
不要更新 README，除非我要求。
