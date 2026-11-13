# Spanish (Standard, values-es) Translation Audit, Revision & English 1-to-1 Comparison

> **Scope:** `app/src/main/res/values-es/strings.xml` was audited against `app/src/main/res/values/strings.xml` and `fossify-commons` (`values-es`). Previously, `values-es` contained 257 `<string>` and 2 `<plurals>` entries (missing all 50 fork/new strings). 29 existing entries with mistranslations, typos, missing clauses, formatting bugs, or terminology inconsistencies were fixed, and all 50 missing strings were translated into Standard Spanish (`es`), bringing `values-es` to full parity (347 `<string>` + 2 `<plurals>` = 349 resource entries; `search` and 14 shared UI keys from `fossify-commons`; 361 total comparison rows with Spanish CLDR `one`/`many`/`other` plural forms expanded). Nine keys that were missing from `values-es/strings.xml` were translated and added (`number_of_items`, `random`, `shuffle`, `stop_showing_excluded`, `copy_to_clipboard`, `encrypt`, `decrypt`, `encrypt_album`, `decrypt_album`), and eleven existing entries were revised so that files and albums use the new \"encrypt / decrypt\" wording and so that the favorites import/export labels mention virtual albums as well.
> **Key Terminology:** `Settings` → `Ajustes` · `Default` → `Predeterminado` / `Sistema por defecto` · `Folder` → `Carpeta` · `Album` → `Álbum` · `Encrypted Vault` → `Bóveda cifrada` · `Hidden` → `Oculto` · `Excluded` → `Excluido` · `Password` → `Contraseña` · `Encrypt / Decrypt` → `Cifrar / Descifrar` · `Copy to` → `Copiar a` (matches `fossify-commons`) · `Clipboard` → `Portapapeles` · `Search` → `Buscar` (inherited from `fossify-commons`)

## 1. Audit of Existing Translations & Newly Added Entries

### 1.1 Review of Revised Existing Translations

| # | Key (`name`) | Old `values-es` | New `values-es` | Reason / Notes |
|--:|---|---|---|---|
| 1 | `excluded` | (no incluido) | (excluido) | Aligned with `exclude_folder_description`, `faq_5_text`, `faq_8_text`, and regional Spanish variants |
| 2 | `unpin_folder` | No fijar carpeta | Desfijar carpeta | **Fixed meaning:** `No fijar` means "Do not pin"; `Desfijar` correctly expresses unpinning an already pinned folder |
| 3 | `all_folders` | Todos los medios | Todas las carpetas | **Fixed mistranslation:** English source is `All folders` (not "All media"); matches `show_all` and `values-b+es+419` |
| 4 | `force_landscape_reverse` | Fuerza apaisado (inverso) | Forzar paisaje (inverso) | Standardized infinitive verb and term `paisaje` to match `force_portrait` (`Forzar retrato`) and `force_landscape` (`Forzar paisaje`) |
| 5 | `fixing` | Fijando… | Arreglando… | **Fixed mistranslation:** *Fix* here means repair/correct (matching `Arreglar fecha de la toma`), not pin/fasten |
| 6 | `dates_fixed_successfully` | Fecha fijada correctamente | Fechas arregladas correctamente | **Fixed mistranslation & number:** Plural `Fechas` and `arregladas` instead of `fijada` |
| 7 | `no_date_takens_found` | No se han encontrado valores de fechas tomadas | No se han encontrado valores de fecha de la toma | Aligned terminology with `fix_date_taken` (`fecha de la toma`) |
| 8 | `restore_confirmation` (`many`) | ¿Confirma que quiere restaurar %d de archivos? | ¿Confirma que quiere restaurar %d archivos? | Removed ungrammatical `de` before `archivos` when `%d` renders digits |
| 9 | `full_storage_permission_required` | La galería de Fossify… videos… Configuración… | La Galería Fossify… vídeos… Ajustes… | Standardized `Galería Fossify`, `vídeos`, and `Ajustes` for `es` |
| 10 | `hide_folder_description` | …opción 'Mostrar carpetas ocultas' en los Ajustes. ¿Continuar\? | …opción \'Mostrar elementos ocultos\' en los Ajustes. ¿Continuar? | **Fixed setting name:** matches `fossify-commons` `show_hidden_items` (`Mostrar elementos ocultos`) |
| 11 | `exclude_folder_description` | …sólo de la Galería Fossify… en la Configuración. | …solo de la Galería Fossify… en los Ajustes. | Standardized `Ajustes` and modern RAE `solo` |
| 12 | `excluded_activity_placeholder` | *(literal XML newlines around `\n`)* | `…\n\n…` | Normalized multiline XML whitespace and `solo` |
| 13 | `included_activity_placeholder` | *(literal XML newlines around `\n`)* | `…\n\n…` | Normalized multiline XML whitespace |
| 14 | `cant_unhide_folder` | …estará oculta. Debes eliminar el punto… | …está oculta y no se puede volver a mostrar de esta manera. Debes eliminar el punto… | **Restored omitted clause:** `"and cannot be unhidden like this"` was missing |
| 15 | `failed_to_resize_images` | …de la imagen %d / …de las imágenes %d | …de %d imagen / …de %d imágenes | **Fixed placeholder grammar:** moved `%d` before the noun as a count instead of an index |
| 16 | `overwrite_original` | Sobreescribir original | Sobrescribir original | Fixed spelling (`sobrescribir` with single `e`, matching `error_saving_file`) |
| 17 | `crop` | Cultivos | Recortar | **Fixed critical mistranslation:** `Cultivos` means agricultural crops; image crop is `Recortar` |
| 18 | `by_last_modified_daily` / `_monthly` | Fecha de modificación (Día) / (Mes) | Fecha de modificación (día) / (mes) | Standardized lowercase `(día)` / `(mes)` to match `by_date_taken_daily` / `_monthly` |
| 19 | `by_file_type` | Tipo de arhivo | Tipo de archivo | Fixed typo (`arhivo` → `archivo`) |
| 20 | `allow_instant_change` | Desplazarse en los medios haciendo clic en los lados de la pantalla | Cambiar de medio al instante tocando los lados de la pantalla | Clarified instant media switching on tap |
| 21 | `allow_one_to_one_zoom` | Permitir zoom 1:1 con un toque doble | Permitir zoom 1:1 con dos toques dobles | **Fixed mistranslation:** English source is `two double taps`, not one double tap |
| 22 | `media_only` | Sólo archivos multimedia | Solo archivos multimedia | Standardized modern RAE orthography (`Solo`) |
| 23 | `faq_1_text` | …actualmente por defecto… Configuración… vídeo… Galería de Fossify… | …actualmente predeterminada… ajustes… vídeo… Galería Fossify… | Standardized `Galería Fossify` and natural phrasing |
| 24 | `faq_2_text` | …configuración… \"Borrar datos \". Restablecerá todas sus configuraciones… | …ajustes… \"Borrar datos\". Restablecerá todos sus ajustes… | Removed stray trailing space inside quotes and standardized `ajustes` |
| 25 | `faq_3_text` | …icono Pin… anclar varias carpetas, los artículos fijados… | …icono Fijar… fijar varias carpetas; los elementos fijados… | **Fixed mistranslation:** `artículos` (goods/articles) → `elementos`; unified `Fijar` |
| 26 | `faq_4_text` | …barra de búsqueda… configuración… | …barra de progreso… ajustes… | **Fixed mistranslation:** *seekbar* is `barra de progreso`, not `barra de búsqueda` (search bar) |
| 27 | `faq_6_text` | …presionándolos durante mucho tiempo… | …manteniéndolos pulsados… | Replaced literal phrasing with idiomatic Spanish `manteniéndolos pulsados` |
| 28 | `faq_7_text` / `faq_8_text` | Configuración -&gt; Administrar carpetas… | Ajustes -&gt; Gestionar carpetas… | Matched `fossify-commons` `values-es` (`Ajustes` and `Gestionar carpetas excluidas/incluidas`) |
| 29 | `faq_11_text` / `faq_14_text` / `faq_17_text` / `faq_18_text` | `\"Agrupar por \"` / `\"Mostrar elementos ocultos temporalmente\"` / `no puede navegar… más` / `que aplicación` | `\"Agrupar por\"` / `\"Mostrar ocultos temporalmente\"` / `ya no puede explorar…` / `qué aplicación` | Fixed stray quote space, `fossify-commons` label, broken syntax in FAQ 17, missing accent in `qué`, and mixed `video`/`vídeo` across remaining strings |



### 1.2 Newly Added Fork-Only Strings (66 Keys)

These 66 strings were introduced by this fork (Gallery-Cipher) and do not exist in the original Fossify Gallery:

