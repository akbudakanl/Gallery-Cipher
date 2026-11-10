# Norwegian Nynorsk (values-nn) Translation Completion & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-nn/strings.xml` was audited from scratch against `app/src/main/res/values/strings.xml`. Previously, `values-nn` contained only 23 `<string>` entries (and 0 `<plurals>`). Three inaccurate/typoed existing entries were fixed, and all 285 missing `<string>` entries plus 2 `<plurals>` entries (`restore_confirmation` and `failed_to_resize_images`) were translated into Norwegian Nynorsk, bringing `values-nn` to full parity (307 `<string>` + 2 `<plurals>` + `search` inherited from `fossify-commons` = 310 resource entries; 312 total comparison rows with plural forms expanded).
> **Key Terminology:** `Settings` → `Innstillingar` · `Default` → `Standard` / `Systemstandard` · `Folder` → `Mappe` · `Album` → `Album` · `Secret Vault` → `Hemmeleg kvelv` · `Hidden` → `Skjult` · `Excluded` → `Ekskludert` · `Password` → `Passord` · `Encrypt / Decrypt` → `Kryptera / Dekryptera` · `Thumbnails` → `Minibilete` · `Recycle bin` → `Papirkorg` · `Clipboard` → `Utklippstavla`

## 1. Review of 23 Previously Existing Translations and Changes Made

| Key (`name`) | Old `values-nn` | New `values-nn` | Status / Notes |
|---|---|---|---|
| `app_launcher_name` | Galleri | Galleri | Kept |
| `edit` | Gjer om | Rediger | **Fixed** (*Gjer om* means undo/redo; standard Nynorsk for *Edit* is *Rediger*) |
| `open_camera` | Opna kameraet | Opna kameraet | Kept |
| `pin_folder` | Fest mappa | Fest mappa | Kept |
| `unpin_folder` | Løys mappa | Løys mappa | Kept |
| `pin_to_the_top` | Fest øvst | Fest øvst | Kept |
| `show_all` | Syn innhaldet i alle mapper | Syn innhaldet i alle mapper | Kept |
| `all_folders` | Alle mapper | Alle mapper | Kept |
| `folder_view` | Byt til mappesyning | Byt til mappesyning | Kept |
| `other_folder` | Anna mappe | Anna mappe | Kept |
| `show_on_map` | Syn på kartet | Syn på kartet | Kept |
| `unknown_location` | Ukjend plassering | Ukjend plassering | Kept |
| `volume` | Ljodstyrke | Ljodstyrke | Kept |
| `brightness` | Ljosstyrke | Ljosstyrke | Kept |
| `force_portrait` | Tvinga portrettmodus | Tvinga portrettmodus | Kept |
| `force_landscape` | Tvinga landskapsmodus | Tvinga landskapsmodus | Kept |
| `force_landscape_reverse` | Tvinga landskapsmodus (vrengd) | Tvinga landskapsmodus (omvend) | **Fixed** (*vrengd* means turned inside out; standard Nynorsk for *reverse* is *omvend*) |
| `fix_date_taken` | Retta dato på opptak | Retta dato på opptak | Kept |
| `fixing` | Rettar… | Rettar… | Kept |
| `language` | Språk | Språk | Kept |
| `search` | Søk | Søk | Kept (Used in ``fossify-commons`` UI) |
| `system_default` | Systemstandard | Systemstandard | Kept |
| `open_system_language_settings` | Opna språkiinnstillingane i systemet | Opna språkinnstillingane i systemet | **Fixed** (Removed double-``i`` typo in *språkiinnstillingane* → *språkinnstillingane*) |

> **Added Entries:** All remaining 285 `<string>` keys and 2 `<plurals>` blocks (4 CLDR `one`/`other` items) from `values/strings.xml` were translated following standard Norwegian Nynorsk orthography and consistent terminology with the existing entries (e.g., `syna`, `ljosstyrke`, `ljodstyrke`, `bilete`, `minibilete`). See Section 2 for the complete 1-to-1 English comparison.

## 2. Full 1-to-1 Comparison: English vs. Norwegian Nynorsk

