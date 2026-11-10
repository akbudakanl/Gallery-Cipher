# Translation Blind Tests

This directory stores translation audit and blind-review comparison reports for **Fossify Gallery Cipher**. Each target language has its own dedicated subfolder so that string audits, terminology notes, and 1-to-1 English comparison tables remain cleanly organized and easy to maintain.

> [!NOTE]
> **Notes — Translation Requests & Feedback:**  
> If you notice any inaccurate, unnatural, or missing translations—or would like to request full audit/support for a new language—please submit a report, complaint, or Pull Request on the project's [GitHub Issues](https://github.com/akbudakanl/Gallery-Cipher/issues) page.

## Directory Structure

```text
translation-blind-tests/
├── README.md
├── az/
│   └── az_translation_comparison.md
├── de/
│   └── de_translation_comparison.md
├── es/
│   ├── b+es+419_translation_comparison.md
│   ├── es_translation_comparison.md
│   └── es-rUS_translation_comparison.md
├── ja/
│   └── ja_translation_comparison.md
├── no/
│   ├── nb-rNO_translation_comparison.md
│   └── nn_translation_comparison.md
└── zh-rTW/
    └── zh-rTW_translation_comparison.md
```

### Language Subfolders

| Subfolder | Android Resource Dir | Language | Audit File |
|---|---|---|---|
| `az/` | `app/src/main/res/values-az/` | Azerbaijani | [`az_translation_comparison.md`](az/az_translation_comparison.md) |
| `de/` | `app/src/main/res/values-de/` | German | [`de_translation_comparison.md`](de/de_translation_comparison.md) |
| `es/` | `app/src/main/res/values-es/` | Spanish (Standard) | [`es_translation_comparison.md`](es/es_translation_comparison.md) |
| `es/` | `app/src/main/res/values-b+es+419/` | Spanish (Latin America) | [`b+es+419_translation_comparison.md`](es/b+es+419_translation_comparison.md) |
| `es/` | `app/src/main/res/values-es-rUS/` | Spanish (United States) | [`es-rUS_translation_comparison.md`](es/es-rUS_translation_comparison.md) |
| `ja/` | `app/src/main/res/values-ja/` | Japanese | [`ja_translation_comparison.md`](ja/ja_translation_comparison.md) |
| `no/` | `app/src/main/res/values-nb-rNO/` | Norwegian Bokmål | [`nb-rNO_translation_comparison.md`](no/nb-rNO_translation_comparison.md) |
| `no/` | `app/src/main/res/values-nn/` | Norwegian Nynorsk | [`nn_translation_comparison.md`](no/nn_translation_comparison.md) |
| `zh-rTW/` | `app/src/main/res/values-zh-rTW/` | Traditional Chinese (Taiwan) | [`zh-rTW_translation_comparison.md`](zh-rTW/zh-rTW_translation_comparison.md) |

## Purpose & Methodology

1. **Dual-Source Completeness Audit (App + `fossify-commons`):** Every target `values-<lang>/strings.xml` file must be checked against **both** string sources:
   - **App-owned resources:** `app/src/main/res/values/strings.xml` (`326 <string>` and `2 <plurals>` entries = `328` resource entries).
   - **Inherited library resources (`org.fossify:commons`):** Strings used in Settings, Options menus, About (`AboutActivity`), contextual action bars (CAB), and shared dialogs (such as `change_date_and_time_format`, `password_protect_file_deletion`, `keep_last_modified`, `clear_cache`, `export_settings`, `import_settings`, `temporarily_show_excluded`, `change_view_type`, `contributors`, `known_issues`, `donate_to_fossify`, `privacy_policy`, `create_shortcut`, `print`, and `search`) live in the `org.fossify:commons` AAR dependency (`res/values/values.xml` and `res/values-<lang>/values-<lang>.xml`) rather than `app/src/main/res/values/strings.xml`. Because several locales in `org.fossify:commons` are incomplete, any missing `fossify-commons` keys should be overridden locally in `app/src/main/res/values-<lang>/strings.xml`.
2. **Blind Translation & Consistency Review:** Missing or inaccurate strings are translated independently from the English source while matching existing Fossify Gallery terminology, UI tone, placeholders (`%s`, `%d`), HTML/escape sequences (`&gt;`, `\'`, `\"`, `\n`), and plural rules (CLDR categories per language).
3. **1-to-1 Comparison Report:** Each `<lang>_translation_comparison.md` file documents:
   - Scope and key terminology glossary.
   - Section 1: Summary of newly added or revised translations with contextual notes.
   - Section 2: Full 1-to-1 comparison table pairing every English source entry (plus any overridden `fossify-commons` keys) with its localized counterpart.

## Notes on Shared & Inherited Resources

- **Why `fossify-commons` must be checked alongside `values/strings.xml`:** Checking only `app/src/main/res/values/strings.xml` can leave visible UI labels in English if `org.fossify:commons`'s own `res/values-<lang>/values-<lang>.xml` is missing those keys. Adding local `<string>` overrides in `app/src/main/res/values-<lang>/strings.xml` takes precedence during Android resource merging without duplicating the entire `fossify-commons` resource table into `app/src/main/res/values/strings.xml`.
- **Plural forms (`<plurals>`):** English defines `one` and `other` quantities for `restore_confirmation` and `failed_to_resize_images`. Languages with different CLDR plural rules (for example, Japanese using only `other`) map accordingly in XML while retaining full row-by-row parity in the comparison table.

## Adding a New Language Audit

1. Create a subfolder named after the Android locale code (e.g., `fr/`, `es/`, `zh-rCN/`).
2. Audit `app/src/main/res/values-<lang>/strings.xml` against **both** `app/src/main/res/values/strings.xml` **and** `org.fossify:commons`'s `res/values/values.xml` / `res/values-<lang>/values-<lang>.xml`, and add any missing entries or local `fossify-commons` overrides to `app/src/main/res/values-<lang>/strings.xml`.
3. Add `<lang>_translation_comparison.md` inside `translation-blind-tests/<lang>/` with English descriptions, a summary of added/updated keys, and the full English-vs-target comparison table.
4. Register the new subfolder in the table above.
