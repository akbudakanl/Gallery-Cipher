# Norwegian Bokmål (values-nb-rNO) Translation Completion & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-nb-rNO/strings.xml` was audited against `app/src/main/res/values/strings.xml`. Three existing errors/typos were corrected, two multiline placeholders were normalized, and all 91 missing strings were translated into Norwegian Bokmål and added, bringing `values-nb-rNO` to full parity (363 `<string>` + 2 `<plurals>` = 365 app resource entries; `search` and 17 previously overridden shared UI keys from `fossify-commons` plus 4 additional `fossify-commons` UI keys (`exclude_folder`, `column_count`, `open_the_recycle_bin`, `version_placeholder`) newly overridden because they are missing in `fossify-commons` `values-nb-rNO`; 364 total comparison rows with plural forms expanded). Nine keys that were missing from `values-nb-rNO/strings.xml` were translated and added (`number_of_items`, `random`, `shuffle`, `stop_showing_excluded`, `copy_to_clipboard`, `encrypt`, `decrypt`, `encrypt_album`, `decrypt_album`), and eleven existing entries were revised so that files and albums use the new \"encrypt / decrypt\" wording and so that the favorites import/export labels mention virtual albums as well. Subsequently, 22 additional keys were localized in `values-nb-rNO/strings.xml`, `hide_folder_description` was removed, and `default_hidden_album` was marked `translatable="false"` (`Default Secret Album`), bringing `values-nb-rNO/strings.xml` to 384 `<string>` + 2 `<plurals>` = 386 resource entries and 385 total comparison rows.
> **Key Terminology:** `Settings` → `Innstillinger` · `Default` → `Standard` / `Systemstandard` · `Folder` → `Mappe` · `Album` → `Album` · `Encrypted Vault` → `Kryptert hvelv` · `Hidden` → `Skjult` · `Excluded` → `Ekskludert` · `Password` → `Passord` · `Encrypt / Decrypt` → `Krypter / Dekrypter` · `Thumbnails` → `Minibilder` · `Recycle bin` → `Papirkurv` · `Clipboard` → `Utklippstavlen`

## 1. Summary of Revisions & Newly Added Norwegian Bokmål Strings & Translations

### 1.1 Revisions to Previously Existing Translations

| Key (`name`) | Old `values-nb-rNO` | New `values-nb-rNO` | Status / Notes |
|---|---|---|---|
| `no_media_with_filters` | Ingen media-filer er funnet med de valgte filtrene. | Ingen mediafiler er funnet med de valgte filtrene. | **Improved** (Removed stray hyphen to match `mediafiler` in `no_media_add_included`) |
| `change_filters_underlined` | `<u>Endre filtere</u>` | `<u>Endre filtre</u>` | **Fixed** (Correct Bokmål plural of *filter* is *filtre*, not *filtere*) |
| `invalid_values` | Oppfør en gyldig oppløsning | Vennligst oppgi en gyldig oppløsning | **Fixed** (*oppfør* means behave/list; *oppgi* means enter) |
| `included_activity_placeholder` | *(literal newlines around `\n`)* | `...\n\n...` | **Normalized** (Removed stray spaces caused by literal XML newlines) |
| `excluded_activity_placeholder` | *(literal newlines around `\n`)* | `...\n\n...` | **Normalized** (Removed stray spaces caused by literal XML newlines) |

### 1.2 Newly Added Fork-Only Strings (66 Keys)

These 66 strings were introduced by this fork (Gallery-Cipher) and do not exist in the original Fossify Gallery:

| 1 | 1 | `video_double_tap_duration` | Double-tap seek duration | Video settings: double-tap forward/rewind seek interval |
| 2 | 2 | `video_seek_duration_seconds` | %d seconds | Used with durations 3, 5, 10, 15 via String.format |
| 3 | 3 | `create_virtual_album` | Create virtual album | Virtual albums section action |
| 4 | 4 | `new_virtual_album` | New album… | Action label for creating a new virtual album |
| 5 | 5 | `rename_virtual_album` | Rename album | Action label to rename a virtual album |
| 6 | 6 | `album_name` | Album name | Input hint / label for album name |
| 7 | 7 | `virtual_folder_name` | Virtual folder name | Input hint / label for virtual folder |
| 8 | 8 | `virtual_album_name_empty` | The album name cannot be empty | Validation error when album name is empty |
| 9 | 9 | `virtual_album_exists` | An album with this name already exists | Validation error when album name already exists |
| 10 | 10 | `add_to_virtual_album` | Add to album | Menu action to add items to an album |
| 11 | 11 | `remove_from_virtual_album` | Remove from album | Menu action to remove items from an album |
| 12 | 12 | `added_to_virtual_album` | Added to \"%s\" | Toast message preserving \"%s\" |
| 13 | 13 | `already_in_virtual_album` | Already in \"%s\" | Toast message preserving \"%s\" |
| 14 | 14 | `removed_from_virtual_album` | Removed from the album | Toast message after removing items from an album |
| 15 | 15 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | No extra quotes around %s (formatted with album name or count) |
| 16 | 16 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Empty state placeholder referencing "Kopier til" (Copy to) |
| 17 | 17 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Toast message when action is unsupported on virtual albums |
| 18 | 18 | `alphanumeric_password` | Password | Password input label |
| 19 | 19 | `repeat_password` | Repeat password | Password confirmation prompt |
| 20 | 20 | `wrong_password` | Wrong password | Password validation error |
| 21 | 21 | `encrypting_files` | Encrypting files… | Progress message |
| 22 | 22 | `encrypting_folder` | Encrypting folder… | Progress message |
| 23 | 23 | `folder_encrypted_successfully` | Folder encrypted successfully | Success message |
| 24 | 24 | `decrypting_folder` | Decrypting folder… | Progress message |
| 25 | 25 | `folder_decrypted_successfully` | Folder decrypted successfully | Success message |
| 26 | 26 | `decrypting_files` | Decrypting files… | Progress message |
| 27 | 27 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Fingerprint authentication prompt |
| 28 | 28 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Pattern lock validation prompt |
| 29 | 29 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | Keeps literal folder name Cipher_Secrets |
| 30 | 30 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Keeps literal folder name Cipher_Secrets |
| 31 | 31 | `confirm_checkbox_label` | I confirm | Checkbox label, suffixed with countdown (10) in dialog |
| 32 | 32 | `secret_vault` | Encrypted Vault | Vault feature title ("Kryptert hvelv") |
| 33 | 33 | `secret_vault_empty` | Encrypted Vault is empty | Empty state placeholder for Encrypted Vault |
| 34 | 34 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | Confirmation prompt for singular/plural file(s) |
| 35 | 35 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Security notice while vault is unlocked |
| 36 | 36 | `on_vault_exit` | On leaving encrypted section | Settings label for behavior when leaving encrypted section |
| 37 | 37 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | Radio option |
| 38 | 38 | `on_vault_exit_hide` | Hide encrypted albums | Radio option |
| 39 | 39 | `on_vault_exit_ask_every_time` | Always ask | Radio option |
| 40 | 40 | `vault_exit_dialog_title` | Exiting Encrypted Section | Dialog title |
| 41 | 41 | `vault_exit_question_single` | Show encrypted album together with other albums?\n\nIn this case, during the app session, you will need to unlock the hidden vault whenever you send the app to the background and return to it, or turn the screen off and back on. | Dialog question (single album) |
| 42 | 42 | `vault_exit_question_multiple` | Show encrypted albums together with other albums?\n\nIn this case, during the app session, you will need to unlock the hidden vault whenever you send the app to the background and return to it, or turn the screen off and back on. | Dialog question (multiple albums) |
| 43 | 43 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | References Innstillinger > Når du forlater kryptert seksjon |
| 44 | 63 | `open_system_language_settings` | Open system language settings | Fork-only addition: opens the device language picker (not in the original Fossify Gallery) |
| 45 | 44 | `file_encrypted_successfully` | File encrypted successfully | Filen ble kryptert                           |
| 46 | 45 | `files_encrypted_successfully` | Files encrypted successfully | Filene ble kryptert                           |
| 47 | 46 | `file_decrypted_successfully` | File decrypted successfully | Filen ble dekryptert                         |
| 48 | 47 | `files_decrypted_successfully` | Files decrypted successfully | Filene ble dekryptert                         |
| 49 | 48 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Settings switch, setup prompt title, and vault security dialog button |
| 50 | 49 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Toast shown when enabling biometrics before hidden-item password protection |
| 51 | 50 | `biometric_unlock_vault` | Unlock encrypted vault | BiometricPrompt title when unlocking the encrypted vault |
| 52 | 51 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | BiometricPrompt subtitle when unlocking hidden items |
| 53 | 52 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | BiometricPrompt subtitle when enabling biometric vault access |
| 54 | 53 | `use_password_or_pin` | Use PIN / Password | BiometricPrompt negative button to fall back to PIN/password |
| 55 | 54 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Toast shown when biometric credentials changed and Keystore key invalidated |
| 56 | 55 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | Toast shown when no biometric hardware or enrolled credentials exist |
| 57 | 56 | `default_hidden_album` | Default Hidden Album | Settings / picker label for default hidden album |
| 58 | 57 | `select_hidden_album` | Select Encrypted Album | Velg kryptert album                                  |
| 59 | 58 | `create_new_hidden_album` | Create New Encrypted Album | Opprett nytt kryptert album                  |
| 60 | 59 | `new_hidden_album` | New encrypted album | Nytt kryptert album                                  |
| 61 | 60 | `hidden_album_name_empty` | Album name cannot be empty | Validation error when hidden album name is blank |
| 62 | 61 | `file_hidden_to_album` | File encrypted to \"%s\" | Fil kryptert i «%s»                                          |
| 63 | 62 | `files_hidden_to_album` | %1$d files encrypted to \"%2$s\" | %1$d filer kryptert i «%2$s»                                  |
| 64 | `pull_down_to_open_vault` | Keep pulling down for Encrypted Vault | Fortsett å dra ned for kryptert hvelv | Vault pull gesture hint shown while the pull is below the open threshold |
| 65 | `keep_holding_to_open_vault` | Keep holding to open Encrypted Vault | Fortsett å holde for å åpne kryptert hvelv | Vault pull gesture hint shown once the pull passes the threshold, before the 2.5 s hold completes |
| 66 | `release_to_open_vault` | Release to open Encrypted Vault | Slipp for å åpne kryptert hvelv | Vault pull gesture hint shown after holding the pull for 2.5 s; releasing opens the vault |

