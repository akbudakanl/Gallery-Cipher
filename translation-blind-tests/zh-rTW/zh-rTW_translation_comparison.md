# Traditional Chinese (Taiwan, values-zh-rTW) Translation Audit, Revision & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-zh-rTW/strings.xml` was audited against `app/src/main/res/values/strings.xml` and `fossify-commons` (`values-zh-rTW`). 14 existing entries with mistranslations, Mainland Chinese phrasing, terminology mismatches, or formatting bugs were revised, and all 50 missing strings were translated into Taiwanese Traditional Chinese (`zh-rTW`) and added, bringing `values-zh-rTW` to full parity (343 `<string>` + 2 `<plurals>` = 345 resource entries; 363 total comparison rows including English `one`/`other` plural forms and 29 `fossify-commons` UI keys).
> **Key Terminology:** `Settings` → `設定` · `Default` → `預設` / `系統預設` · `Folder` → `資料夾` · `Album` → `相簿` · `Encrypted Vault` → `加密保險箱` · `Hidden` → `隱藏` · `Excluded` → `已排除` · `Password` → `密碼` · `Encrypt / Decrypt` → `加密 / 解密` · `Copy to` → `複製` (matches `fossify-commons`) · `Clipboard` → `剪貼簿` · `Search` → `搜尋` (inherited from `fossify-commons`)

## 1. Audit of Existing Translations & Newly Added Entries

### 1.1 Review of 14 Revised Existing Translations

|   # | Key (`name`)                          | Old `values-zh-rTW`                                                                          | New `values-zh-rTW`                                                                 | Reason / Notes                                                                                                            |
| --: | ------------------------------------- | -------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
|   1 | `share_resized`                       | 共享調整尺寸後的版本                                                                                   | 分享調整尺寸後的版本                                                                          | Replaced Mainland term `共享` with standard Taiwan UI term `分享`                                                             |
|   2 | `portraits`                           | 直向                                                                                           | 人像                                                                                  | **Fixed mistranslation:** `TYPE_PORTRAITS` filters Portrait mode photo folders (`IMG_*`), not vertical screen orientation |
|   3 | `other_aspect_ratio`                  | 其它                                                                                           | 其他                                                                                  | Standardized character usage with `other_folder` (`其他資料夾`)                                                                |
|   4 | `by_last_modified_daily`              | 最後修改 （按日）                                                                                    | 最後修改（按日）                                                                            | Removed stray space before `（按日）` for consistency with `最後修改（按月）`, `拍攝日期（按日）`, `拍攝日期（按月）`                                 |
|   5 | `compromise`                          | 妥協                                                                                           | 折衷                                                                                  | Improved Taiwan UI terminology for `PRIORITY_COMPROMISE` under `檔案讀取優先權`                                                  |
|   6 | `password_protect_excluded`           | 密碼保護以隱藏特定資料夾                                                                                 | 以密碼保護已排除資料夾的可見性                                                                     | **Fixed meaning:** previously confused `excluded` (`已排除`) with `hidden` (`隱藏`)                                            |
|   7 | `toggle_file_visibility`              | 檔案顯示                                                                                         | 切換檔案可見性                                                                             | Clarified toggle action to match `toggle_favorite` (`切換我的最愛`)                                                             |
|   8 | `faq_16_text_extra`                   | 或者，您也可以通過您的裝置設定授予此相簿存取所有檔案的權限，這將允許我們顯示隱藏項目並使檔案操作更為可靠。                                        | 或者，您也可以透過您的裝置設定授予此相簿存取所有檔案的權限，這將允許我們顯示隱藏項目並使檔案操作更為可靠。                               | Replaced Mainland term `通過` with Taiwanese `透過`                                                                           |
|   9 | `faq_18_text`                         | …嘗試在裝置設定中找到您的預設影片播放器，然後對其進行清除其預設值。…                                                          | …請試著在裝置設定中找到您的預設影片播放器，並清除其預設值。…                                                     | Fixed duplicated phrasing (`對其進行清除其預設值`)                                                                                  |
|  10 | `excluded_activity_placeholder`       | 排除資料夾功能僅在 Fossify 圖片庫中排除選定的資料夾及其所有子資料夾，供其他應用程式正常顯示。\n(raw newlines)\n如要同時在其他應用程式中隱藏，請使用隱藏功能。 | 排除資料夾功能僅會在 Fossify 圖庫中排除選定的資料夾及其所有子資料夾，在其他應用程式中仍會正常顯示。\n\n如要同時在其他應用程式中隱藏，請使用「隱藏」功能。 | Removed stray literal line breaks before `\n` and standardized app name to `Fossify 圖庫`                                   |
|  11 | `exclude_folder_description`          | 僅在 Fossify 圖片庫中排除選定的資料夾及其所有子資料夾。稍後可在設定中管理排除的資料夾。                                             | 僅在 Fossify 圖庫中排除選定的資料夾及其所有子資料夾。稍後可在設定中管理已排除的資料夾。                                    | Standardized app name to `Fossify 圖庫`                                                                                     |
|  12 | `faq_5_text`                          | …而隱藏則是系統級別的操作…這是通過在指定資料夾創建…像是相機、截圖和下載等資料夾。                                                   | …而隱藏則是系統層級的操作…這是透過在指定資料夾建立…像是相機、螢幕截圖和下載等資料夾。                                        | Replaced Mainland terms (`級別`, `通過`, `創建`) with Taiwanese usage (`層級`, `透過`, `建立`, `螢幕截圖`) and standardized `Fossify 圖庫`  |
|  13 | `overwrite_original`                  | 覆寫原始內容                                                                                       | 覆寫原始檔案                                                                              | Clarified that the editor overwrites the original file                                                                    |
|  14 | `on_video_tap` / `open_in_app_player` | 輕點影片 / 開啟應用程式內播放程式                                                                           | 輕觸影片 / 開啟應用程式內播放器                                                                   | Replaced Mainland `輕點` with `輕觸` (consistent with `輕觸螢幕邊緣`) and unified `播放器` with `open_system_default_player`           |

*(Note: `on_video_tap` and `open_in_app_player` are listed together above for conciseness, totaling 15 adjusted string entries across 14 audit items.)*



### 1.2 Newly Added Fork-Only Strings (66 Keys)

These 66 strings were introduced by this fork (Gallery-Cipher) and do not exist in the original Fossify Gallery:

| # | Key (`name`) | English (`values/strings.xml`) | Taiwanese (`values-zh-rTW/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `video_double_tap_duration` | Double-tap seek duration | 點兩下跳轉時間 | Video settings: double-tap forward/rewind seek interval; uses `點兩下` matching `allow_one_to_one_zoom` |
| 2 | `video_seek_duration_seconds` | %d seconds | %d 秒 | Used with durations 3, 5, 10, 15 via `String.format`; includes Taiwan CJK-number spacing |
| 3 | `create_virtual_album` | Create virtual album | 建立虛擬相簿 | Virtual albums section action |
| 4 | `new_virtual_album` | New album… | 新增相簿… | Action label for creating a new virtual album |
| 5 | `rename_virtual_album` | Rename album | 重新命名相簿 | Action label to rename a virtual album |
| 6 | `album_name` | Album name | 相簿名稱 | Input hint / label for album name |
| 7 | `virtual_folder_name` | Virtual folder name | 虛擬資料夾名稱 | Input hint / label for virtual folder |
| 8 | `virtual_album_name_empty` | The album name cannot be empty | 相簿名稱不可為空 | Validation error when album name is empty |
| 9 | `virtual_album_exists` | An album with this name already exists | 已存在相同名稱的相簿 | Validation error when album name already exists |
| 10 | `add_to_virtual_album` | Add to album | 加入相簿 | Menu action to add items to an album |
| 11 | `remove_from_virtual_album` | Remove from album | 從相簿中移除 | Menu action to remove items from an album |
| 12 | `added_to_virtual_album` | Added to \"%s\" | 已加入「%s」 | Toast message using Taiwanese corner brackets `「%s」` |
| 13 | `already_in_virtual_album` | Already in \"%s\" | 已在「%s」中 | Toast message using Taiwanese corner brackets `「%s」` |
| 14 | `removed_from_virtual_album` | Removed from the album | 已從相簿中移除 | Toast message after removing items from an album |
| 15 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | 確定要刪除相簿 %s 嗎？僅會移除相簿本身，您的相片和影片仍會保留在原處。 | No extra quotes around `%s` (code passes quoted name or item count) |
| 16 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | 此相簿為空。請在任意資料夾中選取項目，並使用「複製」。 | References `fossify-commons` `copy_to` (`複製`) |
| 17 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | 此操作不適用於虛擬相簿 | Toast message when action is unsupported on virtual albums |
| 18 | `alphanumeric_password` | Password | 密碼 | Matches `fossify-commons` `password` (`密碼`) |
| 19 | `repeat_password` | Repeat password | 再次輸入密碼 | Matches `fossify-commons` `repeat_pin` (`再次輸入PIN碼`) |
| 20 | `wrong_password` | Wrong password | 密碼錯誤 | Matches `fossify-commons` `wrong_pin` (`PIN碼錯誤`) |
| 21 | `encrypting_files` | Encrypting files… | 正在加密檔案… | Progress message using `正在…` style |
| 22 | `encrypting_folder` | Encrypting folder… | 正在加密資料夾… | Progress message using `正在…` style |
| 23 | `folder_encrypted_successfully` | Folder encrypted successfully | 資料夾加密成功 | Success message matching `圖片尺寸調整成功` |
| 24 | `decrypting_folder` | Decrypting folder… | 正在解密資料夾… | Progress message using `正在…` style |
| 25 | `folder_decrypted_successfully` | Folder decrypted successfully | 資料夾解密成功 | Success message matching `圖片尺寸調整成功` |
| 26 | `decrypting_files` | Decrypting files… | 正在解密檔案… | Progress message using `正在…` style |
| 27 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | 請將手指放在指紋感應器上 | Matches `fossify-commons` `place_finger` terminology (`指紋感應器`) |
| 28 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | 請至少連接 4 個點 | Pattern lock validation prompt |
| 29 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | 您要存取現有的 Cipher_Secrets 資料夾嗎？ | Keeps literal folder name `Cipher_Secrets` |
| 30 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | 現有的 Cipher_Secrets 資料夾將會被刪除並建立新的資料夾。確定要繼續嗎？ | Keeps literal folder name `Cipher_Secrets` |
| 31 | `confirm_checkbox_label` | I confirm | 我確認 | Checkbox label, suffixed with countdown `(10)` in dialog |
| 32 | `secret_vault` | Encrypted Vault | 加密保險箱 | Vault feature title |
| 33 | `secret_vault_empty` | Encrypted Vault is empty | 加密保險箱為空 | Empty state placeholder for Encrypted Vault |
| 34 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | 確定要解密隱藏的檔案嗎？ | Confirmation prompt for decrypting hidden file(s) |
| 35 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | 基於安全考量，保險箱解鎖期間已停用複製到剪貼簿功能。 | Security notice using `剪貼簿` matching `fossify-commons` |
| 36 | `on_vault_exit` | On leaving encrypted section | 離開加密區域時 | Settings label for behavior when leaving encrypted section |
| 37 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | 繼續顯示加密相簿 | Radio option |
| 38 | `on_vault_exit_hide` | Hide encrypted albums | 隱藏加密相簿 | Radio option |
| 39 | `on_vault_exit_ask_every_time` | Always ask | 每次詢問 | Radio option |
| 40 | `vault_exit_dialog_title` | Exiting Encrypted Section | 正在離開加密區域 | Dialog title |
| 41 | `vault_exit_question_single` | Show encrypted album together with other albums? | 是否將加密相簿與其他相簿一起顯示？ | Dialog question (single album) |
| 42 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? | 是否將加密相簿與其他相簿一起顯示？ | Dialog question (multiple albums) |
| 43 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | 如需更多選項，請前往「設定」&gt;「離開加密區域時」 | References `設定` and `離開加密區域時` |
| 44 | `open_system_language_settings` | Open system language settings | 開啟系統語言設定 | Neutral dialog button in language picker |
| 45 | `file_encrypted_successfully` | File hidden successfully | 已成功隱藏檔案 | Success toast after encrypting/hiding a single file |
| 46 | `files_encrypted_successfully` | Files hidden successfully | 已成功隱藏檔案 | Success toast after encrypting/hiding multiple files |
| 47 | `file_decrypted_successfully` | File unhidden successfully | 已成功解除隱藏檔案 | Success toast after decrypting/unhiding a single file |
| 48 | `files_decrypted_successfully` | Files unhidden successfully | 已成功解除隱藏檔案 | Success toast after decrypting/unhiding multiple files |
| 49 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | 使用生物辨識存取隱藏項目 | Settings switch, setup prompt title, and vault security dialog button |
| 50 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | 請先啟用隱藏項目的密碼保護 | Toast shown when enabling biometrics before hidden-item password protection |
| 51 | `biometric_unlock_vault` | Unlock encrypted vault | 解鎖加密保險箱 | BiometricPrompt title when unlocking the encrypted vault |
| 52 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | 驗證您的生物辨識身分以存取隱藏項目 | BiometricPrompt subtitle when unlocking hidden items |
| 53 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | 確認您的生物辨識身分以啟用保險箱的生物辨識存取 | BiometricPrompt subtitle when enabling biometric vault access |
| 54 | `use_password_or_pin` | Use PIN / Password | 使用 PIN 碼 / 密碼 | BiometricPrompt negative button to fall back to PIN/password |
| 55 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | 生物辨識憑證已變更，請輸入您的 PIN 碼或密碼。 | Toast shown when biometric credentials changed and Keystore key invalidated |
| 56 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | 此裝置上找不到生物辨識硬體或已註冊的憑證 | Toast shown when no biometric hardware or enrolled credentials exist |
| 57 | `default_hidden_album` | Default Hidden Album | 預設隱藏相簿 | Settings / picker label for default hidden album |
| 58 | `select_hidden_album` | Select Hidden Album | 選擇隱藏相簿 | Dialog title for choosing target hidden album |
| 59 | `create_new_hidden_album` | Create New Hidden Album | 建立新隱藏相簿 | Action label to create a new hidden album |
| 60 | `new_hidden_album` | New hidden album | 新隱藏相簿 | Input hint / default label for new hidden album |
| 61 | `hidden_album_name_empty` | Album name cannot be empty | 相簿名稱不能為空 | Validation error when hidden album name is blank |
| 62 | `file_hidden_to_album` | File hidden to \"%s\" | 檔案已隱藏至「%s」 | Success toast after hiding a single file into album `%s` |
| 63 | `files_hidden_to_album` | %1$d files hidden to \"%2$s\" | %1$d 個檔案已隱藏至「%2$s」 | Success toast after hiding `%1$d` files into album `%2$s` |
| 64 | `pull_down_to_open_vault` | Keep pulling down for Encrypted Vault | 繼續向下拉以開啟加密保險箱 | Vault pull gesture hint shown while the pull is below the open threshold |
| 65 | `keep_holding_to_open_vault` | Keep holding to open Encrypted Vault | 按住不放以開啟加密保險箱 | Vault pull gesture hint shown once the pull passes the threshold, before the 2.5 s hold completes |
| 66 | `release_to_open_vault` | Release to open Encrypted Vault | 放開以開啟加密保險箱 | Vault pull gesture hint shown after holding the pull for 2.5 s; releasing opens the vault |