| # | Key (`name`) | English (`values/strings.xml`) | Spanish (`values-es/strings.xml`) | Notes |
|--:|---|---|---|---|
| 1 | `video_double_tap_duration` | Double-tap seek duration | Duración de salto con doble toque | Video double-tap forward/rewind interval |
| 2 | `video_seek_duration_seconds` | %d seconds | %d segundos | Used with 3, 5, 10, 15 via `String.format` |
| 3 | `create_virtual_album` | Create virtual album | Crear álbum virtual | Virtual albums section action |
| 4 | `new_virtual_album` | New album… | Nuevo álbum… | Action label for creating a virtual album |
| 5 | `rename_virtual_album` | Rename album | Renombrar álbum | Action label to rename a virtual album |
| 6 | `album_name` | Album name | Nombre del álbum | Input hint/label |
| 7 | `virtual_folder_name` | Virtual folder name | Nombre de la carpeta virtual | Input hint/label |
| 8 | `virtual_album_name_empty` | The album name cannot be empty | El nombre del álbum no puede estar vacío | Validation error |
| 9 | `virtual_album_exists` | An album with this name already exists | Ya existe un álbum con este nombre | Validation error |
| 10 | `add_to_virtual_album` | Add to album | Añadir al álbum | Menu action (`añadir` in `es`) |
| 11 | `remove_from_virtual_album` | Remove from album | Quitar del álbum | Menu action |
| 12 | `added_to_virtual_album` | Added to \"%s\" | Añadido a \"%s\" | Toast confirmation |
| 13 | `already_in_virtual_album` | Already in \"%s\" | Ya está en \"%s\" | Toast message |
| 14 | `removed_from_virtual_album` | Removed from the album | Quitado del álbum | Toast message |
| 15 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | ¿Eliminar el álbum %s? Solo se eliminará el álbum; tus fotos y vídeos permanecerán donde están. | No extra quotes around `%s`; uses `vídeos` for `es` |
| 16 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Este álbum está vacío. Selecciona elementos en cualquier carpeta y usa \"Copiar a\". | Matches `fossify-commons` `copy_to` (`Copiar a`) |
| 17 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Esta acción no está disponible para álbumes virtuales | Toast error |
| 18 | `alphanumeric_password` | Password | Contraseña | Matches `fossify-commons` `password` |
| 19 | `repeat_password` | Repeat password | Repetir contraseña | Matches `fossify-commons` `repeat_pin` (`Repetir PIN`) |
| 20 | `wrong_password` | Wrong password | Contraseña incorrecta | Password verification error |
| 21 | `encrypting_files` | Encrypting files… | Cifrando archivos… | Standard Spanish crypto terminology (`cifrar`) |
| 22 | `encrypting_folder` | Encrypting folder… | Cifrando carpeta… | Progress title |
| 23 | `folder_encrypted_successfully` | Folder encrypted successfully | Carpeta cifrada correctamente | Success toast |
| 24 | `decrypting_folder` | Decrypting folder… | Descifrando carpeta… | Progress title |
| 25 | `folder_decrypted_successfully` | Folder decrypted successfully | Carpeta descifrada correctamente | Success toast |
| 26 | `decrypting_files` | Decrypting files… | Descifrando archivos… | Progress toast |
| 27 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Por favor, ponga el dedo en el sensor de huellas | Matches `usted` register and `sensor de huellas` |
| 28 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Conecta al menos 4 puntos | Pattern lock minimum dots validation |
| 29 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | ¿Desea acceder a su carpeta Cipher_Secrets existente? | Keeps `Cipher_Secrets` literal (`usted` register) |
| 30 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Se eliminará la carpeta Cipher_Secrets existente y se creará una nueva. ¿Está seguro? | Keeps `Cipher_Secrets` literal (`usted` register) |
| 31 | `confirm_checkbox_label` | I confirm | Confirmo | Checkbox label in `DeleteCipherSecretsDialog` |
| 32 | `secret_vault` | Encrypted Vault | Bóveda cifrada | Vault section title |
| 33 | `secret_vault_empty` | Encrypted Vault is empty | La bóveda cifrada está vacía | Empty state message |
| 34 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | ¿Está seguro de que desea descifrar los archivos ocultos? | Decryption confirmation (`usted` register) |
| 35 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Por seguridad, copiar al portapapeles está deshabilitado mientras la bóveda está desbloqueada. | Clipboard security notice |
| 36 | `on_vault_exit` | On leaving encrypted section | Al salir de la sección cifrada | Settings title |
| 37 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | Seguir mostrando álbumes cifrados | Radio option |
| 38 | `on_vault_exit_hide` | Hide encrypted albums | Ocultar álbumes cifrados | Radio option |
| 39 | `on_vault_exit_ask_every_time` | Always ask | Preguntar siempre | Radio option |
| 40 | `vault_exit_dialog_title` | Exiting Encrypted Section | Saliendo de la sección cifrada | Dialog title |
| 41 | `vault_exit_question_single` | Show encrypted album together with other albums? | ¿Mostrar el álbum cifrado junto con otros álbumes? | Dialog question (single album) |
| 42 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? | ¿Mostrar los álbumes cifrados junto con otros álbumes? | Dialog question (multiple albums) |
| 43 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | Para más opciones, consulta Ajustes &gt; Al salir de la sección cifrada | References `Ajustes` and `on_vault_exit` |
| 44 | `open_system_language_settings` | Open system language settings | Abrir ajustes de idioma del sistema | Uses `ajustes` for `es` |
| 45 | `file_encrypted_successfully` | File encrypted successfully | Archivo cifrado correctamente  | Success toast after encrypting a single file |
| 46 | `files_encrypted_successfully` | Files encrypted successfully | Archivos cifrados correctamente  | Success toast after encrypting multiple files |
| 47 | `file_decrypted_successfully` | File decrypted successfully | Archivo descifrado correctamente | Success toast after decrypting a single file |
| 48 | `files_decrypted_successfully` | Files decrypted successfully | Archivos descifrados correctamente | Success toast after decrypting multiple files |
| 49 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Acceder a elementos ocultos con biometría | Settings switch, setup prompt title, and vault security dialog button |
| 50 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Por favor, active primero la protección con contraseña para elementos ocultos | Toast shown when enabling biometrics before hidden-item password protection |
| 51 | `biometric_unlock_vault` | Unlock encrypted vault | Desbloquear bóveda cifrada | BiometricPrompt title when unlocking the encrypted vault |
| 52 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | Verifique su identidad biométrica para acceder a los elementos ocultos | BiometricPrompt subtitle when unlocking hidden items |
| 53 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | Confirme su identidad biométrica para activar el acceso biométrico a la bóveda | BiometricPrompt subtitle when enabling biometric vault access |
| 54 | `use_password_or_pin` | Use PIN / Password | Usar PIN / Contraseña | BiometricPrompt negative button to fall back to PIN/password |
| 55 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Las credenciales biométricas han cambiado. Por favor, introduzca su PIN o contraseña. | Toast shown when biometric credentials changed and Keystore key invalidated |
| 56 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | No se ha encontrado hardware biométrico ni credenciales registradas en este dispositivo | Toast shown when no biometric hardware or enrolled credentials exist |
| 57 | `default_hidden_album` | Default Hidden Album | Álbum oculto predeterminado | Settings / picker label for default hidden album |
| 58 | `select_hidden_album` | Select Encrypted Album | Seleccionar álbum cifrado | Dialog title for choosing the target encrypted album |
| 59 | `create_new_hidden_album` | Create New Encrypted Album | Crear nuevo álbum cifrado | Action label to create a new encrypted album |
| 60 | `new_hidden_album` | New encrypted album | Nuevo álbum cifrado | Input hint / default label for a new encrypted album |
| 61 | `hidden_album_name_empty` | Album name cannot be empty | El nombre del álbum no puede estar vacío | Validation error when hidden album name is blank |
| 62 | `file_hidden_to_album` | File encrypted to \"%s\" | Archivo cifrado en \"%s\"  | Success toast after encrypting a single file into album `%s` |
| 63 | `files_hidden_to_album` | %1$d files encrypted to \"%2$s\" | %1$d archivos cifrados en \"%2$s\"  | Success toast after encrypting `%1$d` files into album `%2$s` |
| 64 | `pull_down_to_open_vault` | Keep pulling down for Encrypted Vault | Siga deslizando hacia abajo para la bóveda | Vault pull gesture hint shown while the pull is below the open threshold |
| 65 | `keep_holding_to_open_vault` | Keep holding to open Encrypted Vault | Siga manteniendo pulsado para abrir la bóveda cifrada | Vault pull gesture hint shown once the pull passes the threshold, before the 2.5 s hold completes |
| 66 | `release_to_open_vault` | Release to open Encrypted Vault | Suelte para abrir la bóveda cifrada | Vault pull gesture hint shown after holding the pull for 2.5 s; releasing opens the vault |

### 1.3 Newly Added Translations for Strings Already Present in the Original Fossify Gallery (3 Keys)

These 3 strings already exist in the original Fossify Gallery (upstream); only the translations below were newly added in this fork:

| # | Key (`name`) | English (`values/strings.xml`) | Spanish (`values-es/strings.xml`) | Notes |
|--:|---|---|---|---|
| 1 | `enter_password` | Enter password | Introduzca la contraseña | Matches `fossify-commons` `values-es` `enter_password` |
| 2 | `system_default` | System default | Sistema por defecto | Matches `fossify-commons` `values-es` |
| 3 | `language` | Language | Idioma | Language selector title |

### 1.4 `fossify-commons` Inherited UI Strings (14 Keys)

All 14 shared UI keys below are already localized in `org.fossify:commons:7.1.3` (`res/values-es/values-es.xml`); no local override in `values-es/strings.xml` was required:

| # | Key (`name`) | English (`fossify-commons`) | Spanish (`fossify-commons`) | Source / Notes |
|---:|---|---|---|---|
| 1 | `change_date_and_time_format` | Change date and time format | Cambiar formato de fecha y hora | Inherited from `fossify-commons` (Settings) |
| 2 | `password_protect_file_deletion` | Password protect file deletion and moving | Proteger con contraseña eliminar y mover archivos | Inherited from `fossify-commons` (Settings) |
| 3 | `keep_last_modified` | Keep old last-modified value at file operations | Mantener el último valor modificado por última vez en las operaciones de archivo | Inherited from `fossify-commons` (Settings) |
| 4 | `clear_cache` | Clear cache | Limpiar cache | Inherited from `fossify-commons` (Settings) |
| 5 | `export_settings` | Export settings | Exportar ajustes | Inherited from `fossify-commons` (Settings) |
| 6 | `import_settings` | Import settings | Importar ajustes | Inherited from `fossify-commons` (Settings) |
| 7 | `temporarily_show_excluded` | Temporarily show excluded | Mostrar excluidos temporalmente | Inherited from `fossify-commons` (Options menu) |
| 8 | `change_view_type` | Change view type | Cambiar tipo de vista | Inherited from `fossify-commons` (Options menu) |
| 9 | `contributors` | Contributors | Contribuyentes | Inherited from `fossify-commons` (About) |
| 10 | `known_issues` | Known issues | Problemas conocidos | Inherited from `fossify-commons` (About) |
| 11 | `donate_to_fossify` | Donate to Fossify | Apoya a Fossify | Inherited from `fossify-commons` (About) |
| 12 | `privacy_policy` | Privacy policy | Política de privacidad | Inherited from `fossify-commons` (About) |
| 13 | `create_shortcut` | Create shortcut | Crear atajo | Inherited from `fossify-commons` (CAB / ViewPager menu) |
| 14 | `print` | Print | Imprimir | Inherited from `fossify-commons` (ViewPager menu) |
| 15 | `search_files` | Search files | Buscar archivos | Inherited from `fossify-commons` (Search bar hint) |
| 16 | `search_folders` | Search folders | Buscar carpetas | Inherited from `fossify-commons` (Search bar hint) |

### Newly Added Missing Translations (9 Keys) & Revised Entries (11 Keys)

Nine keys that were missing from `values-es/strings.xml` were translated and added, and eleven existing entries were revised so that files and albums use the new \"encrypt / decrypt\" wording and so that the favorites import/export labels mention virtual albums as well.

| # | Key (`name`) | English (`values/strings.xml`) | Spanish (Standard) (`values-es/strings.xml`) | Notes |
|---:|---|---|---|---|
| 1 | `number_of_items` | Number of items | Número de elementos | Sorting option shown in the album view only; overridden locally because it is missing in `fossify-commons` for this locale |
| 2 | `random` | Random | Aleatorio | Sorting option offered in every sorting state; overridden locally because it is missing in `fossify-commons` for this locale |
| 3 | `shuffle` | Shuffle | Mezclar | Fullscreen viewer action that randomizes the media order; app owned string that was only available in English |
| 4 | `stop_showing_excluded` | Stop showing excluded | Dejar de mostrar los excluidos | Main screen options-menu action that ends the temporary preview of excluded folders |
| 5 | `copy_to_clipboard` | Copy to clipboard | Copiar al portapapeles | Fullscreen viewer action that copies a file to the clipboard |
| 6 | `encrypt` | Encrypt | Cifrar | Replaces the inherited `hide` label for files; files and albums are encrypted, not hidden |
| 7 | `decrypt` | Decrypt | Descifrar | Replaces the inherited `unhide` label for files |
| 8 | `encrypt_album` | Encrypt album | Cifrar el álbum | Album (folder) context action that encrypts the album into the vault |
| 9 | `decrypt_album` | Decrypt album | Descifrar el álbum | Album (folder) context action that decrypts the album back to its original location |
| 10 | `export_favorite_paths` | Export favorites and virtual albums | Exportar favoritos y álbumes virtuales | Settings label; renamed because virtual albums are exported together with the favorites |
| 11 | `import_favorite_paths` | Import favorites and virtual albums | Importar favoritos y álbumes virtuales | Settings label; renamed because virtual albums are imported together with the favorites |
| 12 | `file_encrypted_successfully` | File encrypted successfully | Archivo cifrado correctamente | Success toast after encrypting a single file |
| 13 | `files_encrypted_successfully` | Files encrypted successfully | Archivos cifrados correctamente | Success toast after encrypting multiple files |
| 14 | `file_decrypted_successfully` | File decrypted successfully | Archivo descifrado correctamente | Success toast after decrypting a single file |
| 15 | `files_decrypted_successfully` | Files decrypted successfully | Archivos descifrados correctamente | Success toast after decrypting multiple files |
| 16 | `file_hidden_to_album` | File encrypted to \"%s\" | Archivo cifrado en \"%s\" | Success toast after encrypting a single file into album `%s` |
| 17 | `files_hidden_to_album` | %1$d files encrypted to \"%2$s\" | %1$d archivos cifrados en \"%2$s\" | Success toast after encrypting `%1$d` files into album `%2$s` |
| 18 | `select_hidden_album` | Select Encrypted Album | Seleccionar álbum cifrado | Dialog title for choosing the target encrypted album |
| 19 | `create_new_hidden_album` | Create New Encrypted Album | Crear nuevo álbum cifrado | Action label to create a new encrypted album |
| 20 | `new_hidden_album` | New encrypted album | Nuevo álbum cifrado | Input hint / default label for a new encrypted album |

## 2. Full 1-to-1 Comparison: English vs. Spanish (Standard)