### 1.3 Newly Added Translations for Strings Already Present in the Original Fossify Gallery (45 Keys)

These 45 strings already exist in the original Fossify Gallery (upstream); only the translations below were newly added in this fork:

| # | Key (`name`) | English (`values/strings.xml`) | Norwegian Bokmål (`values-nb-rNO/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Hvis en mappe eller en av dens overordnede mapper har et punktum foran navnet, er den skjult og kan ikke vises igjen på denne måten. Du må fjerne punktumet ved å gi den nytt navn. | Explanation of dot-prefixed hidden folders |
| 2 | `fade` | Fade | Uttoning | Slideshow transition animation option |
| 3 | `slide` | Slide | Glidning | Slideshow transition animation option |
| 4 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Vis Ultra HDR-bilder i HDR | Media settings: render Ultra HDR photos in HDR |
| 5 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Noe gikk galt. Gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. | Error fallback directing user to Android Special app access settings |
| 6 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis omdirigeringen ikke fungerer, gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. | Redirect fallback note for Media management apps permission |
| 7 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis du ikke vil gjøre det, kan du også gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. | Alternative instructions for Media management apps permission |
| 8 | `failed_to_load_media` | Failed to load media. | Kunne ikke laste inn media. | Media viewer error message |
| 9 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | Filen er ugyldig eller ødelagt. | Media player/viewer error message |
| 10 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Mediet overstiger enhetens kapasitet. | Media player/viewer error message |
| 11 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | Hvordan kan jeg gjøre Fossify Gallery til enhetens standardgalleri? | FAQ 1 question |
| 12 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\".         The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Først må du finne det nåværende standardgalleriet i Apper-delen av enhetsinnstillingene, se etter en knapp som heter noe slikt som \"Åpne som standard\", trykke på den og deretter velge \"Fjern standardvalg\".\nNeste gang du prøver å åpne et bilde eller en video, skal du se en appvelger der du kan velge Fossify Gallery og gjøre den til standardapp. | FAQ 1 answer; matches standard Norwegian Android settings labels |
| 13 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Jeg låste appen med et passord, men jeg har glemt det. Hva kan jeg gjøre? | FAQ 2 question |
| 14 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Du kan løse det på to måter. Du kan enten installere appen på nytt, eller finne appen i enhetsinnstillingene og velge \"Slett data\". Det vil tilbakestille alle innstillingene dine, men det vil ikke fjerne noen mediafiler. | FAQ 2 answer |
| 15 | `faq_3_title` | How can I make an album always appear at the top? | Hvordan kan jeg få et album til å alltid vises øverst? | FAQ 3 question |
| 16 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Du kan trykke og holde på ønsket album og velge Fest-ikonet i handlingsmenyen for å feste det til toppen. Du kan også feste flere mapper; festede elementer blir sortert etter standard sorteringsmetode. | FAQ 3 answer; matches "Fest" terminology |
| 17 | `faq_4_title` | How can I fast forward videos? | Hvordan kan jeg spole fremover i videoer? | FAQ 4 question |
| 18 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Du kan gjøre det ved å dobbelttrykke på siden av skjermen, eller trykke på teksten for nåværende eller maksimal varighet nær søkelinjen. Hvis du aktiverer avspilling av videoer på en separat skjerm i appinnstillingene, kan du også bruke horisontale bevegelser. | FAQ 4 answer |
| 19 | `faq_5_title` | What is the difference between hiding and excluding a folder? | Hva er forskjellen mellom å skjule og å ekskludere en mappe? | FAQ 5 question |
| 20 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Ekskluder hindrer at mappen vises kun i Fossify Gallery, mens Skjul fungerer på systemnivå og skjuler mappen også for andre gallerier. Det fungerer ved å opprette en tom \".nomedia\"-fil i den aktuelle mappen, som du også kan fjerne med en hvilken som helst filbehandler. Merk at enkelte enheter ikke tillater å skjule mapper som Camera, Screenshots og Downloads. | FAQ 5 answer; keeps folder names Camera, Screenshots, Downloads literal |
| 21 | `faq_6_title` | Why do folders with music cover art or stickers show up? | Hvorfor vises mapper med plateomslag eller klistremerker? | FAQ 6 question |
| 22 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Det kan skje at uvanlige album dukker opp. Du kan enkelt ekskludere dem ved å trykke og holde på dem og velge Ekskluder. I den neste dialogen kan du så velge den overordnede mappen, noe som sannsynligvis også vil hindre at andre relaterte album vises. | FAQ 6 answer |
| 23 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | En mappe med bilder vises ikke, eller den viser ikke alle elementer. Hva kan jeg gjøre? | FAQ 7 question |
| 24 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Det kan ha flere årsaker, men det er enkelt å løse. Bare gå til Innstillinger -&gt; Håndter inkluderte mapper, velg Pluss og naviger til ønsket mappe. | FAQ 7 answer; matches "Håndter inkluderte mapper" |
| 25 | `faq_8_title` | What if I want just a few particular folders visible? | Hva om jeg bare vil at noen bestemte mapper skal være synlige? | FAQ 8 question |
| 26 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders.         That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Å legge til en mappe under Inkluderte mapper ekskluderer ikke automatisk noe annet. Det du kan gjøre er å gå til Innstillinger -&gt; Håndter ekskluderte mapper, ekskludere rotmappen \"/\", og deretter legge til ønskede mapper under Innstillinger -&gt; Håndter inkluderte mapper.\nDet vil gjøre bare de valgte mappene synlige, siden både ekskludering og inkludering er rekursive, og hvis en mappe både er ekskludert og inkludert, vil den vises. | FAQ 8 answer; matches "Håndter ekskluderte/inkluderte mapper" |
| 27 | `faq_10_title` | Can I crop images with this app? | Kan jeg beskjære bilder med denne appen? | FAQ 10 question |
| 28 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Ja, du kan beskjære bilder i redigeringsverktøyet ved å dra i bildehjørnene. Du kommer til redigeringsverktøyet enten ved å trykke og holde på et minibilde og velge Rediger, eller ved å velge Rediger fra fullskjermvisningen. | FAQ 10 answer |
| 29 | `faq_11_title` | Can I somehow group media file thumbnails? | Kan jeg på noen måte gruppere minibilder av mediafiler? | FAQ 11 question |
| 30 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Selvfølgelig, bare bruk menyelementet \"Grupper etter\" når du er i minibildevisningen. Du kan gruppere filer etter flere kriterier, inkludert Dato tatt. Hvis du bruker funksjonen \"Vis alle mappers innhold\", kan du også gruppere dem etter mapper. | FAQ 11 answer; matches "Grupper etter", "Dato tatt", "Vis alle mappers innhold" |
| 31 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | Sortering etter Dato tatt ser ikke ut til å fungere skikkelig, hvordan kan jeg fikse det? | FAQ 12 question |
| 32 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Det skyldes sannsynligvis at filene er kopiert fra et annet sted. Du kan fikse det ved å merke minibildene og velge \"Korriger Dato tatt-verdi\". | FAQ 12 answer; matches "Korriger Dato tatt-verdi" and "minibilder" |
| 33 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Jeg ser fargebånd på bildene. Hvordan kan jeg forbedre kvaliteten? | FAQ 13 question |
| 34 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | Den nåværende løsningen for bildevisning fungerer fint i de aller fleste tilfeller, men hvis du ønsker enda bedre bildekvalitet, kan du aktivere \"Vis bilder i høyest mulig kvalitet\" i appinnstillingene under delen \"Dyp zoombare bilder\". | FAQ 13 answer; matches "Vis bilder i høyest mulig kvalitet" and "Dyp zoombare bilder" |
| 35 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Jeg har skjult en fil/mappe. Hvordan kan jeg gjøre den synlig igjen? | FAQ 14 question |
| 36 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Du kan enten trykke på menyelementet \"Vis skjulte elementer midlertidig\" på hovedskjermen, eller slå på \"Vis skjulte elementer\" i appinnstillingene for å se det skjulte elementet. Hvis du vil gjøre det synlig igjen, trykker og holder du bare på det og velger \"Vis igjen\". Mapper skjules ved å legge til en skjult \".nomedia\"-fil i dem; du kan også slette denne filen med en hvilken som helst filbehandler. Merk imidlertid at skjuling fungerer rekursivt, så hvis du skjuler en mappe, blir alle undermapper også skjult. For å gjøre undermappene synlige igjen må du derfor gjøre den overordnede mappen synlig igjen. | FAQ 14 answer |
| 37 | `faq_15_title` | Why does the app take up so much space? | Hvorfor tar appen opp så mye plass? | FAQ 15 question |
| 38 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | Appens hurtigbuffer kan ta opptil 250 MB; den sørger for raskere innlasting av bilder. Hvis appen tar opp enda mer plass, skyldes det sannsynligvis at du har elementer i papirkurven. Disse filene teller med i appstørrelsen. Du kan tømme papirkurven ved å åpne den og slette alle filer, eller fra appinnstillingene. Hver fil i papirkurven slettes automatisk etter 30 dager. | FAQ 15 answer; matches "papirkurven" |
| 39 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | Hva skjedde med skjuling av filer og mapper, og hvorfor kan jeg ikke se skjulte elementer lenger? | FAQ 16 question (faq_16_text was already present) |
| 40 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | Eller du kan også gi dette galleriet tilgang til Alle filer via enhetsinnstillingene; det lar oss vise skjulte elementer og gjør filoperasjoner mer pålitelige generelt. | FAQ 16 follow-up regarding All Files access |
| 41 | `faq_17_title` | Why cannot I include missing folders anymore? | Hvorfor kan jeg ikke inkludere manglende mapper lenger? | FAQ 17 question |
| 42 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Det sluttet også å fungere på grunn av systemendringene som kom med Android 11; appen kan ikke lenger bla gjennom ekte mapper, men er avhengig av såkalte MediaStore for å hente data. | FAQ 17 answer; keeps MediaStore literal |
| 43 | `faq_18_title` | Why do I see ads during video playback? | Hvorfor ser jeg reklame under videoavspilling? | FAQ 18 question |
| 44 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Appene våre har ingen reklame overhodet. Hvis du ser reklame under videoavspilling, bruker du sannsynligvis videospilleren til en annen app. Prøv å finne standard videospiller i enhetsinnstillingene, og velg \"Fjern standardvalg\" på den. Neste gang du åpner en video, vil du se en appvelger der du kan velge hvilken app du vil bruke. | FAQ 18 answer |
| 45 | `enter_password` | Enter password | Skriv inn passord | Password prompt |

