# Spanish (United States, values-es-rUS) Translation Completion & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-es-rUS/strings.xml` was updated to full parity using Standard Spanish (`values-es/strings.xml`) as reference and adapting all entries to United States Spanish (`es-rUS`). Previously, `values-es-rUS` contained only 9 `<string>` entries (and 0 `<plurals>`). All 9 existing entries (including the local `search` override, which is absent in `fossify-commons` `values-es-rUS`) were preserved, and all 298 missing `<string>` entries plus 2 `<plurals>` blocks were added (327 `<string>` + 2 `<plurals>` = 329 resource entries; 347 total comparison rows including 14 `fossify-commons` UI overrides).
> **Key Terminology & Regional Adaptations vs. `values-es`:** `Ajustes` → `Configuración` · `vídeo/vídeos` → `video/videos` · `ficheros` → `archivos` · `añadir` → `agregar` · `Gestionar` → `Administrar` · `Sistema por defecto` → `Predeterminado del sistema` · `Introduzca la contraseña` → `Ingresar contraseña` · `pegatinas` → `stickers` · `pulsado` → `presionado` · `Search` → `Buscar` (local override kept for `dialog_select_language.xml`)

## 1. Existing Regional Entries & Key Adaptations from Standard Spanish

### 1.1 Preserved 9 Existing `values-es-rUS` Entries

All 9 previously existing strings in `values-es-rUS/strings.xml` were verified and preserved: `app_launcher_name` (`Galería`), `edit` (`Editar`), `open_camera` (`Abrir cámara`), `hidden` (`(oculto)`), `excluded` (`(excluido)`), `language` (`Idioma`), `search` (`Buscar`), `system_default` (`Predeterminado del sistema`), and `open_system_language_settings` (`Abrir configuración de idioma del sistema`).

### 1.2 Regional Adaptations Relative to `values-es/strings.xml` & `values-b+es+419`

| Category | Standard Spanish (`values-es`) | US Spanish (`values-es-rUS`) | Rationale |
|---|---|---|---|
| Settings label | `Ajustes` | `Configuración` | Matches existing `open_system_language_settings` and US Android UI |
| Video spelling | `vídeo` / `vídeos` | `video` / `videos` | Standard US Spanish spelling |
| File & add terms | `ficheros`, `añadir` | `archivos`, `agregar` | Standard US/Pan-American Spanish usage |
| Orientation & pin | `Bloquear orientación` / `Anclar arriba` | `Bloquear orientación` / `Fijar arriba` | Keeps standard `Bloquear/Desbloquear orientación` and neutral `Fijar arriba` (vs. Rioplatense `arriba de todo` in `es-419`) |
| Hidden items & touch | `Mostrar elementos ocultos` / `mantener pulsado` | `Mostrar elementos ocultos` / `mantener presionado` | Uses `elementos` (rather than `ítems`) and consistent `tú` register (`coloca tu dedo`, `toca`, `puedes`) |
| Password & system | `Introduzca la contraseña` / `Sistema por defecto` | `Ingresar contraseña` / `Predeterminado del sistema` | Matches existing `Predeterminado del sistema` in `values-es-rUS` |
| Search override | *(inherited from commons)* | `Buscar` *(explicit in `values-es-rUS`)* | `fossify-commons` `values-es-rUS` omits `search` |

### 1.3 `fossify-commons` Inherited UI Overrides (14 Keys)

`org.fossify:commons:7.1.3` (`res/values-es-rUS/values-es-rUS.xml`) contains only 6 base strings and omits all 14 keys below, so local overrides were added to `app/src/main/res/values-es-rUS/strings.xml`:

| # | Key (`name`) | English (`fossify-commons`) | US Spanish (`values-es-rUS/strings.xml`) | Source / Notes |
|---:|---|---|---|---|
| 1 | `change_date_and_time_format` | Change date and time format | Cambiar formato de fecha y hora | Local override in `values-es-rUS/strings.xml` (Settings) |
| 2 | `password_protect_file_deletion` | Password protect file deletion and moving | Proteger con contraseña la eliminación y el movimiento de archivos | Local override in `values-es-rUS/strings.xml` (Settings) |
| 3 | `keep_last_modified` | Keep old last-modified value at file operations | Mantener el valor anterior de última modificación en las operaciones de archivo | Local override in `values-es-rUS/strings.xml` (Settings) |
| 4 | `clear_cache` | Clear cache | Limpiar caché | Local override in `values-es-rUS/strings.xml` (Settings) |
| 5 | `export_settings` | Export settings | Exportar configuración | Local override in `values-es-rUS/strings.xml` (Settings) |
| 6 | `import_settings` | Import settings | Importar configuración | Local override in `values-es-rUS/strings.xml` (Settings) |
| 7 | `temporarily_show_excluded` | Temporarily show excluded | Mostrar excluidos temporalmente | Local override in `values-es-rUS/strings.xml` (Options menu) |
| 8 | `change_view_type` | Change view type | Cambiar tipo de vista | Local override in `values-es-rUS/strings.xml` (Options menu) |
| 9 | `contributors` | Contributors | Colaboradores | Local override in `values-es-rUS/strings.xml` (About) |
| 10 | `known_issues` | Known issues | Problemas conocidos | Local override in `values-es-rUS/strings.xml` (About) |
| 11 | `donate_to_fossify` | Donate to Fossify | Donar a Fossify | Local override in `values-es-rUS/strings.xml` (About) |
| 12 | `privacy_policy` | Privacy policy | Política de privacidad | Local override in `values-es-rUS/strings.xml` (About) |
| 13 | `create_shortcut` | Create shortcut | Crear atajo | Local override in `values-es-rUS/strings.xml` (CAB / ViewPager menu) |
| 14 | `print` | Print | Imprimir | Local override in `values-es-rUS/strings.xml` (ViewPager menu) |
| 15 | `search_files` | Search files | Buscar archivos | Local override in `values-es-rUS/strings.xml` (Search bar hint) |
| 16 | `search_folders` | Search folders | Buscar carpetas | Local override in `values-es-rUS/strings.xml` (Search bar hint) |

