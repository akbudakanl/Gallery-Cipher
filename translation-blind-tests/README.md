# Translation Blind Tests

This directory stores translation audit and blind-review comparison reports for **Fossify Gallery Cipher**. Each target language has its own dedicated subfolder so that string audits, terminology notes, and 1-to-1 English comparison tables remain cleanly organized and easy to maintain.

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

1. **Completeness Audit:** Every target `values-<lang>/strings.xml` file is checked against the canonical English source file (`app/src/main/res/values/strings.xml`, which currently contains `307 <string>` and `2 <plurals>` entries = `309` resource entries).
2. **Blind Translation & Consistency Review:** Missing or inaccurate strings are translated independently from the English source while matching existing Fossify Gallery terminology, UI tone, placeholders (`%s`, `%d`), HTML/escape sequences (`&gt;`, `\'`, `\"`, `\n`), and plural rules (CLDR categories per language).
3. **1-to-1 Comparison Report:** Each `<lang>_translation_comparison.md` file documents:
   - Scope and key terminology glossary.
   - Section 1: Summary of newly added or revised translations with contextual notes.
   - Section 2: Full 1-to-1 comparison table pairing every English source entry with its localized counterpart.

## Notes on Shared & Inherited Resources

- **`fossify-commons` inherited keys (e.g., `search`):** Some keys referenced by layouts or shared UI components (such as `@string/search` in `app/src/main/res/layout/dialog_select_language.xml`) are not defined in `app/src/main/res/values/strings.xml` because the default English value is inherited from the `org.fossify.commons` dependency. Certain locale files (such as `values-az/strings.xml`) define a local override (e.g., `<string name="search">Axtar</string>`) to ensure the localized term appears consistently.
- **Plural forms (`<plurals>`):** English defines `one` and `other` quantities for `restore_confirmation` and `failed_to_resize_images`. Languages with different CLDR plural rules (for example, Japanese using only `other`) map accordingly in XML while retaining full row-by-row parity in the comparison table.

## Adding a New Language Audit

1. Create a subfolder named after the Android locale code (e.g., `fr/`, `es/`, `zh-rCN/`).
2. Audit `app/src/main/res/values-<lang>/strings.xml` against `app/src/main/res/values/strings.xml` and complete any missing entries.
3. Add `<lang>_translation_comparison.md` inside `translation-blind-tests/<lang>/` with English descriptions, a summary of added/updated keys, and the full English-vs-target comparison table.
4. Register the new subfolder in the table above.