### 1.4 `fossify-commons` Inherited UI Strings & Local Overrides (21 Keys)

10 of these 21 shared UI keys are inherited from `org.fossify:commons:7.1.3` (`res/values-nb-rNO/values-nb-rNO.xml`), while `contributors`, `known_issues`, `donate_to_fossify`, `privacy_policy`, `skip_the_recycle_bin`, `exclude_folder`, `column_count`, `open_the_recycle_bin`, and `version_placeholder` were missing and added as local overrides in `app/src/main/res/values-nb-rNO/strings.xml`:

| # | Key (`name`) | English (`fossify-commons`) | Norwegian Bokmål | Source / Notes |
|---:|---|---|---|---|
| 1 | `change_date_and_time_format` | Change date and time format | Endre format for dato og klokkeslett | Inherited from `fossify-commons` (Settings) |
| 2 | `password_protect_file_deletion` | Password protect file deletion and moving | Passordbeskytt sletting og flytting av filer | Inherited from `fossify-commons` (Settings) |
| 3 | `keep_last_modified` | Keep old last-modified value at file operations | Behold sist-endret-verdi ved filoperasjoner | Inherited from `fossify-commons` (Settings) |
| 4 | `clear_cache` | Clear cache | Tøm hurtiglager | Inherited from `fossify-commons` (Settings) |
| 5 | `export_settings` | Export settings | Eksporter innstillinger | Inherited from `fossify-commons` (Settings) |
| 6 | `import_settings` | Import settings | Importer innstillinger | Inherited from `fossify-commons` (Settings) |
| 7 | `temporarily_show_excluded` | Temporarily show excluded | Vis ekskluderte midlertidig | Inherited from `fossify-commons` (Options menu) |
| 8 | `change_view_type` | Change view type | Endre visningstype | Inherited from `fossify-commons` (Options menu) |
| 9 | `contributors` | Contributors | Bidragsytere | Local override in `values-nb-rNO/strings.xml` (About) |
| 10 | `known_issues` | Known issues | Kjente problemer | Local override in `values-nb-rNO/strings.xml` (About) |
| 11 | `donate_to_fossify` | Donate to Fossify | Doner til Fossify | Local override in `values-nb-rNO/strings.xml` (About) |
| 12 | `privacy_policy` | Privacy policy | Personvernerklæring | Local override in `values-nb-rNO/strings.xml` (About) |
| 13 | `create_shortcut` | Create shortcut | Opprett snarvei | Inherited from `fossify-commons` (CAB / ViewPager menu) |
| 14 | `print` | Print | Skriv ut | Inherited from `fossify-commons` (ViewPager menu) |
| 15 | `search_files` | Search files | Søk i filer | Local override in `values-nb-rNO/strings.xml` (Search bar hint) |
| 16 | `search_folders` | Search folders | Søk i mapper | Local override in `values-nb-rNO/strings.xml` (Search bar hint) |
| 17 | `skip_the_recycle_bin` | Skip the recycle bin, delete files directly | Hopp over papirkurven og slett filene direkte | Local override in `values-nb-rNO/strings.xml` (Delete confirmation dialog); key missing in `fossify-commons` `values-nb-rNO` |
| 18 | `exclude_folder` | Exclude folder | Ekskluder mappe | Local override in `values-nb-rNO/strings.xml` (Options menu); key missing in `fossify-commons` `values-nb-rNO` |
| 19 | `column_count` | Column count | Antall kolonner | Local override in `values-nb-rNO/strings.xml` (Main screen and in-album Options menu); key missing in `fossify-commons` `values-nb-rNO` |
| 20 | `open_the_recycle_bin` | Open the recycle bin | Åpne papirkurven | Local override in `values-nb-rNO/strings.xml` (In-album Options menu); key missing in `fossify-commons` `values-nb-rNO` |
| 21 | `version_placeholder` | Version %s | Versjon %s | Local override in `values-nb-rNO/strings.xml` (About screen version text); key missing in `fossify-commons` `values-nb-rNO` |

### Newly Added Missing Translations (9 Keys) & Revised Entries (11 Keys)

Nine keys that were missing from `values-nb-rNO/strings.xml` were translated and added, and eleven existing entries were revised so that files and albums use the new \"encrypt / decrypt\" wording and so that the favorites import/export labels mention virtual albums as well.

| # | Key (`name`) | English (`values/strings.xml`) | Norwegian Bokmål (`values-nb-rNO/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `number_of_items` | Number of items | Antall elementer | Sorting option shown in the album view only; overridden locally because it is missing in `fossify-commons` for this locale |
| 2 | `random` | Random | Tilfeldig | Sorting option offered in every sorting state; overridden locally because it is missing in `fossify-commons` for this locale |
| 3 | `shuffle` | Shuffle | Bland | Fullscreen viewer action that randomizes the media order; app owned string that was only available in English |
| 4 | `stop_showing_excluded` | Stop showing excluded | Slutt å vise ekskluderte | Main screen options-menu action that ends the temporary preview of excluded folders |
| 5 | `copy_to_clipboard` | Copy to clipboard | Kopier til utklippstavlen | Fullscreen viewer action that copies a file to the clipboard |
| 6 | `encrypt` | Encrypt | Krypter | Replaces the inherited `hide` label for files; files and albums are encrypted, not hidden |
| 7 | `decrypt` | Decrypt | Dekrypter | Replaces the inherited `unhide` label for files |
| 8 | `encrypt_album` | Encrypt album | Krypter album | Album (folder) context action that encrypts the album into the vault |
| 9 | `decrypt_album` | Decrypt album | Dekrypter album | Album (folder) context action that decrypts the album back to its original location |
| 10 | `export_favorite_paths` | Export favorites and virtual albums | Eksporter favoritter og virtuelle album | Settings label; renamed because virtual albums are exported together with the favorites |
| 11 | `import_favorite_paths` | Import favorites and virtual albums | Importer favoritter og virtuelle album | Settings label; renamed because virtual albums are imported together with the favorites |
| 12 | `file_encrypted_successfully` | File encrypted successfully | Filen ble kryptert | Success toast after encrypting a single file |
| 13 | `files_encrypted_successfully` | Files encrypted successfully | Filene ble kryptert | Success toast after encrypting multiple files |
| 14 | `file_decrypted_successfully` | File decrypted successfully | Filen ble dekryptert | Success toast after decrypting a single file |
| 15 | `files_decrypted_successfully` | Files decrypted successfully | Filene ble dekryptert | Success toast after decrypting multiple files |
| 16 | `file_hidden_to_album` | File encrypted to \"%s\" | Fil kryptert i «%s» | Success toast after encrypting a single file into album `%s` |
| 17 | `files_hidden_to_album` | %1$d files encrypted to \"%2$s\" | %1$d filer kryptert i «%2$s» | Success toast after encrypting `%1$d` files into album `%2$s` |
| 18 | `select_hidden_album` | Select Encrypted Album | Velg kryptert album | Dialog title for choosing the target encrypted album |
| 19 | `create_new_hidden_album` | Create New Encrypted Album | Opprett nytt kryptert album | Action label to create a new encrypted album |
| 20 | `new_hidden_album` | New encrypted album | Nytt kryptert album | Input hint / default label for a new encrypted album |

### Newly Added UI, Backup, Encryption Confirmation & Vault Strings (22 Keys)

Following the previous audit, 22 additional keys were localized in `values-nb-rNO/strings.xml` (`create_new_encrypted_virtual_album` and `new_album` for vault destination dialogs, plus 20 strings covering backup export/import, auto-backup prompt, encryption confirmations, delete-behavior options, album lock/unlock labels, and progress notifications; `hide_folder_description` was removed from the app, and `default_hidden_album` was marked `translatable="false"` as `Default Secret Album`):