| # | Key (`name`) | English (`values/strings.xml`) | Spanish (`values-es/strings.xml`) |
|--:|---|---|---|
| 1 | `app_launcher_name` | Gallery | Galería |
| 2 | `edit` | Edit | Editar |
| 3 | `open_camera` | Open camera | Abrir la cámara |
| 4 | `hidden` | (hidden) | (oculto) |
| 5 | `excluded` | (excluded) | (excluido) |
| 6 | `pin_folder` | Pin folder | Fijar carpeta |
| 7 | `unpin_folder` | Unpin folder | Desfijar carpeta |
| 8 | `pin_to_the_top` | Pin to the top | Anclar arriba |
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
| 23 | `use_default_orientation` | Use default orientation | Usar la orientación por defecto |
| 24 | `fix_date_taken` | Fix Date Taken value | Arreglar fecha de la toma |
| 25 | `fixing` | Fixing… | Arreglando… |
| 26 | `dates_fixed_successfully` | Dates fixed successfully | Fechas arregladas correctamente |
| 27 | `no_date_takens_found` | No Date Taken values have been found | No se han encontrado valores de fecha de la toma |
| 28 | `share_resized` | Share a resized version | Compartir una versión redimensionada |
| 29 | `switch_to_file_search` | Switch to file search across all visible folders | Cambiar a la búsqueda de archivos en todas las carpetas visibles |
| 30 | `set_as_default_folder` | Set as default folder | Poner como carpeta predeterminada |
| 31 | `unset_as_default_folder` | Unset as default folder | Quitar como carpeta predeterminada |
| 32 | `reorder_by_dragging` | Reorder folders by dragging | Reordenar carpetas arrastrándolas |
| 33 | `reorder_by_dragging_pro` | Reorder folders by dragging (Pro) | Reordenar carpetas arrastrándolas (Pro) |
| 34 | `restore_to_path` | Restoring to \'%s\' | Restaurando a \'%s\' |
| 35 | `full_storage_permission_required` | Fossify Gallery needs full access to display all your photos and videos. Go to Settings &gt; Permissions &gt; Photos and videos &gt; Allow all. | La Galería Fossify necesita acceso total para mostrar todas tus fotos y vídeos. Ve a Ajustes &gt; Permisos &gt; Fotos y vídeos &gt; Permitir todo. |
| 36 | `filter_media` | Filter media | Filtro de medios |
| 37 | `images` | Images | Imágenes |
| 38 | `videos` | Videos | Vídeos |
| 39 | `gifs` | GIFs | Imágenes con animación |
| 40 | `raw_images` | RAW images | Imágenes RAW |
| 41 | `svgs` | SVGs | SVG |
| 42 | `portraits` | Portraits | Retratos |
| 43 | `no_media_with_filters` | No media files have been found with the selected filters. | No se han encontrado ficheros con los filtros seleccionados. |
| 44 | `change_filters_underlined` | <u>Change filters</u> | <u>Cambiar filtros</u> |
| 45 | `hide_folder_description` | This function hides the folder by adding a \'.nomedia\' file into it, it will hide all subfolders too. You can see them by toggling the \'Show hidden items\' option in Settings. Continue? | Esta función oculta la carpeta agregando un archivo \'.nomedia\' en ella, y también ocultará las subcarpetas. Puede mostrarlas activando la opción \'Mostrar elementos ocultos\' en los Ajustes. ¿Continuar? |
| 46 | `exclude_folder_description` | This will exclude the selection together with its subfolders from Fossify Gallery only. You can manage excluded folders in Settings. | Esto excluirá la selección junto con sus subcarpetas solo de la Galería Fossify. Puede gestionar las carpetas excluidas en los Ajustes. |
| 47 | `exclude_folder_parent` | Exclude a parent instead? | ¿Excluir mejor la carpeta superior? |
| 48 | `excluded_activity_placeholder` | Excluding folders will make them together with their subfolders hidden just in Fossify Gallery, they will still be visible in other applications.\n\nIf you want to hide them from other apps too, use the Hide function. | Al excluir las carpetas, estas y sus subcarpetas quedarán ocultas solo en la Galería Fossify, pero seguirán siendo visibles en otras aplicaciones.\n\nSi desea ocultarlas también de otras aplicaciones, utilice la función Ocultar. |
| 49 | `hidden_folders` | Hidden folders | Carpetas ocultas |
| 50 | `manage_hidden_folders` | Manage hidden folders | Gestionar carpetas ocultas |
| 51 | `hidden_folders_placeholder` | Seems like you don\'t have any folders hidden with a \".nomedia\" file. | Parece que no tiene carpetas ocultas con archivos \".nomedia\". |
| 52 | `hidden_all_files` | You have to grant the app All Files access to see hidden files, else it cannot work. | Tienes que conceder a la aplicación el acceso a todos los archivos para ver los archivos ocultos, de lo contrario no puede funcionar. |
| 53 | `cant_unhide_folder` | If a folder or one of its parent folders has a dot before its name, it is hidden and cannot be unhidden like this. You have to remove the dot by renaming it. | Si una carpeta o una de sus carpetas principales tiene un punto antes del nombre, está oculta y no se puede volver a mostrar de esta manera. Debes eliminar el punto cambiándole el nombre. |
| 54 | `include_folders` | Included folders | Carpetas incluidas |
| 55 | `manage_included_folders` | Manage included folders | Gestionar carpetas incluidas |
| 56 | `add_folder` | Add folder | Agregar carpeta |
| 57 | `included_activity_placeholder` | If you have some folders which contain media, but were not recognized by the app, you can add them manually here.\n\nAdding some items here will not exclude any other folder. | Si tiene algunas carpetas que contengan multimedia, pero que no fueron reconocidas por la aplicación, puede añadirlas manualmente aquí.\n\nAñadir elementos aquí no excluirá ninguna otra carpeta. |
| 58 | `no_media_add_included` | No media files have been found. You can solve it by adding the folders containing media files manually. | Ningún archivo multimedia ha sido encontrado. Puede resolverlo añadiendo las carpetas que contengan archivos multimedia manualmente. |
| 59 | `resize_and_save` | Resize selection and save | Redimensionar la selección y guardar |
| 60 | `width` | Width | Ancho |
| 61 | `height` | Height | Alto |
| 62 | `keep_aspect_ratio` | Keep aspect ratio | Mantener proporciones |
| 63 | `invalid_values` | Please enter a valid resolution | Por favor, introduzca una resolución válida |
| 64 | `resize_multiple_images` | Resize multiple images | Redimensionar varias imágenes |
| 65 | `resize_factor` | Resize factor | Factor de redimensión |
| 66 | `resize_factor_info` | Resize images to the given percentage, value must be within 10 and 90. | Redimensiona las imágenes al porcentaje dado, el valor debe estar entre 10 y 90. |
| 67 | `resize_factor_error` | Enter a number between 10 and 90 | Introduce un número entre 10 y 90 |
| 68 | `images_resized_successfully` | Images resized successfully | Imágenes redimensionadas correctamente |
| 69 | `editor` | Editor | Editor |
| 70 | `basic_editor` | Basic Editor | Editor básico |
| 71 | `advanced_editor` | Advanced Editor | Editor avanzado |
| 72 | `rotate` | Rotate | Rotar |
| 73 | `invalid_image_path` | Invalid image path | Ruta de imagen inválida |
| 74 | `invalid_video_path` | Invalid video path | Ruta de vídeo inválida |
| 75 | `image_editing_failed` | Image editing failed | Edición de imagen fallida |
| 76 | `video_editing_failed` | Video editing failed | Edición de vídeo fallida |
| 77 | `image_editing_cancelled` | Image editing cancelled | Edición de imagen cancelada |
| 78 | `video_editing_cancelled` | Video editing cancelled | Edición de vídeo cancelada |
| 79 | `file_edited_successfully` | File edited successfully | Archivo editado con éxito |
| 80 | `image_edited_successfully` | Image edited successfully | Imagen editada con éxito |
| 81 | `video_edited_successfully` | Video edited successfully | Vídeo editado con éxito |
| 82 | `edit_image_with` | Edit image with: | Editar imagen con: |
| 83 | `edit_video_with` | Edit video with: | Editar vídeo con: |
| 84 | `no_image_editor_found` | No image editor found | No se encontró editor de imágenes |
| 85 | `no_video_editor_found` | No video editor found | No se encontró editor de vídeo |
| 86 | `unknown_file_location` | Unknown file location | Ubicación del archivo desconocida |
| 87 | `overwrite_original` | Overwrite original | Sobrescribir original |
| 88 | `error_saving_file` | Could not overwrite the source file | No se ha podido sobrescribir el archivo de origen |
| 89 | `rotate_left` | Rotate left | Rotar a la izquierda |
| 90 | `rotate_right` | Rotate right | Rotar a la derecha |
| 91 | `rotate_one_eighty` | Rotate by 180º | Rotar 180º |
| 92 | `transform` | Transform | Transformar |
| 93 | `crop` | Crop | Recortar |
| 94 | `draw` | Draw | Dibujar |
| 95 | `flip` | Flip | Girar |
| 96 | `flip_horizontally` | Flip horizontally | Horizontalmente |
| 97 | `flip_vertically` | Flip vertically | Verticalmente |
| 98 | `free_aspect_ratio` | Free | Libre |
| 99 | `other_aspect_ratio` | Other | Otro |
| 100 | `wallpaper` | Wallpaper | Fondo de pantalla |
| 101 | `set_as_wallpaper` | Set as Wallpaper | Establecer como fondo de pantalla |
| 102 | `set_as_wallpaper_failed` | Setting as Wallpaper failed | Error al establecer el fondo de pantalla |
| 103 | `set_as_wallpaper_with` | Set as wallpaper with: | Establecer como fondo de pantalla con: |
| 104 | `setting_wallpaper` | Setting wallpaper… | Estableciendo fondo de pantalla… |
| 105 | `wallpaper_set_successfully` | Wallpaper set successfully | Fondo de pantalla establecido correctamente |
| 106 | `portrait_aspect_ratio` | Portrait aspect ratio | Relación de aspecto tipo retrato |
| 107 | `landscape_aspect_ratio` | Landscape aspect ratio | Relación de aspecto tipo paisaje |
| 108 | `home_screen` | Home screen | Pantalla de inicio |
| 109 | `lock_screen` | Lock screen | Pantalla de bloqueo |
| 110 | `home_and_lock_screen` | Home and lock screen | Pantalla de inicio y de bloqueo |
| 111 | `allow_changing_aspect_ratio` | Allow changing the aspect ratio | Permitir cambiar la relación de aspecto |
| 112 | `slideshow` | Slideshow | Presentación de diapositivas |
| 113 | `interval` | Interval | Intervalo |
| 114 | `include_photos` | Include photos | Incluir fotos |
| 115 | `include_videos` | Include videos | Incluir vídeos |
| 116 | `include_gifs` | Include GIFs | Incluir GIF |
| 117 | `random_order` | Random order | Orden aleatorio |
| 118 | `move_backwards` | Move backwards | Mover hacia atrás |
| 119 | `loop_slideshow` | Loop slideshow | Presentación en bucle |
| 120 | `animation` | Animation | Animación |
| 121 | `no_animation` | None | Ninguna |
| 122 | `fade` | Fade | Desvanecimiento |
| 123 | `slide` | Slide | Desplazamiento |
| 124 | `slideshow_ended` | The slideshow ended | La presentación de diapositivas terminó |
| 125 | `no_media_for_slideshow` | No media for the slideshow have been found | No se han encontrado medios para la presentación de diapositivas |
| 126 | `group_direct_subfolders` | Group direct subfolders | Agrupar subcarpetas directas |
| 127 | `group_by` | Group by | Agrupar por |
| 128 | `do_not_group_files` | Do not group files | No agrupar |
| 129 | `by_folder` | Folder | Carpeta |
| 130 | `by_last_modified` | Last modified | Fecha de modificación |
| 131 | `by_last_modified_daily` | Last modified (daily) | Fecha de modificación (día) |
| 132 | `by_last_modified_monthly` | Last modified (monthly) | Fecha de modificación (mes) |
| 133 | `by_date_taken` | Date taken | Fecha de creación |
| 134 | `by_date_taken_daily` | Date taken (daily) | Fecha de creación (día) |
| 135 | `by_date_taken_monthly` | Date taken (monthly) | Fecha de creación (mes) |
| 136 | `by_file_type` | File type | Tipo de archivo |
| 137 | `by_extension` | Extension | Extensión |
| 138 | `show_file_count_section_header` | Show file count at section headers | Mostrar recuento de archivos en los encabezados de sección |
| 139 | `grouping_and_sorting` | Please note that grouping and sorting are 2 independent fields | Tenga en cuenta que la agrupación y la clasificación son 2 campos independientes |
| 140 | `folder_on_widget` | Folder shown on the widget: | Carpeta mostrada en el widget: |
| 141 | `show_folder_name` | Show folder name | Mostrar nombre de carpeta |
| 142 | `autoplay_videos` | Play videos automatically | Reproducir vídeos automáticamente |
| 143 | `remember_last_video_position` | Remember last video playback position | Recordar la última posición de avance en la reproducción de vídeo |
| 144 | `loop_videos` | Loop videos | Repetir reproducción de vídeos en bucle |
| 145 | `video_double_tap_duration` | Double-tap seek duration | Duración de salto con doble toque |
| 146 | `video_seek_duration_seconds` | %d seconds | %d segundos |
| 147 | `animate_gifs` | Animate GIFs at thumbnails | Animar las miniaturas de GIF |
| 148 | `max_brightness` | Max brightness when viewing fullscreen media | Incrementar el brillo de la pantalla al máximo cuando se muestran los archivos multimedia |
| 149 | `render_ultra_hdr_photos` | Show Ultra HDR photos in HDR | Mostrar las fotos Ultra HDR en HDR |
| 150 | `crop_thumbnails` | Crop thumbnails into squares | Recortar miniaturas en cuadrados |
| 151 | `show_thumbnail_video_duration` | Show video durations | Mostrar la duración de los vídeos |
| 152 | `screen_rotation_by` | Rotate fullscreen media by | Rotar multimedia en pantalla completa según |
| 153 | `screen_rotation_system_setting` | System setting | Ajustes del sistema |
| 154 | `screen_rotation_device_rotation` | Device rotation | Rotación del dispositivo |
| 155 | `screen_rotation_aspect_ratio` | Aspect ratio | Relación de aspecto |
| 156 | `black_background_at_fullscreen` | Black background at fullscreen media | Fondo y barra de estado completamente oscuros en medios al estar en pantalla completa |
| 157 | `scroll_thumbnails_horizontally` | Scroll thumbnails horizontally | Desplazar miniaturas horizontalmente |
| 158 | `hide_system_ui_at_fullscreen` | Automatically hide system UI at fullscreen media | Ocultar la interfaz del sistema en pantalla completa |
| 159 | `delete_empty_folders` | Delete empty folders after deleting their content | Eliminar carpetas vacías después de borrar su contenido |
| 160 | `keep_screen_on_fullscreen_photos` | Keep the screen on while viewing fullscreen photos | Mantener pantalla encendida al ver fotos en pantalla completa |
| 161 | `allow_photo_gestures` | Allow controlling photo brightness with vertical gestures | Permitir controlar el brillo en las fotos con gestos verticales |
| 162 | `allow_video_gestures` | Allow controlling video volume and brightness with vertical gestures | Permitir controlar el volumen y el brillo del vídeo con gestos verticales |
| 163 | `playback_speed` | Playback speed | Velocidad de reproducción |
| 164 | `show_media_count` | Show folder media count on the main view | Mostrar el número de archivos dentro de las carpetas en la página principal |
| 165 | `show_extended_details` | Show extended details over fullscreen media | Mostrar información de los archivos en pantalla completa |
| 166 | `manage_extended_details` | Manage extended details | Información de los archivos a mostrar |
| 167 | `one_finger_zoom` | Allow one finger zoom at fullscreen media | Permitir zoom con un dedo en pantalla completa |
| 168 | `allow_instant_change` | Allow instantly changing media by clicking on screen sides | Cambiar de medio al instante tocando los lados de la pantalla |
| 169 | `allow_deep_zooming_images` | Allow deep zooming images | Permitir zoom profundo |
| 170 | `hide_extended_details` | Hide extended details when status bar is hidden | Ocultar detalles ampliados cuando la barra de estado está oculta |
| 171 | `show_at_bottom` | Show some action buttons at the bottom of the screen | Mostrar botones de acción en la parte inferior de la pantalla |
| 172 | `show_recycle_bin` | Show the recycle bin on the folders screen | Mostrar la papelera de reciclaje en la vista de carpetas |
| 173 | `deep_zoomable_images` | Deep zoomable images | Imágenes ampliables con zoom |
| 174 | `show_highest_quality` | Show images in the highest possible quality | Mostrar imágenes con la mayor calidad posible |
| 175 | `show_recycle_bin_last` | Show the recycle bin as the last item on the main screen | Mostrar la papelera de reciclaje como último elemento en la pantalla principal |
| 176 | `allow_down_gesture` | Allow closing the fullscreen view with a down gesture | Permitir cerrar la vista de pantalla completa con un gesto hacia abajo |
| 177 | `allow_one_to_one_zoom` | Allow 1:1 zooming in with two double taps | Permitir zoom 1:1 con dos toques dobles |
| 178 | `open_videos_on_separate_screen` | Play videos on a separate screen with horizontal gestures | Reproducir vídeos en una pantalla separada con gestos horizontales |
| 179 | `on_video_tap` | On video tap | Al tocar el vídeo |
| 180 | `open_in_app_player` | Open in-app player | Abrir el reproductor de la aplicación |
| 181 | `open_system_default_player` | Open default player app | Abrir la aplicación de reproducción predeterminada |
| 182 | `show_notch` | Show a notch if available | Mostrar una muesca si está disponible |
| 183 | `allow_rotating_gestures` | Allow rotating images with gestures | Permitir girar con gestos |
| 184 | `file_loading_priority` | File loading priority | Prioridad de carga de archivos |
| 185 | `speed` | Speed | Velocidad |
| 186 | `compromise` | Compromise | Compromiso |
| 187 | `avoid_showing_invalid_files` | Avoid showing invalid files | Evitar mostrar archivos inválidos |
| 188 | `show_image_file_types` | Show image file types | Mostrar tipos de archivo de imagen |
| 189 | `allow_zooming_videos` | Allow zooming videos with double tapping them | Permitir hacer zoom en vídeos con doble toque |
| 190 | `folder_thumbnail_style` | Folder thumbnail style | Estilo de las miniaturas de las carpetas |
| 191 | `file_thumbnail_style` | File thumbnail style | Estilo de las miniaturas de los archivos |
| 192 | `mark_favorite_items` | Mark favorite items | Marcar elementos favoritos |
| 193 | `thumbnail_spacing` | Thumbnail spacing | Espaciado de las miniaturas |
| 194 | `show_file_count_line` | Show file count on a separate line | Mostrar el recuento de archivos en una línea separada |
| 195 | `show_file_count_brackets` | Show file count in brackets | Mostrar el recuento de archivos entre paréntesis |
| 196 | `show_file_count_none` | Do not show file count | No mostrar el recuento de archivos |
| 197 | `limit_folder_title` | Limit long folder titles to 1 line | Limitar los títulos largos de las carpetas a 1 línea |
| 198 | `square` | Square | Cuadrado |
| 199 | `rounded_corners` | Rounded corners | Esquinas redondeadas |
| 200 | `export_favorite_paths` | Export favorites and virtual albums | Exportar favoritos y álbumes virtuales |
| 201 | `import_favorite_paths` | Import favorites and virtual albums | Importar favoritos y álbumes virtuales |
| 202 | `paths_imported_successfully` | Paths imported successfully | Rutas importadas correctamente |
| 203 | `media_management_prompt` | To make sure that all file operations work reliably, please make this app a Media management app in your device settings. | Para asegurarse de que todas las operaciones en ficheros funcionan de forma segura, por favor haga esta aplicación una aplicación de manejo de medios en los ajustes de su dispositivo. |
| 204 | `password_protect_excluded` | Password protect excluded folder visibility | Proteger con contraseña la visibilidad de las carpetas excluidas |
| 205 | `media_management_manual` | Something went wrong, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Algo ha ido mal, por favor vaya a los Ajustes de su dispositivo - Aplicaciones - Acceso especial de aplicaciones - Aplicaciones de gestión de multimedia y permita que esta aplicación gestione los medios. |
| 206 | `media_management_note` | If the redirection does not work, please go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Si la redirección no funciona, ve a los Ajustes de tu dispositivo - Aplicaciones - Acceso especial de aplicaciones - Aplicaciones de gestión de multimedia y permite que esta aplicación gestione los medios. |
| 207 | `media_management_alternative` | If you do not want to do it, you can also go into your device Settings - Apps - Special app access - Media management apps and allow this app to manage media. | Si no quieres hacerlo, también puedes ir a los Ajustes de tu dispositivo - Aplicaciones - Acceso especial de aplicaciones - Aplicaciones de gestión de multimedia y permitir que esta aplicación gestione los medios. |
| 208 | `alternative_media_access` | Alternatively, you can allow accessing media files only. In that case you will not be able to work with hidden files though. | Alternativamente, puede permitir el acceso a los archivos multimedia solamente. Sin embargo, en ese caso no podrá trabajar con los archivos ocultos. |
| 209 | `media_only` | Media only | Solo archivos multimedia |
| 210 | `all_files` | All files | Todos los archivos |
| 211 | `main_screen_view_on_launch` | Main screen view on launch | Vista de la pantalla principal al iniciar |
| 212 | `show_folders` | Show folders | Mostrar carpetas |
| 213 | `show_files` | Show files | Mostrar archivos |
| 214 | `show_all_folders` | Show a menu button for toggling Show All Folders Content quickly | Mostrar un botón de menú para ver rápidamente el contenido de todas las carpetas |
| 215 | `thumbnails` | Thumbnails | Miniaturas |
| 216 | `fullscreen_media` | Fullscreen media | Medios a pantalla completa |
| 217 | `extended_details` | Extended details | Detalles ampliados |
| 218 | `bottom_actions` | Bottom actions | Acciones en la parte inferior |
| 219 | `manage_bottom_actions` | Manage visible bottom actions | Administrar los botones de la parte inferior |
| 220 | `toggle_favorite` | Toggle favorite | Alternar favorito |
| 221 | `toggle_file_visibility` | Toggle file visibility | Alternar visibilidad del archivo |
| 222 | `failed_to_load_media` | Failed to load media. | Error al cargar el medio. |
| 223 | `file_is_malformed_or_corrupted` | File is malformed or corrupted. | El archivo está malformado o dañado. |
| 224 | `media_exceeds_device_capabilities` | Media exceeds device capabilities. | Los medios superan las capacidades del dispositivo. |
| 225 | `unsupported_format` | Unsupported format. | Formato no soportado. |
| 226 | `video_no_sound` | This video has no audio | Este vídeo no tiene audio |
| 227 | `faq_1_title` | How can I make Fossify Gallery the default device gallery? | ¿Cómo puedo hacer que la Galería Fossify sea la galería predeterminada del dispositivo? |
| 228 | `faq_1_text` | First you have to find the currently default gallery in the Apps section of your device settings, look for a button that says something like \"Open by default\", click on it, then select \"Clear defaults\". The next time you will try opening an image or video you should see an app picker, where you can select Fossify Gallery and make it the default app. | Primero tienes que encontrar la galería actualmente predeterminada en la sección de Aplicaciones de los ajustes de tu dispositivo, busca un botón que diga algo así como \"Abrir por defecto\", haz clic en él y luego selecciona \"Borrar valores predeterminados\". La próxima vez que intentes abrir una imagen o un vídeo deberías ver un selector de aplicaciones, donde puedes seleccionar Galería Fossify y convertirla en la aplicación predeterminada. |
| 229 | `faq_2_title` | I locked the app with a password, but I forgot it. What can I do? | He protegido la aplicación con una contraseña, pero la he olvidado. ¿Qué puedo hacer? |
| 230 | `faq_2_text` | You can solve it in 2 ways. You can either reinstall the app, or find the app in your device settings and select \"Clear data\". It will reset all your settings, it will not remove any media files. | Puede resolverlo de 2 maneras. Puede reinstalar la aplicación o encontrar la aplicación en los ajustes de su dispositivo y seleccionar \"Borrar datos\". Restablecerá todos sus ajustes, no eliminará ningún archivo multimedia. |
| 231 | `faq_3_title` | How can I make an album always appear at the top? | ¿Cómo puedo hacer que un álbum siempre aparezca en la parte superior? |
| 232 | `faq_3_text` | You can long press the desired album and select the Pin icon at the actions menu, that will pin it to the top. You can pin multiple folders too, pinned items will be sorted by the default sorting method. | Puede mantener pulsado el álbum deseado y seleccionar el icono Fijar en el menú de acción, que lo fijará en la parte superior. También puede fijar varias carpetas; los elementos fijados se ordenarán por el método de clasificación predeterminado. |
| 233 | `faq_4_title` | How can I fast forward videos? | ¿Cómo puedo avanzar vídeos? |
| 234 | `faq_4_text` | You can do it by double tapping the side of the screen, or tapping the current or max duration texts near the seekbar. If you enable opening videos on a separate screen in the app settings, you can use horizontal gestures too. | Puede hacerlo tocando dos veces el costado de la pantalla o tocando los textos de duración actual o máxima cerca de la barra de progreso. Si habilita la apertura de vídeos en una pantalla separada en los ajustes de la aplicación, también puede usar gestos horizontales. |
| 235 | `faq_5_title` | What is the difference between hiding and excluding a folder? | ¿Cuál es la diferencia entre ocultar y excluir una carpeta? |
| 236 | `faq_5_text` | Exclude prevents displaying the folder only in Fossify Gallery, while Hide works system-wise and it hides the folder from other galleries too. It works by creating an empty \".nomedia\" file in the given folder, which you can then remove with any file manager too. Note that some devices do not allow hiding folders like Camera, Screenshots and Downloads. | Excluir evita que la carpeta se muestre solo en la Galería Fossify, mientras que Ocultar funciona a nivel de sistema y oculta la carpeta de otras galerías también. Funciona mediante la creación de un archivo vacío \".nomedia\" en la carpeta dada, que luego se puede eliminar con cualquier gestor de archivos también. Ten en cuenta que algunos dispositivos no permiten ocultar carpetas como Cámara, Capturas de pantalla y Descargas. |
| 237 | `faq_6_title` | Why do folders with music cover art or stickers show up? | ¿Por qué aparecen carpetas con la portada de la música o pegatinas? |
| 238 | `faq_6_text` | It can happen that you will see some unusual albums show up. You can easily exclude them by long pressing them and selecting Exclude. In the next dialog you can then select the parent folder, chances are it will prevent the other related albums showing up too. | Puede suceder que vea aparecer algunos álbumes inusuales. Puede excluirlos fácilmente manteniéndolos pulsados y seleccionando Excluir. En el siguiente cuadro de diálogo, puede seleccionar la carpeta principal; lo más probable es que evite que aparezcan otros álbumes relacionados. |
| 239 | `faq_7_title` | A folder with images isn\'t showing up, or it doesn\'t show all items.  What can I do? | Una carpeta con imágenes no aparece, ¿qué puedo hacer? |
| 240 | `faq_7_text` | That can have multiple reasons, but solving it is easy. Just go in Settings -&gt; Manage Included Folders, select Plus and navigate to the required folder. | Eso puede tener múltiples razones, pero resolverlo es fácil. Simplemente vaya a Ajustes -&gt; Gestionar carpetas incluidas, seleccione Más y vaya a la carpeta requerida. |
| 241 | `faq_8_title` | What if I want just a few particular folders visible? | ¿Qué pasa si quiero solo algunas carpetas concretas visibles? |
| 242 | `faq_8_text` | Adding a folder at the Included Folders doesn\'t automatically exclude anything. What you can do is go in Settings -&gt; Manage Excluded Folders, exclude the root folder \"/\", then add the desired folders at Settings -&gt; Manage Included Folders. That will make only the selected folders visible, as both excluding and including are recursive and if a folder is both excluded and included, it will show up. | Agregar una carpeta a Carpetas incluidas no excluye automáticamente nada. Lo que puede hacer es ir a Ajustes -&gt; Gestionar carpetas excluidas, excluir la carpeta raíz \"/\", luego agregar las carpetas deseadas en Ajustes -&gt; Gestionar carpetas incluidas. Esto hará que solo las carpetas seleccionadas sean visibles, ya que tanto la exclusión como la inclusión son recursivas y si una carpeta está excluida e incluida, aparecerá. |
| 243 | `faq_10_title` | Can I crop images with this app? | ¿Puedo recortar imágenes con esta aplicación? |
| 244 | `faq_10_text` | Yes, you can crop images in the editor, by dragging the image corners. You can get to the editor either by long pressing an image thumbnail and selecting Edit, or selecting Edit from the fullscreen view. | Sí, puede recortar imágenes en el editor arrastrando las esquinas de la imagen. Puede acceder al editor pulsando prolongadamente una imagen en miniatura y seleccionando Editar, o seleccionando Editar en la vista de pantalla completa. |
| 245 | `faq_11_title` | Can I somehow group media file thumbnails? | ¿Puedo de alguna manera agrupar miniaturas de archivos multimedia? |
| 246 | `faq_11_text` | Sure, just use the \"Group by\" menu item while at the thumbnails view. You can group files by multiple criteria, including Date Taken. If you use the \"Show all folders content\" function you can group them by folders too. | Claro, solo use el elemento de menú \"Agrupar por\" mientras esté en la vista de miniaturas. Puede agrupar archivos según varios criterios, incluida la Fecha de creación. Si usa la función \"Mostrar el contenido de todas las carpetas\" también puede agruparlas por carpetas. |
| 247 | `faq_12_title` | Sorting by Date Taken doesn\'t seem to work properly, how can I fix it? | La ordenación por fecha tomada no parece funcionar correctamente, ¿cómo puedo solucionarlo? |
| 248 | `faq_12_text` | It is most likely caused by the files being copied from somewhere. You can fix it by selecting the file thumbnails and selecting \"Fix Date Taken value\". | Lo más probable es que sea causado por los archivos que se copian de algún lugar. Puedes arreglarlo seleccionando las miniaturas de archivo y seleccionando \"Arreglar fecha de la toma\". |
| 249 | `faq_13_title` | I see some color banding on the images. How can I improve the quality? | Veo algunas bandas de color en las imágenes. ¿Cómo puedo mejorar la calidad? |
| 250 | `faq_13_text` | The current solution for displaying images works fine in the vast majority of cases, but if you want even better image quality, you can enable the \"Show images in the highest possible quality\" at the app settings, in the \"Deep zoomable images\" section. | La solución actual para mostrar imágenes funciona bien en la gran mayoría de los casos, pero si desea una calidad de imagen aún mejor, puede habilitar \"Mostrar imágenes con la mayor calidad posible\" en los ajustes de la aplicación, en la sección \"Imágenes ampliables con zoom\". |
| 251 | `faq_14_title` | I have hidden a file/folder. How can I unhide it? | He ocultado un archivo/carpeta. ¿Cómo puedo mostrarlo de nuevo? |
| 252 | `faq_14_text` | You can either press the \"Temporarily show hidden items\" menu item at the main screen, or toggle \"Show hidden items\" in the app settings to see the hidden item. If you want to unhide it, just long press it and select \"Unhide\". Folders are hidden by adding a hidden \".nomedia\" file into them, you can delete the file with any file manager too. Note that hiding works recursively though, so if you hide a folder, all subfolders will become hidden too. So for unhiding the subfolders you have to unhide the parent folder. | Puede presionar el elemento de menú \"Mostrar ocultos temporalmente\" en la pantalla principal, o alternar \"Mostrar elementos ocultos\" en los ajustes de la aplicación para ver el elemento oculto. Si desea mostrarlo, simplemente manténgalo presionado y seleccione \"Mostrar\". Las carpetas se ocultan agregando un archivo \".nomedia\" oculto en ellas; también puede eliminar el archivo con cualquier administrador de archivos. Sin embargo, tenga en cuenta que la ocultación funciona de forma recursiva, por lo que si oculta una carpeta, todas las subcarpetas también se ocultarán. Así que, para mostrar las subcarpetas, debe mostrar la carpeta principal. |
| 253 | `faq_15_title` | Why does the app take up so much space? | ¿Por qué la aplicación ocupa tanto espacio? |
| 254 | `faq_15_text` | App cache can take up to 250MB, it ensures quicker image loading. If the app is taking up even more space, it is most likely caused by you having items in the recycle bin. Those files count to the app size. You can clear the recycle bin by opening it and deleting all files, or from the app settings. Every file in the bin is deleted automatically after 30 days. | La caché de la aplicación puede ocupar hasta 250 MB, lo que permite cargar las imágenes más rápido. Si la aplicación está usando aún más espacio, lo más probable es que tengas archivos en la papelera de reciclaje, ya que también cuentan dentro del tamaño de la aplicación. Puedes vaciar la papelera abriéndola y eliminando todos los archivos, o desde los ajustes de la aplicación. Cada archivo de la papelera se elimina automáticamente después de 30 días. |
| 255 | `faq_16_title` | What happened to file and folder hiding and why cannot I see hidden items anymore? | ¿Qué ha pasado con la ocultación de archivos y carpetas y por qué ya no puedo ver los elementos ocultos? |
| 256 | `faq_16_text` | Starting with Android 11 you cannot hide or unhide files or folders anymore, you cannot see the hidden ones in gallery apps due to system restrictions either. You will have to use some file manager for that. | A partir de Android 11 ya no puede ocultar o mostrar archivos o carpetas, tampoco puede ver los ocultos en aplicaciones de galería debido a restricciones del sistema. Tendrá que usar algún gestor de archivos para ello. |
| 257 | `faq_16_text_extra` | Or you can also grant this gallery access to All Files through your device settings, that will allow us showing hidden items and make file operations more reliable in general. | O también puede conceder a esta galería el acceso a todos los archivos a través de los ajustes de su dispositivo, lo que nos permitirá mostrar los elementos ocultos y hacer más fiables las operaciones con los archivos en general. |
| 258 | `faq_17_title` | Why cannot I include missing folders anymore? | ¿Por qué ya no puedo incluir las carpetas que faltan? |
| 259 | `faq_17_text` | That stopped working due to the system changes that came with Android 11 too, the app cannot browse real folders anymore, it relies on the so called MediaStore at fetching data. | Eso dejó de funcionar debido a los cambios en el sistema que vinieron con Android 11 también; la aplicación ya no puede explorar las carpetas reales, sino que se basa en el llamado MediaStore para obtener los datos. |
| 260 | `faq_18_title` | Why do I see ads during video playback? | ¿Por qué veo anuncios durante la reproducción de vídeo? |
| 261 | `faq_18_text` | Our apps have no ads whatsoever. If you see them during video playback, you must be using some other apps video player. Try finding your default video player in the device settings, then do a \"Clear defaults\" on it. The next time you invoke some video intent you will see an app picker prompt, where you can select what app you want to use. | Nuestras aplicaciones no tienen anuncios de ninguna clase. Si los ve durante la reproducción de vídeo, entonces debe estar usando el reproductor de vídeo de otra aplicación. Pruebe encontrando su reproductor de vídeo por defecto en los ajustes del dispositivo, luego realice un \"Borrar predeterminados\" en él. La siguiente vez que vaya a ver un vídeo verá un selector de aplicaciones, en el cual puede seleccionar qué aplicación quiere usar para reproducirlo. |
| 262 | `create_virtual_album` | Create virtual album | Crear álbum virtual |
| 263 | `new_virtual_album` | New album… | Nuevo álbum… |
| 264 | `rename_virtual_album` | Rename album | Renombrar álbum |
| 265 | `album_name` | Album name | Nombre del álbum |
| 266 | `virtual_folder_name` | Virtual folder name | Nombre de la carpeta virtual |
| 267 | `virtual_album_name_empty` | The album name cannot be empty | El nombre del álbum no puede estar vacío |
| 268 | `virtual_album_exists` | An album with this name already exists | Ya existe un álbum con este nombre |
| 269 | `add_to_virtual_album` | Add to album | Añadir al álbum |
| 270 | `remove_from_virtual_album` | Remove from album | Quitar del álbum |
| 271 | `added_to_virtual_album` | Added to \"%s\" | Añadido a \"%s\" |
| 272 | `already_in_virtual_album` | Already in \"%s\" | Ya está en \"%s\" |
| 273 | `removed_from_virtual_album` | Removed from the album | Quitado del álbum |
| 274 | `delete_virtual_album_confirmation` | Delete the album %s? Only the album is removed, your photos and videos stay where they are. | ¿Eliminar el álbum %s? Solo se eliminará el álbum; tus fotos y vídeos permanecerán donde están. |
| 275 | `virtual_album_empty` | This album is empty. Select items in any folder and use \"Copy to\". | Este álbum está vacío. Selecciona elementos en cualquier carpeta y usa \"Copiar a\". |
| 276 | `virtual_album_cannot_be_modified` | This action is not available for virtual albums | Esta acción no está disponible para álbumes virtuales |
| 277 | `alphanumeric_password` | Password | Contraseña |
| 278 | `enter_password` | Enter password | Introduzca la contraseña |
| 279 | `repeat_password` | Repeat password | Repetir contraseña |
| 280 | `wrong_password` | Wrong password | Contraseña incorrecta |
| 281 | `encrypting_files` | Encrypting files… | Cifrando archivos… |
| 282 | `encrypting_folder` | Encrypting folder… | Cifrando carpeta… |
| 283 | `folder_encrypted_successfully` | Folder encrypted successfully | Carpeta cifrada correctamente |
| 284 | `file_encrypted_successfully` | File encrypted successfully | Archivo cifrado correctamente  |
| 285 | `files_encrypted_successfully` | Files encrypted successfully | Archivos cifrados correctamente  |
| 286 | `decrypting_folder` | Decrypting folder… | Descifrando carpeta… |
| 287 | `folder_decrypted_successfully` | Folder decrypted successfully | Carpeta descifrada correctamente |
| 288 | `file_decrypted_successfully` | File decrypted successfully | Archivo descifrado correctamente |
| 289 | `files_decrypted_successfully` | Files decrypted successfully | Archivos descifrados correctamente |
| 290 | `decrypting_files` | Decrypting files… | Descifrando archivos… |
| 291 | `place_finger_prompt` | Please place your finger on the fingerprint sensor | Por favor, ponga el dedo en el sensor de huellas |
| 292 | `connect_at_least_4_dots_prompt` | Connect at least 4 dots | Conecta al menos 4 puntos |
| 293 | `access_existing_cipher_secrets_prompt` | Do you want to access your existing Cipher_Secrets folder? | ¿Desea acceder a su carpeta Cipher_Secrets existente? |
| 294 | `delete_and_recreate_cipher_secrets_prompt` | Existing Cipher_Secrets folder will be deleted and a new one will be created. Are you sure? | Se eliminará la carpeta Cipher_Secrets existente y se creará una nueva. ¿Está seguro? |
| 295 | `confirm_checkbox_label` | I confirm | Confirmo |
| 296 | `secret_vault` | Encrypted Vault | Bóveda cifrada |
| 297 | `secret_vault_empty` | Encrypted Vault is empty | La bóveda cifrada está vacía |
| 298 | `confirm_decrypt_hidden_file` | Are you sure you want to decrypt the hidden file(s)? | ¿Está seguro de que desea descifrar los archivos ocultos? |
| 299 | `copy_to_clipboard_disabled_in_vault` | Copying to clipboard is disabled while vault is unlocked for security. | Por seguridad, copiar al portapapeles está deshabilitado mientras la bóveda está desbloqueada. |
| 300 | `on_vault_exit` | On leaving encrypted section | Al salir de la sección cifrada |
| 301 | `on_vault_exit_keep_showing` | Keep showing encrypted albums | Seguir mostrando álbumes cifrados |
| 302 | `on_vault_exit_hide` | Hide encrypted albums | Ocultar álbumes cifrados |
| 303 | `on_vault_exit_ask_every_time` | Always ask | Preguntar siempre |
| 304 | `vault_exit_dialog_title` | Exiting Encrypted Section | Saliendo de la sección cifrada |
| 305 | `vault_exit_question_single` | Show encrypted album together with other albums? | ¿Mostrar el álbum cifrado junto con otros álbumes? |
| 306 | `vault_exit_question_multiple` | Show encrypted albums together with other albums? | ¿Mostrar los álbumes cifrados junto con otros álbumes? |
| 307 | `vault_exit_settings_hint` | For more options, see Settings &gt; On leaving encrypted section | Para más opciones, consulta Ajustes &gt; Al salir de la sección cifrada |
| 308 | `stop_showing_hidden` | Stop showing encrypted media | Dejar de mostrar medios cifrados |
| 309 | `system_default` | System default | Sistema por defecto |
| 310 | `language` | Language | Idioma |
| 311 | `open_system_language_settings` | Open system language settings | Abrir ajustes de idioma del sistema |
| 312 | `access_hidden_items_with_biometrics` | Access hidden items with biometrics | Acceder a elementos ocultos con biometría |
| 313 | `enable_hidden_protection_first_prompt` | Please enable hidden item password protection first | Por favor, active primero la protección con contraseña para elementos ocultos |
| 314 | `biometric_unlock_vault` | Unlock encrypted vault | Desbloquear bóveda cifrada |
| 315 | `biometric_unlock_prompt` | Verify your biometric identity to access hidden items | Verifique su identidad biométrica para acceder a los elementos ocultos |
| 316 | `biometric_setup_prompt` | Confirm your biometric identity to enable biometric vault access | Confirme su identidad biométrica para activar el acceso biométrico a la bóveda |
| 317 | `use_password_or_pin` | Use PIN / Password | Usar PIN / Contraseña |
| 318 | `biometric_key_invalidated` | Biometric credentials have changed. Please enter your PIN or password. | Las credenciales biométricas han cambiado. Por favor, introduzca su PIN o contraseña. |
| 319 | `no_biometric_hardware_or_enrolled` | No biometric hardware or enrolled credentials found on this device | No se ha encontrado hardware biométrico ni credenciales registradas en este dispositivo |
| 320 | `default_hidden_album` | Default Hidden Album | Álbum oculto predeterminado |
| 321 | `select_hidden_album` | Select Encrypted Album | Seleccionar álbum cifrado |
| 322 | `create_new_hidden_album` | Create New Encrypted Album | Crear nuevo álbum cifrado |
| 323 | `new_hidden_album` | New encrypted album | Nuevo álbum cifrado |
| 324 | `hidden_album_name_empty` | Album name cannot be empty | El nombre del álbum no puede estar vacío |
| 325 | `file_hidden_to_album` | File encrypted to \"%s\" | Archivo cifrado en \"%s\"  |
| 326 | `files_hidden_to_album` | %1$d files encrypted to \"%2$s\" | %1$d archivos cifrados en \"%2$s\"  |
| 327 | `restore_confirmation` (`one`) | Are you sure you want to restore %d file? | ¿Confirma que quiere restaurar %d archivo? |
| 328 | `restore_confirmation` (`many`) | *(CLDR `many` — falls back to English `other`)* | ¿Confirma que quiere restaurar %d archivos? |
| 329 | `restore_confirmation` (`other`) | Are you sure you want to restore %d files? | ¿Confirma que quiere restaurar %d archivos? |
| 330 | `failed_to_resize_images` (`one`) | Failed to resize %d image | Error al cambiar el tamaño de %d imagen |
| 331 | `failed_to_resize_images` (`many`) | *(CLDR `many` — falls back to English `other`)* | Error al cambiar el tamaño de %d imágenes |
| 332 | `failed_to_resize_images` (`other`) | Failed to resize %d images | Error al cambiar el tamaño de %d imágenes |
| 333 | `search` *(fossify-commons)* | *(inherited from commons)* | Buscar |
| 334 | `change_date_and_time_format` *(fossify-commons)* | Change date and time format | Cambiar formato de fecha y hora |
| 335 | `password_protect_file_deletion` *(fossify-commons)* | Password protect file deletion and moving | Proteger con contraseña eliminar y mover archivos |
| 336 | `keep_last_modified` *(fossify-commons)* | Keep old last-modified value at file operations | Mantener el último valor modificado por última vez en las operaciones de archivo |
| 337 | `clear_cache` *(fossify-commons)* | Clear cache | Limpiar cache |
| 338 | `export_settings` *(fossify-commons)* | Export settings | Exportar ajustes |
| 339 | `import_settings` *(fossify-commons)* | Import settings | Importar ajustes |
| 340 | `temporarily_show_excluded` *(fossify-commons)* | Temporarily show excluded | Mostrar excluidos temporalmente |
| 341 | `change_view_type` *(fossify-commons)* | Change view type | Cambiar tipo de vista |
| 342 | `contributors` *(fossify-commons)* | Contributors | Contribuyentes |
| 343 | `known_issues` *(fossify-commons)* | Known issues | Problemas conocidos |
| 344 | `donate_to_fossify` *(fossify-commons)* | Donate to Fossify | Apoya a Fossify |
| 345 | `privacy_policy` *(fossify-commons)* | Privacy policy | Política de privacidad |
| 346 | `create_shortcut` *(fossify-commons)* | Create shortcut | Crear atajo |
| 347 | `print` *(fossify-commons)* | Print | Imprimir |
| 348 | `search_files` *(fossify-commons)* | Search files | Buscar archivos |
| 349 | `search_folders` *(fossify-commons)* | Search folders | Buscar carpetas |
| 350 | `pull_down_to_open_vault` | Keep pulling down for Encrypted Vault | Siga deslizando hacia abajo para la bóveda |
| 351 | `keep_holding_to_open_vault` | Keep holding to open Encrypted Vault | Siga manteniendo pulsado para abrir la bóveda cifrada |
| 352 | `release_to_open_vault` | Release to open Encrypted Vault | Suelte para abrir la bóveda cifrada |
| 353 | `number_of_items` *(fossify-commons)* | Number of items | Número de elementos |
| 354 | `random` *(fossify-commons)* | Random | Aleatorio |
| 355 | `shuffle` | Shuffle | Mezclar |
| 356 | `stop_showing_excluded` *(fossify-commons)* | Stop showing excluded | Dejar de mostrar los excluidos |
| 357 | `copy_to_clipboard` *(fossify-commons)* | Copy to clipboard | Copiar al portapapeles |
| 358 | `encrypt` | Encrypt | Cifrar |
| 359 | `decrypt` | Decrypt | Descifrar |
| 360 | `encrypt_album` | Encrypt album | Cifrar el álbum |
| 361 | `decrypt_album` | Decrypt album | Descifrar el álbum |