### 1.4 Newly Added Fork-Only Strings (63 Keys)

These 63 strings were introduced by this fork (Gallery-Cipher) and do not exist in the original Fossify Gallery:

| # | Key (`name`) | English (`values/strings.xml`) | Spanish (United States) (`values-es-rUS/strings.xml`) | Notes |
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


## 2. Full 1-to-1 Comparison: English vs. Spanish (United States)

| # | Key (`name`) | English (`values/strings.xml`) | Spanish (United States, `values-es-rUS/strings.xml`) |
|--:|---|---|---|
| 1 | `app_launcher_name` | Gallery | Galería |
| 2 | `edit` | Edit | Editar |
| 3 | `open_camera` | Open camera | Abrir cámara |
| 4 | `hidden` | (hidden) | (oculto) |
| 5 | `excluded` | (excluded) | (excluido) |
| 6 | `pin_folder` | Pin folder | Fijar carpeta |
| 7 | `unpin_folder` | Unpin folder | Desfijar carpeta |
| 8 | `pin_to_the_top` | Pin to the top | Fijar arriba |
| 9 | `show_all` | Show all folders content | Mostrar el contenido de todas las carpetas |
| 10 | `all_folders` | All folders | Todas las carpetas |
| 11 | `folder_view` | Switch to folder view | Cambiar a vista de carpeta |
| 12 | `other_folder` | Other folder | Otra carpeta |
| 13 | `show_on_map` | Show on map | Mostrar en el mapa |
| 14 | `unknown_location` | Unknown location | Ubicación desconocida |
| 15 | `volume` | Volume | Volumen |
| 16 | `brightness` | Brightness | Brillo |
| 17 | `lock_orientation` | Lock orientation | Bloquear orientación |
| 18 | `unlock_orientation` | Unlock orientation | Desbloquear orientación |
| 19 | `change_orientation` | Change orientation | Cambiar orientación |
| 20 | `force_portrait` | Force portrait | Forzar retrato |
| 21 | `force_landscape` | Force landscape | Forzar paisaje |
| 22 | `force_landscape_reverse` | Force landscape (reverse) | Forzar paisaje (inverso) |
| 23 | `use_default_orientation` | Use default orientation | Usar la orientación predeterminada |
| 24 | `fix_date_taken` | Fix Date Taken value | Corregir fecha de captura |
| 25 | `fixing` | Fixing… | Corrigiendo… |
| 26 | `dates_fixed_successfully` | Dates fixed successfully | Fechas corregidas correctamente |
| 27 | `no_date_takens_found` | No Date Taken values have been found | No se encontraron valores de fecha de captura |
| 28 | `share_resized` | Share a resized version | Compartir una versión redimensionada |
| 29 | `switch_to_file_search` | Switch to file search across all visible folders | Cambiar a búsqueda de archivos en todas las carpetas visibles |
| 30 | `set_as_default_folder` | Set as default folder | Establecer como carpeta predeterminada |
| 31 | `unset_as_default_folder` | Unset as default folder | Quitar como carpeta predeterminada |
| 32 | `reorder_by_dragging` | Reorder folders by dragging | Reordenar carpetas arrastrándolas |
| 33 | `reorder_by_dragging_pro` | Reorder folders by dragging (Pro) | Reordenar carpetas arrastrándolas (Pro) |
| 34 | `restore_to_path` | Restoring to \'%s\' | Restaurando en \'%s\' |
| 35 | `full_storage_permission_required` | Fossify Gallery needs full access to display all your photos and videos. Go to Settings &gt; Permissions &gt; Photos and videos &gt; Allow all. | La Galería Fossify necesita acceso total para mostrar todas tus fotos y videos. Ve a Configuración &gt; Permisos &gt; Fotos y videos &gt; Permitir todo. |
| 36 | `filter_media` | Filter media | Filtrar medios |
| 37 | `images` | Images | Imágenes |
| 38 | `videos` | Videos | Videos |
| 39 | `gifs` | GIFs | GIF |
| 40 | `raw_images` | RAW images | Imágenes RAW |
| 41 | `svgs` | SVGs | SVG |
| 42 | `portraits` | Portraits | Retratos |
| 43 | `no_media_with_filters` | No media files have been found with the selected filters. | No se encontraron archivos multimedia con los filtros seleccionados. |
| 44 | `change_filters_underlined` | <u>Change filters</u> | <u>Cambiar filtros</u> |
| 45 | `hide_folder_description` | This function hides the folder by adding a \'.nomedia\' file into it, it will hide all subfolders too. You can see them by toggling the \'Show hidden items\' option in Settings. Continue? | Esta función oculta la carpeta agregando un archivo \'.nomedia\' en ella, y también ocultará todas las subcarpetas. Puedes verlas activando la opción \'Mostrar elementos ocultos\' en Configuración. ¿Continuar? |
| 46 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | Esto excluirá la selección junto con sus subcarpetas solo de la Galería Fossify. Puedes administrar las carpetas excluidas en Configuración. |
| 47 | `exclude_folder_parent` | Exclude a parent instead? | ¿Excluir la carpeta superior en su lugar? |
| 48 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | Al excluir carpetas, estas y sus subcarpetas quedarán ocultas solo en la Galería Fossify, pero seguirán siendo visibles en otras aplicaciones.\n\nSi quieres ocultarlas también de otras aplicaciones, usa la función Ocultar. |
| 49 | `hidden_folders` | Hidden folders | Carpetas ocultas |
| 50 | `manage_hidden_folders` | Manage hidden folders | Administrar carpetas ocultas |
| 51 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | Parece que no tienes carpetas ocultas con un archivo \".nomedia\". |
| 52 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | Debes otorgar a la aplicación acceso a todos los archivos para ver los archivos ocultos; de lo contrario, no funcionará. |
| 53 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Si una carpeta o una de sus carpetas superiores tiene un punto antes de su nombre, está oculta y no se puede volver a mostrar de esta manera. Debes eliminar el punto cambiándole el nombre. |
| 54 | `include_folders` | Included folders | Carpetas incluidas |
| 55 | `manage_included_folders` | Manage included folders | Administrar carpetas incluidas |
| 56 | `add_folder` | Add folder | Agregar carpeta |
| 57 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | Si tienes algunas carpetas que contienen archivos multimedia, pero la aplicación no las reconoció, puedes agregarlas manualmente aquí.\n\nAgregar elementos aquí no excluirá ninguna otra carpeta. |
| 58 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | No se encontraron archivos multimedia. Puedes solucionarlo agregando manualmente las carpetas que contienen archivos multimedia. |
| 59 | `resize_and_save` | Resize selection and save | Redimensionar selección y guardar |
| 60 | `width` | Width | Ancho |
| 61 | `height` | Height | Alto |
| 62 | `keep_aspect_ratio` | Keep aspect ratio | Mantener relación de aspecto |
| 63 | `invalid_values` | Please enter a valid resolution | Por favor, ingresa una resolución válida |
| 64 | `resize_multiple_images` | Resize multiple images | Redimensionar varias imágenes |
| 65 | `resize_factor` | Resize factor | Factor de redimensión |
| 66 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | Redimensiona las imágenes al porcentaje indicado; el valor debe estar entre 10 y 90. |
| 67 | `resize_factor_error` | Enter a number between 10 and 90 | Ingresa un número entre 10 y 90 |
| 68 | `images_resized_successfully` | Images resized successfully | Imágenes redimensionadas correctamente |
| 69 | `editor` | Editor | Editor |
| 70 | `basic_editor` | Basic Editor | Editor básico |
| 71 | `advanced_editor` | Advanced Editor | Editor avanzado |
| 72 | `rotate` | Rotate | Rotar |
| 73 | `invalid_image_path` | Invalid image path | Ruta de imagen no válida |
| 74 | `invalid_video_path` | Invalid video path | Ruta de video no válida |
| 75 | `image_editing_failed` | Image editing failed | Error al editar la imagen |
| 76 | `video_editing_failed` | Video editing failed | Error al editar el video |
| 77 | `image_editing_cancelled` | Image editing cancelled | Edición de imagen cancelada |
| 78 | `video_editing_cancelled` | Video editing cancelled | Edición de video cancelada |
| 79 | `file_edited_successfully` | File edited successfully | Archivo editado correctamente |
| 80 | `image_edited_successfully` | Image edited successfully | Imagen editada correctamente |
| 81 | `video_edited_successfully` | Video edited successfully | Video editado correctamente |
| 82 | `edit_image_with` | Edit image with: | Editar imagen con: |
| 83 | `edit_video_with` | Edit video with: | Editar video con: |
| 84 | `no_image_editor_found` | No image editor found | No se encontró editor de imágenes |
| 85 | `no_video_editor_found` | No video editor found | No se encontró editor de videos |
| 86 | `unknown_file_location` | Unknown file location | Ubicación del archivo desconocida |
| 87 | `overwrite_original` | Overwrite original | Sobrescribir original |
| 88 | `error_saving_file` | Could not overwrite the source file | No se pudo sobrescribir el archivo de origen |
| 89 | `rotate_left` | Rotate left | Rotar a la izquierda |
| 90 | `rotate_right` | Rotate right | Rotar a la derecha |
| 91 | `rotate_one_eighty` | Rotate by 180º | Rotar 180º |
| 92 | `transform` | Transform | Transformar |
| 93 | `crop` | Crop | Recortar |
| 94 | `draw` | Draw | Dibujar |
| 95 | `flip` | Flip | Voltear |
| 96 | `flip_horizontally` | Flip horizontally | Voltear horizontalmente |
| 97 | `flip_vertically` | Flip vertically | Voltear verticalmente |
| 98 | `free_aspect_ratio` | Free | Libre |
| 99 | `other_aspect_ratio` | Other | Otro |
| 100 | `wallpaper` | Wallpaper | Fondo de pantalla |
| 101 | `set_as_wallpaper` | Set as Wallpaper | Establecer como fondo de pantalla |
| 102 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | Error al establecer el fondo de pantalla |
| 103 | `set_as_wallpaper_with` | Set as wallpaper with: | Establecer como fondo de pantalla con: |
| 104 | `setting_wallpaper` | Setting wallpaper… | Estableciendo fondo de pantalla… |
| 105 | `wallpaper_set_successfully` | Wallpaper set successfully | Fondo de pantalla establecido correctamente |
| 106 | `portrait_aspect_ratio` | Portrait aspect ratio | Relación de aspecto vertical |
| 107 | `landscape_aspect_ratio` | Landscape aspect ratio | Relación de aspecto horizontal |
| 108 | `home_screen` | Home screen | Pantalla de inicio |
| 109 | `lock_screen` | Lock screen | Pantalla de bloqueo |
| 110 | `home_and_lock_screen` | Home and lock screen | Pantalla de inicio y de bloqueo |
| 111 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | Permitir cambiar la relación de aspecto |
| 112 | `slideshow` | Slideshow | Presentación de diapositivas |
| 113 | `interval` | Interval | Intervalo |
| 114 | `include_photos` | Include photos | Incluir fotos |
| 115 | `include_videos` | Include videos | Incluir videos |
| 116 | `include_gifs` | Include GIFs | Incluir GIF |
| 117 | `random_order` | Random order | Orden aleatorio |
| 118 | `move_backwards` | Move backwards | Mover hacia atrás |
| 119 | `loop_slideshow` | Loop slideshow | Presentación en bucle |
| 120 | `animation` | Animation | Animación |
| 121 | `no_animation` | None | Ninguna |
| 122 | `fade` | Fade | Desvanecimiento |
| 123 | `slide` | Slide | Desplazamiento |
| 124 | `slideshow_ended` | The slideshow ended | Finalizó la presentación de diapositivas |
| 125 | `no_media_for_slideshow` | No media for the slideshow have been found | No se encontraron archivos multimedia para la presentación de diapositivas |
| 126 | `group_direct_subfolders` | Group direct subfolders | Agrupar subcarpetas directas |
| 127 | `group_by` | Group by | Agrupar por |
| 128 | `do_not_group_files` | Do not group files | No agrupar archivos |
| 129 | `by_folder` | Folder | Carpeta |
| 130 | `by_last_modified` | Last modified | Última modificación |
| 131 | `by_last_modified_daily` | Last modified (daily) | Última modificación (día) |
| 132 | `by_last_modified_monthly` | Last modified (monthly) | Última modificación (mes) |
| 133 | `by_date_taken` | Date taken | Fecha de captura |
| 134 | `by_date_taken_daily` | Date taken (daily) | Fecha de captura (día) |
| 135 | `by_date_taken_monthly` | Date taken (monthly) | Fecha de captura (mes) |
| 136 | `by_file_type` | File type | Tipo de archivo |
| 137 | `by_extension` | Extension | Extensión |
| 138 | `show_file_count_section_header` | Show file count at section headers | Mostrar cantidad de archivos en los encabezados de sección |
| 139 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | Ten en cuenta que la agrupación y el ordenamiento son 2 campos independientes |
| 140 | `folder_on_widget` | Folder shown on the widget: | Carpeta mostrada en el widget: |
| 141 | `show_folder_name` | Show folder name | Mostrar nombre de carpeta |
| 142 | `autoplay_videos` | Play videos automatically | Reproducir videos automáticamente |
| 143 | `remember_last_video_position` | Remember last video playback position | Recordar la última posición de reproducción del video |
| 144 | `loop_videos` | Loop videos | Repetir videos en bucle |
| 145 | `video_double_tap_duration` | Double-tap seek duration | Duración de salto con doble toque |
| 146 | `video_seek_duration_seconds` | %d seconds | %d segundos |
| 147 | `animate_gifs` | Animate GIFs at thumbnails | Animar GIF en las miniaturas |
| 148 | `max_brightness` | Max brightness when viewing fullscreen media | Brillo máximo al ver medios en pantalla completa |
| 149 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Mostrar fotos Ultra HDR en HDR |
| 150 | `crop_thumbnails` | Crop thumbnails into squares | Recortar miniaturas en cuadrados |
| 151 | `show_thumbnail_video_duration` | Show video durations | Mostrar duración de los videos |
| 152 | `screen_rotation_by` | Rotate fullscreen media by | Rotar medios en pantalla completa según |
| 153 | `screen_rotation_system_setting` | System setting | Configuración del sistema |
| 154 | `screen_rotation_device_rotation` | Device rotation | Rotación del dispositivo |
| 155 | `screen_rotation_aspect_ratio` | Aspect ratio | Relación de aspecto |
| 156 | `black_background_at_fullscreen` | Black background at fullscreen media | Fondo negro en medios en pantalla completa |
| 157 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | Desplazar miniaturas horizontalmente |
| 158 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | Ocultar automáticamente la interfaz del sistema en pantalla completa |
| 159 | `delete_empty_folders` | Delete empty folders after deleting their content | Eliminar carpetas vacías después de borrar su contenido |
| 160 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | Mantener la pantalla encendida al ver fotos en pantalla completa |
| 161 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | Permitir controlar el brillo en fotos con gestos verticales |
| 162 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | Permitir controlar el volumen y el brillo del video con gestos verticales |
| 163 | `playback_speed` | Playback speed | Velocidad de reproducción |
| 164 | `show_media_count` | Show folder media count on the main view | Mostrar cantidad de archivos de las carpetas en la vista principal |
| 165 | `show_extended_details` | Show extended details over fullscreen media | Mostrar detalles extendidos sobre medios en pantalla completa |
| 166 | `manage_extended_details` | Manage extended details | Administrar detalles extendidos |
| 167 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | Permitir zoom con un dedo en pantalla completa |
| 168 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | Permitir cambiar de medio al instante tocando los lados de la pantalla |
| 169 | `allow_deep_zooming_images` | Allow deep zooming images | Permitir zoom profundo en imágenes |
| 170 | `hide_extended_details` | Hide extended details when status bar is hidden | Ocultar detalles extendidos cuando la barra de estado está oculta |
| 171 | `show_at_bottom` | Show some action buttons at the bottom of the screen | Mostrar algunos botones de acción en la parte inferior de la pantalla |
| 172 | `show_recycle_bin` | Show the recycle bin on the folders screen | Mostrar la papelera de reciclaje en la pantalla de carpetas |
| 173 | `deep_zoomable_images` | Deep zoomable images | Imágenes con zoom profundo |
| 174 | `show_highest_quality` | Show images in the highest possible quality | Mostrar imágenes con la mayor calidad posible |
| 175 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | Mostrar la papelera de reciclaje como último elemento en la pantalla principal |
| 176 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | Permitir cerrar la vista de pantalla completa con un gesto hacia abajo |
| 177 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | Permitir zoom 1:1 con dos toques dobles |
| 178 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | Reproducir videos en una pantalla separada con gestos horizontales |
| 179 | `on_video_tap` | On video tap | Al tocar el video |
| 180 | `open_in_app_player` | Open in-app player | Abrir reproductor integrado |
| 181 | `open_system_default_player` | Open default player app | Abrir aplicación de reproducción predeterminada |
| 182 | `show_notch` | Show a notch if available | Mostrar una muesca si está disponible |
| 183 | `allow_rotating_gestures` | Allow rotating images with gestures | Permitir rotar imágenes con gestos |
| 184 | `file_loading_priority` | File loading priority | Prioridad de carga de archivos |
| 185 | `speed` | Speed | Velocidad |
| 186 | `compromise` | Compromise | Equilibrio |
| 187 | `avoid_showing_invalid_files` | Avoid showing invalid files | Evitar mostrar archivos no válidos |
| 188 | `show_image_file_types` | Show image file types | Mostrar tipos de archivo de imagen |
| 189 | `allow_zooming_videos` | Allow zooming videos with double tapping them | Permitir hacer zoom en videos con doble toque |
| 190 | `folder_thumbnail_style` | Folder thumbnail style | Estilo de miniatura de carpetas |
| 191 | `file_thumbnail_style` | File thumbnail style | Estilo de miniatura de archivos |
| 192 | `mark_favorite_items` | Mark favorite items | Marcar elementos favoritos |
| 193 | `thumbnail_spacing` | Thumbnail spacing | Espaciado de miniaturas |
| 194 | `show_file_count_line` | Show file count on a separate line | Mostrar cantidad de archivos en una línea separada |
| 195 | `show_file_count_brackets` | Show file count in brackets | Mostrar cantidad de archivos entre paréntesis |
| 196 | `show_file_count_none` | Do not show file count | No mostrar cantidad de archivos |
| 197 | `limit_folder_title` | Limit long folder titles to 1 line | Limitar los títulos largos de carpetas a 1 línea |
| 198 | `square` | Square | Cuadrado |
| 199 | `rounded_corners` | Rounded corners | Esquinas redondeadas |
| 200 | `export_favorite_paths` | Export favorites | Exportar favoritos |
| 201 | `import_favorite_paths` | Import favorites | Importar favoritos |
| 202 | `paths_imported_successfully` | Paths imported successfully | Rutas importadas correctamente |
| 203 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | Para asegurarte de que todas las operaciones con archivos funcionen de manera confiable, configura esta aplicación como aplicación de administración de medios en la configuración de tu dispositivo. |
| 204 | `password_protect_excluded` | Password protect excluded folder visibility | Proteger con contraseña la visibilidad de carpetas excluidas |
| 205 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Algo salió mal; ve a Configuración de tu dispositivo - Aplicaciones - Acceso especial a aplicaciones - Aplicaciones de administración de medios y permite que esta aplicación administre medios. |
| 206 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Si la redirección no funciona, ve a Configuración de tu dispositivo - Aplicaciones - Acceso especial a aplicaciones - Aplicaciones de administración de medios y permite que esta aplicación administre medios. |
| 207 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Si no quieres hacerlo, también puedes ir a Configuración de tu dispositivo - Aplicaciones - Acceso especial a aplicaciones - Aplicaciones de administración de medios y permitir que esta aplicación administre medios. |
| 208 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | Como alternativa, puedes permitir el acceso solo a archivos multimedia. Sin embargo, en ese caso no podrás trabajar con archivos ocultos. |
| 209 | `media_only` | Media only | Solo multimedia |
| 210 | `all_files` | All files | Todos los archivos |
| 211 | `main_screen_view_on_launch` | Main screen view on launch | Vista de la pantalla principal al iniciar |
| 212 | `show_folders` | Show folders | Mostrar carpetas |
| 213 | `show_files` | Show files | Mostrar archivos |
| 214 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | Mostrar un botón de menú para alternar rápidamente Mostrar el contenido de todas las carpetas |
| 215 | `thumbnails` | Thumbnails | Miniaturas |
| 216 | `fullscreen_media` | Fullscreen media | Medios en pantalla completa |
| 217 | `extended_details` | Extended details | Detalles extendidos |
| 218 | `bottom_actions` | Bottom actions | Acciones inferiores |
| 219 | `manage_bottom_actions` | Manage visible bottom actions | Administrar acciones inferiores visibles |
| 220 | `toggle_favorite` | Toggle favorite | Alternar favorito |
| 221 | `toggle_file_visibility` | Toggle file visibility | Alternar visibilidad del archivo |
| 222 | `failed_to_load_media` | Failed to load media. | Error al cargar el medio. |
| 223 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | El archivo está dañado o tiene un formato incorrecto. |
| 224 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | El medio supera las capacidades del dispositivo. |
| 225 | `unsupported_format` | Unsupported format. | Formato no compatible. |
| 226 | `video_no_sound` | This video has no audio | Este video no tiene audio |
| 227 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | ¿Cómo puedo hacer que la Galería Fossify sea la galería predeterminada del dispositivo? |
| 228 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\". The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Primero debes buscar la galería predeterminada actual en la sección Aplicaciones de la configuración de tu dispositivo, buscar un botón que diga algo como \"Abrir de forma predeterminada\", tocarlo y luego seleccionar \"Borrar valores predeterminados\". La próxima vez que intentes abrir una imagen o un video verás un selector de aplicaciones, donde podrás elegir la Galería Fossify y establecerla como aplicación predeterminada. |
| 229 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | Bloqueé la aplicación con una contraseña, pero la olvidé. ¿Qué puedo hacer? |
| 230 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Puedes solucionarlo de 2 maneras. Puedes reinstalar la aplicación o buscarla en la configuración de tu dispositivo y seleccionar \"Borrar datos\". Esto restablecerá toda tu configuración, pero no eliminará ningún archivo multimedia. |
| 231 | `faq_3_title` | How can I make an album always appear at the top? | ¿Cómo puedo hacer que un álbum aparezca siempre arriba? |
| 232 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Puedes mantener presionado el álbum deseado y seleccionar el ícono Fijar en el menú de acciones, lo que lo fijará arriba. También puedes fijar varias carpetas; los elementos fijados se ordenarán según el método de ordenamiento predeterminado. |
| 233 | `faq_4_title` | How can I fast forward videos? | ¿Cómo puedo adelantar videos? |
| 234 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Puedes hacerlo tocando dos veces el lado de la pantalla o tocando los textos de duración actual o máxima cerca de la barra de progreso. Si activas la reproducción de videos en una pantalla separada en la configuración de la aplicación, también puedes usar gestos horizontales. |
| 235 | `faq_5_title` | What is the difference between hiding and excluding a folder? | ¿Cuál es la diferencia entre ocultar y excluir una carpeta? |
| 236 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Excluir evita que la carpeta se muestre solo en la Galería Fossify, mientras que Ocultar funciona a nivel del sistema y la oculta también de otras galerías. Funciona creando un archivo vacío \".nomedia\" en la carpeta indicada, que luego también puedes eliminar con cualquier administrador de archivos. Ten en cuenta que algunos dispositivos no permiten ocultar carpetas como Cámara, Capturas de pantalla y Descargas. |
| 237 | `faq_6_title` | Why do folders with music cover art or stickers show up? | ¿Por qué aparecen carpetas con portadas de música o stickers? |
| 238 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Puede ocurrir que aparezcan algunos álbumes inusuales. Puedes excluirlos fácilmente manteniéndolos presionados y seleccionando Excluir. En el siguiente cuadro de diálogo puedes seleccionar la carpeta superior; es muy probable que esto evite también que aparezcan otros álbumes relacionados. |
| 239 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | Una carpeta con imágenes no aparece o no muestra todos los elementos. ¿Qué puedo hacer? |
| 240 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Eso puede deberse a varios motivos, pero solucionarlo es fácil. Solo ve a Configuración -&gt; Administrar carpetas incluidas, selecciona el signo Más y navega hasta la carpeta deseada. |
| 241 | `faq_8_title` | What if I want just a few particular folders visible? | ¿Qué hago si solo quiero ver algunas carpetas en particular? |
| 242 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders. That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Agregar una carpeta en Carpetas incluidas no excluye nada automáticamente. Lo que puedes hacer es ir a Configuración -&gt; Administrar carpetas excluidas, excluir la carpeta raíz \"/\" y luego agregar las carpetas deseadas en Configuración -&gt; Administrar carpetas incluidas. Eso hará que solo se muestren las carpetas seleccionadas, ya que tanto la exclusión como la inclusión son recursivas y, si una carpeta está excluida e incluida a la vez, se mostrará. |
| 243 | `faq_10_title` | Can I crop images with this app? | ¿Puedo recortar imágenes con esta aplicación? |
| 244 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Sí, puedes recortar imágenes en el editor arrastrando las esquinas de la imagen. Puedes acceder al editor manteniendo presionada la miniatura de una imagen y seleccionando Editar, o seleccionando Editar desde la vista de pantalla completa. |
| 245 | `faq_11_title` | Can I somehow group media file thumbnails? | ¿Puedo agrupar de alguna forma las miniaturas de archivos multimedia? |
| 246 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Claro, solo usa la opción \"Agrupar por\" mientras estás en la vista de miniaturas. Puedes agrupar archivos según varios criterios, incluida la Fecha de captura. Si usas la función \"Mostrar el contenido de todas las carpetas\", también puedes agruparlos por carpetas. |
| 247 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | El ordenamiento por fecha de captura no parece funcionar correctamente, ¿cómo puedo solucionarlo? |
| 248 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Lo más probable es que se deba a que los archivos se copiaron desde otro lugar. Puedes solucionarlo seleccionando las miniaturas de los archivos y eligiendo \"Corregir fecha de captura\". |
| 249 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Veo bandas de color en las imágenes. ¿Cómo puedo mejorar la calidad? |
| 250 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | La solución actual para mostrar imágenes funciona bien en la gran mayoría de los casos, pero si quieres una calidad de imagen aún mejor, puedes activar \"Mostrar imágenes con la mayor calidad posible\" en la configuración de la aplicación, en la sección \"Imágenes con zoom profundo\". |
| 251 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | Oculté un archivo o una carpeta. ¿Cómo puedo volver a mostrarlo? |
| 252 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Puedes tocar la opción \"Mostrar ocultos temporalmente\" en la pantalla principal o activar \"Mostrar elementos ocultos\" en la configuración de la aplicación para ver el elemento oculto. Si deseas volver a mostrarlo, solo mantenlo presionado y selecciona \"Mostrar\". Las carpetas se ocultan agregando un archivo oculto \".nomedia\" en ellas; también puedes eliminar ese archivo con cualquier administrador de archivos. Ten en cuenta que la ocultación funciona de forma recursiva, por lo que si ocultas una carpeta, todas sus subcarpetas también quedarán ocultas. Por eso, para mostrar las subcarpetas debes volver a mostrar la carpeta superior. |
| 253 | `faq_15_title` | Why does the app take up so much space? | ¿Por qué la aplicación ocupa tanto espacio? |
| 254 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | La caché de la aplicación puede ocupar hasta 250 MB para garantizar una carga más rápida de las imágenes. Si la aplicación ocupa aún más espacio, lo más probable es que tengas elementos en la papelera de reciclaje, ya que esos archivos cuentan dentro del tamaño de la aplicación. Puedes vaciar la papelera abriéndola y eliminando todos los archivos, o desde la configuración de la aplicación. Cada archivo en la papelera se elimina automáticamente después de 30 días. |
| 255 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | ¿Qué pasó con la ocultación de archivos y carpetas y por qué ya no puedo ver los elementos ocultos? |
| 256 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | A partir de Android 11 ya no es posible ocultar ni mostrar archivos o carpetas, ni ver los elementos ocultos en aplicaciones de galería debido a restricciones del sistema. Tendrás que usar algún administrador de archivos para hacerlo. |
| 257 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | También puedes otorgar a esta galería acceso a Todos los archivos desde la configuración de tu dispositivo, lo que nos permitirá mostrar elementos ocultos y hacer que las operaciones con archivos sean más confiables en general. |
| 258 | `faq_17_title` | Why cannot I include missing folders anymore? | ¿Por qué ya no puedo incluir carpetas faltantes? |
| 259 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Eso también dejó de funcionar debido a los cambios del sistema que llegaron con Android 11; la aplicación ya no puede explorar carpetas reales y depende del llamado MediaStore para obtener los datos. |
| 260 | `faq_18_title` | Why do I see ads during video playback? | ¿Por qué veo anuncios durante la reproducción de videos? |
| 261 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Nuestras aplicaciones no tienen ningún tipo de anuncios. Si los ves al reproducir videos, seguramente estás usando el reproductor de video de otra aplicación. Busca tu reproductor de video predeterminado en la configuración del dispositivo y selecciona \"Borrar valores predeterminados\". La próxima vez que abras un video verás un selector de aplicaciones donde podrás elegir qué aplicación usar. |
| 262 | `create_virtual_album` | Create virtual album | Crear álbum virtual |
| 263 | `new_virtual_album` | New album… | Nuevo álbum… |
| 264 | `rename_virtual_album` | Rename album | Renombrar álbum |
| 265 | `album_name` | Album name | Nombre del álbum |
| 266 | `virtual_folder_name` | Virtual folder name | Nombre de la carpeta virtual |
| 267 | `virtual_album_name_empty` | The album name cannot be empty | El nombre del álbum no puede estar vacío |
| 268 | `virtual_album_exists` | An album with this name already exists | Ya existe un álbum con este nombre |
| 269 | `add_to_virtual_album` | Add to album | Agregar al álbum |
| 270 | `remove_from_virtual_album` | Remove from album | Quitar del álbum |
| 271 | `added_to_virtual_album` | Added to \"%s\" | Agregado a \"%s\" |
| 272 | `already_in_virtual_album` | Already in \"%s\" | Ya está en \"%s\" |
| 273 | `removed_from_virtual_album` | Removed from the album | Quitado del álbum |
| 274 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | ¿Eliminar el álbum %s? Solo se eliminará el álbum; tus fotos y videos permanecerán donde están. |
| 275 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Este álbum está vacío. Selecciona elementos en cualquier carpeta y usa \"Copiar a\". |
| 276 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Esta acción no está disponible para álbumes virtuales |
| 277 | `alphanumeric_password` | Password | Contraseña |
| 278 | `enter_password` | Enter password | Ingresar contraseña |
| 279 | `repeat_password` | Repeat password | Repetir contraseña |
| 280 | `wrong_password` | Wrong password | Contraseña incorrecta |
| 281 | `encrypting_files` | Encrypting files… | Cifrando archivos… |
| 282 | `encrypting_folder` | Encrypting folder… | Cifrando carpeta… |
| 283 | `folder_encrypted_successfully` | Folder encrypted successfully | Carpeta cifrada correctamente |
| 284 | `file_encrypted_successfully` | File hidden successfully | Archivo ocultado correctamente |
| 285 | `files_encrypted_successfully` | Files hidden successfully | Archivos ocultados correctamente |
| 286 | `decrypting_folder` | Decrypting folder… | Descifrando carpeta… |
| 287 | `folder_decrypted_successfully` | Folder decrypted successfully | Carpeta descifrada correctamente |
| 288 | `file_decrypted_successfully` | File unhidden successfully | Archivo mostrado correctamente |
| 289 | `files_decrypted_successfully` | Files unhidden successfully | Archivos mostrados correctamente |
| 290 | `decrypting_files` | Decrypting files… | Descifrando archivos… |
| 291 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Por favor, coloca tu dedo sobre el sensor de huellas |
| 292 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Conecta al menos 4 puntos |
| 293 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | ¿Deseas acceder a tu carpeta Cipher_Secrets existente? |
| 294 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Se eliminará la carpeta Cipher_Secrets existente y se creará una nueva. ¿Estás seguro? |
| 295 | `confirm_checkbox_label` | I confirm | Confirmo |
| 296 | `secret_vault` | Secret Vault | Bóveda secreta |
| 297 | `secret_vault_empty` | Secret Vault is empty | La bóveda secreta está vacía |
| 298 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | ¿Seguro que quieres descifrar los archivos ocultos? |
| 299 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Por seguridad, copiar al portapapeles está deshabilitado mientras la bóveda está desbloqueada. |
| 300 | `on_vault_exit` | On leaving encrypted section | Al salir de la sección cifrada |
| 301 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | Seguir mostrando álbumes cifrados |
| 302 | `on_vault_exit_hide` | Hide encrypted albums | Ocultar álbumes cifrados |
| 303 | `on_vault_exit_ask_every_time` | Always ask | Preguntar siempre |
| 304 | `vault_exit_dialog_title` | Exiting Encrypted Section | Saliendo de la sección cifrada |
| 305 | `vault_exit_question_single` | Show encrypted album together with other albums? | ¿Mostrar el álbum cifrado junto con otros álbumes? |
| 306 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? | ¿Mostrar los álbumes cifrados junto con otros álbumes? |
| 307 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | Para más opciones, consulta Configuración &gt; Al salir de la sección cifrada |
| 308 | `stop_showing_hidden` | Stop showing encrypted media | Dejar de mostrar medios cifrados |
| 309 | `system_default` | System default | Predeterminado del sistema |
| 310 | `language` | Language | Idioma |
| 311 | `open_system_language_settings` | Open system language settings | Abrir configuración de idioma del sistema |
| 312 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Acceder a elementos ocultos con biometría |
| 313 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Por favor, activa primero la protección con contraseña para elementos ocultos |
| 314 | `biometric_unlock_vault` | Unlock hidden vault | Desbloquear bóveda oculta |
| 315 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | Verifica tu identidad biométrica para acceder a los elementos ocultos |
| 316 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | Confirma tu identidad biométrica para activar el acceso biométrico a la bóveda |
| 317 | `use_password_or_pin` | Use PIN / Password | Usar PIN / Contraseña |
| 318 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Las credenciales biométricas han cambiado. Por favor, ingresa tu PIN o contraseña. |
| 319 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | No se encontró hardware biométrico ni credenciales registradas en este dispositivo |
| 320 | `default_hidden_album` | Default Hidden Album | Álbum oculto predeterminado |
| 321 | `select_hidden_album` | Select Hidden Album | Seleccionar álbum oculto |
| 322 | `create_new_hidden_album` | Create New Hidden Album | Crear nuevo álbum oculto |
| 323 | `new_hidden_album` | New hidden album | Nuevo álbum oculto |
| 324 | `hidden_album_name_empty` | Album name cannot be empty | El nombre del álbum no puede estar vacío |
| 325 | `file_hidden_to_album` | File hidden to \"%s\" | Archivo ocultado en \"%s\" |
| 326 | `files_hidden_to_album` | %1$d files hidden to \"%2$s\" | %1$d archivos ocultados en \"%2$s\" |
| 327 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | ¿Seguro que quieres restaurar %d archivo? |
| 328 | `restore_confirmation` (`many`) | *(CLDR `many` — falls back to English `other`)* | ¿Seguro que quieres restaurar %d archivos? |
| 329 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | ¿Seguro que quieres restaurar %d archivos? |
| 330 | `failed_to_resize_images` (`one`) | Failed to resize %d image | Error al cambiar el tamaño de %d imagen |
| 331 | `failed_to_resize_images` (`many`) | *(CLDR `many` — falls back to English `other`)* | Error al cambiar el tamaño de %d imágenes |
| 332 | `failed_to_resize_images` (`other`) | Failed to resize %d images | Error al cambiar el tamaño de %d imágenes |
| 333 | `search` *(fossify-commons)* | *(inherited from commons)* | Buscar |
| 334 | `change_date_and_time_format` *(fossify-commons)* | Change date and time format | Cambiar formato de fecha y hora |
| 335 | `password_protect_file_deletion` *(fossify-commons)* | Password protect file deletion and moving | Proteger con contraseña la eliminación y el movimiento de archivos |
| 336 | `keep_last_modified` *(fossify-commons)* | Keep old last-modified value at file operations | Mantener el valor anterior de última modificación en las operaciones de archivo |
| 337 | `clear_cache` *(fossify-commons)* | Clear cache | Limpiar caché |
| 338 | `export_settings` *(fossify-commons)* | Export settings | Exportar configuración |
| 339 | `import_settings` *(fossify-commons)* | Import settings | Importar configuración |
| 340 | `temporarily_show_excluded` *(fossify-commons)* | Temporarily show excluded | Mostrar excluidos temporalmente |
| 341 | `change_view_type` *(fossify-commons)* | Change view type | Cambiar tipo de vista |
| 342 | `contributors` *(fossify-commons)* | Contributors | Colaboradores |
| 343 | `known_issues` *(fossify-commons)* | Known issues | Problemas conocidos |
| 344 | `donate_to_fossify` *(fossify-commons)* | Donate to Fossify | Donar a Fossify |
| 345 | `privacy_policy` *(fossify-commons)* | Privacy policy | Política de privacidad |
| 346 | `create_shortcut` *(fossify-commons)* | Create shortcut | Crear atajo |
| 347 | `print` *(fossify-commons)* | Print | Imprimir |
| 348 | `search_files` *(fossify-commons)* | Search files | Buscar archivos |
| 349 | `search_folders` *(fossify-commons)* | Search folders | Buscar carpetas |
