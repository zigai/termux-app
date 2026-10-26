package com.termux.app.terminal.keyboard.config;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.termux.shared.termux.TermuxConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Resolves, validates off-thread, and atomically publishes configuration snapshots. */
public final class KeyboardConfigRepository {
    public static final String ASSET = "keyboard/default-keyboard.json";
    private final Context context;
    private final KeyboardConfigParser parser = new KeyboardConfigParser();
    private final File primary;
    private final File secondary;
    private final ExecutorService executor;
    private final Handler main;
    private volatile KeyboardConfigSnapshot current;
    private volatile List<KeyboardDiagnostic> diagnostics = Collections.emptyList();
    private volatile boolean closed;

    public KeyboardConfigRepository(Context context) {
        this(
                context,
                new File(TermuxConstants.TERMUX_DATA_HOME_DIR_PATH, "keyboard.json"),
                new File(TermuxConstants.TERMUX_CONFIG_HOME_DIR_PATH, "keyboard.json"),
                Executors.newSingleThreadExecutor(),
                new Handler(Looper.getMainLooper()));
    }

    KeyboardConfigRepository(
            Context context,
            File primary,
            File secondary,
            ExecutorService executor,
            Handler main) {
        this.context = context.getApplicationContext();
        this.primary = primary;
        this.secondary = secondary;
        this.executor = executor;
        this.main = main;
    }

    public KeyboardConfigSnapshot current() {
        return current;
    }

    public List<KeyboardDiagnostic> diagnostics() {
        return diagnostics;
    }

    public void loadInitial(Listener listener) {
        reload(true, listener);
    }

    public void reload(Listener listener) {
        reload(false, listener);
    }

    private void reload(boolean coldStart, Listener listener) {
        if (closed) return;
        try {
            executor.execute(
                    () -> {
                        Source source = resolve();
                        KeyboardConfigParser.Result result;
                        try {
                            result = parser.parse(read(source.stream), source.name);
                        } catch (IOException e) {
                            result = parser.parse("", source.name);
                        }
                        if (!result.isSuccess()
                                && (coldStart || current == null)
                                && !source.bundled) {
                            result = bundledFallback(result.diagnostics);
                        }
                        if (closed) return;
                        KeyboardConfigParser.Result published = result;
                        main.post(
                                () -> {
                                    if (closed) return;
                                    diagnostics = published.diagnostics;
                                    if (published.isSuccess()) current = published.snapshot;
                                    if (listener != null) {
                                        listener.onReload(
                                                current,
                                                diagnostics,
                                                published.isSuccess());
                                    }
                                });
                    });
        } catch (RejectedExecutionException e) {
            // A close raced with this reload request; no result should be published.
        }
    }

    private KeyboardConfigParser.Result bundledFallback(
            List<KeyboardDiagnostic> originalDiagnostics) {
        try (InputStream in = context.getAssets().open(ASSET)) {
            KeyboardConfigParser.Result fallback =
                    parser.parse(read(in), "asset:///" + ASSET);
            if (fallback.isSuccess()) {
                return KeyboardConfigParser.Result.fallback(
                        fallback.snapshot, originalDiagnostics);
            }
        } catch (IOException e) {
            // Retain the custom configuration diagnostics when the bundled asset cannot load.
        }
        return KeyboardConfigParser.Result.failure(originalDiagnostics);
    }

    private Source resolve() {
        try {
            if (primary.isFile())
                return new Source(primary.getAbsolutePath(), new FileInputStream(primary), false);
            if (secondary.isFile())
                return new Source(
                        secondary.getAbsolutePath(), new FileInputStream(secondary), false);
            return new Source("asset:///" + ASSET, context.getAssets().open(ASSET), true);
        } catch (IOException e) {
            return new Source(
                    primary.getAbsolutePath(),
                    new java.io.ByteArrayInputStream(new byte[0]),
                    false);
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream input = in;
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
    }

    public interface Listener {
        void onReload(
                KeyboardConfigSnapshot snapshot,
                List<KeyboardDiagnostic> diagnostics,
                boolean applied);
    }

    private static final class Source {
        final String name;
        final InputStream stream;
        final boolean bundled;

        Source(String n, InputStream s, boolean b) {
            name = n;
            stream = s;
            bundled = b;
        }
    }
}
