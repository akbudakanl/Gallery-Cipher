# Azerbaijani (values-az) Translation Revision & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-az/strings.xml` was reviewed from scratch: existing inaccurate translations were corrected, and all missing keys from `values/strings.xml` (326 `<string>` + 2 `<plurals>` = 328 app resource entries; `search` and 14 shared UI keys from `fossify-commons`; 345 total comparison rows) were completed.
> **Key Terminology:** `Settings` → `Parametrlər` · `Default` → `Susmaya görə` · `Folder` → `Qovluq` · `Secret Vault` → `Gizli Seyf` · `Password` → `Parol` · `Encrypt/Decrypt` → `Şifrələ / Şifrəsini aç` · `Thumbnails` → `Miniatürlər` · `Recycle bin` → `Zibil qutusu` · `Clipboard` → `Mübadilə buferi`

## 1. Review of 11 Previously Existing Translations and Changes Made

| Key | Old `values-az` | New `values-az` | Status / Notes |
|---|---|---|---|
| `app_launcher_name` | Qalereya | Qalereya | Kept |
| `edit` | Redaktə | Redaktə | Kept |
| `open_camera` | Kameranı aç | Kameranı aç | Kept |
| `hidden` | (gizli) | (gizli) | Kept |
| `pin_folder` | Pin qovluğu | Qovluğu bərkit | **Fixed** (Removed English verb and incorrect noun phrase) |
| `unpin_folder` | Unpin qovluğu | Bərkitməni ləğv et | **Fixed** (Removed English verb and incorrect noun phrase) |
| `toggle_favorite` | Sevimlini göstər/gizlət | Sevimliyə əlavə et/çıxar | **Fixed** (Meaning is add/remove favorite, not show/hide) |
| `language` | Dil | Dil | Kept |
| `search` | Axtar | Axtar | Kept (Used in `fossify-commons` UI) |
| `system_default` | Sistem standartı | Sistem (susmaya görə) | **Improved** (Standard Azerbaijani software terminology) |
| `open_system_language_settings` | Sistem dil parametrlərini aç | Sistem dil parametrlərini aç | Kept |

### `fossify-commons` Inherited UI Overrides (14 Keys)

`org.fossify:commons:7.1.3` (`res/values-az/values-az.xml`) omitted these 14 UI strings, so local overrides were added to `app/src/main/res/values-az/strings.xml`:

| # | Key (`name`) | English (`fossify-commons`) | Azerbaijani (`values-az/strings.xml`) | Source / Notes |
|---:|---|---|---|---|
| 1 | `change_date_and_time_format` | Change date and time format | Tarix və saat formatını dəyiş | Local override in `values-az/strings.xml` (Settings) |
| 2 | `password_protect_file_deletion` | Password protect file deletion and moving | Faylların silinməsini və köçürülməsini parolla qoru | Local override in `values-az/strings.xml` (Settings) |
| 3 | `keep_last_modified` | Keep old last-modified value at file operations | Fayl əməliyyatlarında köhnə son dəyişdirilmə dəyərini qoru | Local override in `values-az/strings.xml` (Settings) |
| 4 | `clear_cache` | Clear cache | Keşi təmizlə | Local override in `values-az/strings.xml` (Settings) |
| 5 | `export_settings` | Export settings | Parametrləri ixrac et | Local override in `values-az/strings.xml` (Settings) |
| 6 | `import_settings` | Import settings | Parametrləri idxal et | Local override in `values-az/strings.xml` (Settings) |
| 7 | `temporarily_show_excluded` | Temporarily show excluded | İstisna edilmişləri müvəqqəti göstər | Local override in `values-az/strings.xml` (Options menu) |
| 8 | `change_view_type` | Change view type | Görünüş növünü dəyiş | Local override in `values-az/strings.xml` (Options menu) |
| 9 | `contributors` | Contributors | Töhfə verənlər | Local override in `values-az/strings.xml` (About) |
| 10 | `known_issues` | Known issues | Məlum problemlər | Local override in `values-az/strings.xml` (About) |
| 11 | `donate_to_fossify` | Donate to Fossify | Fossify-a ianə et | Local override in `values-az/strings.xml` (About) |
| 12 | `privacy_policy` | Privacy policy | Məxfilik siyasəti | Local override in `values-az/strings.xml` (About) |
| 13 | `create_shortcut` | Create shortcut | Qısayol yarat | Local override in `values-az/strings.xml` (CAB / ViewPager menu) |
| 14 | `print` | Print | Çap et | Local override in `values-az/strings.xml` (ViewPager menu) |
| 15 | `search_files` | Search files | Faylları axtar | Local override in `values-az/strings.xml` (Search bar hint) |
| 16 | `search_folders` | Search folders | Qovluqları axtar | Local override in `values-az/strings.xml` (Search bar hint) |

### Newly Added Fork-Only Strings (63 Keys)

These 63 strings were introduced by this fork (Gallery-Cipher) and do not exist in the original Fossify Gallery:

| # | Key (`name`) | English (`values/strings.xml`) | Azerbaijani (`values-az/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | 1 | `video_double_tap_duration` | Double-tap seek duration |  |
| 2 | 2 | `video_seek_duration_seconds` | %d seconds |  |
| 3 | 3 | `create_virtual_album` | Create virtual album |  |
| 4 | 4 | `new_virtual_album` | New album… |  |
| 5 | 5 | `rename_virtual_album` | Rename album |  |
| 6 | 6 | `album_name` | Album name |  |
| 7 | 7 | `virtual_folder_name` | Virtual folder name |  |
| 8 | 8 | `virtual_album_name_empty` | The album name cannot be empty |  |
| 9 | 9 | `virtual_album_exists` | An album with this name already exists |  |
| 10 | 10 | `add_to_virtual_album` | Add to album |  |
| 11 | 11 | `remove_from_virtual_album` | Remove from album |  |
| 12 | 12 | `added_to_virtual_album` | Added to \"%s\" |  |
| 13 | 13 | `already_in_virtual_album` | Already in \"%s\" |  |
| 14 | 14 | `removed_from_virtual_album` | Removed from the album |  |
| 15 | 15 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. |  |
| 16 | 16 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". |  |
| 17 | 17 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums |  |
| 18 | 18 | `alphanumeric_password` | Password |  |
| 19 | 19 | `repeat_password` | Repeat password |  |
| 20 | 20 | `wrong_password` | Wrong password |  |
| 21 | 21 | `encrypting_files` | Encrypting files… |  |
| 22 | 22 | `encrypting_folder` | Encrypting folder… |  |
| 23 | 23 | `folder_encrypted_successfully` | Folder encrypted successfully |  |
| 24 | 24 | `decrypting_folder` | Decrypting folder… |  |
| 25 | 25 | `folder_decrypted_successfully` | Folder decrypted successfully |  |
| 26 | 26 | `decrypting_files` | Decrypting files… |  |
| 27 | 27 | `place_finger_prompt` | Please place your finger on the fingerprint sensor |  |
| 28 | 28 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots |  |
| 29 | 29 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? |  |
| 30 | 30 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? |  |
| 31 | 31 | `confirm_checkbox_label` | I confirm |  |
| 32 | 32 | `secret_vault` | Secret Vault |  |
| 33 | 33 | `secret_vault_empty` | Secret Vault is empty |  |
| 34 | 34 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? |  |
| 35 | 35 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. |  |
| 36 | 36 | `on_vault_exit` | On leaving encrypted section |  |
| 37 | 37 | `on_vault_exit_keep_showing` | Keep showing encrypted albums |  |
| 38 | 38 | `on_vault_exit_hide` | Hide encrypted albums |  |
| 39 | 39 | `on_vault_exit_ask_every_time` | Always ask |  |
| 40 | 40 | `vault_exit_dialog_title` | Exiting Encrypted Section |  |
| 41 | 41 | `vault_exit_question_single` | Show encrypted album together with other albums? |  |
| 42 | 42 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? |  |
| 43 | 43 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section |  |
| 44 | 44 | `open_system_language_settings` | Open system language settings |  |
| 45 | 45 | `file_encrypted_successfully` | File hidden successfully | Success toast after encrypting/hiding a single file |
| 46 | 46 | `files_encrypted_successfully` | Files hidden successfully | Success toast after encrypting/hiding multiple files |
| 47 | 47 | `file_decrypted_successfully` | File unhidden successfully | Success toast after decrypting/unhiding a single file |
| 48 | 48 | `files_decrypted_successfully` | Files unhidden successfully | Success toast after decrypting/unhiding multiple files |
| 49 | 49 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Settings switch, setup prompt title, and vault security dialog button |
| 50 | 50 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Toast shown when enabling biometrics before hidden-item password protection |
| 51 | 51 | `biometric_unlock_vault` | Unlock hidden vault | BiometricPrompt title when unlocking the hidden vault |
| 52 | 52 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | BiometricPrompt subtitle when unlocking hidden items |
| 53 | 53 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | BiometricPrompt subtitle when enabling biometric vault access |
| 54 | 54 | `use_password_or_pin` | Use PIN / Password | BiometricPrompt negative button to fall back to PIN/password |
| 55 | 55 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Toast shown when biometric credentials changed and Keystore key invalidated |
| 56 | 56 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | Toast shown when no biometric hardware or enrolled credentials exist |
| 57 | 57 | `default_hidden_album` | Default Hidden Album | Settings / picker label for default hidden album |
| 58 | 58 | `select_hidden_album` | Select Hidden Album | Dialog title for choosing target hidden album |
| 59 | 59 | `create_new_hidden_album` | Create New Hidden Album | Action label to create a new hidden album |
| 60 | 60 | `new_hidden_album` | New hidden album | Input hint / default label for new hidden album |
| 61 | 61 | `hidden_album_name_empty` | Album name cannot be empty | Validation error when hidden album name is blank |
| 62 | 62 | `file_hidden_to_album` | File hidden to \"%s\" | Success toast after hiding a single file into album `%s` |
| 63 | 63 | `files_hidden_to_album` | %1$d files hidden to \"%2$s\" | Success toast after hiding `%1$d` files into album `%2$s` |


## 2. Full 1-to-1 Comparison: English vs. Azerbaijani

