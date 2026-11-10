# Norwegian Bokmål (values-nb-rNO) Translation Completion & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-nb-rNO/strings.xml` was audited against `app/src/main/res/values/strings.xml`. Three existing errors/typos were corrected, two multiline placeholders were normalized, and all 91 missing strings were translated into Norwegian Bokmål and added, bringing `values-nb-rNO` to full parity (307 `<string>` + 2 `<plurals>` + `search` inherited from `fossify-commons` = 310 resource entries; 312 total comparison rows with plural forms expanded).
> **Key Terminology:** `Settings` → `Innstillinger` · `Default` → `Standard` / `Systemstandard` · `Folder` → `Mappe` · `Album` → `Album` · `Secret Vault` → `Hemmelig hvelv` · `Hidden` → `Skjult` · `Excluded` → `Ekskludert` · `Password` → `Passord` · `Encrypt / Decrypt` → `Krypter / Dekrypter` · `Thumbnails` → `Minibilder` · `Recycle bin` → `Papirkurv` · `Clipboard` → `Utklippstavlen`

## 1. Summary of Revisions & 91 Newly Added Norwegian Bokmål Translations

### 1.1 Revisions to Previously Existing Translations

| Key (`name`) | Old `values-nb-rNO` | New `values-nb-rNO` | Status / Notes |
|---|---|---|---|
| `no_media_with_filters` | Ingen media-filer er funnet med de valgte filtrene. | Ingen mediafiler er funnet med de valgte filtrene. | **Improved** (Removed stray hyphen to match `mediafiler` in `no_media_add_included`) |
| `change_filters_underlined` | `<u>Endre filtere</u>` | `<u>Endre filtre</u>` | **Fixed** (Correct Bokmål plural of *filter* is *filtre*, not *filtere*) |
| `invalid_values` | Oppfør en gyldig oppløsning | Vennligst oppgi en gyldig oppløsning | **Fixed** (*oppfør* means behave/list; *oppgi* means enter) |
| `included_activity_placeholder` | *(literal newlines around `\n`)* | `...\n\n...` | **Normalized** (Removed stray spaces caused by literal XML newlines) |
| `excluded_activity_placeholder` | *(literal newlines around `\n`)* | `...\n\n...` | **Normalized** (Removed stray spaces caused by literal XML newlines) |

### 1.2 Newly Added Translations (91 Keys)

