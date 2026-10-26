package com.termux.app.terminal.keyboard.config;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonEncodingException;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Strict Moshi decoder plus semantic validation. */
public final class KeyboardConfigParser {
    private final JsonAdapter<KeyboardDocument> adapter =
            new Moshi.Builder().build().adapter(KeyboardDocument.class).failOnUnknown().nonNull();

    public Result parse(String json, String source) {
        try {
            KeyboardDocument document = adapter.fromJson(json);
            List<KeyboardDiagnostic> diagnostics =
                    new KeyboardConfigValidator(source).validate(document);
            return diagnostics.isEmpty()
                    ? Result.success(new KeyboardConfigSnapshot(document, source))
                    : Result.failure(diagnostics);
        } catch (JsonDataException | JsonEncodingException e) {
            return Result.failure(
                    Collections.singletonList(
                            new KeyboardDiagnostic(
                                    source,
                                    jsonPath(e.getMessage()),
                                    "invalid or unknown JSON field",
                                    safeMessage(e))));
        } catch (IOException | RuntimeException e) {
            return Result.failure(
                    Collections.singletonList(
                            new KeyboardDiagnostic(
                                    source, "$", "cannot parse configuration", safeMessage(e))));
        }
    }

    private static String jsonPath(String message) {
        if (message == null) return "$";
        int at = message.lastIndexOf(" at path ");
        return at < 0 ? "$" : message.substring(at + 9);
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    public static final class Result {
        public final KeyboardConfigSnapshot snapshot;
        public final List<KeyboardDiagnostic> diagnostics;

        private Result(KeyboardConfigSnapshot snapshot, List<KeyboardDiagnostic> diagnostics) {
            this.snapshot = snapshot;
            this.diagnostics = diagnostics;
        }

        public boolean isSuccess() {
            return snapshot != null;
        }

        static Result success(KeyboardConfigSnapshot s) {
            return new Result(s, Collections.emptyList());
        }

        static Result failure(List<KeyboardDiagnostic> d) {
            return new Result(null, Collections.unmodifiableList(d));
        }

        static Result fallback(KeyboardConfigSnapshot s, List<KeyboardDiagnostic> d) {
            return new Result(s, Collections.unmodifiableList(d));
        }
    }
}
