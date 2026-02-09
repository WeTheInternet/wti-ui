package net.wti.ui.test.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

///
/// GdxDesktopTestHarness
///
/// A small **test-only** harness that boots a real **libGDX LWJGL3** application
/// and exposes helpers to execute code on the **libGDX render thread**.
///
/// This is intended for:
/// - Scene2D UI construction and rebuild tests (tables, scroll panes, layout)
/// - Skin / TextureAtlas / font loading tests (requires a real GL context)
/// - Anything that depends on `Gdx.app`, `Gdx.files`, `Gdx.graphics`, etc.
///
/// It is *not* intended for:
/// - Production code
/// - CI without a display server (on Linux CI you usually need Xvfb/Wayland; out of scope here)
///
/// ### Why this exists
/// libGDX has strong thread-affinity expectations:
/// - Many operations are safest (or required) on the **render thread**
/// - `Gdx.app.postRunnable(...)` is the canonical bridge into that thread
///
/// Headless backends + mocks tend to break once UI code touches GL-backed resources.
/// This harness avoids those pitfalls by creating a real LWJGL3 `Application`.
///
/// ### Spock usage (recommended: one harness per Spec)
/// ```groovy
/// import spock.lang.Shared
/// import spock.lang.Specification
///
/// final class MyUiSpec extends Specification {
///
///   @Shared
///   private final GdxDesktopTestHarness gdx = new GdxDesktopTestHarness()
///
///   def setupSpec() {
///     gdx.start()
///   }
///
///   def cleanupSpec() {
///     gdx.stop()
///   }
///
///   def "rebuild does not throw"() {
///     when:
///     gdx.runOnGdxThread {
///       // Build Skin/Stage/View
///       // Trigger refresh/rebuild
///       // stage.act(0f)
///       // stage.root.invalidateHierarchy()
///       // stage.root.layout()
///     }
///
///     then:
///     noExceptionThrown()
///   }
/// }
/// ```
///
/// ### Threading rules
/// - Call `start()` / `stop()` from the test thread (Spock/JUnit thread).
/// - Do *all* Scene2D mutations, Skin loading, and layout within `runOnGdxThread(...)`.
/// - If you need a return value, prefer the `Callable<T>` overload.
///
/// ### Timeouts
/// All cross-thread operations have timeouts:
/// - `start(timeout)` waits for `ApplicationListener#create()` to run.
/// - `runOnGdxThread(..., timeout)` waits for the posted runnable to complete.
/// - Timeouts indicate either a stuck render loop or deadlock.
///
/// ### Windowing / “offscreen” notes
/// The LWJGL3 backend uses GLFW. On Linux this typically requires an X11/Wayland
/// environment even if the window is tiny or unfocused.
/// Locally (desktop dev) this is usually fine; CI often needs Xvfb.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2026-02-08
///
public class GdxDesktopTestHarness {

    private final AtomicReference<Throwable> startupFailure = new AtomicReference<>();
    private volatile Thread appThread;
    private volatile CountDownLatch createdLatch;

    ///
    /// Starts a dedicated LWJGL3 application thread and blocks until `create()` runs.
    ///
    /// After this returns successfully:
    /// - `Gdx.app` is non-null
    /// - the render loop is running (so `postRunnable` can execute)
    ///
    /// Safe to call multiple times; subsequent calls are a no-op.
    ///
    /// Notes for casual libGDX users:
    /// - This creates a real OpenGL context (unlike `HeadlessApplication`).
    /// - `cfg.setIdleFPS(0)` reduces CPU while still pumping the event loop enough
    ///   to execute `postRunnable` callbacks.
    /// - The spawned thread is a daemon thread to avoid hanging a JVM on hard failures.
    ///
    public synchronized void start(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (appThread != null) {
            return;
        }

        createdLatch = new CountDownLatch(1);

        appThread = new Thread(() -> {
            try {
                Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
                cfg.setTitle("gdx-test");
                cfg.setWindowedMode(1, 1);
                cfg.setResizable(false);
                cfg.useVsync(false);
                cfg.setIdleFPS(0); // keep CPU low; still pumps postRunnable() during render loop

                new Lwjgl3Application(new ApplicationAdapter() {
                    @Override
                    public void create() {
                        createdLatch.countDown();
                    }

                    @Override
                    public void render() {
                        // No rendering required; loop exists to execute posted runnables.
                        //
                        // If you later add screenshot tests, you can render here and/or
                        // drive explicit frames from your test thread.
                    }
                }, cfg);

            } catch (Throwable t) {
                startupFailure.set(t);
                createdLatch.countDown();
            }
        }, "gdx-lwjgl3-test-thread");

        appThread.setDaemon(true);
        appThread.start();

        awaitCreated(timeout);

        Throwable failure = startupFailure.get();
        if (failure != null) {
            throw new RuntimeException("Failed to start LWJGL3 test application", failure);
        }
    }