### 1.3 Newly Added Translations for Strings Already Present in the Original Fossify Gallery (3 Keys)

These 3 strings already exist in the original Fossify Gallery (upstream); only the translations below were newly added in this fork:

| # | Key (`name`) | English (`values/strings.xml`) | Taiwanese (`values-zh-rTW/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `enter_password` | Enter password | 輸入密碼 | Matches `fossify-commons` `enter_password` (`輸入密碼`) |
| 2 | `system_default` | System default | 系統預設 | Matches `fossify-commons` `system_default` (`系統預設`) |
| 3 | `language` | Language | 語言 | Matches `fossify-commons` `language` (`語言`) |

### 1.4 `fossify-commons` Inherited UI Strings (29 Keys)

All 29 shared UI keys below are already localized in `org.fossify:commons:7.1.3` (`res/values-zh-rTW/values-zh-rTW.xml`); no local override in `values-zh-rTW/strings.xml` was required (the last 13 keys were verified as fully translated in this revision):

| # | Key (`name`) | English (`fossify-commons`) | Taiwanese (`fossify-commons`) | Source / Notes |
|---:|---|---|---|---|
| 1 | `change_date_and_time_format` | Change date and time format | 更改日期和時間格式 | Inherited from `fossify-commons` (Settings) |
| 2 | `password_protect_file_deletion` | Password protect file deletion and moving | 保護檔案刪除和移動 | Inherited from `fossify-commons` (Settings) |
| 3 | `keep_last_modified` | Keep old last-modified value at file operations | 變更檔案後仍保留原有的最後修改日期 | Inherited from `fossify-commons` (Settings) |
| 4 | `clear_cache` | Clear cache | 清除快取 | Inherited from `fossify-commons` (Settings) |
| 5 | `export_settings` | Export settings | 匯出設定 | Inherited from `fossify-commons` (Settings) |
| 6 | `import_settings` | Import settings | 匯入設定 | Inherited from `fossify-commons` (Settings) |
| 7 | `temporarily_show_excluded` | Temporarily show excluded | 暫停排除項目 | Inherited from `fossify-commons` (Options menu) |
| 8 | `change_view_type` | Change view type | 顯示方式 | Inherited from `fossify-commons` (Options menu) |
| 9 | `contributors` | Contributors | 貢獻者 | Inherited from `fossify-commons` (About) |
| 10 | `known_issues` | Known issues | 已知問題 | Inherited from `fossify-commons` (About) |
| 11 | `donate_to_fossify` | Donate to Fossify | 贊助 Fossify | Inherited from `fossify-commons` (About) |
| 12 | `privacy_policy` | Privacy policy | 隱私政策 | Inherited from `fossify-commons` (About) |
| 13 | `create_shortcut` | Create shortcut | 建立捷徑 | Inherited from `fossify-commons` (CAB / ViewPager menu) |
| 14 | `print` | Print | 列印 | Inherited from `fossify-commons` (ViewPager menu) |
| 15 | `search_files` | Search files | 搜尋檔案 | Inherited from `fossify-commons` (Search bar hint) |
| 16 | `search_folders` | Search folders | 搜尋資料夾 | Inherited from `fossify-commons` (Search bar hint) |
| 17 | `manage_excluded_folders` | Manage excluded folders | 管理排除的資料夾 | Inherited from `fossify-commons` (Settings); verified translated in this revision |
| 18 | `exclude` | Exclude | 排除 | Inherited from `fossify-commons` (Album selection / long-press menu); verified translated in this revision |
| 19 | `exclude_folder` | Exclude folder | 排除資料夾 | Inherited from `fossify-commons` (Options menu); verified translated in this revision |
| 20 | `lock_folder` | Lock folder | 上鎖資料夾 | Inherited from `fossify-commons` (Album selection / long-press menu); verified translated in this revision |
| 21 | `change_cover_image` | Change cover image | 更換封面圖片 | Inherited from `fossify-commons` (Album selection / long-press menu); verified translated in this revision |
| 22 | `select_photo` | Select photo | 選擇相片 | Inherited from `fossify-commons` (Album selection, "Change cover image" dialog); verified translated in this revision |
| 23 | `use_default` | Use default | 使用預設 | Inherited from `fossify-commons` (Album selection, "Change cover image" dialog); verified translated in this revision |
| 24 | `column_count` | Column count | 欄數 | Inherited from `fossify-commons` (Main screen and in-album Options menu); verified translated in this revision |
| 25 | `open_the_recycle_bin` | Open the recycle bin | 開啟回收桶 | Inherited from `fossify-commons` (In-album Options menu); verified translated in this revision |
| 26 | `grid` | Grid | 格狀 | Inherited from `fossify-commons` (In-album Options menu, view type); verified translated in this revision |
| 27 | `list` | List | 列表 | Inherited from `fossify-commons` (In-album Options menu, view type); verified translated in this revision |
| 28 | `resize` | Resize | 調整尺寸 | Inherited from `fossify-commons` (Selection CAB menu); verified translated in this revision |
| 29 | `version_placeholder` | Version %s | 版本 %s | Inherited from `fossify-commons` (About screen version text); verified translated in this revision |

## 2. Full 1-to-1 Comparison: English vs. Traditional Chinese (Taiwan)

