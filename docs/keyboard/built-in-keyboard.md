# Built-in terminal keyboard

The built-in keyboard is the default terminal input provider. It is rendered inside the terminal activity and never acts as an Android input method. Settings, rename dialogs, and other ordinary text fields continue to use the Android system keyboard.

Choose **Settings → Termux → Terminal I/O → Terminal input provider → Android system keyboard** to restore Termux's existing soft-keyboard and extra-keys toolbar behavior. The drawer's **Keyboard** action always shows or hides the active terminal keyboard. Built-in visibility is remembered.

## Configuration

The first existing file wins:

1. `~/.termux/keyboard.json`
2. `~/.config/termux/keyboard.json`
3. the bundled adaptive keyboard

The app never rewrites a user document. The format is versioned and strict: unknown fields, unsupported actions, bad references, duplicate identifiers, overlapping responsive ranges, macro cycles, non-finite geometry, and repeat/long-press ambiguity are errors. The authored Draft 2020-12 schema is [termux-keyboard.schema.json](termux-keyboard.schema.json) and is also packaged in the APK at `assets/keyboard/termux-keyboard.schema.json`.

Reload with `termux-reload-settings` or **Reload built-in keyboard** in Terminal I/O settings. Parsing and validation happen off the UI thread. A valid result is swapped atomically; an invalid reload leaves the working keyboard active. The settings diagnostic includes the source file, JSON path, rule, and a bounded offending value. An invalid cold-start file falls back to the bundled keyboard while retaining its diagnostic.

Profiles contain themes, reusable styles, finite macros, named layers, and non-overlapping width variants. Key actions are closed and discriminated: `text`, `terminalKey`, `modifier`, `layer`, `macro`, `app`, and `profile`; `delay` is allowed only as a bounded macro step. Allowlisted app actions are `paste`, `drawer`, `hideKeyboard`, and `speechInput`.

Appearance is deliberately split into three levels so the full keyboard and the narrow landscape rails do not inherit geometry that only works for the other form:

- `profile.theme` contains shared colors, interaction feedback, repeat timing, and default typography.
- `variant.appearance` contains full-width layout geometry and optional visual overrides.
- `variant.landscapeSplit.appearance` contains independent rail geometry and optional visual overrides.

Both appearance blocks support `horizontalGap`, `verticalGap`, `horizontalPadding`, `verticalPadding`, `cornerRadius`, `textSize`, `iconSize`, `secondaryTextScale`, and `verticalAlignment`. Rows may override `horizontalGap`, `verticalGap`, `height`, `offset`, and `endOffset`. Shared and per-layout values use the normal theme cascade, so the split layout can be compact without forcing the portrait keyboard to use the same spacing, radius, font size, or padding.

`fontFamily` is configurable in the shared theme, either appearance block, a reusable style, or an individual key. Allowed values are `system`, `sans`, `sansMedium`, `monospace`, and `terminal`; `terminal` loads the same `~/.termux/font.ttf` file used by the terminal and otherwise falls back to monospace. `fontWeight` accepts `normal`, `medium`, or `bold`. Primary legends automatically shrink to their available key bounds rather than overflowing.

Icon-only keys use a closed, bundled vector set through the `icon` field: `shift`, `backspace`, `enter`, `microphone`, `tab`, `space`, and the four `arrow*` values. Icon-only keys require `accessibilityLabel`; arbitrary resource names and file paths are not accepted. The bundled layout uses icons for phone-standard controls while retaining short text for terminal modifiers such as Ctrl, Alt, and Esc.

Ctrl, Shift, Alt, and layer keys (including bundled AltGr) can be held for a multi-touch chord, tapped for one output, or tapped again to cancel. Controls without alternatives can be long-pressed to lock. One-shot state is consumed only by terminal output. Accessibility click activation also honors the configured `momentary`, `once`, `locked`, and `smart` modes; Smart cycles armed, locked, and inactive so TalkBack and D-pad users do not need a touch long-press. Session, profile, configuration, or provider changes clear all modifier state, pointers, popups, previews, repeat timers, and macros.

The bundled Space key enables `cursorGesture`. Hold Space until cursor-control feedback, then drag horizontally: each bounded movement step sends the terminal's Left or Right key sequence. Releasing ends cursor control without inserting a space; a normal tap still inserts one. Custom keys may opt into the gesture with `"cursorGesture": true`, which is mutually exclusive with repeat and long-press alternatives.