    ///
    /// Convenience start with a default timeout (15 seconds).
    ///
    /// Use the overload with a custom timeout if your laptop is under heavy load
    /// or you expect shaders/assets to compile on startup in the future.
    ///
    public void start() {
        start(Duration.ofSeconds(15));
    }

    ///
    /// Stops the LWJGL3 application and joins the application thread.
    ///
    /// This is best-effort:
    /// - We request shutdown via `Gdx.app.exit()` on the render thread.
    /// - Then we join the app thread up to the provided timeout.
    ///
    /// Casual libGDX note:
    /// - `Gdx.app.exit()` triggers normal libGDX disposal flow.
    /// - If your tests allocate disposable resources (Skin/Stage/Textures),
    ///   prefer disposing them explicitly inside `runOnGdxThread(...)` as part
    ///   of your test/suite teardown.
    ///
    /// Safe to call multiple times; subsequent calls are a no-op.
    ///
    public synchronized void stop(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (appThread == null) {
            return;
        }

        try {
            // Best-effort: exit from render thread.
            runOnGdxThread(() -> {
                if (Gdx.app != null) {
                    Gdx.app.exit();
                }
                return null;
            }, Duration.ofSeconds(5));
        } catch (Throwable ignored) {
            // If the app is already gone or never fully started, ignore.
        }

        try {
            appThread.join(timeout.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            appThread = null;
            createdLatch = null;
            startupFailure.set(null);
        }
    }

    ///
    /// Convenience stop with a default timeout (10 seconds).
    ///
    public void stop() {
        stop(Duration.ofSeconds(10));
    }

    ///
    /// Schedules work on the libGDX render thread and blocks the caller until completion.
    ///
    /// This should wrap:
    /// - any Scene2D mutations (adding/removing actors, calling `refresh()`, `layout()`, etc.)
    /// - any asset work that touches GL-backed resources (Skin loading, TextureAtlas usage, etc.)
    ///
    /// If the work throws, the exception is rethrown on the calling test thread so Spock
    /// can report the original failure.
    ///
    public void runOnGdxThread(Runnable work, Duration timeout) {
        Objects.requireNonNull(work, "work");
        runOnGdxThread(() -> {
            work.run();
            return null;
        }, timeout);
    }

    ///
    /// Like {@link #runOnGdxThread(Runnable, Duration)} but returns a value.
    ///
    /// Casual libGDX note:
    /// - `Gdx.app.postRunnable(...)` is asynchronous; this method turns it into a synchronous call
    ///   by waiting on a latch.
    /// - If this times out, it generally means the render loop is not pumping frames (stuck),
    ///   or you deadlocked by waiting on the test thread from inside the render thread.
    ///
    public <T> T runOnGdxThread(Callable<T> work, Duration timeout) {
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(timeout, "timeout");

        if (Gdx.app == null) {
            throw new IllegalStateException("Gdx.app is null. Did you call start() in setupSpec()?");
        }

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Gdx.app.postRunnable(() -> {
            try {
                result.set(work.call());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        boolean finished;
        try {
            finished = done.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for GDX thread work", e);
        }

        if (!finished) {
            throw new RuntimeException("Timed out waiting for work on GDX thread (" + timeout + ")");
        }

        Throwable t = error.get();
        if (t != null) {
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            throw new RuntimeException(t);
        }

        return result.get();
    }

    ///
    /// Convenience overload with a default timeout (10 seconds).
    ///
    public <T> T runOnGdxThread(Callable<T> work) {
        return runOnGdxThread(work, Duration.ofSeconds(10));
    }

    ///
    /// Convenience overload with a default timeout (10 seconds).
    ///
    public void runOnGdxThread(Runnable work) {
        runOnGdxThread(work, Duration.ofSeconds(10));
    }

    ///
    /// Waits until the LWJGL3 app has invoked `create()`.
    ///
    /// This is the earliest moment when it is safe to assume:
    /// - `Gdx.app` is present
    /// - the GL context exists
    ///
    /// If this times out, it generally indicates:
    /// - the app thread failed before calling `create()` (check {@link #startupFailure})
    /// - a display/GL context could not be created (common on CI without DISPLAY)
    /// - a deadlock or infinite loop inside application startup
    ///
    private void awaitCreated(Duration timeout) {
        CountDownLatch latch = createdLatch;
        if (latch == null) {
            throw new IllegalStateException("Harness internal error: createdLatch is null");
        }
        boolean ok;
        try {
            ok = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for libGDX create()", e);
        }
        if (!ok) {
            throw new RuntimeException("Timed out waiting for libGDX create() (" + timeout + ")");
        }
    }
}
