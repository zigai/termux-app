package com.termux.app.terminal.keyboard.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
public class TerminalViewKeyboardSinkSpeechTest {
    @Test
    public void dictatedTextIsSanitizedAndBypassesModifiersAtTerminalBoundary() throws Exception {
        TerminalView view = new TerminalView(RuntimeEnvironment.getApplication(), null);
        TerminalSession session = session();
        enableOutputQueue(session);
        int[] modifierCallbacks = {0};
        view.setTerminalViewClient(alwaysModifiedClient(modifierCallbacks));
        view.attachSession(session);
        TerminalViewKeyboardSink sink = new TerminalViewKeyboardSink(view, callbacks());

        sink.insertLiteralText("a\nb\u0000");

        assertEquals("a b", readOutputQueue(session));
        assertEquals(0, modifierCallbacks[0]);
    }

    @Test
    public void terminalKeysAreIgnoredUntilTheTerminalIsReady() {
        TerminalView view = new TerminalView(RuntimeEnvironment.getApplication(), null);
        TerminalViewKeyboardSink sink = new TerminalViewKeyboardSink(view, callbacks());

        assertNull(view.getCurrentSession());
        sink.terminalKey("ENTER", false, false, false);

        view.attachSession(session());
        assertNull(view.getCurrentSession().getEmulator());
        sink.terminalKey("ENTER", false, false, false);
    }

    private static TerminalViewKeyboardSink.AppCallbacks callbacks() {
        return new TerminalViewKeyboardSink.AppCallbacks() {
            public void appAction(String action) {}

            public void profileAction(String profile) {}
        };
    }

    private static TerminalSession session() {
        return new TerminalSession("", "", new String[0], new String[0], 100, null);
    }

    private static void enableOutputQueue(TerminalSession session) throws Exception {
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(session, 1);
    }

    private static String readOutputQueue(TerminalSession session) throws Exception {
        Field field = TerminalSession.class.getDeclaredField("mTerminalToProcessIOQueue");
        field.setAccessible(true);
        Object queue = field.get(session);
        Method read = queue.getClass().getDeclaredMethod("read", byte[].class, boolean.class);
        read.setAccessible(true);
        byte[] bytes = new byte[32];
        int count = (int) read.invoke(queue, bytes, false);
        return new String(bytes, 0, count, StandardCharsets.UTF_8);
    }

    private static TerminalViewClient alwaysModifiedClient(int[] callbacks) {
        return (TerminalViewClient)
                Proxy.newProxyInstance(
                        TerminalViewClient.class.getClassLoader(),
                        new Class<?>[] {TerminalViewClient.class},
                        (proxy, method, args) -> {
                            if (method.getName().startsWith("read")
                                    || "onCodePoint".equals(method.getName())) {
                                callbacks[0]++;
                            }
                            if (method.getReturnType() == boolean.class) return true;
                            if (method.getReturnType() == float.class) return 1f;
                            return null;
                        });
    }
}