| # | Key (`name`) | English (`values/strings.xml`) | Taiwanese (`values-zh-rTW/strings.xml`) |
|---:|---|---|---|
| 1 | `app_launcher_name` | Gallery | 圖庫 |
| 2 | `edit` | Edit | 編輯 |
| 3 | `open_camera` | Open camera | 開啟相機 |
| 4 | `hidden` | (hidden) | （已隱藏） |
| 5 | `excluded` | (excluded) | （已排除） |
| 6 | `pin_folder` | Pin folder | 釘選資料夾 |
| 7 | `unpin_folder` | Unpin folder | 取消釘選資料夾 |
| 8 | `pin_to_the_top` | Pin to the top | 釘選至頂部 |
| 9 | `show_all` | Show all folders content | 顯示全部資料夾內容 |
| 10 | `all_folders` | All folders | 全部資料夾 |
| 11 | `folder_view` | Switch to folder view | 切換為資料夾檢視 |
| 12 | `other_folder` | Other folder | 其他資料夾 |
| 13 | `show_on_map` | Show on map | 在地圖上顯示 |
| 14 | `unknown_location` | Unknown location | 未知的位置 |
| 15 | `volume` | Volume | 音量 |
| 16 | `brightness` | Brightness | 亮度 |
| 17 | `lock_orientation` | Lock orientation | 鎖定方向 |
| 18 | `unlock_orientation` | Unlock orientation | 解除鎖定方向 |
| 19 | `change_orientation` | Change orientation | 改變方向 |
| 20 | `force_portrait` | Force portrait | 強制直向 |
| 21 | `force_landscape` | Force landscape | 強制橫向 |
| 22 | `force_landscape_reverse` | Force landscape (reverse) | 強制橫向（反轉） |
| 23 | `use_default_orientation` | Use default orientation | 使用預設方向 |
| 24 | `fix_date_taken` | Fix Date Taken value | 修復拍攝日期 |
| 25 | `fixing` | Fixing… | 正在修復… |
| 26 | `dates_fixed_successfully` | Dates fixed successfully | 日期修復完成 |
| 27 | `no_date_takens_found` | No Date Taken values have been found | 未發現拍攝日期 |
| 28 | `share_resized` | Share a resized version | 分享調整尺寸後的版本 |
| 29 | `switch_to_file_search` | Switch to file search across all visible folders | 搜尋所有可見資料夾 |
| 30 | `set_as_default_folder` | Set as default folder | 設為預設資料夾 |
| 31 | `unset_as_default_folder` | Unset as default folder | 取消設為預設資料夾 |
| 32 | `reorder_by_dragging` | Reorder folders by dragging | 拖曳以重新排序資料夾 |
| 33 | `reorder_by_dragging_pro` | Reorder folders by dragging (Pro) | 拖曳以重新排序資料夾（專業版） |
| 34 | `restore_to_path` | Restoring to \'%s\' | 還原至「%s」 |
| 35 | `full_storage_permission_required` | Fossify Gallery needs full access to display all your photos and videos. Go to Settings &gt; Permissions &gt; Photos and videos &gt; Allow all. | Fossify 圖庫需要完整存取權限才能顯示您所有的照片與影片。請到「設定」→「隱私權」→「應用程式權限」→「相片和影片」→「一律全部允許」。 |
| 36 | `filter_media` | Filter media | 篩選媒體檔案 |
| 37 | `images` | Images | 圖片 |
| 38 | `videos` | Videos | 影片 |
| 39 | `gifs` | GIFs | GIF |
| 40 | `raw_images` | RAW images | RAW 圖檔 |
| 41 | `svgs` | SVGs | SVG |
| 42 | `portraits` | Portraits | 人像 |
| 43 | `no_media_with_filters` | No media files have been found with the selected filters. | 未發現符合篩選條件的媒體檔案。 |
| 44 | `change_filters_underlined` | <u>Change filters</u> | <u>更改篩選條件</u> |
| 45 | `hide_folder_description` | This function hides the folder by adding a \'.nomedia\' file into it, it will hide all subfolders too. You can see them by toggling the \'Show hidden items\' option in Settings. Continue? | 啟用此功能將在選定資料夾中建立「.nomedia」檔案，該資料夾及其所有子資料夾將被隱藏。如要檢視隱藏的資料夾，可在設定中啟用「顯示隱藏項目」。是否繼續？ |
| 46 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | 僅在 Fossify 圖庫中排除選定的資料夾及其所有子資料夾。稍後可在設定中管理已排除的資料夾。 |
| 47 | `exclude_folder_parent` | Exclude a parent instead? | 是否排除上層資料夾？ |
| 48 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | 排除資料夾功能僅會在 Fossify 圖庫中排除選定的資料夾及其所有子資料夾，在其他應用程式中仍會正常顯示。\n\n如要同時在其他應用程式中隱藏，請使用「隱藏」功能。 |
| 49 | `hidden_folders` | Hidden folders | 隱藏資料夾 |
| 50 | `manage_hidden_folders` | Manage hidden folders | 管理隱藏的資料夾 |
| 51 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | 未發現以「.nomedia」檔案來隱藏的資料夾。 |
| 52 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | 請務必授予此應用程式「所有檔案存取權」以檢視隱藏的檔案。 |
| 53 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | 如果資料夾或其上層資料夾的名稱開頭為「.」，該資料夾將會被隱藏且無法直接取消隱藏。您需要重新命名資料夾以移除前方的「.」。 |
| 54 | `include_folders` | Included folders | 包含資料夾 |
| 55 | `manage_included_folders` | Manage included folders | 管理包含的資料夾 |
| 56 | `add_folder` | Add folder | 新增資料夾 |
| 57 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | 如要新增包含媒體檔案但未被應用程式辨識的資料夾，可在此處手動新增。 \n \n新增項目不會排除其他資料夾。 |
| 58 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | 未發現媒體檔案。您可以手動加入包含媒體檔案的資料夾來解決。 |
| 59 | `resize_and_save` | Resize selection and save | 調整選取項目尺寸並儲存 |
| 60 | `width` | Width | 寬度 |
| 61 | `height` | Height | 高度 |
| 62 | `keep_aspect_ratio` | Keep aspect ratio | 維持長寬比 |
| 63 | `invalid_values` | Please enter a valid resolution | 請輸入有效的解析度 |
| 64 | `resize_multiple_images` | Resize multiple images | 批次調整圖片尺寸 |
| 65 | `resize_factor` | Resize factor | 調整比例 |
| 66 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | 調整圖片尺寸至指定百分比，數值範圍為 10 至 90。 |
| 67 | `resize_factor_error` | Enter a number between 10 and 90 | 請輸入 10 到 90 之間的數值 |
| 68 | `images_resized_successfully` | Images resized successfully | 圖片尺寸調整成功 |
| 69 | `editor` | Editor | 編輯工具 |
| 70 | `basic_editor` | Basic Editor | 基礎編輯工具 |
| 71 | `advanced_editor` | Advanced Editor | 進階編輯工具 |
| 72 | `rotate` | Rotate | 旋轉 |
| 73 | `invalid_image_path` | Invalid image path | 無效的圖片路徑 |
| 74 | `invalid_video_path` | Invalid video path | 無效的影片路徑 |
| 75 | `image_editing_failed` | Image editing failed | 圖片編輯失敗 |
| 76 | `video_editing_failed` | Video editing failed | 影片編輯失敗 |
| 77 | `image_editing_cancelled` | Image editing cancelled | 圖片編輯取消 |
| 78 | `video_editing_cancelled` | Video editing cancelled | 影片編輯取消 |
| 79 | `file_edited_successfully` | File edited successfully | 檔案編輯成功 |
| 80 | `image_edited_successfully` | Image edited successfully | 圖片編輯成功 |
| 81 | `video_edited_successfully` | Video edited successfully | 影片編輯成功 |
| 82 | `edit_image_with` | Edit image with: | 編輯圖片: |
| 83 | `edit_video_with` | Edit video with: | 編輯影片: |
| 84 | `no_image_editor_found` | No image editor found | 找不到圖片編輯工具 |
| 85 | `no_video_editor_found` | No video editor found | 找不到影片編輯工具 |
| 86 | `unknown_file_location` | Unknown file location | 未知的檔案位置 |
| 87 | `overwrite_original` | Overwrite original | 覆寫原始檔案 |
| 88 | `error_saving_file` | Could not overwrite the source file | 無法覆蓋原始檔案 |
| 89 | `rotate_left` | Rotate left | 向左轉 |
| 90 | `rotate_right` | Rotate right | 向右轉 |
| 91 | `rotate_one_eighty` | Rotate by 180º | 旋轉 180º |
| 92 | `transform` | Transform | 變形 |
| 93 | `crop` | Crop | 裁切 |
| 94 | `draw` | Draw | 繪畫 |
| 95 | `flip` | Flip | 翻轉 |
| 96 | `flip_horizontally` | Flip horizontally | 水平翻轉 |
| 97 | `flip_vertically` | Flip vertically | 垂直翻轉 |
| 98 | `free_aspect_ratio` | Free | 自由 |
| 99 | `other_aspect_ratio` | Other | 其他 |
| 100 | `wallpaper` | Wallpaper | 桌布 |
| 101 | `set_as_wallpaper` | Set as Wallpaper | 設為桌布 |
| 102 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | 設為桌布失敗 |
| 103 | `set_as_wallpaper_with` | Set as wallpaper with: | 用其他程式設為桌布: |
| 104 | `setting_wallpaper` | Setting wallpaper… | 正在設定桌布… |
| 105 | `wallpaper_set_successfully` | Wallpaper set successfully | 成功設為桌布 |
| 106 | `portrait_aspect_ratio` | Portrait aspect ratio | 直向長寬比 |
| 107 | `landscape_aspect_ratio` | Landscape aspect ratio | 橫向長寬比 |
| 108 | `home_screen` | Home screen | 主畫面 |
| 109 | `lock_screen` | Lock screen | 鎖定畫面 |
| 110 | `home_and_lock_screen` | Home and lock screen | 主畫面及鎖定畫面 |
| 111 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | 允許變更長寬比 |
| 112 | `slideshow` | Slideshow | 投影片 |
| 113 | `interval` | Interval | 間隔 |
| 114 | `include_photos` | Include photos | 包含照片 |
| 115 | `include_videos` | Include videos | 包含影片 |
| 116 | `include_gifs` | Include GIFs | 包含 GIF |
| 117 | `random_order` | Random order | 隨機順序 |
| 118 | `move_backwards` | Move backwards | 反向播放 |
| 119 | `loop_slideshow` | Loop slideshow | 投影片循環 |
| 120 | `animation` | Animation | 動畫 |
| 121 | `no_animation` | None | 無 |
| 122 | `fade` | Fade | 漸變 |
| 123 | `slide` | Slide | 滑動 |
| 124 | `slideshow_ended` | The slideshow ended | 投影片結束 |
| 125 | `no_media_for_slideshow` | No media for the slideshow have been found | 找不到投影片的媒體檔案 |
| 126 | `group_direct_subfolders` | Group direct subfolders | 分組子資料夾 |
| 127 | `group_by` | Group by | 分組方式 |
| 128 | `do_not_group_files` | Do not group files | 關閉分組 |
| 129 | `by_folder` | Folder | 資料夾 |
| 130 | `by_last_modified` | Last modified | 最後修改 |
| 131 | `by_last_modified_daily` | Last modified (daily) | 最後修改（按日） |
| 132 | `by_last_modified_monthly` | Last modified (monthly) | 最後修改（按月） |
| 133 | `by_date_taken` | Date taken | 拍攝日期 |
| 134 | `by_date_taken_daily` | Date taken (daily) | 拍攝日期（按日） |
| 135 | `by_date_taken_monthly` | Date taken (monthly) | 拍攝日期（按月） |
| 136 | `by_file_type` | File type | 檔案類型 |
| 137 | `by_extension` | Extension | 副檔名 |
| 138 | `show_file_count_section_header` | Show file count at section headers | 於分段標題顯示檔案數量 |
| 139 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | 排序方式與分組方式為獨立設定 |
| 140 | `folder_on_widget` | Folder shown on the widget: | 在小工具顯示資料夾: |
| 141 | `show_folder_name` | Show folder name | 顯示資料夾名稱 |
| 142 | `autoplay_videos` | Play videos automatically | 自動播放影片 |
| 143 | `remember_last_video_position` | Remember last video playback position | 記住影片上次播放位置 |
| 144 | `loop_videos` | Loop videos | 循環播放影片 |
| 145 | `video_double_tap_duration` | Double-tap seek duration | 點兩下跳轉時間 |
| 146 | `video_seek_duration_seconds` | %d seconds | %d 秒 |
| 147 | `animate_gifs` | Animate GIFs at thumbnails | 在縮圖顯示 GIF 動畫 |
| 148 | `max_brightness` | Max brightness when viewing fullscreen media | 檢視全螢幕媒體時將亮度設定為最高亮度 |
| 149 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | 以 HDR 模式顯示 Ultra HDR 照片 |
| 150 | `crop_thumbnails` | Crop thumbnails into squares | 裁剪縮圖為正方形 |
| 151 | `show_thumbnail_video_duration` | Show video durations | 顯示影片長度 |
| 152 | `screen_rotation_by` | Rotate fullscreen media by | 全螢幕時旋轉方向 |
| 153 | `screen_rotation_system_setting` | System setting | 系統設定方向 |
| 154 | `screen_rotation_device_rotation` | Device rotation | 裝置實際方向 |
| 155 | `screen_rotation_aspect_ratio` | Aspect ratio | 圖片長寬比 |
| 156 | `black_background_at_fullscreen` | Black background at fullscreen media | 全螢幕使用黑色背景 |
| 157 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | 使用橫向捲動 |
| 158 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | 全螢幕時自動隱藏系統介面 |
| 159 | `delete_empty_folders` | Delete empty folders after deleting their content | 資料夾清空後自動刪除 |
| 160 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | 檢視全螢幕照片時維持螢幕開啟 |
| 161 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | 啟用相片亮度控制手勢 |
| 162 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | 啟用影片音量與亮度控制手勢 |
| 163 | `playback_speed` | Playback speed | 播放速度 |
| 164 | `show_media_count` | Show folder media count on the main view | 在主畫面顯示資料夾內的媒體檔案數量 |
| 165 | `show_extended_details` | Show extended details over fullscreen media | 全螢幕時顯示詳細資訊 |
| 166 | `manage_extended_details` | Manage extended details | 管理詳細資訊 |
| 167 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | 全螢幕時使用單指縮放 |
| 168 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | 輕觸螢幕邊緣來快速切換媒體檔案 |
| 169 | `allow_deep_zooming_images` | Allow deep zooming images | 啟用強制放大圖片 |
| 170 | `hide_extended_details` | Hide extended details when status bar is hidden | 狀態欄隱藏時，同時隱藏詳細資訊 |
| 171 | `show_at_bottom` | Show some action buttons at the bottom of the screen | 在螢幕底部顯示一些操作按鈕 |
| 172 | `show_recycle_bin` | Show the recycle bin on the folders screen | 在資料夾畫面顯示回收桶 |
| 173 | `deep_zoomable_images` | Deep zoomable images | 可強制放大的圖片 |
| 174 | `show_highest_quality` | Show images in the highest possible quality | 以最高品質顯示圖片 |
| 175 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | 回收桶顯示在主畫面最後 |
| 176 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | 以下滑手勢關閉全螢幕檢視 |
| 177 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | 點兩下螢幕來 1:1 縮放 |
| 178 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | 透過橫向手勢在獨立螢幕上播放影片 |
| 179 | `on_video_tap` | On video tap | 輕觸影片 |
| 180 | `open_in_app_player` | Open in-app player | 開啟應用程式內播放器 |
| 181 | `open_system_default_player` | Open default player app | 開啟預設播放器應用程式 |
| 182 | `show_notch` | Show a notch if available | 允許螢幕凹口遮擋畫面 |
| 183 | `allow_rotating_gestures` | Allow rotating images with gestures | 使用手勢來旋轉圖片 |
| 184 | `file_loading_priority` | File loading priority | 檔案讀取優先權 |
| 185 | `speed` | Speed | 速度 |
| 186 | `compromise` | Compromise | 折衷 |
| 187 | `avoid_showing_invalid_files` | Avoid showing invalid files | 避免顯示無效的檔案 |
| 188 | `show_image_file_types` | Show image file types | 顯示圖片檔案類型 |
| 189 | `allow_zooming_videos` | Allow zooming videos with double tapping them | 點兩下螢幕來縮放影片 |
| 190 | `folder_thumbnail_style` | Folder thumbnail style | 資料夾縮圖樣式 |
| 191 | `file_thumbnail_style` | File thumbnail style | 檔案縮圖樣式 |
| 192 | `mark_favorite_items` | Mark favorite items | 標記我的最愛 |
| 193 | `thumbnail_spacing` | Thumbnail spacing | 縮圖間距 |
| 194 | `show_file_count_line` | Show file count on a separate line | 獨立一行顯示檔案數量 |
| 195 | `show_file_count_brackets` | Show file count in brackets | 在括號內顯示檔案數量 |
| 196 | `show_file_count_none` | Do not show file count | 不顯示檔案數量 |
| 197 | `limit_folder_title` | Limit long folder titles to 1 line | 資料夾名稱最長只顯示一行 |
| 198 | `square` | Square | 方形 |
| 199 | `rounded_corners` | Rounded corners | 圓角 |
| 200 | `export_favorite_paths` | Export favorites | 匯出我的最愛 |
| 201 | `import_favorite_paths` | Import favorites | 匯入我的最愛 |
| 202 | `paths_imported_successfully` | Paths imported successfully | 路徑匯入成功 |
| 203 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | 為了確保所有檔案操作都能可靠地運作，請將此應用程式設為裝置設定中的媒體管理應用程式。 |
| 204 | `password_protect_excluded` | Password protect excluded folder visibility | 以密碼保護已排除資料夾的可見性 |
| 205 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | 出錯了，請至裝置的設定 - 應用程式（和通知） - 特殊應用程式存取權 - 媒體管理應用程式，允許本應用程式管理媒體。 |
| 206 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | 若重新導向未順利運作，請至裝置的設定 - 應用程式（和通知） - 特殊應用程式存取權 - 媒體管理應用程式，允許本應用程式管理媒體。 |
| 207 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | 如果您不想這麼做，也可以進入您的裝置設定 - 應用程式 - 特殊應用程式存取 - 媒體管理應用程式，並允許此應用程式管理媒體。 |
| 208 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | 或者，您只能允許存取媒體檔案。但在這種情況下，您將無法處理隱藏檔案。 |
| 209 | `media_only` | Media only | 僅媒體 |
| 210 | `all_files` | All files | 所有檔案 |
| 211 | `main_screen_view_on_launch` | Main screen view on launch | 啟動時的主畫面檢視 |
| 212 | `show_folders` | Show folders | 顯示資料夾 |
| 213 | `show_files` | Show files | 顯示檔案 |
| 214 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | 顯示一個選單按鈕，快速切換「顯示所有資料夾內容」 |
| 215 | `thumbnails` | Thumbnails | 縮圖 |
| 216 | `fullscreen_media` | Fullscreen media | 全螢幕媒體檔案 |
| 217 | `extended_details` | Extended details | 詳細資訊 |
| 218 | `bottom_actions` | Bottom actions | 底部操作 |
| 219 | `manage_bottom_actions` | Manage visible bottom actions | 管理要顯示的底部操作 |
| 220 | `toggle_favorite` | Toggle favorite | 切換我的最愛 |
| 221 | `toggle_file_visibility` | Toggle file visibility | 切換檔案可見性 |
| 222 | `failed_to_load_media` | Failed to load media. | 載入媒體失敗。 |
| 223 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | 檔案格式錯誤或損毀。 |
| 224 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | 媒體超出裝置的播放能力。 |
| 225 | `unsupported_format` | Unsupported format. | 不支援的格式。 |
| 226 | `video_no_sound` | This video has no audio | 此影片沒有音訊 |
| 227 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | 怎樣將 Fossify Gallery 設為本裝置的預設相簿？ |
| 228 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\". The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | 首先，您需要在裝置設定的應用程式部分找到目前預設的相簿，尋找一個寫著「預設開啟」之類的按鈕，點擊它，然後選擇「清除預設」。下次嘗試開啟圖片或影片時，您應該會看到一個應用程式選擇器，您可以在其中選擇 Fossify Gallery 並將其設為預設應用程式。 |
| 229 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | 用密碼把這個應用程式上鎖後忘記密碼該怎麼辦？ |
| 230 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | 有兩種解決方法。一是重新安裝本應用程式。一是在裝置的設定中找到本應用程式，然後按「清除資料」（或「清除儲存空間」等類似的字眼），應用程式的設定將會被重設，但不會移除任何媒體檔案。 |
| 231 | `faq_3_title` | How can I make an album always appear at the top? | 如何使某個相簿顯示在最頂端？ |
| 232 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | 長按該相簿，接著按圖釘圖示或選單中的「釘選在頂端」。您可以同時釘選數個相簿，它們將會依照預設排序方式排序。 |
| 233 | `faq_4_title` | How can I fast forward videos? | 如何快轉影片？ |
| 234 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | 點兩下螢幕邊邊，或者按一下進度條旁邊的目前播放時間／影片時長文字。若您在設定中啟用了「總是用新的水平手勢在獨立畫面開啟影片」，則您也可以使用水平手勢快轉。 |
| 235 | `faq_5_title` | What is the difference between hiding and excluding a folder? | 「隱藏」與「排除」資料夾之間有什麼差別？ |
| 236 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | 排除是指僅在 Fossify Gallery 中不顯示該資料夾，而隱藏則是系統層級的操作，它也會將資料夾隱藏於其他相簿。這是透過在指定資料夾建立一個名為「.nomedia」的空檔案來實現的，您也可以使用任何檔案管理員刪除它。請注意，有些裝置不允許隱藏像是相機、螢幕截圖和下載等資料夾。 |
| 237 | `faq_6_title` | Why do folders with music cover art or stickers show up? | 為何出現了音樂的封面圖或是貼圖的資料夾？ |
| 238 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | 有時候會出現一些異常的相簿。長按並點選排除可排除它們，在接下來的對話框選擇它們的上層資料夾的話，有機會避免相關的相簿出現。 |
| 239 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | 有個含有圖片的資料夾沒出現，抑或是沒有顯示所有項目時，該怎麼辦？ |
| 240 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | 可能有很多種原因，但解法很簡單。前往「設定」 -&gt; 「管理包含資料夾」、按加號（+）、選擇該資料夾即可。 |
| 241 | `faq_8_title` | What if I want just a few particular folders visible? | 如果只想顯示特定資料夾呢？ |
| 242 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders. That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | 如果只將某資料夾加入至「包含資料夾」的話，其他的資料夾並不會被自動排除。請前往「設定」-&gt;「管理排除資料夾」、將根資料夾「/」排除，並在「設定」-&gt;「管理包含資料夾」中加入欲顯示的資料夾。 這樣就只會顯示所選的資料夾，因為排除與包含都是遞迴的，所以若某資料夾同時被包含及排除，其將會顯示。 |
| 243 | `faq_10_title` | Can I crop images with this app? | 這個應用程式可以裁剪圖片嗎？ |
| 244 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | 可以，在編輯工具中拖動圖片的角落即可裁剪圖片。對縮圖長按並選擇編輯，或者在全螢幕檢視時選擇編輯皆可進入編輯工具。 |
| 245 | `faq_11_title` | Can I somehow group media file thumbnails? | 有辦法分類縮圖嗎？ |
| 246 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | 可以，在檢視縮圖時使用選單中的「分組方式」即可，有多種分類方式可供選擇，包含拍攝日期等。若使用了「資料夾內容全部顯示」功能，則也可以依資料夾來分類。 |
| 247 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | 依照拍攝日期的排序好像排得不太對，要怎麼修正？ |
| 248 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | 很有可能是因為檔案是從其他地方複製過來的。選擇縮圖並選擇「修復拍攝日期數值」即可修正。 |
| 249 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | 看到圖片上出現了色帶問題，要如何改善畫質？ |
| 250 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | 目前顯示圖片的方式在大多數情況下都能正常運作，但若想要更好的畫質，可以啟用「設定」-&gt;「可強制放大的圖片」段落中的「以最高品質顯示圖片」。 |
| 251 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | 如何取消隱藏檔案／資料夾？ |
| 252 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | 按主畫面選單中的「暫時顯示隱藏的檔案」或設定中的「顯示隱藏的項目」就能看到隱藏的項目。若要取消隱藏，長按並選擇「取消隱藏」即可。資料夾隱藏的原理是在其內新增一個「.nomedia」檔案，因此可用任意檔案管理器將其刪除。檔案／資料夾是以遞迴的方式被隱藏的，因此若某資料夾被隱藏，其子資料夾也會被隱藏。若要取消隱藏子資料夾，其上層資料夾需先被取消隱藏。 |
| 253 | `faq_15_title` | Why does the app take up so much space? | 這個應用程式怎麼佔用了這麼多空間？ |
| 254 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | 應用程式快取可協助圖片載入更快，最高僅佔 250 MB。如果此應用程式佔用超過 250 MB 空間，可能是回收桶中的檔案仍在佔用空間。您可刪除回收桶中的所有檔案，或在設定中快速清除。檔案進入回收桶的 30 天後將自動刪除。 |
| 255 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | 檔案／資料夾隱藏怎麼了？為什麼我看不到隱藏的項目了？ |
| 256 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | 自 Android 11 開始，因系統限制，無法隱藏或解除隱藏檔案或資料夾，也無法在相簿應用程式中看到已隱藏的項目。請使用檔案管理程式達成上述操作。 |
| 257 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | 或者，您也可以透過您的裝置設定授予此相簿存取所有檔案的權限，這將允許我們顯示隱藏項目並使檔案操作更為可靠。 |
| 258 | `faq_17_title` | Why cannot I include missing folders anymore? | 為什麼不能再包含缺漏的資料夾了？ |
| 259 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | 因 Android 11 的系統變更，這項功能無法再運作。應用程式沒辦法瀏覽實際的資料夾，而是透過所謂的 MediaStore 來擷取資料。 |
| 260 | `faq_18_title` | Why do I see ads during video playback? | 為什麼我在播放影片時看到廣告？ |
| 261 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | 我們的應用程式完全沒有廣告。如果您在播放影片時看到它們，那麼您一定是在使用其他應用程式的影片播放器。請試著在裝置設定中找到您的預設影片播放器，並清除其預設值。下次您嘗試播放影片時，系統會提示您選擇應用程式，您可以在此選擇您想要使用的影片播放器。 |
| 262 | `create_virtual_album` | Create virtual album | 建立虛擬相簿 |
| 263 | `new_virtual_album` | New album… | 新增相簿… |
| 264 | `rename_virtual_album` | Rename album | 重新命名相簿 |
| 265 | `album_name` | Album name | 相簿名稱 |
| 266 | `virtual_folder_name` | Virtual folder name | 虛擬資料夾名稱 |
| 267 | `virtual_album_name_empty` | The album name cannot be empty | 相簿名稱不可為空 |
| 268 | `virtual_album_exists` | An album with this name already exists | 已存在相同名稱的相簿 |
| 269 | `add_to_virtual_album` | Add to album | 加入相簿 |
| 270 | `remove_from_virtual_album` | Remove from album | 從相簿中移除 |
| 271 | `added_to_virtual_album` | Added to \"%s\" | 已加入「%s」 |
| 272 | `already_in_virtual_album` | Already in \"%s\" | 已在「%s」中 |
| 273 | `removed_from_virtual_album` | Removed from the album | 已從相簿中移除 |
| 274 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | 確定要刪除相簿 %s 嗎？僅會移除相簿本身，您的相片和影片仍會保留在原處。 |
| 275 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | 此相簿為空。請在任意資料夾中選取項目，並使用「複製」。 |
| 276 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | 此操作不適用於虛擬相簿 |
| 277 | `alphanumeric_password` | Password | 密碼 |
| 278 | `enter_password` | Enter password | 輸入密碼 |
| 279 | `repeat_password` | Repeat password | 再次輸入密碼 |
| 280 | `wrong_password` | Wrong password | 密碼錯誤 |
| 281 | `encrypting_files` | Encrypting files… | 正在加密檔案… |
| 282 | `encrypting_folder` | Encrypting folder… | 正在加密資料夾… |
| 283 | `folder_encrypted_successfully` | Folder encrypted successfully | 資料夾加密成功 |
| 284 | `file_encrypted_successfully` | File hidden successfully | 已成功隱藏檔案 |
| 285 | `files_encrypted_successfully` | Files hidden successfully | 已成功隱藏檔案 |
| 286 | `decrypting_folder` | Decrypting folder… | 正在解密資料夾… |
| 287 | `folder_decrypted_successfully` | Folder decrypted successfully | 資料夾解密成功 |
| 288 | `file_decrypted_successfully` | File unhidden successfully | 已成功解除隱藏檔案 |
| 289 | `files_decrypted_successfully` | Files unhidden successfully | 已成功解除隱藏檔案 |
| 290 | `decrypting_files` | Decrypting files… | 正在解密檔案… |
| 291 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | 請將手指放在指紋感應器上 |
| 292 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | 請至少連接 4 個點 |
| 293 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | 您要存取現有的 Cipher_Secrets 資料夾嗎？ |
| 294 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | 現有的 Cipher_Secrets 資料夾將會被刪除並建立新的資料夾。確定要繼續嗎？ |
| 295 | `confirm_checkbox_label` | I confirm | 我確認 |
| 296 | `secret_vault` | Encrypted Vault | 加密保險箱 |
| 297 | `secret_vault_empty` | Encrypted Vault is empty | 加密保險箱為空 |
| 298 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | 確定要解密隱藏的檔案嗎？ |
| 299 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | 基於安全考量，保險箱解鎖期間已停用複製到剪貼簿功能。 |
| 300 | `on_vault_exit` | On leaving encrypted section | 離開加密區域時 |
| 301 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | 繼續顯示加密相簿 |
| 302 | `on_vault_exit_hide` | Hide encrypted albums | 隱藏加密相簿 |
| 303 | `on_vault_exit_ask_every_time` | Always ask | 每次詢問 |
| 304 | `vault_exit_dialog_title` | Exiting Encrypted Section | 正在離開加密區域 |
| 305 | `vault_exit_question_single` | Show encrypted album together with other albums? | 是否將加密相簿與其他相簿一起顯示？ |
| 306 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? | 是否將加密相簿與其他相簿一起顯示？ |
| 307 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | 如需更多選項，請前往「設定」&gt;「離開加密區域時」 |
| 308 | `stop_showing_hidden` | Stop showing encrypted media | 停止顯示已加密媒體 |
| 309 | `system_default` | System default | 系統預設 |
| 310 | `language` | Language | 語言 |
| 311 | `open_system_language_settings` | Open system language settings | 開啟系統語言設定 |
| 312 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | 使用生物辨識存取隱藏項目 |
| 313 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | 請先啟用隱藏項目的密碼保護 |
| 314 | `biometric_unlock_vault` | Unlock encrypted vault | 解鎖加密保險箱 |
| 315 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | 驗證您的生物辨識身分以存取隱藏項目 |
| 316 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | 確認您的生物辨識身分以啟用保險箱的生物辨識存取 |
| 317 | `use_password_or_pin` | Use PIN / Password | 使用 PIN 碼 / 密碼 |
| 318 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | 生物辨識憑證已變更，請輸入您的 PIN 碼或密碼。 |
| 319 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | 此裝置上找不到生物辨識硬體或已註冊的憑證 |
| 320 | `default_hidden_album` | Default Hidden Album | 預設隱藏相簿 |
| 321 | `select_hidden_album` | Select Hidden Album | 選擇隱藏相簿 |
| 322 | `create_new_hidden_album` | Create New Hidden Album | 建立新隱藏相簿 |
| 323 | `new_hidden_album` | New hidden album | 新隱藏相簿 |
| 324 | `hidden_album_name_empty` | Album name cannot be empty | 相簿名稱不能為空 |
| 325 | `file_hidden_to_album` | File hidden to \"%s\" | 檔案已隱藏至「%s」 |
| 326 | `files_hidden_to_album` | %1$d files hidden to \"%2$s\" | %1$d 個檔案已隱藏至「%2$s」 |
| 327 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | 您確定要還原 %d 個檔案嗎？ *(CLDR `other`)* |
| 328 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | 您確定要還原 %d 個檔案嗎？ |
| 329 | `failed_to_resize_images` (`one`) | Failed to resize %d image | %d 張圖片尺寸調整失敗 *(CLDR `other`)* |
| 330 | `failed_to_resize_images` (`other`) | Failed to resize %d images | %d 張圖片尺寸調整失敗 |
| 331 | `search` *(fossify-commons)* | *(inherited from commons)* | 搜尋 |
| 332 | `change_date_and_time_format` *(fossify-commons)* | Change date and time format | 更改日期和時間格式 |
| 333 | `password_protect_file_deletion` *(fossify-commons)* | Password protect file deletion and moving | 保護檔案刪除和移動 |
| 334 | `keep_last_modified` *(fossify-commons)* | Keep old last-modified value at file operations | 變更檔案後仍保留原有的最後修改日期 |
| 335 | `clear_cache` *(fossify-commons)* | Clear cache | 清除快取 |
| 336 | `export_settings` *(fossify-commons)* | Export settings | 匯出設定 |
| 337 | `import_settings` *(fossify-commons)* | Import settings | 匯入設定 |
| 338 | `temporarily_show_excluded` *(fossify-commons)* | Temporarily show excluded | 暫停排除項目 |
| 339 | `change_view_type` *(fossify-commons)* | Change view type | 顯示方式 |
| 340 | `contributors` *(fossify-commons)* | Contributors | 貢獻者 |
| 341 | `known_issues` *(fossify-commons)* | Known issues | 已知問題 |
| 342 | `donate_to_fossify` *(fossify-commons)* | Donate to Fossify | 贊助 Fossify |
| 343 | `privacy_policy` *(fossify-commons)* | Privacy policy | 隱私政策 |
| 344 | `create_shortcut` *(fossify-commons)* | Create shortcut | 建立捷徑 |
| 345 | `print` *(fossify-commons)* | Print | 列印 |
| 346 | `search_files` *(fossify-commons)* | Search files | 搜尋檔案 |
| 347 | `search_folders` *(fossify-commons)* | Search folders | 搜尋資料夾 |
| 348 | `pull_down_to_open_vault` | Keep pulling down for Encrypted Vault | 繼續向下拉以開啟加密保險箱 |
| 349 | `keep_holding_to_open_vault` | Keep holding to open Encrypted Vault | 按住不放以開啟加密保險箱 |
| 350 | `release_to_open_vault` | Release to open Encrypted Vault | 放開以開啟加密保險箱 |
| 351 | `manage_excluded_folders` *(fossify-commons)* | Manage excluded folders | 管理排除的資料夾 |
| 352 | `exclude` *(fossify-commons)* | Exclude | 排除 |
| 353 | `exclude_folder` *(fossify-commons)* | Exclude folder | 排除資料夾 |
| 354 | `lock_folder` *(fossify-commons)* | Lock folder | 上鎖資料夾 |
| 355 | `change_cover_image` *(fossify-commons)* | Change cover image | 更換封面圖片 |
| 356 | `select_photo` *(fossify-commons)* | Select photo | 選擇相片 |
| 357 | `use_default` *(fossify-commons)* | Use default | 使用預設 |
| 358 | `column_count` *(fossify-commons)* | Column count | 欄數 |
| 359 | `open_the_recycle_bin` *(fossify-commons)* | Open the recycle bin | 開啟回收桶 |
| 360 | `grid` *(fossify-commons)* | Grid | 格狀 |
| 361 | `list` *(fossify-commons)* | List | 列表 |
| 362 | `resize` *(fossify-commons)* | Resize | 調整尺寸 |
| 363 | `version_placeholder` *(fossify-commons)* | Version %s | 版本 %s |