| # | Key (`name`) | English (`values/strings.xml`) | Norwegian Bokmål (`values-nb-rNO/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `create_new_encrypted_virtual_album` | Create new encrypted virtual album | Opprett nytt kryptert virtuelt album | Dialog option for creating an encrypted virtual album inside the vault |
| 2 | `new_album` | New album | Nytt album | Compact neutral button label in the move destination dialog |
| 3 | `filename_without_zip` | Filename (without .zip) | Filnavn (uten .zip) | Input hint in the export favorites and virtual albums dialog |
| 4 | `favorites_already_exist` | You already have a favorites list | Du har allerede en favorittliste | Dialog title when importing favorites while a favorites list already exists |
| 5 | `delete_existing_list` | Delete existing list | Slett eksisterende liste | Radio option to replace the existing favorites list on import |
| 6 | `merge_lists` | Merge lists | Slå sammen lister | Radio option to merge imported favorites with the existing list |
| 7 | `confirm` | Confirm | Bekreft | Positive button label in the favorites import conflict dialog |
| 8 | `confirm_encrypt_album` | Are you sure you want to encrypt this album? All of its media files will be encrypted and moved to the encrypted vault. The original files will be deleted. | Er du sikker på at du vil kryptere dette albumet? Alle mediefilene i det blir kryptert og flyttet til det krypterte hvelvet. Originalfilene blir slettet. | Confirmation prompt before encrypting a single album into the vault |
| 9 | `confirm_encrypt_albums` | Are you sure you want to encrypt the selected albums? All of their media files will be encrypted and moved to the encrypted vault. The original files will be deleted. | Er du sikker på at du vil kryptere de valgte albumene? Alle mediefilene i dem blir kryptert og flyttet til det krypterte hvelvet. Originalfilene blir slettet. | Confirmation prompt before encrypting multiple selected albums into the vault |
| 10 | `confirm_encrypt_file` | Are you sure you want to encrypt this file? It will be encrypted and moved to the encrypted vault. The original file will be deleted. | Er du sikker på at du vil kryptere denne filen? Den blir kryptert og flyttet til det krypterte hvelvet. Originalfilen blir slettet. | Confirmation prompt before encrypting a single file into the vault |
| 11 | `confirm_encrypt_files` | Are you sure you want to encrypt the selected files? They will be encrypted and moved to the encrypted vault. The original files will be deleted. | Er du sikker på at du vil kryptere de valgte filene? De blir kryptert og flyttet til det krypterte hvelvet. Originalfilene blir slettet. | Confirmation prompt before encrypting multiple selected files into the vault |
| 12 | `confirm_encrypt_files_to_vault` | The selected files will be encrypted when they are copied into the encrypted vault. Do you want to continue? | De valgte filene blir kryptert når de kopieres til det krypterte hvelvet. Vil du fortsette? | Confirmation prompt when copying unencrypted files into an encrypted vault destination |
| 13 | `encrypt_decrypt_notifications` | Encrypt / Decrypt | Krypter / Dekrypter | Notification channel name for ongoing encrypt/decrypt progress notifications |
| 14 | `allow_batch_renaming` | Allow batch renaming when multiple files are selected | Tillat masseendring av navn når flere filer er valgt | Retained in values/strings.xml for parity (its Settings switch was removed in a6a78d444) |
| 15 | `keep_original_remove_from_virtual_album` | Keep the original file, remove it only from this virtual album | Behold originalfilen, fjern den bare fra dette virtuelle albumet | Checkbox in delete confirmation dialog when deleting from a virtual album |
| 16 | `keep_original_remove_from_favorites` | Keep the original file, remove it only from favorites | Behold originalfilen, fjern den bare fra favoritter | Checkbox in delete confirmation dialog when deleting from favorites |
| 17 | `delete_original_from_favorites_and_virtual_albums` | Delete the original version of a file when it is deleted from favorites and virtual albums | Slett originalversjonen av en fil når den slettes fra favoritter og virtuelle album | Settings switch controlling default delete behavior from favorites and virtual albums |
| 18 | `cannot_target_encrypted_album` | Encrypted albums cannot be targeted for this operation | Krypterte album kan ikke velges som mål for denne handlingen | Toast shown when an encrypted album cannot be targeted for an operation |
| 19 | `cannot_mix_encrypted_and_unencrypted` | Cannot combine encrypted and unencrypted items in this operation | Kan ikke kombinere krypterte og ukrypterte elementer i denne handlingen | Toast shown when an operation attempts to combine encrypted and unencrypted items |
| 20 | `lock_album` | Lock album | Lås album | Album selection menu action replacing lock_folder |
| 21 | `unlock_album` | Unlock album | Lås opp album | Album selection menu action replacing unlock_folder |
| 22 | `auto_backup_prompt_message` | Do you want your favorites and virtual albums to be regularly written to system storage outside of app memory? This will ensure that you do not lose your favorites list and virtual albums if you uninstall the app. You can manually create a backup from app settings at any time. | Vil du at favorittene og de virtuelle albumene dine skal lagres regelmessig i systemlagringen utenfor appminnet? Dette sikrer at du ikke mister favorittlisten og de virtuelle albumene dine hvis du avinstallerer appen. Du kan når som helst opprette en sikkerhetskopi manuelt fra appinnstillingene. | Full-screen prompt asking whether to regularly back up favorites and virtual albums to shared storage |

## 2. Full 1-to-1 Comparison: English vs. Norwegian Bokmål