On compact phones in landscape orientation, the built-in keyboard becomes two narrow side rails and the terminal is resized into the space between them. Portrait uses the full-width keyboard. The rails preserve the QWERTY left and right letter blocks, form one logical keyboard, share modifier/layer state for cross-screen multi-touch chords, and each contain a Space key (including cursor drag). Each effective rail width is capped at 20% of the screen even when `railMinWidthDp`, an old stored preference, or a custom request would be wider, leaving at least 60% as a continuous full-height terminal area. The bundled default is 18% with a 72dp preferred touch floor, leaving 64% for the terminal. `maxHeightDp` prevents phone rails from replacing the normal adaptive keyboard on tablets. **Landscape keyboard side width** in Terminal I/O settings selects 12% through 20% and overrides the profile default.

A variant configures the rails with `landscapeSplit`, including `railWidthPercent`, `railMinWidthDp`, `maxHeightDp`, `rowHeightDp`, `utilityRowHeightDp`, `appearance`, optional paired `leftUtility`/`rightUtility` headers, and independent `left`/`right` primary rows of up to five key IDs. Utility headers contain one to five IDs and are rendered before the primary rows at their shorter configured height. IDs resolve strictly against that variant's normal rows and may appear only once per rail across the header and primary rows. Android-system-keyboard mode retains its existing layout and extra-keys behavior.

The bundled split layout uses five primary rows. Its first full-size row visibly exposes `1–5` on the left and `6–0` on the right, including their Shift symbols; numbers do not depend on long press. `QWERT`/`YUIOP` and `ASDFG`/`HJKL` retain equal five-unit rows; Shift precedes Z, B starts the right lower row, Backspace and Enter stay on the right edge, and Up sits directly above Down in the inverted-T arrow cluster. A 24dp utility header exposes Esc, Tab, microphone, Home, right-side Ctrl, minus, equals, and slash outside the main grid. Ctrl, terminal Alt, comma, and the wider left Space fill the left bottom row; the right Space, period, and arrows use equal touch units so none are squeezed below the letter-key width. Both Space keys retain cursor drag.

The bundled full keyboard uses six equal-height, edge-to-edge rows in both compact and wide variants, with no invisible spacer cells. Geometry is intentionally row-specific instead of forcing the phone through one desktop-sized column grid. The number row is 11.5 units wide and ends with a 1.5-unit Backspace. The three typing rows use a wider 10-unit grid: `Q–P`, `A–L` plus semicolon, then a 1.5-unit Shift, `Z–M`, and a 1.5-unit Enter.

The fifth row contains Esc, Tab, the most useful shell delimiters, and `Home / Up / End`. The Android-style bottom row starts with adjacent Ctrl and terminal Alt keys, followed by comma, microphone input, a wide Space key, and period. It ends with individual Left, Down, and Right keys; Up sits directly above Down, so the four arrows form a familiar inverted-T cluster rather than a gesture pad. Home and End align above Left and Right. Long-press Alt exposes AltGr, long-press Home and End expose Page Up and Page Down, and Shift+Backspace sends forward Delete. Shift, AltGr, and long-press alternatives retain the remaining shell punctuation without narrowing the letter keys.

## Speech input

The bundled `Mic` key invokes the allowlisted `{"type":"app","app":"speechInput"}` action. It requests microphone permission only when invoked, presents an accessible in-app listening surface with partial transcription, Stop, and Cancel, then inserts only the provider's final result. Dictated line breaks, NUL, DEL, and other terminal control characters are removed or flattened to spaces; speech never synthesizes Enter and never inherits Ctrl, Alt, or Shift state.

Terminal I/O settings provide three native Android modes: **Automatic**, **Android on-device only**, and **Android system service**. Automatic evaluates an ordered registry of provider factories, preferring the API 31+ Android on-device recognizer and then the installed system `RecognitionService`. If an automatic provider reports an unavailable engine or language, the controller advances to the next registered provider. Explicit modes never silently change providers. The optional speech language setting accepts a BCP 47 tag; empty uses the provider default.

Speech transcription is isolated behind `SpeechTranscriptionProvider` and its factory contract. Permission flow, listening UI, result safety, JSON actions, and terminal insertion do not import an engine-specific API. A future native, embedded, or vendor-backed implementation registers another factory and implements start, stop, cancel, destroy, and event callbacks; it does not alter keyboard layouts or action semantics. Android's `SpeechRecognizer` is the first adapter and always runs as a foreground, user-initiated activity feature. It is cancelled on keyboard hiding, input-provider/profile/configuration/session changes, pause, and rotation, and destroyed with the activity. Speech errors remain visible and are recorded in Terminal I/O diagnostics.