| # | Key (`name`) | English (`values/strings.xml`) | Norwegian Nynorsk (`values-nn/strings.xml`) |
|---:|---|---|---|
| 1 | `app_launcher_name` | Gallery | Galleri |
| 2 | `edit` | Edit | Rediger |
| 3 | `open_camera` | Open camera | Opna kameraet |
| 4 | `hidden` | (hidden) | (skjult) |
| 5 | `excluded` | (excluded) | (ekskludert) |
| 6 | `pin_folder` | Pin folder | Fest mappa |
| 7 | `unpin_folder` | Unpin folder | Løys mappa |
| 8 | `pin_to_the_top` | Pin to the top | Fest øvst |
| 9 | `show_all` | Show all folders content | Syn innhaldet i alle mapper |
| 10 | `all_folders` | All folders | Alle mapper |
| 11 | `folder_view` | Switch to folder view | Byt til mappesyning |
| 12 | `other_folder` | Other folder | Anna mappe |
| 13 | `show_on_map` | Show on map | Syn på kartet |
| 14 | `unknown_location` | Unknown location | Ukjend plassering |
| 15 | `volume` | Volume | Ljodstyrke |
| 16 | `brightness` | Brightness | Ljosstyrke |
| 17 | `lock_orientation` | Lock orientation | Lås skjermretning |
| 18 | `unlock_orientation` | Unlock orientation | Lås opp skjermretning |
| 19 | `change_orientation` | Change orientation | Endra retning |
| 20 | `force_portrait` | Force portrait | Tvinga portrettmodus |
| 21 | `force_landscape` | Force landscape | Tvinga landskapsmodus |
| 22 | `force_landscape_reverse` | Force landscape (reverse) | Tvinga landskapsmodus (omvend) |
| 23 | `use_default_orientation` | Use default orientation | Bruk standardretning |
| 24 | `fix_date_taken` | Fix Date Taken value | Retta dato på opptak |
| 25 | `fixing` | Fixing… | Rettar… |
| 26 | `dates_fixed_successfully` | Dates fixed successfully | Datoar er retta |
| 27 | `no_date_takens_found` | No Date Taken values have been found | Fann ingen verdiar for dato på opptak |
| 28 | `share_resized` | Share a resized version | Del ei utgåve med endra storleik |
| 29 | `switch_to_file_search` | Switch to file search across all visible folders | Byt til filsøk i alle synlege mapper |
| 30 | `set_as_default_folder` | Set as default folder | Set som standardmappe |
| 31 | `unset_as_default_folder` | Unset as default folder | Ikkje bruk som standardmappe lenger |
| 32 | `reorder_by_dragging` | Reorder folders by dragging | Endra mapperekkjefølgje ved å dra |
| 33 | `reorder_by_dragging_pro` | Reorder folders by dragging (Pro) | Endra mapperekkjefølgje ved å dra (Pro) |
| 34 | `restore_to_path` | Restoring to \'%s\' | Gjenopprettar til \'%s\' |
| 35 | `full_storage_permission_required` | Fossify Gallery needs full access to display all your photos and videos. Go to Settings &gt; Permissions &gt; Photos and videos &gt; Allow all. | Fossify Gallery treng full tilgang for å syna alle bileta og videoane dine. Gå til Innstillingar &gt; Løyve &gt; Bilete og videoar &gt; Tillat alle. |
| 36 | `filter_media` | Filter media | Filtrer media |
| 37 | `images` | Images | Bilete |
| 38 | `videos` | Videos | Videoar |
| 39 | `gifs` | GIFs | GIF-bilete |
| 40 | `raw_images` | RAW images | RAW-bilete |
| 41 | `svgs` | SVGs | SVG-bilete |
| 42 | `portraits` | Portraits | Portrett |
| 43 | `no_media_with_filters` | No media files have been found with the selected filters. | Fann ingen mediafiler med dei valde filtra. |
| 44 | `change_filters_underlined` | <u>Change filters</u> | <u>Endra filtre</u> |
| 45 | `hide_folder_description` | This function hides the folder by adding a \'.nomedia\' file into it, it will hide all subfolders too. You can see them by toggling the \'Show hidden items\' option in Settings. Continue? | Denne funksjonen skjuler mappa ved å leggja til ei \'.nomedia\'-fil i henne, og skjuler alle undermapper òg. Du kan sjå dei ved å slå på valet \'Syn skjulte element\' i Innstillingar. Halda fram? |
| 46 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | Dette vil ekskludera utvalet saman med undermapper berre frå Fossify Gallery. Du kan handsama ekskluderte mapper i Innstillingar. |
| 47 | `exclude_folder_parent` | Exclude a parent instead? | Ekskludera ei overordna mappe i staden? |
| 48 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | Ekskludering av mapper gjer dei og undermappene deira skjulte berre i Fossify Gallery; dei vil framleis vera synlege i andre appar.\n\nViss du vil skjula dei frå andre appar òg, bruk Skjul-funksjonen. |
| 49 | `hidden_folders` | Hidden folders | Skjulte mapper |
| 50 | `manage_hidden_folders` | Manage hidden folders | Handsama skjulte mapper |
| 51 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | Det ser ikkje ut til at du har nokon mapper skjulte med ei \".nomedia\"-fil. |
| 52 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | Du må gje appen tilgang til alle filer for å sjå skjulte filer, elles fungerer det ikkje. |
| 53 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Viss ei mappe eller ei av dei overordna mappene har eit punktum framfor namnet, er ho skjult og kan ikkje synast att slik. Du må fjerna punktumet ved å gje henne nytt namn. |
| 54 | `include_folders` | Included folders | Inkluderte mapper |
| 55 | `manage_included_folders` | Manage included folders | Handsama inkluderte mapper |
| 56 | `add_folder` | Add folder | Legg til mappe |
| 57 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | Viss du har mapper som inneheld media, men som ikkje vart attkjende av appen, kan du leggja dei til manuelt her.\n\nÅ leggja til element her ekskluderer ikkje andre mapper. |
| 58 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | Fann ingen mediafiler. Du kan løysa det ved å leggja til mappene som inneheld mediafiler manuelt. |
| 59 | `resize_and_save` | Resize selection and save | Endra storleik på utval og lagra |
| 60 | `width` | Width | Breidd |
| 61 | `height` | Height | Høgd |
| 62 | `keep_aspect_ratio` | Keep aspect ratio | Hald sideforhold |
| 63 | `invalid_values` | Please enter a valid resolution | Ver venleg og oppgje ei gyldig oppløysing |
| 64 | `resize_multiple_images` | Resize multiple images | Endra storleik på fleire bilete |
| 65 | `resize_factor` | Resize factor | Storleiksfaktor |
| 66 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | Endra storleiken på bilete til den oppgjevne prosenten; verdien må vera mellom 10 og 90. |
| 67 | `resize_factor_error` | Enter a number between 10 and 90 | Skriv inn eit tal mellom 10 og 90 |
| 68 | `images_resized_successfully` | Images resized successfully | Storleiken på bileta vart endra |
| 69 | `editor` | Editor | Redigering |
| 70 | `basic_editor` | Basic Editor | Enkel redigering |
| 71 | `advanced_editor` | Advanced Editor | Avansert redigering |
| 72 | `rotate` | Rotate | Roter |
| 73 | `invalid_image_path` | Invalid image path | Ugyldig biletsti |
| 74 | `invalid_video_path` | Invalid video path | Ugyldig videosti |
| 75 | `image_editing_failed` | Image editing failed | Biletredigering mislukkast |
| 76 | `video_editing_failed` | Video editing failed | Videoredigering mislukkast |
| 77 | `image_editing_cancelled` | Image editing cancelled | Biletredigering avbroten |
| 78 | `video_editing_cancelled` | Video editing cancelled | Videoredigering avbroten |
| 79 | `file_edited_successfully` | File edited successfully | Fila vart redigert |
| 80 | `image_edited_successfully` | Image edited successfully | Biletet vart redigert |
| 81 | `video_edited_successfully` | Video edited successfully | Videoen vart redigert |
| 82 | `edit_image_with` | Edit image with: | Rediger bilete med: |
| 83 | `edit_video_with` | Edit video with: | Rediger video med: |
| 84 | `no_image_editor_found` | No image editor found | Fann ingen biletredigeringsapp |
| 85 | `no_video_editor_found` | No video editor found | Fann ingen videoredigeringsapp |
| 86 | `unknown_file_location` | Unknown file location | Ukjend filplassering |
| 87 | `overwrite_original` | Overwrite original | Overskriv original |
| 88 | `error_saving_file` | Could not overwrite the source file | Kunne ikkje overskriva kjeldefila |
| 89 | `rotate_left` | Rotate left | Roter mot venstre |
| 90 | `rotate_right` | Rotate right | Roter mot høgre |
| 91 | `rotate_one_eighty` | Rotate by 180º | Roter 180º |
| 92 | `transform` | Transform | Transformer |
| 93 | `crop` | Crop | Beskjer |
| 94 | `draw` | Draw | Teikn |
| 95 | `flip` | Flip | Speglvend |
| 96 | `flip_horizontally` | Flip horizontally | Speglvend vassrett |
| 97 | `flip_vertically` | Flip vertically | Speglvend loddrett |
| 98 | `free_aspect_ratio` | Free | Fri |
| 99 | `other_aspect_ratio` | Other | Anna |
| 100 | `wallpaper` | Wallpaper | Bakgrunnsbilete |
| 101 | `set_as_wallpaper` | Set as Wallpaper | Set som bakgrunnsbilete |
| 102 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | Kunne ikkje setja som bakgrunnsbilete |
| 103 | `set_as_wallpaper_with` | Set as wallpaper with: | Set som bakgrunnsbilete med: |
| 104 | `setting_wallpaper` | Setting wallpaper… | Set bakgrunnsbilete… |
| 105 | `wallpaper_set_successfully` | Wallpaper set successfully | Bakgrunnsbiletet vart sett |
| 106 | `portrait_aspect_ratio` | Portrait aspect ratio | Portrett-sideforhold |
| 107 | `landscape_aspect_ratio` | Landscape aspect ratio | Landskap-sideforhold |
| 108 | `home_screen` | Home screen | Heimeskjerm |
| 109 | `lock_screen` | Lock screen | Låseskjerm |
| 110 | `home_and_lock_screen` | Home and lock screen | Heime- og låseskjerm |
| 111 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | Tillat endring av sideforholdet |
| 112 | `slideshow` | Slideshow | Lysbileteframsyning |
| 113 | `interval` | Interval | Intervall |
| 114 | `include_photos` | Include photos | Ta med foto |
| 115 | `include_videos` | Include videos | Ta med videoar |
| 116 | `include_gifs` | Include GIFs | Ta med GIF-bilete |
| 117 | `random_order` | Random order | Tilfeldig rekkjefølgje |
| 118 | `move_backwards` | Move backwards | Spel baklengs |
| 119 | `loop_slideshow` | Loop slideshow | Gjenta lysbileteframsyning |
| 120 | `animation` | Animation | Animasjon |
| 121 | `no_animation` | None | Ingen |
| 122 | `fade` | Fade | Ton ut |
| 123 | `slide` | Slide | Gliding |
| 124 | `slideshow_ended` | The slideshow ended | Lysbileteframsyninga er slutt |
| 125 | `no_media_for_slideshow` | No media for the slideshow have been found | Fann ikkje media til lysbileteframsyninga |
| 126 | `group_direct_subfolders` | Group direct subfolders | Grupper direkte undermapper |
| 127 | `group_by` | Group by | Grupper etter |
| 128 | `do_not_group_files` | Do not group files | Ikkje grupper filer |
| 129 | `by_folder` | Folder | Mappe |
| 130 | `by_last_modified` | Last modified | Sist endra |
| 131 | `by_last_modified_daily` | Last modified (daily) | Sist endra (dagleg) |
| 132 | `by_last_modified_monthly` | Last modified (monthly) | Sist endra (månadleg) |
| 133 | `by_date_taken` | Date taken | Dato teken |
| 134 | `by_date_taken_daily` | Date taken (daily) | Dato teken (dagleg) |
| 135 | `by_date_taken_monthly` | Date taken (monthly) | Dato teken (månadleg) |
| 136 | `by_file_type` | File type | Filtype |
| 137 | `by_extension` | Extension | Filending |
| 138 | `show_file_count_section_header` | Show file count at section headers | Syn filtal i seksjonsoverskrifter |
| 139 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | Merk at gruppering og sortering er to uavhengige felt |
| 140 | `folder_on_widget` | Folder shown on the widget: | Mappe synt på modulen: |
| 141 | `show_folder_name` | Show folder name | Syn mappenamn |
| 142 | `autoplay_videos` | Play videos automatically | Spel av videoar automatisk |
| 143 | `remember_last_video_position` | Remember last video playback position | Hugs siste videoavspelingsposisjon |
| 144 | `loop_videos` | Loop videos | Gjenta videoar |
| 145 | `video_double_tap_duration` | Double-tap seek duration | Søketid ved dobbelttrykk |
| 146 | `video_seek_duration_seconds` | %d seconds | %d sekund |
| 147 | `animate_gifs` | Animate GIFs at thumbnails | Animer GIF-bilete i minibilete |
| 148 | `max_brightness` | Max brightness when viewing fullscreen media | Maks ljosstyrke ved fullskjermsvisning |
| 149 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Syn Ultra HDR-bilete i HDR |
| 150 | `crop_thumbnails` | Crop thumbnails into squares | Beskjer minibilete til kvadrat |
| 151 | `show_thumbnail_video_duration` | Show video durations | Syn videolengd |
| 152 | `screen_rotation_by` | Rotate fullscreen media by | Roter fullskjermsmedia etter |
| 153 | `screen_rotation_system_setting` | System setting | Systeminnstilling |
| 154 | `screen_rotation_device_rotation` | Device rotation | Einingsrotasjon |
| 155 | `screen_rotation_aspect_ratio` | Aspect ratio | Sideforhold |
| 156 | `black_background_at_fullscreen` | Black background at fullscreen media | Svart bakgrunn ved fullskjermsmedia |
| 157 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | Rull minibilete vassrett |
| 158 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | Skjul systemlinjer automatisk ved fullskjermsmedia |
| 159 | `delete_empty_folders` | Delete empty folders after deleting their content | Slett tomme mapper etter sletting av innhaldet |
| 160 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | Hald skjermen på ved syning av bilete i fullskjerm |
| 161 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | Tillat styring av bileteljosstyrke med loddrette rørsler |
| 162 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | Tillat styring av videoljodstyrke og ljosstyrke med loddrette rørsler |
| 163 | `playback_speed` | Playback speed | Avspelingsfart |
| 164 | `show_media_count` | Show folder media count on the main view | Syn tal på mediafiler i mapper på hovudsyninga |
| 165 | `show_extended_details` | Show extended details over fullscreen media | Syn utvida detaljar over fullskjermsmedia |
| 166 | `manage_extended_details` | Manage extended details | Handsama utvida detaljar |
| 167 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | Tillat éin-finger-zoom i fullskjermsmedia |
| 168 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | Tillat å byta media straks ved å trykkja på skjermkantane |
| 169 | `allow_deep_zooming_images` | Allow deep zooming images | Tillat djup zooming på bilete |
| 170 | `hide_extended_details` | Hide extended details when status bar is hidden | Skjul utvida detaljar når statuslinja er skjult |
| 171 | `show_at_bottom` | Show some action buttons at the bottom of the screen | Syn nokre handlingsknappar nedst på skjermen |
| 172 | `show_recycle_bin` | Show the recycle bin on the folders screen | Syn papirkorga på mappeskjermen |
| 173 | `deep_zoomable_images` | Deep zoomable images | Djupt zoombare bilete |
| 174 | `show_highest_quality` | Show images in the highest possible quality | Syn bilete i høgst mogleg kvalitet |
| 175 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | Syn papirkorga som siste element på hovudskjermen |
| 176 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | Tillat å lukka fullskjermsvisninga med ei nedoverrørsle |
| 177 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | Tillat 1:1-zooming med to dobbelttrykk |
| 178 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | Spel videoar på ein eigen skjerm med vassrette rørsler |
| 179 | `on_video_tap` | On video tap | Ved videotrykk |
| 180 | `open_in_app_player` | Open in-app player | Opna spelar i appen |
| 181 | `open_system_default_player` | Open default player app | Opna standardspelarapp |
| 182 | `show_notch` | Show a notch if available | Syn skjermhakk viss tilgjengeleg |
| 183 | `allow_rotating_gestures` | Allow rotating images with gestures | Tillat rotering av bilete med fingerrørsler |
| 184 | `file_loading_priority` | File loading priority | Prioritet for fillasting |
| 185 | `speed` | Speed | Fart |
| 186 | `compromise` | Compromise | Kompromiss |
| 187 | `avoid_showing_invalid_files` | Avoid showing invalid files | Unngå å syna ugyldige filer |
| 188 | `show_image_file_types` | Show image file types | Syn biletfiltypar |
| 189 | `allow_zooming_videos` | Allow zooming videos with double tapping them | Tillat zooming av videoar med dobbelttrykk |
| 190 | `folder_thumbnail_style` | Folder thumbnail style | Minibiletestil for mapper |
| 191 | `file_thumbnail_style` | File thumbnail style | Minibiletestil for filer |
| 192 | `mark_favorite_items` | Mark favorite items | Marker favoritelement |
| 193 | `thumbnail_spacing` | Thumbnail spacing | Minibiletemellomrom |
| 194 | `show_file_count_line` | Show file count on a separate line | Syn filtal på ei eiga linje |
| 195 | `show_file_count_brackets` | Show file count in brackets | Syn filtal i parentes |
| 196 | `show_file_count_none` | Do not show file count | Ikkje syn filtal |
| 197 | `limit_folder_title` | Limit long folder titles to 1 line | Avgrens lange mappetitlar til 1 linje |
| 198 | `square` | Square | Firkanta |
| 199 | `rounded_corners` | Rounded corners | Avrunda hjørne |
| 200 | `export_favorite_paths` | Export favorites | Eksporter favorittar |
| 201 | `import_favorite_paths` | Import favorites | Importer favorittar |
| 202 | `paths_imported_successfully` | Paths imported successfully | Filstiar vart importerte |
| 203 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | For å vera sikker på at alle filoperasjonar fungerer påliteleg, gjer denne appen til ein mediaadministrasjonsapp i einingsinnstillingane. |
| 204 | `password_protect_excluded` | Password protect excluded folder visibility | Passordvern for synlegheita til ekskluderte mapper |
| 205 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Noko gjekk gale. Gå til eininga sine Innstillingar – Appar – Spesiell apptilgang – Appar for mediaadministrasjon, og lat denne appen administrera media. |
| 206 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Viss omdirigeringa ikkje fungerer, gå til eininga sine Innstillingar – Appar – Spesiell apptilgang – Appar for mediaadministrasjon, og lat denne appen administrera media. |
| 207 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Viss du ikkje vil gjera det, kan du òg gå til eininga sine Innstillingar – Appar – Spesiell apptilgang – Appar for mediaadministrasjon, og lat denne appen administrera media. |
| 208 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | Alternativt kan du berre tillata tilgang til mediafiler. I så fall vil du ikkje kunna arbeida med skjulte filer. |
| 209 | `media_only` | Media only | Berre media |
| 210 | `all_files` | All files | Alle filer |
| 211 | `search_all_files` | Search all files instead of folders on the main screen | Søk i alle filer i staden for mapper på hovudskjermen |
| 212 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | Syn ein menyknapp for raskt å slå av/på \"Syn innhaldet i alle mapper\" |
| 213 | `thumbnails` | Thumbnails | Minibilete |
| 214 | `fullscreen_media` | Fullscreen media | Fullskjermsmedia |
| 215 | `extended_details` | Extended details | Utvida detaljar |
| 216 | `bottom_actions` | Bottom actions | Handlingar nedst |
| 217 | `manage_bottom_actions` | Manage visible bottom actions | Handsama synlege nedre handlingar |
| 218 | `toggle_favorite` | Toggle favorite | Slå av/på favoritt |
| 219 | `toggle_file_visibility` | Toggle file visibility | Slå av/på filsynlegheit |
| 220 | `failed_to_load_media` | Failed to load media. | Kunne ikkje lasta inn media. |
| 221 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | Fila er ugyldig eller øydelagd. |
| 222 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Mediet overstig kapasiteten til eininga. |
| 223 | `unsupported_format` | Unsupported format. | Formatet er ikkje støtta. |
| 224 | `video_no_sound` | This video has no audio | Denne videoen har ingen ljod |
| 225 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | Korleis kan eg gjera Fossify Gallery til standardgalleriet på eininga? |
| 226 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\".         The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Fyrst må du finna det gjeldande standardgalleriet i Appar-delen av einingsinnstillingane, sjå etter ein knapp som heiter noko slikt som \"Opna som standard\", trykkja på han og deretter velja \"Fjern standardval\".\nNeste gong du prøver å opna eit bilete eller ein video, skal du sjå ein appveljar der du kan velja Fossify Gallery og gjera han til standardapp. |
| 227 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Eg låste appen med eit passord, men eg har gløymt det. Kva kan eg gjera? |
| 228 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Du kan løysa det på to måtar. Du kan anten installera appen på nytt, eller finna appen i einingsinnstillingane og velja \"Fjern data\". Det vil nullstilla alle innstillingane dine, men fjernar ingen mediafiler. |
| 229 | `faq_3_title` | How can I make an album always appear at the top? | Korleis kan eg få eit album til alltid å synast øvst? |
| 230 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Du kan trykkja og halda på ønskt album og velja Fest-ikonet i handlingsmenyen for å festa det øvst. Du kan festa fleire mapper òg; festa element vert sorterte etter standard sorteringsmetode. |
| 231 | `faq_4_title` | How can I fast forward videos? | Korleis kan eg spola framover i videoar? |
| 232 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Du kan gjera det ved å dobbelttrykkja på sida av skjermen, eller trykkja på teksten for gjeldande eller maksimal lengd nær søkelinja. Viss du slår på avspeling av videoar på ein eigen skjerm i appinnstillingane, kan du bruka vassrette rørsler òg. |
| 233 | `faq_5_title` | What is the difference between hiding and excluding a folder? | Kva er skilnaden mellom å skjula og å ekskludera ei mappe? |
| 234 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Ekskluder hindrar at mappa vert synt berre i Fossify Gallery, medan Skjul verkar på systemnivå og skjuler mappa frå andre galleri òg. Det verkar ved å laga ei tom \".nomedia\"-fil i den gjeldande mappa, som du òg kan fjerna med ein kva som helst filhandsamar. Merk at somme einingar ikkje tillèt å skjula mapper som Camera, Screenshots og Downloads. |
| 235 | `faq_6_title` | Why do folders with music cover art or stickers show up? | Kvifor dukkar det opp mapper med plateomslag eller klistremerke? |
| 236 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Det kan henda at uvanlege album dukkar opp. Du kan enkelt ekskludera dei ved å trykkja og halda på dei og velja Ekskluder. I den neste dialogen kan du velja den overordna mappa, noko som truleg hindrar at andre relaterte album vert synte òg. |
| 237 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | Ei mappe med bilete vert ikkje synt, eller ho syner ikkje alle elementa. Kva kan eg gjera? |
| 238 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Det kan ha fleire årsaker, men det er enkelt å løysa. Berre gå til Innstillingar -&gt; Handsama inkluderte mapper, vel Pluss og naviger til den ønskte mappa. |
| 239 | `faq_8_title` | What if I want just a few particular folders visible? | Kva om eg berre vil at nokre bestemte mapper skal vera synlege? |
| 240 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders.         That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Å leggja til ei mappe under Inkluderte mapper ekskluderer ikkje automatisk noko anna. Det du kan gjera, er å gå til Innstillingar -&gt; Handsama ekskluderte mapper, ekskludera rotmappa \"/\", og deretter leggja til dei ønskte mappene under Innstillingar -&gt; Handsama inkluderte mapper.\nDet vil gjera berre dei valde mappene synlege, sidan både ekskludering og inkludering er rekursive, og viss ei mappe både er ekskludert og inkludert, vil ho verta synt. |
| 241 | `faq_10_title` | Can I crop images with this app? | Kan eg beskjera bilete med denne appen? |
| 242 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Ja, du kan beskjera bilete i redigeringsverktøyet ved å dra i bilethjørna. Du kjem til redigeringsverktøyet anten ved å trykkja og halda på eit minibilete og velja Rediger, eller ved å velja Rediger frå fullskjermsvisninga. |
| 243 | `faq_11_title` | Can I somehow group media file thumbnails? | Kan eg på nokon måte gruppera minibilete av mediafiler? |
| 244 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Sjølvsagt, berre bruk menyelementet \"Grupper etter\" når du er i minibiletevisninga. Du kan gruppera filer etter fleire kriterium, inkludert Dato teken. Viss du bruker funksjonen \"Syn innhaldet i alle mapper\", kan du gruppera dei etter mapper òg. |
| 245 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | Sortering etter Dato teken ser ikkje ut til å fungera skikkeleg, korleis kan eg retta det? |
| 246 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Det kjem truleg av at filene er kopierte frå ein annan stad. Du kan retta det ved å merkja filminibileta og velja \"Retta dato på opptak\". |
| 247 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Eg ser fargeband på bileta. Korleis kan eg forbetra kvaliteten? |
| 248 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | Den gjeldande løysinga for biletsyning fungerer fint i dei aller fleste tilfelle, men viss du vil ha endå betre biletkvalitet, kan du slå på \"Syn bilete i høgst mogleg kvalitet\" i appinnstillingane under delen \"Djupt zoombare bilete\". |
| 249 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Eg har skjult ei fil/mappe. Korleis kan eg gjera henne synleg att? |
| 250 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Du kan anten trykkja på menyelementet \"Syn skjulte element mellombels\" på hovudskjermen, eller slå på \"Syn skjulte element\" i appinnstillingane for å sjå det skjulte elementet. Viss du vil gjera det synleg att, trykkjer og held du berre på det og vel \"Syn att\". Mapper vert skjulte ved å leggja ei skjult \".nomedia\"-fil i dei; du kan sletta denne fila med ein kva som helst filhandsamar òg. Merk likevel at skjuling verkar rekursivt, så viss du skjuler ei mappe, vert alle undermapper skjulte òg. For å gjera undermappene synlege att må du difor gjera den overordna mappa synleg att. |
| 251 | `faq_15_title` | Why does the app take up so much space? | Kvifor tek appen opp så mykje plass? |
| 252 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | Appen sitt mellomlager kan ta opptil 250 MB; det syter for raskare lasting av bilete. Viss appen tek opp endå meir plass, kjem det truleg av at du har element i papirkorga. Desse filene tel med i appstorleiken. Du kan tøma papirkorga ved å opna henne og sletta alle filer, eller frå appinnstillingane. Kvar fil i papirkorga vert sletta automatisk etter 30 dagar. |
| 253 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | Kva skjedde med skjuling av filer og mapper, og kvifor kan eg ikkje sjå skjulte element lenger? |
| 254 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | Frå og med Android 11 kan du ikkje lenger skjula eller syna filer eller mapper, og du kan heller ikkje sjå dei skjulte i galleriappar på grunn av systemavgrensingar. Du må bruka ein filhandsamar til det. |
| 255 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | Eller du kan gje dette galleriet tilgang til Alle filer via einingsinnstillingane; det lèt oss syna skjulte element og gjer filoperasjonar meir pålitelege generelt. |
| 256 | `faq_17_title` | Why cannot I include missing folders anymore? | Kvifor kan eg ikkje inkludera manglande mapper lenger? |
| 257 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Det slutta òg å fungera på grunn av systemendringane som kom med Android 11; appen kan ikkje bla gjennom verkelege mapper lenger, men er avhengig av såkalla MediaStore for å henta data. |
| 258 | `faq_18_title` | Why do I see ads during video playback? | Kvifor ser eg reklame under videoavspeling? |
| 259 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Appane våre har inga reklame i det heile. Viss du ser reklame under videoavspeling, bruker du truleg videospelaren til ein annan app. Prøv å finna standard videospelar i einingsinnstillingane, og vel \"Fjern standardval\" på han. Neste gong du opnar ein video, vil du sjå ein appveljar der du kan velja kva app du vil bruka. |
| 260 | `create_virtual_album` | Create virtual album | Lag virtuelt album |
| 261 | `new_virtual_album` | New album… | Nytt album… |
| 262 | `rename_virtual_album` | Rename album | Gje albumet nytt namn |
| 263 | `album_name` | Album name | Albumnamn |
| 264 | `virtual_folder_name` | Virtual folder name | Namn på virtuell mappe |
| 265 | `virtual_album_name_empty` | The album name cannot be empty | Albumnamnet kan ikkje vera tomt |
| 266 | `virtual_album_exists` | An album with this name already exists | Eit album med dette namnet finst alt |
| 267 | `add_to_virtual_album` | Add to album | Legg til i album |
| 268 | `remove_from_virtual_album` | Remove from album | Fjern frå album |
| 269 | `added_to_virtual_album` | Added to \"%s\" | Lagt til i \"%s\" |
| 270 | `already_in_virtual_album` | Already in \"%s\" | Allereie i \"%s\" |
| 271 | `removed_from_virtual_album` | Removed from the album | Fjerna frå albumet |
| 272 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | Sletta albumet %s? Berre albumet vert fjerna, bileta og videoane dine vert verande der dei er. |
| 273 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Dette albumet er tomt. Vel element i ei kva som helst mappe og bruk \"Kopier til\". |
| 274 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Denne handlinga er ikkje tilgjengeleg for virtuelle album |
| 275 | `alphanumeric_password` | Password | Passord |
| 276 | `enter_password` | Enter password | Skriv inn passord |
| 277 | `repeat_password` | Repeat password | Gjenta passord |
| 278 | `wrong_password` | Wrong password | Feil passord |
| 279 | `encrypting_files` | Encrypting files… | Krypterer filer… |
| 280 | `encrypting_folder` | Encrypting folder… | Krypterer mappe… |
| 281 | `folder_encrypted_successfully` | Folder encrypted successfully | Mappa vart kryptert |
| 282 | `decrypting_folder` | Decrypting folder… | Dekrypterer mappe… |
| 283 | `folder_decrypted_successfully` | Folder decrypted successfully | Mappa vart dekryptert |
| 284 | `decrypting_files` | Decrypting files… | Dekrypterer filer… |
| 285 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Plasser fingeren på fingeravtrykkssensoren |
| 286 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Kopla saman minst 4 punkt |
| 287 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | Vil du få tilgang til den eksisterande Cipher_Secrets-mappa di? |
| 288 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Eksisterande Cipher_Secrets-mappe vert sletta og ei ny vert laga. Er du sikker? |
| 289 | `confirm_checkbox_label` | I confirm | Eg stadfestar |
| 290 | `secret_vault` | Secret Vault | Hemmeleg kvelv |
| 291 | `secret_vault_empty` | Secret Vault is empty | Det hemmelege kvelvet er tomt |
| 292 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | Er du sikker på at du vil dekryptera den/dei skjulte fila/filene? |
| 293 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Kopiering til utklippstavla er deaktivert av tryggleiksomsyn medan kvelvet er låst opp. |
| 294 | `on_vault_exit` | On leaving hidden section | Når du forlèt skjult seksjon |
| 295 | `on_vault_exit_keep_showing_all` | Keep showing all hidden albums | Hald fram med å syna alle skjulte album |
| 296 | `on_vault_exit_show_encrypted_only` | Only show encrypted albums | Syn berre krypterte album |
| 297 | `on_vault_exit_show_other_only` | Only show other hidden albums | Syn berre andre skjulte album |
| 298 | `on_vault_exit_hide_all` | Hide all hidden albums | Skjul alle skjulte album |
| 299 | `on_vault_exit_ask_every_time` | Always ask | Spør alltid |
| 300 | `vault_exit_dialog_title` | Exiting Hidden Section | Forlèt skjult seksjon |
| 301 | `vault_exit_keep_showing` | Keep showing hidden albums | Hald fram med å syna skjulte album |
| 302 | `vault_exit_hide` | Hide hidden albums | Skjul skjulte album |
| 303 | `vault_exit_remember_my_choice` | Remember my choice | Hugs valet mitt |
| 304 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving hidden section | For fleire alternativ, sjå Innstillingar &gt; Når du forlèt skjult seksjon |
| 305 | `system_default` | System default | Systemstandard |
| 306 | `language` | Language | Språk |
| 307 | `open_system_language_settings` | Open system language settings | Opna språkinnstillingane i systemet |
| 308 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | Er du sikker på at du vil gjenoppretta %d fil? |
| 309 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | Er du sikker på at du vil gjenoppretta %d filer? |
| 310 | `failed_to_resize_images` (`one`) | Failed to resize %d image | Klarte ikkje å endra storleik på %d bilete |
| 311 | `failed_to_resize_images` (`other`) | Failed to resize %d images | Klarte ikkje å endra storleik på %d bilete |
| 312 | `search` *(fossify-commons)* | *(inherited from commons)* | Søk |