| # | Key (`name`) | English (`values/strings.xml`) | Norwegian Bokmål (`values-nb-rNO/strings.xml`) |
|---:|---|---|---|
| 1 | `app_launcher_name` | Gallery | Galleri |
| 2 | `edit` | Edit | Rediger |
| 3 | `open_camera` | Open camera | Åpne kamera |
| 4 | `hidden` | (hidden) | (skjult) |
| 5 | `excluded` | (excluded) | (ekskludert) |
| 6 | `pin_folder` | Pin folder | Fest mappe |
| 7 | `unpin_folder` | Unpin folder | Løsne mappe |
| 8 | `pin_to_the_top` | Pin to the top | Fest til toppen |
| 9 | `show_all` | Show all folders content | Vis alle mappers innhold |
| 10 | `all_folders` | All folders | Alle mapper |
| 11 | `folder_view` | Switch to folder view | Skift til mappevisning |
| 12 | `other_folder` | Other folder | Annen mappe |
| 13 | `show_on_map` | Show on map | Vis på kart |
| 14 | `unknown_location` | Unknown location | Ukjent plassering |
| 15 | `volume` | Volume | Lydstyrke |
| 16 | `brightness` | Brightness | Lysstyrke |
| 17 | `lock_orientation` | Lock orientation | Lås skjermorientering |
| 18 | `unlock_orientation` | Unlock orientation | Lås opp skjermorientering |
| 19 | `change_orientation` | Change orientation | Endre orientering |
| 20 | `force_portrait` | Force portrait | Framtving portrett |
| 21 | `force_landscape` | Force landscape | Framtving landskap |
| 22 | `force_landscape_reverse` | Force landscape (reverse) | Framtving landskap (omvendt) |
| 23 | `use_default_orientation` | Use default orientation | Bruk standardorientering |
| 24 | `fix_date_taken` | Fix Date Taken value | Korriger Dato tatt-verdi |
| 25 | `fixing` | Fixing… | Korrigerer… |
| 26 | `dates_fixed_successfully` | Dates fixed successfully | Datoer er korrigerte |
| 27 | `no_date_takens_found` | No Date Taken values have been found | Ingen verdier for Dato tatt er funnet |
| 28 | `share_resized` | Share a resized version | Del versjon med endret størrelse |
| 29 | `switch_to_file_search` | Switch to file search across all visible folders | Skift til filsøk i alle synlige mapper |
| 30 | `set_as_default_folder` | Set as default folder | Sett som standardmappe |
| 31 | `unset_as_default_folder` | Unset as default folder | Ikke lenger sett som standardmappe |
| 32 | `reorder_by_dragging` | Reorder folders by dragging | Endre mapperekkefølge ved å dra |
| 33 | `reorder_by_dragging_pro` | Reorder folders by dragging (Pro) | Endre mapperekkefølge ved å dra (Pro) |
| 34 | `restore_to_path` | Restoring to \'%s\' | Gjenoppretter til \'%s\' |
| 35 | `full_storage_permission_required` | Fossify Gallery needs full access to display all your photos and videos. Go to Settings &gt; Permissions &gt; Photos and videos &gt; Allow all. | Fossify Gallery trenger full tilgang for å vise alle dine bilder og videoer. Gå til Innstillinger &gt; Tillatelser &gt; Bilder og videoer &gt; Tillat alle. |
| 36 | `filter_media` | Filter media | Filtrer media |
| 37 | `images` | Images | Bilder |
| 38 | `videos` | Videos | Videoer |
| 39 | `gifs` | GIFs | GIF-bilder |
| 40 | `raw_images` | RAW images | RAW-format-bilder |
| 41 | `svgs` | SVGs | SVG-bilder |
| 42 | `portraits` | Portraits | Portretter |
| 43 | `no_media_with_filters` | No media files have been found with the selected filters. | Ingen mediafiler er funnet med de valgte filtrene. |
| 44 | `change_filters_underlined` | <u>Change filters</u> | <u>Endre filtre</u> |
| 45 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | Dette vil ekskludere valgte sammen med undermapper, bare fra Fossify Gallery. Ekskluderte mapper kan håndteres i Innstillinger. |
| 46 | `exclude_folder_parent` | Exclude a parent instead? | Ekskludere en overordnet isteden\? |
| 47 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | Ekskludering av mapper vil gjøre dem sammen med deres undermapper, skjulte bare i Fossify Gallery. De vil fortsatt være synlige i andre apper.\n\nHvis du vil skjule dem fra andre apper også, bruk Skjul-funksjonen. |
| 48 | `hidden_folders` | Hidden folders | Skjulte mapper |
| 49 | `manage_hidden_folders` | Manage hidden folders | Håndter skjulte mapper |
| 50 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | Ser ut til at du ikke har skjult noen mapper med en \".nomedia\"-fil. |
| 51 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | Du må gi appen tilgang til alle filer for å se skjulte filer, ellers fungerer det ikke. |
| 52 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Hvis en mappe eller en av dens overordnede mapper har et punktum foran navnet, er den skjult og kan ikke vises igjen på denne måten. Du må fjerne punktumet ved å gi den nytt navn. |
| 53 | `include_folders` | Included folders | Inkluderte mapper |
| 54 | `manage_included_folders` | Manage included folders | Håndter inkluderte mapper |
| 55 | `add_folder` | Add folder | Legg til mappe |
| 56 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | Hvis du har noen mapper som inneholder media, men ikke ble gjenkjent av appen, kan du legge dem til manuelt her.\n\nÅ legge til noen elementer her, ekskluderer ikke noen annen mappe. |
| 57 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | Ingen mediafiler er funnet. Dette kan løses ved å legge til mapper som inneholder mediafiler manuelt. |
| 58 | `resize_and_save` | Resize selection and save | Endre størrelse og lagre |
| 59 | `width` | Width | Bredde |
| 60 | `height` | Height | Høyde |
| 61 | `keep_aspect_ratio` | Keep aspect ratio | Behold sideforhold |
| 62 | `invalid_values` | Please enter a valid resolution | Vennligst oppgi en gyldig oppløsning |
| 63 | `resize_multiple_images` | Resize multiple images | Endre størrelse på flere bilder |
| 64 | `resize_factor` | Resize factor | Faktor for størrelsesendring |
| 65 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | Endre størrelse på bilder til den angitte prosenten, verdi må være mellom 10 og 90. |
| 66 | `resize_factor_error` | Enter a number between 10 and 90 | Angi et tall mellom 10 og 90 |
| 67 | `images_resized_successfully` | Images resized successfully | Størrelsen på bildene endret |
| 68 | `editor` | Editor | Redigering |
| 69 | `basic_editor` | Basic Editor | Enkel redigering |
| 70 | `advanced_editor` | Advanced Editor | Avansert redigering |
| 71 | `rotate` | Rotate | Roter |
| 72 | `invalid_image_path` | Invalid image path | Ugyldig bildebane |
| 73 | `invalid_video_path` | Invalid video path | Ugyldig videobane |
| 74 | `image_editing_failed` | Image editing failed | Bilderedigering feilet |
| 75 | `video_editing_failed` | Video editing failed | Videoredigering feilet |
| 76 | `image_editing_cancelled` | Image editing cancelled | Bilderedigering avbrutt |
| 77 | `video_editing_cancelled` | Video editing cancelled | Videoredigering avbrutt |
| 78 | `file_edited_successfully` | File edited successfully | Fil vellykket redigert |
| 79 | `image_edited_successfully` | Image edited successfully | Bilde vellykket redigert |
| 80 | `video_edited_successfully` | Video edited successfully | Video vellykket redigert |
| 81 | `edit_image_with` | Edit image with: | Rediger bilde med: |
| 82 | `edit_video_with` | Edit video with: | Rediger video med: |
| 83 | `no_image_editor_found` | No image editor found | Ingen bilderedigeringsapp funnet |
| 84 | `no_video_editor_found` | No video editor found | Ingen videoredigeringsapp funnet |
| 85 | `unknown_file_location` | Unknown file location | Ukjent filplassering |
| 86 | `overwrite_original` | Overwrite original | Overskriv original |
| 87 | `error_saving_file` | Could not overwrite the source file | Kunne ikke overskrive kildefilen |
| 88 | `rotate_left` | Rotate left | Roter til venstre |
| 89 | `rotate_right` | Rotate right | Roter til høyre |
| 90 | `rotate_one_eighty` | Rotate by 180º | Roter med 180º |
| 91 | `transform` | Transform | Transformer |
| 92 | `crop` | Crop | Beskjær |
| 93 | `draw` | Draw | Tegn |
| 94 | `flip` | Flip | Speilvend |
| 95 | `flip_horizontally` | Flip horizontally | Speilvend horisontalt |
| 96 | `flip_vertically` | Flip vertically | Speilvend vertikalt |
| 97 | `free_aspect_ratio` | Free | Fri |
| 98 | `other_aspect_ratio` | Other | Annen |
| 99 | `wallpaper` | Wallpaper | Bakgrunnsbilde |
| 100 | `set_as_wallpaper` | Set as Wallpaper | Sett som bakgrunnsbilde |
| 101 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | Å sette som bakgrunnsbilde feilet |
| 102 | `set_as_wallpaper_with` | Set as wallpaper with: | Sett som bakgrunnsbilde med: |
| 103 | `setting_wallpaper` | Setting wallpaper… | Stiller inn bakgrunnsbilde… |
| 104 | `wallpaper_set_successfully` | Wallpaper set successfully | Bakgrunnsbilde vellykket stilt inn |
| 105 | `portrait_aspect_ratio` | Portrait aspect ratio | Portrett-sideforhold |
| 106 | `landscape_aspect_ratio` | Landscape aspect ratio | Landskap-sideforhold |
| 107 | `home_screen` | Home screen | Startskjerm |
| 108 | `lock_screen` | Lock screen | Låseskjerm |
| 109 | `home_and_lock_screen` | Home and lock screen | Start- og låseskjerm |
| 110 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | Tillat endring av sideforholdet |
| 111 | `slideshow` | Slideshow | Lysbildeshow |
| 112 | `interval` | Interval | Intervall |
| 113 | `include_photos` | Include photos | Inkluder bilder |
| 114 | `include_videos` | Include videos | Inkluder videoer |
| 115 | `include_gifs` | Include GIFs | Inkluder GIF-bilder |
| 116 | `random_order` | Random order | Tilfeldig rekkefølge |
| 117 | `move_backwards` | Move backwards | Avspill bakover |
| 118 | `loop_slideshow` | Loop slideshow | Gjenta lysbildeshow |
| 119 | `animation` | Animation | Animasjon |
| 120 | `no_animation` | None | Ingen |
| 121 | `fade` | Fade | Uttoning |
| 122 | `slide` | Slide | Glidning |
| 123 | `slideshow_ended` | The slideshow ended | Lysbildeshowet er slutt |
| 124 | `no_media_for_slideshow` | No media for the slideshow have been found | Ingen media for lysbildeshowet er funnet |
| 125 | `group_direct_subfolders` | Group direct subfolders | Grupper direkte undermapper |
| 126 | `group_by` | Group by | Grupper etter |
| 127 | `do_not_group_files` | Do not group files | Ikke grupper filer |
| 128 | `by_folder` | Folder | Mappe |
| 129 | `by_last_modified` | Last modified | Sist endret |
| 130 | `by_last_modified_daily` | Last modified (daily) | Sist endret (daglig) |
| 131 | `by_last_modified_monthly` | Last modified (monthly) | Sist endret (månedlig) |
| 132 | `by_date_taken` | Date taken | Dato tatt |
| 133 | `by_date_taken_daily` | Date taken (daily) | Dato tatt (daglig) |
| 134 | `by_date_taken_monthly` | Date taken (monthly) | Dato tatt (månedlig) |
| 135 | `by_file_type` | File type | Filtype |
| 136 | `by_extension` | Extension | Endelse |
| 137 | `show_file_count_section_header` | Show file count at section headers | Vis filantall i seksjonsoverskrifter |
| 138 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | Vær oppmerksom på at gruppering og sortering er to uavhengige områder |
| 139 | `folder_on_widget` | Folder shown on the widget: | Mappe vist på modulen: |
| 140 | `show_folder_name` | Show folder name | Vis mappenavn |
| 141 | `autoplay_videos` | Play videos automatically | Avspill videoer automatisk |
| 142 | `remember_last_video_position` | Remember last video playback position | Husk siste videoavspillingsposisjon |
| 143 | `loop_videos` | Loop videos | Gjenta videoer |
| 144 | `video_double_tap_duration` | Double-tap seek duration | Søketid ved dobbelttrykk |
| 145 | `video_seek_duration_seconds` | %d seconds | %d sekunder |
| 146 | `animate_gifs` | Animate GIFs at thumbnails | Animert GIF i minibildevisning |
| 147 | `max_brightness` | Max brightness when viewing fullscreen media | Maks lysstyrke ved mediavisning |
| 148 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Vis Ultra HDR-bilder i HDR |
| 149 | `crop_thumbnails` | Crop thumbnails into squares | Beskjær minibilder i kvadrater |
| 150 | `show_thumbnail_video_duration` | Show video durations | Vis videolengde |
| 151 | `screen_rotation_by` | Rotate fullscreen media by | Roter media etter |
| 152 | `screen_rotation_system_setting` | System setting | Systeminnstilling |
| 153 | `screen_rotation_device_rotation` | Device rotation | Enhetsrotasjon |
| 154 | `screen_rotation_aspect_ratio` | Aspect ratio | Sideforhold |
| 155 | `black_background_at_fullscreen` | Black background at fullscreen media | Svart bakgrunn og statuslinje i mediavisningen |
| 156 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | Horisontal rulling av minibilder |
| 157 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | Skjul automatisk systemlinjer ved mediavisning |
| 158 | `delete_empty_folders` | Delete empty folders after deleting their content | Slett tomme mapper etter sletting av deres innhold |
| 159 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | Hold skjermen på ved visning av bilder i fullskjermmodus |
| 160 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | Tillat å styre fotolysstyrke med vertikale bevegelser |
| 161 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | Tillat å styre videovolum og lysstyrke med vertikale bevegelser |
| 162 | `playback_speed` | Playback speed | Avspillingshastighet |
| 163 | `show_media_count` | Show folder media count on the main view | Vis mediaantallet i mapper på hovedvisningen |
| 164 | `show_extended_details` | Show extended details over fullscreen media | Vis flere detaljer i mediavisningen |
| 165 | `manage_extended_details` | Manage extended details | Velg detaljer |
| 166 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | Tillat en-finger-zoom i mediavisningen |
| 167 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | Tillat å skifte media øyeblikkelig ved å trykke på kanten av skjermen |
| 168 | `allow_deep_zooming_images` | Allow deep zooming images | Tillat dyp bildezooming |
| 169 | `hide_extended_details` | Hide extended details when status bar is hidden | Skjul utvidede detaljer når statuslinjen er skjult |
| 170 | `show_at_bottom` | Show some action buttons at the bottom of the screen | Vis noen handlingsknapper nederst på skjermen |
| 171 | `show_recycle_bin` | Show the recycle bin on the folders screen | Vis papirkurven på mappeskjermen |
| 172 | `deep_zoomable_images` | Deep zoomable images | Dyp zoombare bilder |
| 173 | `show_highest_quality` | Show images in the highest possible quality | Vis bilder i høyest mulig kvalitet |
| 174 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | Vis papirkurven som siste element på hovedskjermen |
| 175 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | Tillat lukking av mediavisningen med en nedoverbevegelse |
| 176 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | Tillat å zoome 1:1 med to dobbeltrykk |
| 177 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | Spill videoer på en separat skjerm med horisontale bevegelser |
| 178 | `on_video_tap` | On video tap | Ved videotrykk |
| 179 | `open_in_app_player` | Open in-app player | Åpne spilleren i appen |
| 180 | `open_system_default_player` | Open default player app | Åpne standardspilleren |
| 181 | `show_notch` | Show a notch if available | Vis flik hvis tilgjengelig |
| 182 | `allow_rotating_gestures` | Allow rotating images with gestures | Tillat rotering av bilder med fingerbevegelser |
| 183 | `file_loading_priority` | File loading priority | Fillastingsprioritet |
| 184 | `speed` | Speed | Hastighet |
| 185 | `compromise` | Compromise | Kompromiss |
| 186 | `avoid_showing_invalid_files` | Avoid showing invalid files | Unngå visning av ugyldige filer |
| 187 | `show_image_file_types` | Show image file types | Vis bildefiltyper |
| 188 | `allow_zooming_videos` | Allow zooming videos with double tapping them | Tillat zooming av videoer ved å dobbelttrykke dem |
| 189 | `folder_thumbnail_style` | Folder thumbnail style | Minibildestil for mapper |
| 190 | `file_thumbnail_style` | File thumbnail style | Minibildestil for filer |
| 191 | `mark_favorite_items` | Mark favorite items | Marker favorittelementer |
| 192 | `thumbnail_spacing` | Thumbnail spacing | Minibildemellomrom |
| 193 | `show_file_count_line` | Show file count on a separate line | Vis antall filer på en separat linje |
| 194 | `show_file_count_brackets` | Show file count in brackets | Vis antall filer i parentes |
| 195 | `show_file_count_none` | Do not show file count | Ikke vis antall filer |
| 196 | `limit_folder_title` | Limit long folder titles to 1 line | Begrens lange mappetitler til 1 linje |
| 197 | `square` | Square | Firkantet |
| 198 | `rounded_corners` | Rounded corners | Avrundede hjørner |
| 199 | `export_favorite_paths` | Export favorites and virtual albums | Eksporter favoritter og virtuelle album |
| 200 | `import_favorite_paths` | Import favorites and virtual albums | Importer favoritter og virtuelle album |
| 201 | `paths_imported_successfully` | Paths imported successfully | Filbaner importert |
| 202 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | For å være sikker på at alle filoperasjoner fungerer pålitelig, gjør denne appen til en mediaadministrasjonsapp i enhetsinnstillingene. |
| 203 | `password_protect_excluded` | Password protect excluded folder visibility | Passordbeskytt synligheten til ekskluderte mapper |
| 204 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Noe gikk galt. Gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. |
| 205 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis omdirigeringen ikke fungerer, gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. |
| 206 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis du ikke vil gjøre det, kan du også gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. |
| 207 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | Alternativt kan du kun tillate tilgang til mediafiler. I dette tilfellet vil du riktignok ikke kunne arbeide med skjulte filer. |
| 208 | `media_only` | Media only | Kun media |
| 209 | `all_files` | All files | Alle filer |
| 210 | `main_screen_view_on_launch` | Main screen view on launch | Hovedskjermvisning ved oppstart |
| 211 | `show_folders` | Show folders | Vis mapper |
| 212 | `show_files` | Show files | Vis filer |
| 213 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | Vis en menyknapp for å raskt slå av/på \"Vis alle mappers innhold\" |
| 214 | `thumbnails` | Thumbnails | Minibilder |
| 215 | `fullscreen_media` | Fullscreen media | Mediavisning |
| 216 | `extended_details` | Extended details | Utvidede detaljer |
| 217 | `bottom_actions` | Bottom actions | Handlinger nederst |
| 218 | `manage_bottom_actions` | Manage visible bottom actions | Behandle nedre handlingsknapper |
| 219 | `toggle_favorite` | Toggle favorite | Slå av/på favoritt |
| 220 | `toggle_file_visibility` | Toggle file visibility | Filsynlighet av/på |
| 221 | `failed_to_load_media` | Failed to load media. | Kunne ikke laste inn media. |
| 222 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | Filen er ugyldig eller ødelagt. |
| 223 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Mediet overstiger enhetens kapasitet. |
| 224 | `unsupported_format` | Unsupported format. | Ikke-støttet format. |
| 225 | `video_no_sound` | This video has no audio | Denne videoen har ingen lyd |
| 226 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | Hvordan kan jeg gjøre Fossify Gallery til enhetens standardgalleri? |
| 227 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\".         The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Først må du finne det nåværende standardgalleriet i Apper-delen av enhetsinnstillingene, se etter en knapp som heter noe slikt som \"Åpne som standard\", trykke på den og deretter velge \"Fjern standardvalg\".\nNeste gang du prøver å åpne et bilde eller en video, skal du se en appvelger der du kan velge Fossify Gallery og gjøre den til standardapp. |
| 228 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Jeg låste appen med et passord, men jeg har glemt det. Hva kan jeg gjøre? |
| 229 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Du kan løse det på to måter. Du kan enten installere appen på nytt, eller finne appen i enhetsinnstillingene og velge \"Slett data\". Det vil tilbakestille alle innstillingene dine, men det vil ikke fjerne noen mediafiler. |
| 230 | `faq_3_title` | How can I make an album always appear at the top? | Hvordan kan jeg få et album til å alltid vises øverst? |
| 231 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Du kan trykke og holde på ønsket album og velge Fest-ikonet i handlingsmenyen for å feste det til toppen. Du kan også feste flere mapper; festede elementer blir sortert etter standard sorteringsmetode. |
| 232 | `faq_4_title` | How can I fast forward videos? | Hvordan kan jeg spole fremover i videoer? |
| 233 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Du kan gjøre det ved å dobbelttrykke på siden av skjermen, eller trykke på teksten for nåværende eller maksimal varighet nær søkelinjen. Hvis du aktiverer avspilling av videoer på en separat skjerm i appinnstillingene, kan du også bruke horisontale bevegelser. |
| 234 | `faq_5_title` | What is the difference between hiding and excluding a folder? | Hva er forskjellen mellom å skjule og å ekskludere en mappe? |
| 235 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Ekskluder hindrer at mappen vises kun i Fossify Gallery, mens Skjul fungerer på systemnivå og skjuler mappen også for andre gallerier. Det fungerer ved å opprette en tom \".nomedia\"-fil i den aktuelle mappen, som du også kan fjerne med en hvilken som helst filbehandler. Merk at enkelte enheter ikke tillater å skjule mapper som Camera, Screenshots og Downloads. |
| 236 | `faq_6_title` | Why do folders with music cover art or stickers show up? | Hvorfor vises mapper med plateomslag eller klistremerker? |
| 237 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Det kan skje at uvanlige album dukker opp. Du kan enkelt ekskludere dem ved å trykke og holde på dem og velge Ekskluder. I den neste dialogen kan du så velge den overordnede mappen, noe som sannsynligvis også vil hindre at andre relaterte album vises. |
| 238 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | En mappe med bilder vises ikke, eller den viser ikke alle elementer. Hva kan jeg gjøre? |
| 239 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Det kan ha flere årsaker, men det er enkelt å løse. Bare gå til Innstillinger -&gt; Håndter inkluderte mapper, velg Pluss og naviger til ønsket mappe. |
| 240 | `faq_8_title` | What if I want just a few particular folders visible? | Hva om jeg bare vil at noen bestemte mapper skal være synlige? |
| 241 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders.         That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Å legge til en mappe under Inkluderte mapper ekskluderer ikke automatisk noe annet. Det du kan gjøre er å gå til Innstillinger -&gt; Håndter ekskluderte mapper, ekskludere rotmappen \"/\", og deretter legge til ønskede mapper under Innstillinger -&gt; Håndter inkluderte mapper.\nDet vil gjøre bare de valgte mappene synlige, siden både ekskludering og inkludering er rekursive, og hvis en mappe både er ekskludert og inkludert, vil den vises. |
| 242 | `faq_10_title` | Can I crop images with this app? | Kan jeg beskjære bilder med denne appen? |
| 243 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Ja, du kan beskjære bilder i redigeringsverktøyet ved å dra i bildehjørnene. Du kommer til redigeringsverktøyet enten ved å trykke og holde på et minibilde og velge Rediger, eller ved å velge Rediger fra fullskjermvisningen. |
| 244 | `faq_11_title` | Can I somehow group media file thumbnails? | Kan jeg på noen måte gruppere minibilder av mediafiler? |
| 245 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Selvfølgelig, bare bruk menyelementet \"Grupper etter\" når du er i minibildevisningen. Du kan gruppere filer etter flere kriterier, inkludert Dato tatt. Hvis du bruker funksjonen \"Vis alle mappers innhold\", kan du også gruppere dem etter mapper. |
| 246 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | Sortering etter Dato tatt ser ikke ut til å fungere skikkelig, hvordan kan jeg fikse det? |
| 247 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Det skyldes sannsynligvis at filene er kopiert fra et annet sted. Du kan fikse det ved å merke minibildene og velge \"Korriger Dato tatt-verdi\". |
| 248 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Jeg ser fargebånd på bildene. Hvordan kan jeg forbedre kvaliteten? |
| 249 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | Den nåværende løsningen for bildevisning fungerer fint i de aller fleste tilfeller, men hvis du ønsker enda bedre bildekvalitet, kan du aktivere \"Vis bilder i høyest mulig kvalitet\" i appinnstillingene under delen \"Dyp zoombare bilder\". |
| 250 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Jeg har skjult en fil/mappe. Hvordan kan jeg gjøre den synlig igjen? |
| 251 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Du kan enten trykke på menyelementet \"Vis skjulte elementer midlertidig\" på hovedskjermen, eller slå på \"Vis skjulte elementer\" i appinnstillingene for å se det skjulte elementet. Hvis du vil gjøre det synlig igjen, trykker og holder du bare på det og velger \"Vis igjen\". Mapper skjules ved å legge til en skjult \".nomedia\"-fil i dem; du kan også slette denne filen med en hvilken som helst filbehandler. Merk imidlertid at skjuling fungerer rekursivt, så hvis du skjuler en mappe, blir alle undermapper også skjult. For å gjøre undermappene synlige igjen må du derfor gjøre den overordnede mappen synlig igjen. |
| 252 | `faq_15_title` | Why does the app take up so much space? | Hvorfor tar appen opp så mye plass? |
| 253 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | Appens hurtigbuffer kan ta opptil 250 MB; den sørger for raskere innlasting av bilder. Hvis appen tar opp enda mer plass, skyldes det sannsynligvis at du har elementer i papirkurven. Disse filene teller med i appstørrelsen. Du kan tømme papirkurven ved å åpne den og slette alle filer, eller fra appinnstillingene. Hver fil i papirkurven slettes automatisk etter 30 dager. |
| 254 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | Hva skjedde med skjuling av filer og mapper, og hvorfor kan jeg ikke se skjulte elementer lenger? |
| 255 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | Fra og med Android 11 kan du ikke lenger skjule/vise filer eller mapper, du kan heller ikke se de skjulte i galleriapper pga. systemrestriksjoner. Du må bruke en filbehandlingsapp for det. |
| 256 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | Eller du kan også gi dette galleriet tilgang til Alle filer via enhetsinnstillingene; det lar oss vise skjulte elementer og gjør filoperasjoner mer pålitelige generelt. |
| 257 | `faq_17_title` | Why cannot I include missing folders anymore? | Hvorfor kan jeg ikke inkludere manglende mapper lenger? |
| 258 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Det sluttet også å fungere på grunn av systemendringene som kom med Android 11; appen kan ikke lenger bla gjennom ekte mapper, men er avhengig av såkalte MediaStore for å hente data. |
| 259 | `faq_18_title` | Why do I see ads during video playback? | Hvorfor ser jeg reklame under videoavspilling? |
| 260 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Appene våre har ingen reklame overhodet. Hvis du ser reklame under videoavspilling, bruker du sannsynligvis videospilleren til en annen app. Prøv å finne standard videospiller i enhetsinnstillingene, og velg \"Fjern standardvalg\" på den. Neste gang du åpner en video, vil du se en appvelger der du kan velge hvilken app du vil bruke. |
| 261 | `create_virtual_album` | Create virtual album | Opprett virtuelt album |
| 262 | `new_virtual_album` | New album… | Nytt album… |
| 263 | `rename_virtual_album` | Rename album | Gi albumet nytt navn |
| 264 | `album_name` | Album name | Albumnavn |
| 265 | `virtual_folder_name` | Virtual folder name | Navn på virtuell mappe |
| 266 | `virtual_album_name_empty` | The album name cannot be empty | Albumnavnet kan ikke være tomt |
| 267 | `virtual_album_exists` | An album with this name already exists | Et album med dette navnet finnes allerede |
| 268 | `add_to_virtual_album` | Add to album | Legg til i album |
| 269 | `remove_from_virtual_album` | Remove from album | Fjern fra album |
| 270 | `added_to_virtual_album` | Added to \"%s\" | Lagt til i \"%s\" |
| 271 | `already_in_virtual_album` | Already in \"%s\" | Allerede i \"%s\" |
| 272 | `removed_from_virtual_album` | Removed from the album | Fjernet fra albumet |
| 273 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | Slette albumet %s? Bare albumet fjernes, bildene og videoene dine forblir der de er. |
| 274 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Dette albumet er tomt. Velg elementer i en hvilken som helst mappe og bruk \"Kopier til\". |
| 275 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Denne handlingen er ikke tilgjengelig for virtuelle album |
| 276 | `alphanumeric_password` | Password | Passord |
| 277 | `enter_password` | Enter password | Skriv inn passord |
| 278 | `repeat_password` | Repeat password | Gjenta passord |
| 279 | `wrong_password` | Wrong password | Feil passord |
| 280 | `encrypting_files` | Encrypting files… | Krypterer filer… |
| 281 | `encrypting_folder` | Encrypting folder… | Krypterer mappe… |
| 282 | `folder_encrypted_successfully` | Folder encrypted successfully | Mappen ble kryptert |
| 283 | `file_encrypted_successfully` | File encrypted successfully | Filen ble kryptert |
| 284 | `files_encrypted_successfully` | Files encrypted successfully | Filene ble kryptert |
| 285 | `decrypting_folder` | Decrypting folder… | Dekrypterer mappe… |
| 286 | `folder_decrypted_successfully` | Folder decrypted successfully | Mappen ble dekryptert |
| 287 | `file_decrypted_successfully` | File decrypted successfully | Filen ble dekryptert |
| 288 | `files_decrypted_successfully` | Files decrypted successfully | Filene ble dekryptert |
| 289 | `decrypting_files` | Decrypting files… | Dekrypterer filer… |
| 290 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Plasser fingeren på fingeravtrykkssensoren |
| 291 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Koble sammen minst 4 punkter |
| 292 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | Vil du få tilgang til din eksisterende Cipher_Secrets-mappe? |
| 293 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Eksisterende Cipher_Secrets-mappe vil bli slettet og en ny vil bli opprettet. Er du sikker? |
| 294 | `confirm_checkbox_label` | I confirm | Jeg bekrefter |
| 295 | `secret_vault` | Encrypted Vault | Kryptert hvelv |
| 296 | `secret_vault_empty` | Encrypted Vault is empty | Det krypterte hvelvet er tomt |
| 297 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | Er du sikker på at du vil dekryptere de(n) skjulte filen(e)? |
| 298 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Kopiering til utklippstavlen er deaktivert av sikkerhetshensyn mens hvelvet er låst opp. |
| 299 | `on_vault_exit` | On leaving encrypted section | Når du forlater kryptert seksjon |
| 300 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | Fortsett å vise krypterte album |
| 301 | `on_vault_exit_hide` | Hide encrypted albums | Skjul krypterte album |
| 302 | `on_vault_exit_ask_every_time` | Always ask | Spør alltid |
| 303 | `vault_exit_dialog_title` | Exiting Encrypted Section | Forlater kryptert seksjon |
| 304 | `vault_exit_question_single` | Show encrypted album together with other albums?\n\nIn this case, during the app session, you will need to unlock the hidden vault whenever you send the app to the background and return to it, or turn the screen off and back on. | Vise kryptert album sammen med andre album?\n\nI så fall må du i løpet av appøkten låse opp det skjulte hvelvet hver gang du sender appen til bakgrunnen og går tilbake til den, eller slår skjermen av og på igjen. |
| 305 | `vault_exit_question_multiple` | Show encrypted albums together with other albums?\n\nIn this case, during the app session, you will need to unlock the hidden vault whenever you send the app to the background and return to it, or turn the screen off and back on. | Vise krypterte album sammen med andre album?\n\nI så fall må du i løpet av appøkten låse opp det skjulte hvelvet hver gang du sender appen til bakgrunnen og går tilbake til den, eller slår skjermen av og på igjen. |
| 306 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | For flere alternativer, se Innstillinger &gt; Når du forlater kryptert seksjon |
| 307 | `stop_showing_hidden` | Stop showing encrypted media | Slutt å vise krypterte medier |
| 308 | `system_default` | System default | Systemstandard |
| 309 | `language` | Language | Språk |
| 310 | `open_system_language_settings` | Open system language settings | Åpne systemets språkinnstillinger |
| 311 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Få tilgang til skjulte elementer med biometri |
| 312 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Aktiver passordbeskyttelse for skjulte elementer først |
| 313 | `biometric_unlock_vault` | Unlock encrypted vault | Lås opp kryptert hvelv |
| 314 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | Bekreft din biometriske identitet for å få tilgang til skjulte elementer |
| 315 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | Bekreft din biometriske identitet for å aktivere biometrisk tilgang til hvelvet |
| 316 | `use_password_or_pin` | Use PIN / Password | Bruk PIN / Passord |
| 317 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Biometriske data er endret. Vennligst skriv inn PIN eller passord. |
| 318 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | Ingen biometrisk maskinvare eller registrerte biometriske data funnet på denne enheten |
| 319 | `default_hidden_album` | Default Secret Album | *(non-translatable in `values/strings.xml`)* |
| 320 | `select_hidden_album` | Select Encrypted Album | Velg kryptert album |
| 321 | `create_new_hidden_album` | Create New Encrypted Album | Opprett nytt kryptert album |
| 322 | `new_hidden_album` | New encrypted album | Nytt kryptert album |
| 323 | `hidden_album_name_empty` | Album name cannot be empty | Albumnavn kan ikke være tomt |
| 324 | `file_hidden_to_album` | File encrypted to \"%s\" | Fil kryptert i «%s» |
| 325 | `files_hidden_to_album` | %1$d files encrypted to \"%2$s\" | %1$d filer kryptert i «%2$s» |
| 326 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | Er du sikker på du vil gjenopprette %d fil? |
| 327 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | Er du sikker på du vil gjenopprette %d filer? |
| 328 | `failed_to_resize_images` (`one`) | Failed to resize %d image | Klarte ikke endre størrelse på %d bilde |
| 329 | `failed_to_resize_images` (`other`) | Failed to resize %d images | Klarte ikke endre størrelse på %d bilder |
| 330 | `search` *(fossify-commons)* | *(inherited from commons)* | Søk |
| 331 | `change_date_and_time_format` *(fossify-commons)* | Change date and time format | Endre format for dato og klokkeslett |
| 332 | `password_protect_file_deletion` *(fossify-commons)* | Password protect file deletion and moving | Passordbeskytt sletting og flytting av filer |
| 333 | `keep_last_modified` *(fossify-commons)* | Keep old last-modified value at file operations | Behold sist-endret-verdi ved filoperasjoner |
| 334 | `clear_cache` *(fossify-commons)* | Clear cache | Tøm hurtiglager |
| 335 | `export_settings` *(fossify-commons)* | Export settings | Eksporter innstillinger |
| 336 | `import_settings` *(fossify-commons)* | Import settings | Importer innstillinger |
| 337 | `temporarily_show_excluded` *(fossify-commons)* | Temporarily show excluded | Vis ekskluderte midlertidig |
| 338 | `change_view_type` *(fossify-commons)* | Change view type | Endre visningstype |
| 339 | `contributors` *(fossify-commons)* | Contributors | Bidragsytere |
| 340 | `known_issues` *(fossify-commons)* | Known issues | Kjente problemer |
| 341 | `donate_to_fossify` *(fossify-commons)* | Donate to Fossify | Doner til Fossify |
| 342 | `privacy_policy` *(fossify-commons)* | Privacy policy | Personvernerklæring |
| 343 | `create_shortcut` *(fossify-commons)* | Create shortcut | Opprett snarvei |
| 344 | `print` *(fossify-commons)* | Print | Skriv ut |
| 345 | `search_files` *(fossify-commons)* | Search files | Søk i filer |
| 346 | `search_folders` *(fossify-commons)* | Search folders | Søk i mapper |
| 347 | `skip_the_recycle_bin` *(fossify-commons)* | Skip the recycle bin, delete files directly | Hopp over papirkurven og slett filene direkte |
| 348 | `pull_down_to_open_vault` | Keep pulling down for Encrypted Vault | Fortsett å dra ned for kryptert hvelv |
| 349 | `keep_holding_to_open_vault` | Keep holding to open Encrypted Vault | Fortsett å holde for å åpne kryptert hvelv |
| 350 | `release_to_open_vault` | Release to open Encrypted Vault | Slipp for å åpne kryptert hvelv |
| 351 | `exclude_folder` *(fossify-commons)* | Exclude folder | Ekskluder mappe |
| 352 | `column_count` *(fossify-commons)* | Column count | Antall kolonner |
| 353 | `open_the_recycle_bin` *(fossify-commons)* | Open the recycle bin | Åpne papirkurven |
| 354 | `version_placeholder` *(fossify-commons)* | Version %s | Versjon %s |
| 355 | `number_of_items` *(fossify-commons)* | Number of items | Antall elementer |
| 356 | `random` *(fossify-commons)* | Random | Tilfeldig |
| 357 | `shuffle` | Shuffle | Bland |
| 358 | `stop_showing_excluded` *(fossify-commons)* | Stop showing excluded | Slutt å vise ekskluderte |
| 359 | `copy_to_clipboard` *(fossify-commons)* | Copy to clipboard | Kopier til utklippstavlen |
| 360 | `encrypt` | Encrypt | Krypter |
| 361 | `decrypt` | Decrypt | Dekrypter |
| 362 | `encrypt_album` | Encrypt album | Krypter album |
| 363 | `decrypt_album` | Decrypt album | Dekrypter album |
| 364 | `create_new_encrypted_virtual_album` | Create new encrypted virtual album | Opprett nytt kryptert virtuelt album |
| 365 | `new_album` | New album | Nytt album |
| 366 | `filename_without_zip` | Filename (without .zip) | Filnavn (uten .zip) |
| 367 | `favorites_already_exist` | You already have a favorites list | Du har allerede en favorittliste |
| 368 | `delete_existing_list` | Delete existing list | Slett eksisterende liste |
| 369 | `merge_lists` | Merge lists | Slå sammen lister |
| 370 | `confirm` | Confirm | Bekreft |
| 371 | `confirm_encrypt_album` | Are you sure you want to encrypt this album? All of its media files will be encrypted and moved to the encrypted vault. The original files will be deleted. | Er du sikker på at du vil kryptere dette albumet? Alle mediefilene i det blir kryptert og flyttet til det krypterte hvelvet. Originalfilene blir slettet. |
| 372 | `confirm_encrypt_albums` | Are you sure you want to encrypt the selected albums? All of their media files will be encrypted and moved to the encrypted vault. The original files will be deleted. | Er du sikker på at du vil kryptere de valgte albumene? Alle mediefilene i dem blir kryptert og flyttet til det krypterte hvelvet. Originalfilene blir slettet. |
| 373 | `confirm_encrypt_file` | Are you sure you want to encrypt this file? It will be encrypted and moved to the encrypted vault. The original file will be deleted. | Er du sikker på at du vil kryptere denne filen? Den blir kryptert og flyttet til det krypterte hvelvet. Originalfilen blir slettet. |
| 374 | `confirm_encrypt_files` | Are you sure you want to encrypt the selected files? They will be encrypted and moved to the encrypted vault. The original files will be deleted. | Er du sikker på at du vil kryptere de valgte filene? De blir kryptert og flyttet til det krypterte hvelvet. Originalfilene blir slettet. |
| 375 | `confirm_encrypt_files_to_vault` | The selected files will be encrypted when they are copied into the encrypted vault. Do you want to continue? | De valgte filene blir kryptert når de kopieres til det krypterte hvelvet. Vil du fortsette? |
| 376 | `encrypt_decrypt_notifications` | Encrypt / Decrypt | Krypter / Dekrypter |
| 377 | `allow_batch_renaming` | Allow batch renaming when multiple files are selected | Tillat masseendring av navn når flere filer er valgt |
| 378 | `keep_original_remove_from_virtual_album` | Keep the original file, remove it only from this virtual album | Behold originalfilen, fjern den bare fra dette virtuelle albumet |
| 379 | `keep_original_remove_from_favorites` | Keep the original file, remove it only from favorites | Behold originalfilen, fjern den bare fra favoritter |
| 380 | `delete_original_from_favorites_and_virtual_albums` | Delete the original version of a file when it is deleted from favorites and virtual albums | Slett originalversjonen av en fil når den slettes fra favoritter og virtuelle album |
| 381 | `cannot_target_encrypted_album` | Encrypted albums cannot be targeted for this operation | Krypterte album kan ikke velges som mål for denne handlingen |
| 382 | `cannot_mix_encrypted_and_unencrypted` | Cannot combine encrypted and unencrypted items in this operation | Kan ikke kombinere krypterte og ukrypterte elementer i denne handlingen |
| 383 | `lock_album` | Lock album | Lås album |
| 384 | `unlock_album` | Unlock album | Lås opp album |
| 385 | `auto_backup_prompt_message` | Do you want your favorites and virtual albums to be regularly written to system storage outside of app memory? This will ensure that you do not lose your favorites list and virtual albums if you uninstall the app. You can manually create a backup from app settings at any time. | Vil du at favorittene og de virtuelle albumene dine skal lagres regelmessig i systemlagringen utenfor appminnet? Dette sikrer at du ikke mister favorittlisten og de virtuelle albumene dine hvis du avinstallerer appen. Du kan når som helst opprette en sikkerhetskopi manuelt fra appinnstillingene. |