| # | Key (`name`) | English (`values/strings.xml`) | Azerbaijani (`values-az/strings.xml`) |
|---:|---|---|---|
| 1 | `app_launcher_name` | Gallery | Qalereya |
| 2 | `edit` | Edit | Redaktə |
| 3 | `open_camera` | Open camera | Kameranı aç |
| 4 | `hidden` | (hidden) | (gizli) |
| 5 | `excluded` | (excluded) | (istisna edilmiş) |
| 6 | `pin_folder` | Pin folder | Qovluğu bərkit |
| 7 | `unpin_folder` | Unpin folder | Bərkitməni ləğv et |
| 8 | `pin_to_the_top` | Pin to the top | Yuxarıya bərkit |
| 9 | `show_all` | Show all folders content | Bütün qovluqların məzmununu göstər |
| 10 | `all_folders` | All folders | Bütün qovluqlar |
| 11 | `folder_view` | Switch to folder view | Qovluq görünüşünə keç |
| 12 | `other_folder` | Other folder | Digər qovluq |
| 13 | `show_on_map` | Show on map | Xəritədə göstər |
| 14 | `unknown_location` | Unknown location | Naməlum məkan |
| 15 | `volume` | Volume | Səs səviyyəsi |
| 16 | `brightness` | Brightness | Parlaqlıq |
| 17 | `lock_orientation` | Lock orientation | İstiqaməti kilidlə |
| 18 | `unlock_orientation` | Unlock orientation | İstiqamətin kilidini aç |
| 19 | `change_orientation` | Change orientation | İstiqaməti dəyiş |
| 20 | `force_portrait` | Force portrait | Şaquli rejimi məcbur et |
| 21 | `force_landscape` | Force landscape | Üfüqi rejimi məcbur et |
| 22 | `force_landscape_reverse` | Force landscape (reverse) | Üfüqi rejimi məcbur et (əks) |
| 23 | `use_default_orientation` | Use default orientation | Susmaya görə istiqaməti işlət |
| 24 | `fix_date_taken` | Fix Date Taken value | Çəkilmə tarixi dəyərini düzəlt |
| 25 | `fixing` | Fixing… | Düzəldilir… |
| 26 | `dates_fixed_successfully` | Dates fixed successfully | Tarixlər uğurla düzəldildi |
| 27 | `no_date_takens_found` | No Date Taken values have been found | Heç bir çəkilmə tarixi dəyəri tapılmadı |
| 28 | `share_resized` | Share a resized version | Ölçüsü dəyişdirilmiş versiyanı paylaş |
| 29 | `switch_to_file_search` | Switch to file search across all visible folders | Bütün görünən qovluqlar üzrə fayl axtarışına keç |
| 30 | `set_as_default_folder` | Set as default folder | Susmaya görə qovluq təyin et |
| 31 | `unset_as_default_folder` | Unset as default folder | Susmaya görə qovluq təyinatını ləğv et |
| 32 | `reorder_by_dragging` | Reorder folders by dragging | Qovluqları sürüşdürərək yenidən sırala |
| 33 | `reorder_by_dragging_pro` | Reorder folders by dragging (Pro) | Qovluqları sürüşdürərək yenidən sırala (Pro) |
| 34 | `restore_to_path` | Restoring to \'%s\' | \'%s\' yoluna bərpa edilir |
| 35 | `full_storage_permission_required` | Fossify Gallery needs full access to display all your photos and videos. Go to Settings &gt; Permissions &gt; Photos and videos &gt; Allow all. | Fossify Qalereya bütün foto və videolarınızı göstərmək üçün tam giriş icazəsinə ehtiyac duyur. Parametrlər &gt; İcazələr &gt; Foto və videolar &gt; Hamısına icazə ver bölməsinə keçin. |
| 36 | `filter_media` | Filter media | Medianı süzgəcdən keçir |
| 37 | `images` | Images | Şəkillər |
| 38 | `videos` | Videos | Videolar |
| 39 | `gifs` | GIFs | GIF-lər |
| 40 | `raw_images` | RAW images | RAW şəkillər |
| 41 | `svgs` | SVGs | SVG-lər |
| 42 | `portraits` | Portraits | Portretlər |
| 43 | `no_media_with_filters` | No media files have been found with the selected filters. | Seçilmiş süzgəclərlə heç bir media faylı tapılmadı. |
| 44 | `change_filters_underlined` | <u>Change filters</u> | <u>Süzgəcləri dəyiş</u> |
| 45 | `hide_folder_description` | This function hides the folder by adding a \'.nomedia\' file into it, it will hide all subfolders too. You can see them by toggling the \'Show hidden items\' option in Settings. Continue? | Bu funksiya qovluğun içinə \'.nomedia\' faylı əlavə etməklə onu və bütün alt qovluqlarını gizlədir. Onları Parametrlərdə \'Gizli elementləri göstər\' seçimini aktivləşdirərək görə bilərsiniz. Davam edilsin? |
| 46 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | Bu, seçilmiş qovluğu alt qovluqları ilə birlikdə yalnız Fossify Qalereyadan istisna edəcək. İstisna edilmiş qovluqları Parametrlərdən idarə edə bilərsiniz. |
| 47 | `exclude_folder_parent` | Exclude a parent instead? | Əvəzində üst qovluq istisna edilsin? |
| 48 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | Qovluqları istisna etmək onları alt qovluqları ilə birlikdə yalnız Fossify Qalereyada gizlədər, digər tətbiqlərdə görünməyə davam edər.\n\nƏgər onları digər tətbiqlərdən də gizlətmək istəyirsinizsə, Gizlət funksiyasından istifadə edin. |
| 49 | `hidden_folders` | Hidden folders | Gizli qovluqlar |
| 50 | `manage_hidden_folders` | Manage hidden folders | Gizli qovluqları idarə et |
| 51 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | Görünür, \".nomedia\" faylı ilə gizlədilmiş heç bir qovluğunuz yoxdur. |
| 52 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | Gizli faylları görmək üçün tətbiqə Bütün Fayllara giriş icazəsi verməlisiniz, əks halda işləməyəcək. |
| 53 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Əgər qovluğun və ya onun üst qovluqlarından birinin adının əvvəlində nöqtə varsa, o gizlidir və bu yolla gizlədilməsi ləğv edilə bilməz. Adını dəyişərək nöqtəni silməlisiniz. |
| 54 | `include_folders` | Included folders | Daxil edilmiş qovluqlar |
| 55 | `manage_included_folders` | Manage included folders | Daxil edilmiş qovluqları idarə et |
| 56 | `add_folder` | Add folder | Qovluq əlavə et |
| 57 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | Tərkibində media olan, lakin tətbiq tərəfindən tanınmayan qovluqlarınız varsa, onları bura əl ilə əlavə edə bilərsiniz.\n\nBura element əlavə etmək digər qovluqları istisna etməyəcək. |
| 58 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | Heç bir media faylı tapılmadı. Media faylları olan qovluqları əl ilə əlavə etməklə bunu həll edə bilərsiniz. |
| 59 | `resize_and_save` | Resize selection and save | Seçimi yenidən ölçüləndir və saxla |
| 60 | `width` | Width | En |
| 61 | `height` | Height | Hündürlük |
| 62 | `keep_aspect_ratio` | Keep aspect ratio | Tərəf nisbətini qoru |
| 63 | `invalid_values` | Please enter a valid resolution | Zəhmət olmasa, düzgün qətnamə daxil edin |
| 64 | `resize_multiple_images` | Resize multiple images | Birdən çox şəklin ölçüsünü dəyiş |
| 65 | `resize_factor` | Resize factor | Ölçü dəyişmə əmsalı |
| 66 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | Şəkillərin ölçüsünü göstərilən faizə görə dəyişin, dəyər 10 ilə 90 arasında olmalıdır. |
| 67 | `resize_factor_error` | Enter a number between 10 and 90 | 10 ilə 90 arasında bir rəqəm daxil edin |
| 68 | `images_resized_successfully` | Images resized successfully | Şəkillərin ölçüsü uğurla dəyişdirildi |
| 69 | `editor` | Editor | Redaktor |
| 70 | `basic_editor` | Basic Editor | Sadə Redaktor |
| 71 | `advanced_editor` | Advanced Editor | Təkmil Redaktor |
| 72 | `rotate` | Rotate | Döndər |
| 73 | `invalid_image_path` | Invalid image path | Yanlış şəkil yolu |
| 74 | `invalid_video_path` | Invalid video path | Yanlış video yolu |
| 75 | `image_editing_failed` | Image editing failed | Şəklin redaktəsi uğursuz oldu |
| 76 | `video_editing_failed` | Video editing failed | Videonun redaktəsi uğursuz oldu |
| 77 | `image_editing_cancelled` | Image editing cancelled | Şəklin redaktəsi ləğv edildi |
| 78 | `video_editing_cancelled` | Video editing cancelled | Videonun redaktəsi ləğv edildi |
| 79 | `file_edited_successfully` | File edited successfully | Fayl uğurla redaktə edildi |
| 80 | `image_edited_successfully` | Image edited successfully | Şəkil uğurla redaktə edildi |
| 81 | `video_edited_successfully` | Video edited successfully | Video uğurla redaktə edildi |
| 82 | `edit_image_with` | Edit image with: | Şəkli bununla redaktə et: |
| 83 | `edit_video_with` | Edit video with: | Videonu bununla redaktə et: |
| 84 | `no_image_editor_found` | No image editor found | Şəkil redaktoru tapılmadı |
| 85 | `no_video_editor_found` | No video editor found | Video redaktoru tapılmadı |
| 86 | `unknown_file_location` | Unknown file location | Naməlum fayl yeri |
| 87 | `overwrite_original` | Overwrite original | Orijinalın üzərinə yaz |
| 88 | `error_saving_file` | Could not overwrite the source file | Mənbə faylın üzərinə yazmaq mümkün olmadı |
| 89 | `rotate_left` | Rotate left | Sola döndər |
| 90 | `rotate_right` | Rotate right | Sağa döndər |
| 91 | `rotate_one_eighty` | Rotate by 180º | 180º döndər |
| 92 | `transform` | Transform | Çevirmə |
| 93 | `crop` | Crop | Kəs |
| 94 | `draw` | Draw | Çək |
| 95 | `flip` | Flip | Əks etdir |
| 96 | `flip_horizontally` | Flip horizontally | Üfüqi əks etdir |
| 97 | `flip_vertically` | Flip vertically | Şaquli əks etdir |
| 98 | `free_aspect_ratio` | Free | Sərbəst |
| 99 | `other_aspect_ratio` | Other | Digər |
| 100 | `wallpaper` | Wallpaper | Divar kağızı |
| 101 | `set_as_wallpaper` | Set as Wallpaper | Divar kağızı kimi təyin et |
| 102 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | Divar kağızı kimi təyin etmək uğursuz oldu |
| 103 | `set_as_wallpaper_with` | Set as wallpaper with: | Bununla divar kağızı kimi təyin et: |
| 104 | `setting_wallpaper` | Setting wallpaper… | Divar kağızı təyin edilir… |
| 105 | `wallpaper_set_successfully` | Wallpaper set successfully | Divar kağızı uğurla təyin edildi |
| 106 | `portrait_aspect_ratio` | Portrait aspect ratio | Şaquli tərəf nisbəti |
| 107 | `landscape_aspect_ratio` | Landscape aspect ratio | Üfüqi tərəf nisbəti |
| 108 | `home_screen` | Home screen | Əsas ekran |
| 109 | `lock_screen` | Lock screen | Kilid ekranı |
| 110 | `home_and_lock_screen` | Home and lock screen | Əsas və kilid ekranı |
| 111 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | Tərəf nisbətini dəyişməyə icazə ver |
| 112 | `slideshow` | Slideshow | Slayd-şou |
| 113 | `interval` | Interval | İnterval |
| 114 | `include_photos` | Include photos | Fotoları daxil et |
| 115 | `include_videos` | Include videos | Videoları daxil et |
| 116 | `include_gifs` | Include GIFs | GIF-ləri daxil et |
| 117 | `random_order` | Random order | Təsadüfi sıra |
| 118 | `move_backwards` | Move backwards | Geriyə get |
| 119 | `loop_slideshow` | Loop slideshow | Slayd-şounu təkrarla |
| 120 | `animation` | Animation | Animasiya |
| 121 | `no_animation` | None | Yoxdur |
| 122 | `fade` | Fade | Solğunlaşma |
| 123 | `slide` | Slide | Sürüşmə |
| 124 | `slideshow_ended` | The slideshow ended | Slayd-şou başa çatdı |
| 125 | `no_media_for_slideshow` | No media for the slideshow have been found | Slayd-şou üçün media tapılmadı |
| 126 | `group_direct_subfolders` | Group direct subfolders | Birbaşa alt qovluqları qruplaşdır |
| 127 | `group_by` | Group by | Qruplaşdırma |
| 128 | `do_not_group_files` | Do not group files | Faylları qruplaşdırma |
| 129 | `by_folder` | Folder | Qovluq |
| 130 | `by_last_modified` | Last modified | Son dəyişdirilmə |
| 131 | `by_last_modified_daily` | Last modified (daily) | Son dəyişdirilmə (gündəlik) |
| 132 | `by_last_modified_monthly` | Last modified (monthly) | Son dəyişdirilmə (aylıq) |
| 133 | `by_date_taken` | Date taken | Çəkilmə tarixi |
| 134 | `by_date_taken_daily` | Date taken (daily) | Çəkilmə tarixi (gündəlik) |
| 135 | `by_date_taken_monthly` | Date taken (monthly) | Çəkilmə tarixi (aylıq) |
| 136 | `by_file_type` | File type | Fayl növü |
| 137 | `by_extension` | Extension | Uzantı |
| 138 | `show_file_count_section_header` | Show file count at section headers | Bölmə başlıqlarında fayl sayını göstər |
| 139 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | Nəzərə alın ki, qruplaşdırma və sıralama 2 müstəqil sahədir |
| 140 | `folder_on_widget` | Folder shown on the widget: | Vidcetdə göstərilən qovluq: |
| 141 | `show_folder_name` | Show folder name | Qovluq adını göstər |
| 142 | `autoplay_videos` | Play videos automatically | Videoları avtomatik oxut |
| 143 | `remember_last_video_position` | Remember last video playback position | Videonun son oxutma mövqeyini yadda saxla |
| 144 | `loop_videos` | Loop videos | Videoları təkrarla |
| 145 | `video_double_tap_duration` | Double-tap seek duration | İkiqat toxunuşla irəli/geri çəkmə müddəti |
| 146 | `video_seek_duration_seconds` | %d seconds | %d saniyə |
| 147 | `animate_gifs` | Animate GIFs at thumbnails | Miniatürlərdə GIF-ləri canlandır |
| 148 | `max_brightness` | Max brightness when viewing fullscreen media | Tam ekran mediaya baxarkən maksimum parlaqlıq |
| 149 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Ultra HDR fotoları HDR rejimində göstər |
| 150 | `crop_thumbnails` | Crop thumbnails into squares | Miniatürləri kvadrat şəklində kəs |
| 151 | `show_thumbnail_video_duration` | Show video durations | Video müddətlərini göstər |
| 152 | `screen_rotation_by` | Rotate fullscreen media by | Tam ekran medianı buna görə döndər: |
| 153 | `screen_rotation_system_setting` | System setting | Sistem parametri |
| 154 | `screen_rotation_device_rotation` | Device rotation | Cihazın döndərilməsi |
| 155 | `screen_rotation_aspect_ratio` | Aspect ratio | Tərəf nisbəti |
| 156 | `black_background_at_fullscreen` | Black background at fullscreen media | Tam ekran mediada qara fon |
| 157 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | Miniatürləri üfüqi sürüşdür |
| 158 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | Tam ekran mediada sistem interfeysini avtomatik gizlət |
| 159 | `delete_empty_folders` | Delete empty folders after deleting their content | Məzmunu sildikdən sonra boş qovluqları sil |
| 160 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | Tam ekran fotolara baxarkən ekranı açıq saxla |
| 161 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | Şaquli jestlərlə foto parlaqlığını idarə etməyə icazə ver |
| 162 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | Şaquli jestlərlə video səsini və parlaqlığını idarə etməyə icazə ver |
| 163 | `playback_speed` | Playback speed | Oxutma sürəti |
| 164 | `show_media_count` | Show folder media count on the main view | Əsas görünüşdə qovluq media sayını göstər |
| 165 | `show_extended_details` | Show extended details over fullscreen media | Tam ekran media üzərində ətraflı məlumatı göstər |
| 166 | `manage_extended_details` | Manage extended details | Ətraflı məlumatı idarə et |
| 167 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | Tam ekran mediada tək barmaqla yaxınlaşdırmaya icazə ver |
| 168 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | Ekran kənarlarına klikləməklə medianı dərhal dəyişməyə icazə ver |
| 169 | `allow_deep_zooming_images` | Allow deep zooming images | Şəkillərin dərin yaxınlaşdırılmasına icazə ver |
| 170 | `hide_extended_details` | Hide extended details when status bar is hidden | Status sətri gizlədildikdə ətraflı məlumatı gizlət |
| 171 | `show_at_bottom` | Show some action buttons at the bottom of the screen | Ekranın aşağısında bəzi əməliyyat düymələrini göstər |
| 172 | `show_recycle_bin` | Show the recycle bin on the folders screen | Qovluqlar ekranında zibil qutusunu göstər |
| 173 | `deep_zoomable_images` | Deep zoomable images | Dərin yaxınlaşdırılan şəkillər |
| 174 | `show_highest_quality` | Show images in the highest possible quality | Şəkilləri mümkün olan ən yüksək keyfiyyətdə göstər |
| 175 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | Zibil qutusunu əsas ekranda sonuncu element kimi göstər |
| 176 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | Aşağıya doğru jestlə tam ekran görünüşünü bağlamağa icazə ver |
| 177 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | İki dəfə ikiqat toxunuşla 1:1 yaxınlaşdırmaya icazə ver |
| 178 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | Videoları üfüqi jestlərlə ayrı ekranda oxut |
| 179 | `on_video_tap` | On video tap | Videoya toxunduqda |
| 180 | `open_in_app_player` | Open in-app player | Tətbiqdaxili pleyerdə aç |
| 181 | `open_system_default_player` | Open default player app | Susmaya görə pleyer tətbiqində aç |
| 182 | `show_notch` | Show a notch if available | Varsa ekran kəsiyini göstər |
| 183 | `allow_rotating_gestures` | Allow rotating images with gestures | Şəkilləri jestlərlə döndərməyə icazə ver |
| 184 | `file_loading_priority` | File loading priority | Fayl yükləmə prioriteti |
| 185 | `speed` | Speed | Sürət |
| 186 | `compromise` | Compromise | Tarazlı |
| 187 | `avoid_showing_invalid_files` | Avoid showing invalid files | Etibarsız faylları göstərməkdən yayın |
| 188 | `show_image_file_types` | Show image file types | Şəkil faylı növlərini göstər |
| 189 | `allow_zooming_videos` | Allow zooming videos with double tapping them | İkiqat toxunuşla videoları yaxınlaşdırmağa icazə ver |
| 190 | `folder_thumbnail_style` | Folder thumbnail style | Qovluq miniatürü üslubu |
| 191 | `file_thumbnail_style` | File thumbnail style | Fayl miniatürü üslubu |
| 192 | `mark_favorite_items` | Mark favorite items | Sevimli elementləri işarələ |
| 193 | `thumbnail_spacing` | Thumbnail spacing | Miniatürlər arası məsafə |
| 194 | `show_file_count_line` | Show file count on a separate line | Fayl sayını ayrı sətirdə göstər |
| 195 | `show_file_count_brackets` | Show file count in brackets | Fayl sayını mötərizədə göstər |
| 196 | `show_file_count_none` | Do not show file count | Fayl sayını göstərmə |
| 197 | `limit_folder_title` | Limit long folder titles to 1 line | Uzun qovluq adlarını 1 sətirlə məhdudlaşdır |
| 198 | `square` | Square | Kvadrat |
| 199 | `rounded_corners` | Rounded corners | Dairəvi künclər |
| 200 | `export_favorite_paths` | Export favorites | Sevimliləri ixrac et |
| 201 | `import_favorite_paths` | Import favorites | Sevimliləri idxal et |
| 202 | `paths_imported_successfully` | Paths imported successfully | Yollar uğurla idxal edildi |
| 203 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | Bütün fayl əməliyyatlarının etibarlı işləməsini təmin etmək üçün, zəhmət olmasa, cihaz parametrlərində bu tətbiqi Media idarəetmə tətbiqi kimi təyin edin. |
| 204 | `password_protect_excluded` | Password protect excluded folder visibility | İstisna edilmiş qovluqların görünməsini parolla qoru |
| 205 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Xəta baş verdi. Zəhmət olmasa, cihazınızın Parametrlər - Tətbiqlər - Xüsusi tətbiq girişi - Media idarəetmə tətbiqləri bölməsinə keçin və bu tətbiqə medianı idarə etməyə icazə verin. |
| 206 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Yönləndirmə işləməzsə, zəhmət olmasa, cihazınızın Parametrlər - Tətbiqlər - Xüsusi tətbiq girişi - Media idarəetmə tətbiqləri bölməsinə keçin və bu tətbiqə medianı idarə etməyə icazə verin. |
| 207 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Bunu etmək istəmirsinizsə, cihazınızın Parametrlər - Tətbiqlər - Xüsusi tətbiq girişi - Media idarəetmə tətbiqləri bölməsinə keçib bu tətbiqə medianı idarə etməyə icazə verə bilərsiniz. |
| 208 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | Alternativ olaraq, yalnız media fayllarına girişə icazə verə bilərsiniz. Lakin bu halda gizli fayllarla işləyə bilməyəcəksiniz. |
| 209 | `media_only` | Media only | Yalnız media |
| 210 | `all_files` | All files | Bütün fayllar |
| 211 | `main_screen_view_on_launch` | Main screen view on launch | Açılışda əsas ekran görünüşü |
| 212 | `show_folders` | Show folders | Qovluqları göstər |
| 213 | `show_files` | Show files | Faylları göstər |
| 214 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | Bütün qovluqların məzmununu göstər seçimini tez dəyişmək üçün menyu düyməsini göstər |
| 215 | `thumbnails` | Thumbnails | Miniatürlər |
| 216 | `fullscreen_media` | Fullscreen media | Tam ekran media |
| 217 | `extended_details` | Extended details | Ətraflı məlumat |
| 218 | `bottom_actions` | Bottom actions | Alt əməliyyatlar |
| 219 | `manage_bottom_actions` | Manage visible bottom actions | Görünən alt əməliyyatları idarə et |
| 220 | `toggle_favorite` | Toggle favorite | Sevimliyə əlavə et/çıxar |
| 221 | `toggle_file_visibility` | Toggle file visibility | Faylın görünməsini dəyiş |
| 222 | `failed_to_load_media` | Failed to load media. | Medianı yükləmək mümkün olmadı. |
| 223 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | Fayl pozulub və ya yanlış formatdadır. |
| 224 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Media cihazın imkanlarını aşır. |
| 225 | `unsupported_format` | Unsupported format. | Dəstəklənməyən format. |
| 226 | `video_no_sound` | This video has no audio | Bu videoda səs yoxdur |
| 227 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | Fossify Qalereyanı necə susmaya görə cihaz qalereyası edə bilərəm? |
| 228 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\". The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Əvvəlcə cihaz parametrlərinizin Tətbiqlər bölməsində hazırda susmaya görə təyin edilmiş qalereyanı tapmalı, \"Susmaya görə aç\" kimi bir düyməyə toxunub \"Susmaya görə tənzimləmələri təmizlə\" seçimini etməlisiniz. Növbəti dəfə şəkil və ya video açmağa çalışdığınızda tətbiq seçicisi görünəcək; oradan Fossify Qalereyanı seçib susmaya görə tətbiq edə bilərsiniz. |
| 229 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Tətbiqi parolla kilidlədim, amma unutdum. Nə edə bilərəm? |
| 230 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Bunu 2 yolla həll edə bilərsiniz. Ya tətbiqi yenidən quraşdırın, ya da cihaz parametrlərindən tətbiqi tapıb \"Məlumatları təmizlə\" seçin. Bu, bütün parametrlərinizi sıfırlayacaq, lakin heç bir media faylını silməyəcək. |
| 231 | `faq_3_title` | How can I make an album always appear at the top? | Bir albomun həmişə ən yuxarıda görünməsini necə təmin edə bilərəm? |
| 232 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | İstədiyiniz albomun üzərinə uzun basıb əməliyyatlar menyusundan Bərkit işarəsini seçə bilərsiniz, bu onu yuxarıya bərkidəcək. Birdən çox qovluğu da bərkidə bilərsiniz; bərkidilmiş elementlər susmaya görə sıralama üsulu ilə düzüləcək. |
| 233 | `faq_4_title` | How can I fast forward videos? | Videoları necə sürətlə irəli çəkə bilərəm? |
| 234 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Bunu ekranın kənarına iki dəfə toxunmaqla və ya axtarış zolağının yanındakı cari və ya maksimum müddət yazılarına toxunmaqla edə bilərsiniz. Tətbiq parametrlərində videoların ayrı ekranda açılmasını aktivləşdirsəniz, üfüqi jestlərdən də istifadə edə bilərsiniz. |
| 235 | `faq_5_title` | What is the difference between hiding and excluding a folder? | Qovluğu gizlətmək ilə istisna etmək arasında fərq nədir? |
| 236 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | İstisna et qovluğun yalnız Fossify Qalereyada göstərilməsinin qarşısını alır, Gizlət isə sistem səviyyəsində işləyir və qovluğu digər qalereyalardan da gizlədir. Bu, həmin qovluqda boş \".nomedia\" faylı yaratmaqla işləyir və onu istənilən fayl meneceri ilə də silə bilərsiniz. Nəzərə alın ki, bəzi cihazlar Camera, Screenshots və Downloads kimi qovluqların gizlədilməsinə icazə vermir. |
| 237 | `faq_6_title` | Why do folders with music cover art or stickers show up? | Niyə musiqi üz qabıqları və ya stikerlər olan qovluqlar görünür? |
| 238 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Bəzən qeyri-adi albomların göründüyünü müşahidə edə bilərsiniz. Onların üzərinə uzun basıb İstisna et seçməklə asanlıqla gizlədə bilərsiniz. Növbəti pəncərədə üst qovluğu seçə bilərsiniz; çox güman ki, bu, əlaqəli digər albomların da görünməsinin qarşısını alacaq. |
| 239 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | Şəkillər olan qovluq görünmür və ya bütün elementləri göstərmir. Nə edə bilərəm? |
| 240 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Bunun bir neçə səbəbi ola bilər, lakin həlli asandır. Sadəcə Parametrlər -&gt; Daxil edilmiş qovluqları idarə et bölməsinə keçin, Üstəgəl işarəsini seçin və lazım olan qovluğa gedin. |
| 241 | `faq_8_title` | What if I want just a few particular folders visible? | Bəs yalnız bir neçə müəyyən qovluğun görünməsini istəyirəmsə nə edim? |
| 242 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders. That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Daxil edilmiş qovluqlara qovluq əlavə etmək avtomatik olaraq heç nəyi istisna etmir. Edə biləcəyiniz şey Parametrlər -&gt; İstisna edilmiş qovluqları idarə et bölməsinə keçib kök qovluğu \"/\" istisna etmək, sonra isə Parametrlər -&gt; Daxil edilmiş qovluqları idarə et bölməsindən istədiyiniz qovluqları əlavə etməkdir. Həm istisna etmə, həm də daxil etmə rekursiv işlədiyindən və bir qovluq həm istisna edilib, həm də daxil edildikdə göründüyündən, bu üsul yalnız seçilmiş qovluqları görünən edəcək. |
| 243 | `faq_10_title` | Can I crop images with this app? | Bu tətbiqlə şəkilləri kəsə bilərəmmi? |
| 244 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Bəli, redaktorda şəkil künclərini sürüşdürərək şəkilləri kəsə bilərsiniz. Redaktora şəkil miniatürünə uzun basıb Redaktə seçməklə və ya tam ekran görünüşündən Redaktə seçməklə keçə bilərsiniz. |
| 245 | `faq_11_title` | Can I somehow group media file thumbnails? | Media fayllarının miniatürlərini necəsə qruplaşdıra bilərəmmi? |
| 246 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Əlbəttə, miniatürlər görünüşündə \"Qruplaşdırma\" menyu bəndindən istifadə edin. Faylları Çəkilmə tarixi daxil olmaqla bir neçə meyara görə qruplaşdıra bilərsiniz. \"Bütün qovluqların məzmununu göstər\" funksiyasından istifadə etsəniz, onları qovluqlara görə də qruplaşdıra bilərsiniz. |
| 247 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | Çəkilmə tarixinə görə sıralama düzgün işləmir, necə düzəldə bilərəm? |
| 248 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Bu, çox güman ki, faylların haradansa köçürülməsindən qaynaqlanır. Fayl miniatürlərini seçib \"Çəkilmə tarixi dəyərini düzəlt\" seçimini etməklə bunu düzəldə bilərsiniz. |
| 249 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Şəkillərdə rəng zolaqlanması görürəm. Keyfiyyəti necə artıra bilərəm? |
| 250 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | Şəkillərin göstərilməsi üçün mövcud həll əksər hallarda yaxşı işləyir, lakin daha yüksək görüntü keyfiyyəti istəyirsinizsə, tətbiq parametrlərində \"Dərin yaxınlaşdırılan şəkillər\" bölməsində \"Şəkilləri mümkün olan ən yüksək keyfiyyətdə göstər\" seçimini aktivləşdirə bilərsiniz. |
| 251 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Bir faylı/qovluğu gizlətmişəm. Onu necə yenidən görünən edə bilərəm? |
| 252 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Gizli elementi görmək üçün əsas ekranda \"Gizli elementləri müvəqqəti göstər\" menyu bəndinə toxuna və ya tətbiq parametrlərində \"Gizli elementləri göstər\" seçimini aktivləşdirə bilərsiniz. Gizlətməni ləğv etmək üçün üzərinə uzun basıb \"Gizlətməni ləğv et\" seçin. Qovluqlar içinə gizli \".nomedia\" faylı əlavə edilərək gizlədilir, həmin faylı istənilən fayl meneceri ilə də silə bilərsiniz. Unutmayın ki, gizlətmə rekursiv işləyir; yəni bir qovluğu gizlətsəniz, bütün alt qovluqlar da gizlənir. Ona görə alt qovluqları görünən etmək üçün üst qovluğun gizlədilməsini ləğv etməlisiniz. |
| 253 | `faq_15_title` | Why does the app take up so much space? | Tətbiq niyə bu qədər çox yer tutur? |
| 254 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | Tətbiq keşi 250 MB-a qədər yer tuta bilər, bu, şəkillərin daha sürətli yüklənməsini təmin edir. Tətbiq bundan daha çox yer tutursa, böyük ehtimalla zibil qutusunda elementləriniz var. Həmin fayllar tətbiqin ölçüsünə daxil edilir. Zibil qutusunu açıb bütün faylları silməklə və ya tətbiq parametrlərindən onu təmizləyə bilərsiniz. Zibil qutusundakı hər fayl 30 gündən sonra avtomatik silinir. |
| 255 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | Fayl və qovluq gizlətməyə nə oldu və niyə artıq gizli elementləri görə bilmirəm? |
| 256 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | Android 11-dən başlayaraq sistem məhdudiyyətlərinə görə artıq fayl və ya qovluqları gizlədə, gizlətməsini ləğv edə və ya qalereya tətbiqlərində gizli olanları görə bilmirsiniz. Bunun üçün hansısa fayl menecerindən istifadə etməli olacaqsınız. |
| 257 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | Və ya cihaz parametrləri vasitəsilə bu qalereyaya Bütün Fayllara giriş icazəsi verə bilərsiniz; bu, bizə gizli elementləri göstərməyə və ümumilikdə fayl əməliyyatlarını daha etibarlı etməyə imkan verəcək. |
| 258 | `faq_17_title` | Why cannot I include missing folders anymore? | Niyə artıq görünməyən qovluqları daxil edə bilmirəm? |
| 259 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Bu da Android 11 ilə gələn sistem dəyişikliklərinə görə işləməyi dayandırdı; tətbiq artıq real qovluqlara baxa bilmir və məlumatları almaq üçün MediaStore adlanan mexanizmə əsaslanır. |
| 260 | `faq_18_title` | Why do I see ads during video playback? | Video oxudularkən niyə reklamlar görürəm? |
| 261 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Tətbiqlərimizdə heç bir reklam yoxdur. Video oxudularkən reklam görürsünüzsə, başqa bir tətbiqin video pleyerindən istifadə edirsiniz. Cihaz parametrlərində susmaya görə video pleyerinizi tapıb \"Susmaya görə tənzimləmələri təmizlə\" əməliyyatını edin. Növbəti dəfə video açdıqda istifadə etmək istədiyiniz tətbiqi seçə biləcəyiniz bir tətbiq seçicisi görünəcək. |
| 262 | `create_virtual_album` | Create virtual album | Virtual albom yarat |
| 263 | `new_virtual_album` | New album… | Yeni albom… |
| 264 | `rename_virtual_album` | Rename album | Albomun adını dəyiş |
| 265 | `album_name` | Album name | Albom adı |
| 266 | `virtual_folder_name` | Virtual folder name | Virtual qovluq adı |
| 267 | `virtual_album_name_empty` | The album name cannot be empty | Albom adı boş ola bilməz |
| 268 | `virtual_album_exists` | An album with this name already exists | Bu adda albom artıq mövcuddur |
| 269 | `add_to_virtual_album` | Add to album | Alboma əlavə et |
| 270 | `remove_from_virtual_album` | Remove from album | Albomdan çıxar |
| 271 | `added_to_virtual_album` | Added to \"%s\" | \"%s\" albomuna əlavə edildi |
| 272 | `already_in_virtual_album` | Already in \"%s\" | Artıq \"%s\" albomundadır |
| 273 | `removed_from_virtual_album` | Removed from the album | Albomdan çıxarıldı |
| 274 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | %s albomu silinsin? Yalnız albom silinir, foto və videolarınız olduğu yerdə qalır. |
| 275 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Bu albom boşdur. İstənilən qovluqda elementləri seçin və \"Köçür\" seçimindən istifadə edin. |
| 276 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Bu əməliyyat virtual albomlar üçün əlçatan deyil |
| 277 | `alphanumeric_password` | Password | Parol |
| 278 | `enter_password` | Enter password | Parolu daxil edin |
| 279 | `repeat_password` | Repeat password | Parolu təkrarlayın |
| 280 | `wrong_password` | Wrong password | Yanlış parol |
| 281 | `encrypting_files` | Encrypting files… | Fayllar şifrələnir… |
| 282 | `encrypting_folder` | Encrypting folder… | Qovluq şifrələnir… |
| 283 | `folder_encrypted_successfully` | Folder encrypted successfully | Qovluq uğurla şifrələndi |
| 284 | `file_encrypted_successfully` | File hidden successfully | Fayl uğurla gizlədildi |
| 285 | `files_encrypted_successfully` | Files hidden successfully | Fayllar uğurla gizlədildi |
| 286 | `decrypting_folder` | Decrypting folder… | Qovluğun şifrəsi açılır… |
| 287 | `folder_decrypted_successfully` | Folder decrypted successfully | Qovluğun şifrəsi uğurla açıldı |
| 288 | `file_decrypted_successfully` | File unhidden successfully | Fayl uğurla göstərildi |
| 289 | `files_decrypted_successfully` | Files unhidden successfully | Fayllar uğurla göstərildi |
| 290 | `decrypting_files` | Decrypting files… | Faylların şifrəsi açılır… |
| 291 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Zəhmət olmasa, barmağınızı barmaq izi sensoruna qoyun |
| 292 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Ən azı 4 nöqtəni birləşdirin |
| 293 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | Mövcud Cipher_Secrets qovluğunuza daxil olmaq istəyirsiniz? |
| 294 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Mövcud Cipher_Secrets qovluğu silinəcək və yenisi yaradılacaq. Əminsiniz? |
| 295 | `confirm_checkbox_label` | I confirm | Təsdiqləyirəm |
| 296 | `secret_vault` | Secret Vault | Gizli Seyf |
| 297 | `secret_vault_empty` | Secret Vault is empty | Gizli Seyf boşdur |
| 298 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | Gizli faylın(ların) şifrəsini açmaq istədiyinizə əminsiniz? |
| 299 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Təhlükəsizlik məqsədilə seyf açıq olarkən mübadilə buferinə kopyalama deaktiv edilib. |
| 300 | `on_vault_exit` | On leaving encrypted section | Şifrələnmiş bölmədən çıxarkən |
| 301 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | Şifrələnmiş albomları göstərməyə davam et |
| 302 | `on_vault_exit_hide` | Hide encrypted albums | Şifrələnmiş albomları gizlət |
| 303 | `on_vault_exit_ask_every_time` | Always ask | Hər dəfə soruş |
| 304 | `vault_exit_dialog_title` | Exiting Encrypted Section | Şifrələnmiş Bölmədən Çıxış |
| 305 | `vault_exit_question_single` | Show encrypted album together with other albums? | Şifrələnmiş albom digər albomlarla birlikdə göstərilsin? |
| 306 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? | Şifrələnmiş albomlar digər albomlarla birlikdə göstərilsin? |
| 307 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | Əlavə seçimlər üçün Parametrlər &gt; Şifrələnmiş bölmədən çıxarkən bölməsinə baxın |
| 308 | `stop_showing_hidden` | Stop showing encrypted media | Şifrələnmiş medianı göstərməyi dayandır |
| 309 | `system_default` | System default | Sistem (susmaya görə) |
| 310 | `language` | Language | Dil |
| 311 | `open_system_language_settings` | Open system language settings | Sistem dil parametrlərini aç |
| 312 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Gizli elementlərə biometrika ilə daxil ol |
| 313 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Zəhmət olmasa, əvvəlcə gizli elementlərin parolla qorunmasını aktivləşdirin |
| 314 | `biometric_unlock_vault` | Unlock hidden vault | Gizli seyfin kilidini aç |
| 315 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | Gizli elementlərə daxil olmaq üçün biometrik kimliyinizi təsdiqləyin |
| 316 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | Seyfə biometrik girişi aktivləşdirmək üçün biometrik kimliyinizi təsdiqləyin |
| 317 | `use_password_or_pin` | Use PIN / Password | PIN / Paroldan istifadə et |
| 318 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Biometrik məlumatlar dəyişdirilib. Zəhmət olmasa, PIN və ya parolunuzu daxil edin. |
| 319 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | Bu cihazda biometrik avadanlıq və ya qeydiyyatdan keçmiş məlumat tapılmadı |
| 320 | `default_hidden_album` | Default Hidden Album | Defolt Gizli Albom |
| 321 | `select_hidden_album` | Select Hidden Album | Gizli Albom Seçin |
| 322 | `create_new_hidden_album` | Create New Hidden Album | Yeni Gizli Albom Yarat |
| 323 | `new_hidden_album` | New hidden album | Yeni gizli albom |
| 324 | `hidden_album_name_empty` | Album name cannot be empty | Albom adı boş ola bilməz |
| 325 | `file_hidden_to_album` | File hidden to \"%s\" | Fayl \"%s\" albomuna gizlədildi |
| 326 | `files_hidden_to_album` | %1$d files hidden to \"%2$s\" | %1$d fayl \"%2$s\" albomuna gizlədildi |
| 327 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | %d faylı bərpa etmək istədiyinizə əminsiniz? |
| 328 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | %d faylı bərpa etmək istədiyinizə əminsiniz? |
| 329 | `failed_to_resize_images` (`one`) | Failed to resize %d image | %d şəklin ölçüsünü dəyişmək mümkün olmadı |
| 330 | `failed_to_resize_images` (`other`) | Failed to resize %d images | %d şəklin ölçüsünü dəyişmək mümkün olmadı |
| 331 | `search` *(fossify-commons)* | *(inherited from commons)* | Axtar |
| 332 | `change_date_and_time_format` *(fossify-commons)* | Change date and time format | Tarix və saat formatını dəyiş |
| 333 | `password_protect_file_deletion` *(fossify-commons)* | Password protect file deletion and moving | Faylların silinməsini və köçürülməsini parolla qoru |
| 334 | `keep_last_modified` *(fossify-commons)* | Keep old last-modified value at file operations | Fayl əməliyyatlarında köhnə son dəyişdirilmə dəyərini qoru |
| 335 | `clear_cache` *(fossify-commons)* | Clear cache | Keşi təmizlə |
| 336 | `export_settings` *(fossify-commons)* | Export settings | Parametrləri ixrac et |
| 337 | `import_settings` *(fossify-commons)* | Import settings | Parametrləri idxal et |
| 338 | `temporarily_show_excluded` *(fossify-commons)* | Temporarily show excluded | İstisna edilmişləri müvəqqəti göstər |
| 339 | `change_view_type` *(fossify-commons)* | Change view type | Görünüş növünü dəyiş |
| 340 | `contributors` *(fossify-commons)* | Contributors | Töhfə verənlər |
| 341 | `known_issues` *(fossify-commons)* | Known issues | Məlum problemlər |
| 342 | `donate_to_fossify` *(fossify-commons)* | Donate to Fossify | Fossify-a ianə et |
| 343 | `privacy_policy` *(fossify-commons)* | Privacy policy | Məxfilik siyasəti |
| 344 | `create_shortcut` *(fossify-commons)* | Create shortcut | Qısayol yarat |
| 345 | `print` *(fossify-commons)* | Print | Çap et |
| 346 | `search_files` *(fossify-commons)* | Search files | Faylları axtar |
| 347 | `search_folders` *(fossify-commons)* | Search folders | Qovluqları axtar |
