package com.termux.app.terminal.keyboard.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class KeyboardConfigRepositoryTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final List<KeyboardConfigRepository> repositories = new ArrayList<>();

    @After
    public void closeRepositories() {
        for (KeyboardConfigRepository repository : repositories) repository.close();
    }

    @Test
    public void primaryTakesPrecedenceAndSecondaryLoadsWhenPrimaryIsAbsent() throws Exception {
        File primary = file("data/keyboard.json");
        File secondary = file("config/keyboard.json");
        write(primary, bundled());
        write(secondary, "{}");

        RecordingListener listener = new RecordingListener();
        repository(primary, secondary).loadInitial(listener);
        ShadowLooper.idleMainLooper();

        assertEquals(1, listener.calls);
        assertTrue(listener.applied);
        assertEquals(primary.getAbsolutePath(), listener.snapshot.source);
        assertTrue(listener.diagnostics.isEmpty());

        assertTrue(primary.delete());
        write(secondary, bundled());
        listener = new RecordingListener();
        repository(primary, secondary).loadInitial(listener);
        ShadowLooper.idleMainLooper();

        assertEquals(1, listener.calls);
        assertEquals(secondary.getAbsolutePath(), listener.snapshot.source);
    }

    @Test
    public void invalidCustomConfigFallsBackToBundledConfigOnColdStart() throws Exception {
        File primary = file("data/keyboard.json");
        write(primary, "{}");
        RecordingListener listener = new RecordingListener();

        KeyboardConfigRepository repository = repository(primary, file("config/keyboard.json"));
        repository.loadInitial(listener);
        ShadowLooper.idleMainLooper();

        assertTrue(listener.applied);
        assertNotNull(repository.current());
        assertEquals("asset:///" + KeyboardConfigRepository.ASSET, listener.snapshot.source);
        assertFalse(listener.diagnostics.isEmpty());
        assertEquals(primary.getAbsolutePath(), listener.diagnostics.get(0).file);
    }

    @Test
    public void invalidReloadKeepsLastValidSnapshotAndReportsThatItWasNotApplied()
            throws Exception {
        File primary = file("data/keyboard.json");
        write(primary, bundled());
        KeyboardConfigRepository repository = repository(primary, file("config/keyboard.json"));
        RecordingListener listener = new RecordingListener();
        repository.loadInitial(listener);
        ShadowLooper.idleMainLooper();
        KeyboardConfigSnapshot initial = repository.current();

        write(primary, "{}");
        listener = new RecordingListener();
        repository.reload(listener);
        ShadowLooper.idleMainLooper();

        assertEquals(1, listener.calls);
        assertFalse(listener.applied);
        assertSame(initial, listener.snapshot);
        assertSame(initial, repository.current());
        assertFalse(listener.diagnostics.isEmpty());
    }

    @Test
    public void closePreventsQueuedPublicationAndMakesLateReloadANoOp() throws Exception {
        File primary = file("data/keyboard.json");
        write(primary, bundled());
        KeyboardConfigRepository repository = repository(primary, file("config/keyboard.json"));
        RecordingListener listener = new RecordingListener();

        repository.loadInitial(listener);
        repository.close();
        repository.close();
        repository.reload(listener);
        ShadowLooper.idleMainLooper();

        assertEquals(0, listener.calls);
        assertNull(repository.current());
    }

    private KeyboardConfigRepository repository(File primary, File secondary) {
        Context context = RuntimeEnvironment.getApplication();
        KeyboardConfigRepository repository =
                new KeyboardConfigRepository(
                        context,
                        primary,
                        secondary,
                        new DirectExecutorService(),
                        new Handler(Looper.getMainLooper()));
        repositories.add(repository);
        return repository;
    }

    private File file(String path) throws Exception {
        File file = new File(temporaryFolder.getRoot(), path);
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
        return file;
    }

    private static String bundled() throws Exception {
        return new String(
                Files.readAllBytes(
                        new File("src/main/assets/keyboard/default-keyboard.json").toPath()),
                StandardCharsets.UTF_8);
    }

    private static void write(File file, String content) throws Exception {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static final class RecordingListener implements KeyboardConfigRepository.Listener {
        int calls;
        boolean applied;
        KeyboardConfigSnapshot snapshot;
        List<KeyboardDiagnostic> diagnostics = Collections.emptyList();

        @Override
        public void onReload(
                KeyboardConfigSnapshot snapshot,
                List<KeyboardDiagnostic> diagnostics,
                boolean applied) {
            calls++;
            this.snapshot = snapshot;
            this.diagnostics = diagnostics;
            this.applied = applied;
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            if (shutdown) throw new RejectedExecutionException("executor is shut down");
            command.run();
        }
    }
}