| # | Key (`name`) | English (`values/strings.xml`) | Norwegian Bokmål (`values-nb-rNO/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Hvis en mappe eller en av dens overordnede mapper har et punktum foran navnet, er den skjult og kan ikke vises igjen på denne måten. Du må fjerne punktumet ved å gi den nytt navn. | Explanation of dot-prefixed hidden folders |
| 2 | `fade` | Fade | Uttoning | Slideshow transition animation option |
| 3 | `slide` | Slide | Glidning | Slideshow transition animation option |
| 4 | `video_double_tap_duration` | Double-tap seek duration | Søketid ved dobbelttrykk | Video settings: double-tap forward/rewind seek interval |
| 5 | `video_seek_duration_seconds` | %d seconds | %d sekunder | Used with durations 3, 5, 10, 15 via String.format |
| 6 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Vis Ultra HDR-bilder i HDR | Media settings: render Ultra HDR photos in HDR |
| 7 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Noe gikk galt. Gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. | Error fallback directing user to Android Special app access settings |
| 8 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis omdirigeringen ikke fungerer, gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. | Redirect fallback note for Media management apps permission |
| 9 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis du ikke vil gjøre det, kan du også gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. | Alternative instructions for Media management apps permission |
| 10 | `failed_to_load_media` | Failed to load media. | Kunne ikke laste inn media. | Media viewer error message |
| 11 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | Filen er ugyldig eller ødelagt. | Media player/viewer error message |
| 12 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Mediet overstiger enhetens kapasitet. | Media player/viewer error message |
| 13 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | Hvordan kan jeg gjøre Fossify Gallery til enhetens standardgalleri? | FAQ 1 question |
| 14 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\".         The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Først må du finne det nåværende standardgalleriet i Apper-delen av enhetsinnstillingene, se etter en knapp som heter noe slikt som \"Åpne som standard\", trykke på den og deretter velge \"Fjern standardvalg\".\nNeste gang du prøver å åpne et bilde eller en video, skal du se en appvelger der du kan velge Fossify Gallery og gjøre den til standardapp. | FAQ 1 answer; matches standard Norwegian Android settings labels |
| 15 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Jeg låste appen med et passord, men jeg har glemt det. Hva kan jeg gjøre? | FAQ 2 question |
| 16 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Du kan løse det på to måter. Du kan enten installere appen på nytt, eller finne appen i enhetsinnstillingene og velge \"Slett data\". Det vil tilbakestille alle innstillingene dine, men det vil ikke fjerne noen mediafiler. | FAQ 2 answer |
| 17 | `faq_3_title` | How can I make an album always appear at the top? | Hvordan kan jeg få et album til å alltid vises øverst? | FAQ 3 question |
| 18 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Du kan trykke og holde på ønsket album og velge Fest-ikonet i handlingsmenyen for å feste det til toppen. Du kan også feste flere mapper; festede elementer blir sortert etter standard sorteringsmetode. | FAQ 3 answer; matches "Fest" terminology |
| 19 | `faq_4_title` | How can I fast forward videos? | Hvordan kan jeg spole fremover i videoer? | FAQ 4 question |
| 20 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Du kan gjøre det ved å dobbelttrykke på siden av skjermen, eller trykke på teksten for nåværende eller maksimal varighet nær søkelinjen. Hvis du aktiverer avspilling av videoer på en separat skjerm i appinnstillingene, kan du også bruke horisontale bevegelser. | FAQ 4 answer |
| 21 | `faq_5_title` | What is the difference between hiding and excluding a folder? | Hva er forskjellen mellom å skjule og å ekskludere en mappe? | FAQ 5 question |
| 22 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Ekskluder hindrer at mappen vises kun i Fossify Gallery, mens Skjul fungerer på systemnivå og skjuler mappen også for andre gallerier. Det fungerer ved å opprette en tom \".nomedia\"-fil i den aktuelle mappen, som du også kan fjerne med en hvilken som helst filbehandler. Merk at enkelte enheter ikke tillater å skjule mapper som Camera, Screenshots og Downloads. | FAQ 5 answer; keeps folder names Camera, Screenshots, Downloads literal |
| 23 | `faq_6_title` | Why do folders with music cover art or stickers show up? | Hvorfor vises mapper med plateomslag eller klistremerker? | FAQ 6 question |
| 24 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Det kan skje at uvanlige album dukker opp. Du kan enkelt ekskludere dem ved å trykke og holde på dem og velge Ekskluder. I den neste dialogen kan du så velge den overordnede mappen, noe som sannsynligvis også vil hindre at andre relaterte album vises. | FAQ 6 answer |
| 25 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | En mappe med bilder vises ikke, eller den viser ikke alle elementer. Hva kan jeg gjøre? | FAQ 7 question |
| 26 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Det kan ha flere årsaker, men det er enkelt å løse. Bare gå til Innstillinger -&gt; Håndter inkluderte mapper, velg Pluss og naviger til ønsket mappe. | FAQ 7 answer; matches "Håndter inkluderte mapper" |
| 27 | `faq_8_title` | What if I want just a few particular folders visible? | Hva om jeg bare vil at noen bestemte mapper skal være synlige? | FAQ 8 question |
| 28 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders.         That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Å legge til en mappe under Inkluderte mapper ekskluderer ikke automatisk noe annet. Det du kan gjøre er å gå til Innstillinger -&gt; Håndter ekskluderte mapper, ekskludere rotmappen \"/\", og deretter legge til ønskede mapper under Innstillinger -&gt; Håndter inkluderte mapper.\nDet vil gjøre bare de valgte mappene synlige, siden både ekskludering og inkludering er rekursive, og hvis en mappe både er ekskludert og inkludert, vil den vises. | FAQ 8 answer; matches "Håndter ekskluderte/inkluderte mapper" |
| 29 | `faq_10_title` | Can I crop images with this app? | Kan jeg beskjære bilder med denne appen? | FAQ 10 question |
| 30 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Ja, du kan beskjære bilder i redigeringsverktøyet ved å dra i bildehjørnene. Du kommer til redigeringsverktøyet enten ved å trykke og holde på et minibilde og velge Rediger, eller ved å velge Rediger fra fullskjermvisningen. | FAQ 10 answer |
| 31 | `faq_11_title` | Can I somehow group media file thumbnails? | Kan jeg på noen måte gruppere minibilder av mediafiler? | FAQ 11 question |
| 32 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Selvfølgelig, bare bruk menyelementet \"Grupper etter\" når du er i minibildevisningen. Du kan gruppere filer etter flere kriterier, inkludert Dato tatt. Hvis du bruker funksjonen \"Vis alle mappers innhold\", kan du også gruppere dem etter mapper. | FAQ 11 answer; matches "Grupper etter", "Dato tatt", "Vis alle mappers innhold" |
| 33 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | Sortering etter Dato tatt ser ikke ut til å fungere skikkelig, hvordan kan jeg fikse det? | FAQ 12 question |
| 34 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Det skyldes sannsynligvis at filene er kopiert fra et annet sted. Du kan fikse det ved å merke minibildene og velge \"Korriger Dato tatt-verdi\". | FAQ 12 answer; matches "Korriger Dato tatt-verdi" and "minibilder" |
| 35 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Jeg ser fargebånd på bildene. Hvordan kan jeg forbedre kvaliteten? | FAQ 13 question |
| 36 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | Den nåværende løsningen for bildevisning fungerer fint i de aller fleste tilfeller, men hvis du ønsker enda bedre bildekvalitet, kan du aktivere \"Vis bilder i høyest mulig kvalitet\" i appinnstillingene under delen \"Dyp zoombare bilder\". | FAQ 13 answer; matches "Vis bilder i høyest mulig kvalitet" and "Dyp zoombare bilder" |
| 37 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Jeg har skjult en fil/mappe. Hvordan kan jeg gjøre den synlig igjen? | FAQ 14 question |
| 38 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Du kan enten trykke på menyelementet \"Vis skjulte elementer midlertidig\" på hovedskjermen, eller slå på \"Vis skjulte elementer\" i appinnstillingene for å se det skjulte elementet. Hvis du vil gjøre det synlig igjen, trykker og holder du bare på det og velger \"Vis igjen\". Mapper skjules ved å legge til en skjult \".nomedia\"-fil i dem; du kan også slette denne filen med en hvilken som helst filbehandler. Merk imidlertid at skjuling fungerer rekursivt, så hvis du skjuler en mappe, blir alle undermapper også skjult. For å gjøre undermappene synlige igjen må du derfor gjøre den overordnede mappen synlig igjen. | FAQ 14 answer |
| 39 | `faq_15_title` | Why does the app take up so much space? | Hvorfor tar appen opp så mye plass? | FAQ 15 question |
| 40 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | Appens hurtigbuffer kan ta opptil 250 MB; den sørger for raskere innlasting av bilder. Hvis appen tar opp enda mer plass, skyldes det sannsynligvis at du har elementer i papirkurven. Disse filene teller med i appstørrelsen. Du kan tømme papirkurven ved å åpne den og slette alle filer, eller fra appinnstillingene. Hver fil i papirkurven slettes automatisk etter 30 dager. | FAQ 15 answer; matches "papirkurven" |
| 41 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | Hva skjedde med skjuling av filer og mapper, og hvorfor kan jeg ikke se skjulte elementer lenger? | FAQ 16 question (faq_16_text was already present) |
| 42 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | Eller du kan også gi dette galleriet tilgang til Alle filer via enhetsinnstillingene; det lar oss vise skjulte elementer og gjør filoperasjoner mer pålitelige generelt. | FAQ 16 follow-up regarding All Files access |
| 43 | `faq_17_title` | Why cannot I include missing folders anymore? | Hvorfor kan jeg ikke inkludere manglende mapper lenger? | FAQ 17 question |
| 44 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Det sluttet også å fungere på grunn av systemendringene som kom med Android 11; appen kan ikke lenger bla gjennom ekte mapper, men er avhengig av såkalte MediaStore for å hente data. | FAQ 17 answer; keeps MediaStore literal |
| 45 | `faq_18_title` | Why do I see ads during video playback? | Hvorfor ser jeg reklame under videoavspilling? | FAQ 18 question |
| 46 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Appene våre har ingen reklame overhodet. Hvis du ser reklame under videoavspilling, bruker du sannsynligvis videospilleren til en annen app. Prøv å finne standard videospiller i enhetsinnstillingene, og velg \"Fjern standardvalg\" på den. Neste gang du åpner en video, vil du se en appvelger der du kan velge hvilken app du vil bruke. | FAQ 18 answer |
| 47 | `create_virtual_album` | Create virtual album | Opprett virtuelt album | Virtual albums section action |
| 48 | `new_virtual_album` | New album… | Nytt album… | Action label for creating a new virtual album |
| 49 | `rename_virtual_album` | Rename album | Gi albumet nytt navn | Action label to rename a virtual album |
| 50 | `album_name` | Album name | Albumnavn | Input hint / label for album name |
| 51 | `virtual_folder_name` | Virtual folder name | Navn på virtuell mappe | Input hint / label for virtual folder |
| 52 | `virtual_album_name_empty` | The album name cannot be empty | Albumnavnet kan ikke være tomt | Validation error when album name is empty |
| 53 | `virtual_album_exists` | An album with this name already exists | Et album med dette navnet finnes allerede | Validation error when album name already exists |
| 54 | `add_to_virtual_album` | Add to album | Legg til i album | Menu action to add items to an album |
| 55 | `remove_from_virtual_album` | Remove from album | Fjern fra album | Menu action to remove items from an album |
| 56 | `added_to_virtual_album` | Added to \"%s\" | Lagt til i \"%s\" | Toast message preserving \"%s\" |
| 57 | `already_in_virtual_album` | Already in \"%s\" | Allerede i \"%s\" | Toast message preserving \"%s\" |
| 58 | `removed_from_virtual_album` | Removed from the album | Fjernet fra albumet | Toast message after removing items from an album |
| 59 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | Slette albumet %s? Bare albumet fjernes, bildene og videoene dine forblir der de er. | No extra quotes around %s (formatted with album name or count) |
| 60 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Dette albumet er tomt. Velg elementer i en hvilken som helst mappe og bruk \"Kopier til\". | Empty state placeholder referencing "Kopier til" (Copy to) |
| 61 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Denne handlingen er ikke tilgjengelig for virtuelle album | Toast message when action is unsupported on virtual albums |
| 62 | `alphanumeric_password` | Password | Passord | Password input label |
| 63 | `enter_password` | Enter password | Skriv inn passord | Password prompt |
| 64 | `repeat_password` | Repeat password | Gjenta passord | Password confirmation prompt |
| 65 | `wrong_password` | Wrong password | Feil passord | Password validation error |
| 66 | `encrypting_files` | Encrypting files… | Krypterer filer… | Progress message |
| 67 | `encrypting_folder` | Encrypting folder… | Krypterer mappe… | Progress message |
| 68 | `folder_encrypted_successfully` | Folder encrypted successfully | Mappen ble kryptert | Success message |
| 69 | `decrypting_folder` | Decrypting folder… | Dekrypterer mappe… | Progress message |
| 70 | `folder_decrypted_successfully` | Folder decrypted successfully | Mappen ble dekryptert | Success message |
| 71 | `decrypting_files` | Decrypting files… | Dekrypterer filer… | Progress message |
| 72 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Plasser fingeren på fingeravtrykkssensoren | Fingerprint authentication prompt |
| 73 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Koble sammen minst 4 punkter | Pattern lock validation prompt |
| 74 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | Vil du få tilgang til din eksisterende Cipher_Secrets-mappe? | Keeps literal folder name Cipher_Secrets |
| 75 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Eksisterende Cipher_Secrets-mappe vil bli slettet og en ny vil bli opprettet. Er du sikker? | Keeps literal folder name Cipher_Secrets |
| 76 | `confirm_checkbox_label` | I confirm | Jeg bekrefter | Checkbox label, suffixed with countdown (10) in dialog |
| 77 | `secret_vault` | Secret Vault | Hemmelig hvelv | Vault feature title ("Hemmelig hvelv") |
| 78 | `secret_vault_empty` | Secret Vault is empty | Det hemmelige hvelvet er tomt | Empty state placeholder for Secret Vault |
| 79 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | Er du sikker på at du vil dekryptere de(n) skjulte filen(e)? | Confirmation prompt for singular/plural file(s) |
| 80 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Kopiering til utklippstavlen er deaktivert av sikkerhetshensyn mens hvelvet er låst opp. | Security notice while vault is unlocked |
| 81 | `on_vault_exit` | On leaving hidden section | Når du forlater skjult seksjon | Settings label for behavior when leaving hidden section |
| 82 | `on_vault_exit_keep_showing_all` | Keep showing all hidden albums | Fortsett å vise alle skjulte album | Radio option |
| 83 | `on_vault_exit_show_encrypted_only` | Only show encrypted albums | Vis kun krypterte album | Radio option |
| 84 | `on_vault_exit_show_other_only` | Only show other hidden albums | Vis kun andre skjulte album | Radio option |
| 85 | `on_vault_exit_hide_all` | Hide all hidden albums | Skjul alle skjulte album | Radio option |
| 86 | `on_vault_exit_ask_every_time` | Always ask | Spør alltid | Radio option |
| 87 | `vault_exit_dialog_title` | Exiting Hidden Section | Forlater skjult seksjon | Dialog title |
| 88 | `vault_exit_keep_showing` | Keep showing hidden albums | Fortsett å vise skjulte album | Dialog radio option |
| 89 | `vault_exit_hide` | Hide hidden albums | Skjul skjulte album | Dialog radio option |
| 90 | `vault_exit_remember_my_choice` | Remember my choice | Husk valget mitt | Dialog checkbox label |
| 91 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving hidden section | For flere alternativer, se Innstillinger &gt; Når du forlater skjult seksjon | References Innstillinger > Når du forlater skjult seksjon |

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
| 45 | `hide_folder_description` | This function hides the folder by adding a \'.nomedia\' file into it, it will hide all subfolders too. You can see them by toggling the \'Show hidden items\' option in Settings. Continue? | Denne funksjonen skjuler mappen ved å legge til filen \'.nomedia\' i den. Alle undermapper blir også skjult. Du kan se dem ved å aktivere alternativet \'Vis skjulte elementer\' i innstillinger. Fortsette\? |
| 46 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | Dette vil ekskludere valgte sammen med undermapper, bare fra Fossify Gallery. Ekskluderte mapper kan håndteres i Innstillinger. |
| 47 | `exclude_folder_parent` | Exclude a parent instead? | Ekskludere en overordnet isteden\? |
| 48 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | Ekskludering av mapper vil gjøre dem sammen med deres undermapper, skjulte bare i Fossify Gallery. De vil fortsatt være synlige i andre apper.\n\nHvis du vil skjule dem fra andre apper også, bruk Skjul-funksjonen. |
| 49 | `hidden_folders` | Hidden folders | Skjulte mapper |
| 50 | `manage_hidden_folders` | Manage hidden folders | Håndter skjulte mapper |
| 51 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | Ser ut til at du ikke har skjult noen mapper med en \".nomedia\"-fil. |
| 52 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | Du må gi appen tilgang til alle filer for å se skjulte filer, ellers fungerer det ikke. |
| 53 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Hvis en mappe eller en av dens overordnede mapper har et punktum foran navnet, er den skjult og kan ikke vises igjen på denne måten. Du må fjerne punktumet ved å gi den nytt navn. |
| 54 | `include_folders` | Included folders | Inkluderte mapper |
| 55 | `manage_included_folders` | Manage included folders | Håndter inkluderte mapper |
| 56 | `add_folder` | Add folder | Legg til mappe |
| 57 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | Hvis du har noen mapper som inneholder media, men ikke ble gjenkjent av appen, kan du legge dem til manuelt her.\n\nÅ legge til noen elementer her, ekskluderer ikke noen annen mappe. |
| 58 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | Ingen mediafiler er funnet. Dette kan løses ved å legge til mapper som inneholder mediafiler manuelt. |
| 59 | `resize_and_save` | Resize selection and save | Endre størrelse og lagre |
| 60 | `width` | Width | Bredde |
| 61 | `height` | Height | Høyde |
| 62 | `keep_aspect_ratio` | Keep aspect ratio | Behold sideforhold |
| 63 | `invalid_values` | Please enter a valid resolution | Vennligst oppgi en gyldig oppløsning |
| 64 | `resize_multiple_images` | Resize multiple images | Endre størrelse på flere bilder |
| 65 | `resize_factor` | Resize factor | Faktor for størrelsesendring |
| 66 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | Endre størrelse på bilder til den angitte prosenten, verdi må være mellom 10 og 90. |
| 67 | `resize_factor_error` | Enter a number between 10 and 90 | Angi et tall mellom 10 og 90 |
| 68 | `images_resized_successfully` | Images resized successfully | Størrelsen på bildene endret |
| 69 | `editor` | Editor | Redigering |
| 70 | `basic_editor` | Basic Editor | Enkel redigering |
| 71 | `advanced_editor` | Advanced Editor | Avansert redigering |
| 72 | `rotate` | Rotate | Roter |
| 73 | `invalid_image_path` | Invalid image path | Ugyldig bildebane |
| 74 | `invalid_video_path` | Invalid video path | Ugyldig videobane |
| 75 | `image_editing_failed` | Image editing failed | Bilderedigering feilet |
| 76 | `video_editing_failed` | Video editing failed | Videoredigering feilet |
| 77 | `image_editing_cancelled` | Image editing cancelled | Bilderedigering avbrutt |
| 78 | `video_editing_cancelled` | Video editing cancelled | Videoredigering avbrutt |
| 79 | `file_edited_successfully` | File edited successfully | Fil vellykket redigert |
| 80 | `image_edited_successfully` | Image edited successfully | Bilde vellykket redigert |
| 81 | `video_edited_successfully` | Video edited successfully | Video vellykket redigert |
| 82 | `edit_image_with` | Edit image with: | Rediger bilde med: |
| 83 | `edit_video_with` | Edit video with: | Rediger video med: |
| 84 | `no_image_editor_found` | No image editor found | Ingen bilderedigeringsapp funnet |
| 85 | `no_video_editor_found` | No video editor found | Ingen videoredigeringsapp funnet |
| 86 | `unknown_file_location` | Unknown file location | Ukjent filplassering |
| 87 | `overwrite_original` | Overwrite original | Overskriv original |
| 88 | `error_saving_file` | Could not overwrite the source file | Kunne ikke overskrive kildefilen |
| 89 | `rotate_left` | Rotate left | Roter til venstre |
| 90 | `rotate_right` | Rotate right | Roter til høyre |
| 91 | `rotate_one_eighty` | Rotate by 180º | Roter med 180º |
| 92 | `transform` | Transform | Transformer |
| 93 | `crop` | Crop | Beskjær |
| 94 | `draw` | Draw | Tegn |
| 95 | `flip` | Flip | Speilvend |
| 96 | `flip_horizontally` | Flip horizontally | Speilvend horisontalt |
| 97 | `flip_vertically` | Flip vertically | Speilvend vertikalt |
| 98 | `free_aspect_ratio` | Free | Fri |
| 99 | `other_aspect_ratio` | Other | Annen |
| 100 | `wallpaper` | Wallpaper | Bakgrunnsbilde |
| 101 | `set_as_wallpaper` | Set as Wallpaper | Sett som bakgrunnsbilde |
| 102 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | Å sette som bakgrunnsbilde feilet |
| 103 | `set_as_wallpaper_with` | Set as wallpaper with: | Sett som bakgrunnsbilde med: |
| 104 | `setting_wallpaper` | Setting wallpaper… | Stiller inn bakgrunnsbilde… |
| 105 | `wallpaper_set_successfully` | Wallpaper set successfully | Bakgrunnsbilde vellykket stilt inn |
| 106 | `portrait_aspect_ratio` | Portrait aspect ratio | Portrett-sideforhold |
| 107 | `landscape_aspect_ratio` | Landscape aspect ratio | Landskap-sideforhold |
| 108 | `home_screen` | Home screen | Startskjerm |
| 109 | `lock_screen` | Lock screen | Låseskjerm |
| 110 | `home_and_lock_screen` | Home and lock screen | Start- og låseskjerm |
| 111 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | Tillat endring av sideforholdet |
| 112 | `slideshow` | Slideshow | Lysbildeshow |
| 113 | `interval` | Interval | Intervall |
| 114 | `include_photos` | Include photos | Inkluder bilder |
| 115 | `include_videos` | Include videos | Inkluder videoer |
| 116 | `include_gifs` | Include GIFs | Inkluder GIF-bilder |
| 117 | `random_order` | Random order | Tilfeldig rekkefølge |
| 118 | `move_backwards` | Move backwards | Avspill bakover |
| 119 | `loop_slideshow` | Loop slideshow | Gjenta lysbildeshow |
| 120 | `animation` | Animation | Animasjon |
| 121 | `no_animation` | None | Ingen |
| 122 | `fade` | Fade | Uttoning |
| 123 | `slide` | Slide | Glidning |
| 124 | `slideshow_ended` | The slideshow ended | Lysbildeshowet er slutt |
| 125 | `no_media_for_slideshow` | No media for the slideshow have been found | Ingen media for lysbildeshowet er funnet |
| 126 | `group_direct_subfolders` | Group direct subfolders | Grupper direkte undermapper |
| 127 | `group_by` | Group by | Grupper etter |
| 128 | `do_not_group_files` | Do not group files | Ikke grupper filer |
| 129 | `by_folder` | Folder | Mappe |
| 130 | `by_last_modified` | Last modified | Sist endret |
| 131 | `by_last_modified_daily` | Last modified (daily) | Sist endret (daglig) |
| 132 | `by_last_modified_monthly` | Last modified (monthly) | Sist endret (månedlig) |
| 133 | `by_date_taken` | Date taken | Dato tatt |
| 134 | `by_date_taken_daily` | Date taken (daily) | Dato tatt (daglig) |
| 135 | `by_date_taken_monthly` | Date taken (monthly) | Dato tatt (månedlig) |
| 136 | `by_file_type` | File type | Filtype |
| 137 | `by_extension` | Extension | Endelse |
| 138 | `show_file_count_section_header` | Show file count at section headers | Vis filantall i seksjonsoverskrifter |
| 139 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | Vær oppmerksom på at gruppering og sortering er to uavhengige områder |
| 140 | `folder_on_widget` | Folder shown on the widget: | Mappe vist på modulen: |
| 141 | `show_folder_name` | Show folder name | Vis mappenavn |
| 142 | `autoplay_videos` | Play videos automatically | Avspill videoer automatisk |
| 143 | `remember_last_video_position` | Remember last video playback position | Husk siste videoavspillingsposisjon |
| 144 | `loop_videos` | Loop videos | Gjenta videoer |
| 145 | `video_double_tap_duration` | Double-tap seek duration | Søketid ved dobbelttrykk |
| 146 | `video_seek_duration_seconds` | %d seconds | %d sekunder |
| 147 | `animate_gifs` | Animate GIFs at thumbnails | Animert GIF i minibildevisning |
| 148 | `max_brightness` | Max brightness when viewing fullscreen media | Maks lysstyrke ved mediavisning |
| 149 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Vis Ultra HDR-bilder i HDR |
| 150 | `crop_thumbnails` | Crop thumbnails into squares | Beskjær minibilder i kvadrater |
| 151 | `show_thumbnail_video_duration` | Show video durations | Vis videolengde |
| 152 | `screen_rotation_by` | Rotate fullscreen media by | Roter media etter |
| 153 | `screen_rotation_system_setting` | System setting | Systeminnstilling |
| 154 | `screen_rotation_device_rotation` | Device rotation | Enhetsrotasjon |
| 155 | `screen_rotation_aspect_ratio` | Aspect ratio | Sideforhold |
| 156 | `black_background_at_fullscreen` | Black background at fullscreen media | Svart bakgrunn og statuslinje i mediavisningen |
| 157 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | Horisontal rulling av minibilder |
| 158 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | Skjul automatisk systemlinjer ved mediavisning |
| 159 | `delete_empty_folders` | Delete empty folders after deleting their content | Slett tomme mapper etter sletting av deres innhold |
| 160 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | Hold skjermen på ved visning av bilder i fullskjermmodus |
| 161 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | Tillat å styre fotolysstyrke med vertikale bevegelser |
| 162 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | Tillat å styre videovolum og lysstyrke med vertikale bevegelser |
| 163 | `playback_speed` | Playback speed | Avspillingshastighet |
| 164 | `show_media_count` | Show folder media count on the main view | Vis mediaantallet i mapper på hovedvisningen |
| 165 | `show_extended_details` | Show extended details over fullscreen media | Vis flere detaljer i mediavisningen |
| 166 | `manage_extended_details` | Manage extended details | Velg detaljer |
| 167 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | Tillat en-finger-zoom i mediavisningen |
| 168 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | Tillat å skifte media øyeblikkelig ved å trykke på kanten av skjermen |
| 169 | `allow_deep_zooming_images` | Allow deep zooming images | Tillat dyp bildezooming |
| 170 | `hide_extended_details` | Hide extended details when status bar is hidden | Skjul utvidede detaljer når statuslinjen er skjult |
| 171 | `show_at_bottom` | Show some action buttons at the bottom of the screen | Vis noen handlingsknapper nederst på skjermen |
| 172 | `show_recycle_bin` | Show the recycle bin on the folders screen | Vis papirkurven på mappeskjermen |
| 173 | `deep_zoomable_images` | Deep zoomable images | Dyp zoombare bilder |
| 174 | `show_highest_quality` | Show images in the highest possible quality | Vis bilder i høyest mulig kvalitet |
| 175 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | Vis papirkurven som siste element på hovedskjermen |
| 176 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | Tillat lukking av mediavisningen med en nedoverbevegelse |
| 177 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | Tillat å zoome 1:1 med to dobbeltrykk |
| 178 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | Spill videoer på en separat skjerm med horisontale bevegelser |
| 179 | `on_video_tap` | On video tap | Ved videotrykk |
| 180 | `open_in_app_player` | Open in-app player | Åpne spilleren i appen |
| 181 | `open_system_default_player` | Open default player app | Åpne standardspilleren |
| 182 | `show_notch` | Show a notch if available | Vis flik hvis tilgjengelig |
| 183 | `allow_rotating_gestures` | Allow rotating images with gestures | Tillat rotering av bilder med fingerbevegelser |
| 184 | `file_loading_priority` | File loading priority | Fillastingsprioritet |
| 185 | `speed` | Speed | Hastighet |
| 186 | `compromise` | Compromise | Kompromiss |
| 187 | `avoid_showing_invalid_files` | Avoid showing invalid files | Unngå visning av ugyldige filer |
| 188 | `show_image_file_types` | Show image file types | Vis bildefiltyper |
| 189 | `allow_zooming_videos` | Allow zooming videos with double tapping them | Tillat zooming av videoer ved å dobbelttrykke dem |
| 190 | `folder_thumbnail_style` | Folder thumbnail style | Minibildestil for mapper |
| 191 | `file_thumbnail_style` | File thumbnail style | Minibildestil for filer |
| 192 | `mark_favorite_items` | Mark favorite items | Marker favorittelementer |
| 193 | `thumbnail_spacing` | Thumbnail spacing | Minibildemellomrom |
| 194 | `show_file_count_line` | Show file count on a separate line | Vis antall filer på en separat linje |
| 195 | `show_file_count_brackets` | Show file count in brackets | Vis antall filer i parentes |
| 196 | `show_file_count_none` | Do not show file count | Ikke vis antall filer |
| 197 | `limit_folder_title` | Limit long folder titles to 1 line | Begrens lange mappetitler til 1 linje |
| 198 | `square` | Square | Firkantet |
| 199 | `rounded_corners` | Rounded corners | Avrundede hjørner |
| 200 | `export_favorite_paths` | Export favorites | Eksporter favoritter |
| 201 | `import_favorite_paths` | Import favorites | Importer favoritter |
| 202 | `paths_imported_successfully` | Paths imported successfully | Filbaner importert |
| 203 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | For å være sikker på at alle filoperasjoner fungerer pålitelig, gjør denne appen til en mediaadministrasjonsapp i enhetsinnstillingene. |
| 204 | `password_protect_excluded` | Password protect excluded folder visibility | Passordbeskytt synlighet for ekskludert mappe |
| 205 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Noe gikk galt. Gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. |
| 206 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis omdirigeringen ikke fungerer, gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. |
| 207 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Hvis du ikke vil gjøre det, kan du også gå til enhetens Innstillinger – Apper – Spesiell apptilgang – Apper for mediaadministrasjon, og tillat at denne appen administrerer media. |
| 208 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | Alternativt kan du kun tillate tilgang til mediafiler. I dette tilfellet vil du riktignok ikke kunne arbeide med skjulte filer. |
| 209 | `media_only` | Media only | Kun media |
| 210 | `all_files` | All files | Alle filer |
| 211 | `search_all_files` | Search all files instead of folders on the main screen | Søk alle filer istedenfor mapper på hovedskjermen |
| 212 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | Vis en menyknapp for å raskt slå av/på \"Vis alle mappers innhold\" |
| 213 | `thumbnails` | Thumbnails | Minibilder |
| 214 | `fullscreen_media` | Fullscreen media | Mediavisning |
| 215 | `extended_details` | Extended details | Utvidede detaljer |
| 216 | `bottom_actions` | Bottom actions | Handlinger nederst |
| 217 | `manage_bottom_actions` | Manage visible bottom actions | Behandle nedre handlingsknapper |
| 218 | `toggle_favorite` | Toggle favorite | Slå av/på favoritt |
| 219 | `toggle_file_visibility` | Toggle file visibility | Filsynlighet av/på |
| 220 | `failed_to_load_media` | Failed to load media. | Kunne ikke laste inn media. |
| 221 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | Filen er ugyldig eller ødelagt. |
| 222 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Mediet overstiger enhetens kapasitet. |
| 223 | `unsupported_format` | Unsupported format. | Ikke-støttet format. |
| 224 | `video_no_sound` | This video has no audio | Denne videoen har ingen lyd |
| 225 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | Hvordan kan jeg gjøre Fossify Gallery til enhetens standardgalleri? |
| 226 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\".         The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Først må du finne det nåværende standardgalleriet i Apper-delen av enhetsinnstillingene, se etter en knapp som heter noe slikt som \"Åpne som standard\", trykke på den og deretter velge \"Fjern standardvalg\".\nNeste gang du prøver å åpne et bilde eller en video, skal du se en appvelger der du kan velge Fossify Gallery og gjøre den til standardapp. |
| 227 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Jeg låste appen med et passord, men jeg har glemt det. Hva kan jeg gjøre? |
| 228 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Du kan løse det på to måter. Du kan enten installere appen på nytt, eller finne appen i enhetsinnstillingene og velge \"Slett data\". Det vil tilbakestille alle innstillingene dine, men det vil ikke fjerne noen mediafiler. |
| 229 | `faq_3_title` | How can I make an album always appear at the top? | Hvordan kan jeg få et album til å alltid vises øverst? |
| 230 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Du kan trykke og holde på ønsket album og velge Fest-ikonet i handlingsmenyen for å feste det til toppen. Du kan også feste flere mapper; festede elementer blir sortert etter standard sorteringsmetode. |
| 231 | `faq_4_title` | How can I fast forward videos? | Hvordan kan jeg spole fremover i videoer? |
| 232 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Du kan gjøre det ved å dobbelttrykke på siden av skjermen, eller trykke på teksten for nåværende eller maksimal varighet nær søkelinjen. Hvis du aktiverer avspilling av videoer på en separat skjerm i appinnstillingene, kan du også bruke horisontale bevegelser. |
| 233 | `faq_5_title` | What is the difference between hiding and excluding a folder? | Hva er forskjellen mellom å skjule og å ekskludere en mappe? |
| 234 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Ekskluder hindrer at mappen vises kun i Fossify Gallery, mens Skjul fungerer på systemnivå og skjuler mappen også for andre gallerier. Det fungerer ved å opprette en tom \".nomedia\"-fil i den aktuelle mappen, som du også kan fjerne med en hvilken som helst filbehandler. Merk at enkelte enheter ikke tillater å skjule mapper som Camera, Screenshots og Downloads. |
| 235 | `faq_6_title` | Why do folders with music cover art or stickers show up? | Hvorfor vises mapper med plateomslag eller klistremerker? |
| 236 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Det kan skje at uvanlige album dukker opp. Du kan enkelt ekskludere dem ved å trykke og holde på dem og velge Ekskluder. I den neste dialogen kan du så velge den overordnede mappen, noe som sannsynligvis også vil hindre at andre relaterte album vises. |
| 237 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | En mappe med bilder vises ikke, eller den viser ikke alle elementer. Hva kan jeg gjøre? |
| 238 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Det kan ha flere årsaker, men det er enkelt å løse. Bare gå til Innstillinger -&gt; Håndter inkluderte mapper, velg Pluss og naviger til ønsket mappe. |
| 239 | `faq_8_title` | What if I want just a few particular folders visible? | Hva om jeg bare vil at noen bestemte mapper skal være synlige? |
| 240 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders.         That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Å legge til en mappe under Inkluderte mapper ekskluderer ikke automatisk noe annet. Det du kan gjøre er å gå til Innstillinger -&gt; Håndter ekskluderte mapper, ekskludere rotmappen \"/\", og deretter legge til ønskede mapper under Innstillinger -&gt; Håndter inkluderte mapper.\nDet vil gjøre bare de valgte mappene synlige, siden både ekskludering og inkludering er rekursive, og hvis en mappe både er ekskludert og inkludert, vil den vises. |
| 241 | `faq_10_title` | Can I crop images with this app? | Kan jeg beskjære bilder med denne appen? |
| 242 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Ja, du kan beskjære bilder i redigeringsverktøyet ved å dra i bildehjørnene. Du kommer til redigeringsverktøyet enten ved å trykke og holde på et minibilde og velge Rediger, eller ved å velge Rediger fra fullskjermvisningen. |
| 243 | `faq_11_title` | Can I somehow group media file thumbnails? | Kan jeg på noen måte gruppere minibilder av mediafiler? |
| 244 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Selvfølgelig, bare bruk menyelementet \"Grupper etter\" når du er i minibildevisningen. Du kan gruppere filer etter flere kriterier, inkludert Dato tatt. Hvis du bruker funksjonen \"Vis alle mappers innhold\", kan du også gruppere dem etter mapper. |
| 245 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | Sortering etter Dato tatt ser ikke ut til å fungere skikkelig, hvordan kan jeg fikse det? |
| 246 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Det skyldes sannsynligvis at filene er kopiert fra et annet sted. Du kan fikse det ved å merke minibildene og velge \"Korriger Dato tatt-verdi\". |
| 247 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Jeg ser fargebånd på bildene. Hvordan kan jeg forbedre kvaliteten? |
| 248 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | Den nåværende løsningen for bildevisning fungerer fint i de aller fleste tilfeller, men hvis du ønsker enda bedre bildekvalitet, kan du aktivere \"Vis bilder i høyest mulig kvalitet\" i appinnstillingene under delen \"Dyp zoombare bilder\". |
| 249 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Jeg har skjult en fil/mappe. Hvordan kan jeg gjøre den synlig igjen? |
| 250 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Du kan enten trykke på menyelementet \"Vis skjulte elementer midlertidig\" på hovedskjermen, eller slå på \"Vis skjulte elementer\" i appinnstillingene for å se det skjulte elementet. Hvis du vil gjøre det synlig igjen, trykker og holder du bare på det og velger \"Vis igjen\". Mapper skjules ved å legge til en skjult \".nomedia\"-fil i dem; du kan også slette denne filen med en hvilken som helst filbehandler. Merk imidlertid at skjuling fungerer rekursivt, så hvis du skjuler en mappe, blir alle undermapper også skjult. For å gjøre undermappene synlige igjen må du derfor gjøre den overordnede mappen synlig igjen. |
| 251 | `faq_15_title` | Why does the app take up so much space? | Hvorfor tar appen opp så mye plass? |
| 252 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | Appens hurtigbuffer kan ta opptil 250 MB; den sørger for raskere innlasting av bilder. Hvis appen tar opp enda mer plass, skyldes det sannsynligvis at du har elementer i papirkurven. Disse filene teller med i appstørrelsen. Du kan tømme papirkurven ved å åpne den og slette alle filer, eller fra appinnstillingene. Hver fil i papirkurven slettes automatisk etter 30 dager. |
| 253 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | Hva skjedde med skjuling av filer og mapper, og hvorfor kan jeg ikke se skjulte elementer lenger? |
| 254 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | Fra og med Android 11 kan du ikke lenger skjule/vise filer eller mapper, du kan heller ikke se de skjulte i galleriapper pga. systemrestriksjoner. Du må bruke en filbehandlingsapp for det. |
| 255 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | Eller du kan også gi dette galleriet tilgang til Alle filer via enhetsinnstillingene; det lar oss vise skjulte elementer og gjør filoperasjoner mer pålitelige generelt. |
| 256 | `faq_17_title` | Why cannot I include missing folders anymore? | Hvorfor kan jeg ikke inkludere manglende mapper lenger? |
| 257 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Det sluttet også å fungere på grunn av systemendringene som kom med Android 11; appen kan ikke lenger bla gjennom ekte mapper, men er avhengig av såkalte MediaStore for å hente data. |
| 258 | `faq_18_title` | Why do I see ads during video playback? | Hvorfor ser jeg reklame under videoavspilling? |
| 259 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Appene våre har ingen reklame overhodet. Hvis du ser reklame under videoavspilling, bruker du sannsynligvis videospilleren til en annen app. Prøv å finne standard videospiller i enhetsinnstillingene, og velg \"Fjern standardvalg\" på den. Neste gang du åpner en video, vil du se en appvelger der du kan velge hvilken app du vil bruke. |
| 260 | `create_virtual_album` | Create virtual album | Opprett virtuelt album |
| 261 | `new_virtual_album` | New album… | Nytt album… |
| 262 | `rename_virtual_album` | Rename album | Gi albumet nytt navn |
| 263 | `album_name` | Album name | Albumnavn |
| 264 | `virtual_folder_name` | Virtual folder name | Navn på virtuell mappe |
| 265 | `virtual_album_name_empty` | The album name cannot be empty | Albumnavnet kan ikke være tomt |
| 266 | `virtual_album_exists` | An album with this name already exists | Et album med dette navnet finnes allerede |
| 267 | `add_to_virtual_album` | Add to album | Legg til i album |
| 268 | `remove_from_virtual_album` | Remove from album | Fjern fra album |
| 269 | `added_to_virtual_album` | Added to \"%s\" | Lagt til i \"%s\" |
| 270 | `already_in_virtual_album` | Already in \"%s\" | Allerede i \"%s\" |
| 271 | `removed_from_virtual_album` | Removed from the album | Fjernet fra albumet |
| 272 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | Slette albumet %s? Bare albumet fjernes, bildene og videoene dine forblir der de er. |
| 273 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Dette albumet er tomt. Velg elementer i en hvilken som helst mappe og bruk \"Kopier til\". |
| 274 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Denne handlingen er ikke tilgjengelig for virtuelle album |
| 275 | `alphanumeric_password` | Password | Passord |
| 276 | `enter_password` | Enter password | Skriv inn passord |
| 277 | `repeat_password` | Repeat password | Gjenta passord |
| 278 | `wrong_password` | Wrong password | Feil passord |
| 279 | `encrypting_files` | Encrypting files… | Krypterer filer… |
| 280 | `encrypting_folder` | Encrypting folder… | Krypterer mappe… |
| 281 | `folder_encrypted_successfully` | Folder encrypted successfully | Mappen ble kryptert |
| 282 | `decrypting_folder` | Decrypting folder… | Dekrypterer mappe… |
| 283 | `folder_decrypted_successfully` | Folder decrypted successfully | Mappen ble dekryptert |
| 284 | `decrypting_files` | Decrypting files… | Dekrypterer filer… |
| 285 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Plasser fingeren på fingeravtrykkssensoren |
| 286 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Koble sammen minst 4 punkter |
| 287 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | Vil du få tilgang til din eksisterende Cipher_Secrets-mappe? |
| 288 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Eksisterende Cipher_Secrets-mappe vil bli slettet og en ny vil bli opprettet. Er du sikker? |
| 289 | `confirm_checkbox_label` | I confirm | Jeg bekrefter |
| 290 | `secret_vault` | Secret Vault | Hemmelig hvelv |
| 291 | `secret_vault_empty` | Secret Vault is empty | Det hemmelige hvelvet er tomt |
| 292 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | Er du sikker på at du vil dekryptere de(n) skjulte filen(e)? |
| 293 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Kopiering til utklippstavlen er deaktivert av sikkerhetshensyn mens hvelvet er låst opp. |
| 294 | `on_vault_exit` | On leaving hidden section | Når du forlater skjult seksjon |
| 295 | `on_vault_exit_keep_showing_all` | Keep showing all hidden albums | Fortsett å vise alle skjulte album |
| 296 | `on_vault_exit_show_encrypted_only` | Only show encrypted albums | Vis kun krypterte album |
| 297 | `on_vault_exit_show_other_only` | Only show other hidden albums | Vis kun andre skjulte album |
| 298 | `on_vault_exit_hide_all` | Hide all hidden albums | Skjul alle skjulte album |
| 299 | `on_vault_exit_ask_every_time` | Always ask | Spør alltid |
| 300 | `vault_exit_dialog_title` | Exiting Hidden Section | Forlater skjult seksjon |
| 301 | `vault_exit_keep_showing` | Keep showing hidden albums | Fortsett å vise skjulte album |
| 302 | `vault_exit_hide` | Hide hidden albums | Skjul skjulte album |
| 303 | `vault_exit_remember_my_choice` | Remember my choice | Husk valget mitt |
| 304 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving hidden section | For flere alternativer, se Innstillinger &gt; Når du forlater skjult seksjon |
| 305 | `system_default` | System default | Systemstandard |
| 306 | `language` | Language | Språk |
| 307 | `open_system_language_settings` | Open system language settings | Åpne systemets språkinnstillinger |
| 308 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | Er du sikker på du vil gjenopprette %d fil? |
| 309 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | Er du sikker på du vil gjenopprette %d filer? |
| 310 | `failed_to_resize_images` (`one`) | Failed to resize %d image | Klarte ikke endre størrelse på %d bilde |
| 311 | `failed_to_resize_images` (`other`) | Failed to resize %d images | Klarte ikke endre størrelse på %d bilder |
| 312 | `search` *(fossify-commons)* | *(inherited from commons)* | Søk |
