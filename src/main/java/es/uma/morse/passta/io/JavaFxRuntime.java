package es.uma.morse.passta.io;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

public final class JavaFxRuntime {

    private static final Object LOCK = new Object();

    private static boolean started;
    private static boolean terminated;

    private JavaFxRuntime() {
    }

    public static void start() {
        synchronized (LOCK) {
            if (terminated) {
                throw new IllegalStateException(
                    "JavaFX has already been terminated"
                );
            }

            if (started) {
                return;
            }

            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException exception) {
                /*
                 * JavaFX may already have been initialized
                 * by another JavaFX entry point.
                 */
            }

            started = true;
        }
    }

    public static void runLater(
            Runnable action) {

        Objects.requireNonNull(
            action,
            "Action is null"
        );

        start();

        Platform.runLater(action);
    }

    public static void runAndWait(
            Runnable action) {

        Objects.requireNonNull(
            action,
            "Action is null"
        );

        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        start();

        CountDownLatch completion =
            new CountDownLatch(1);

        AtomicReference<Throwable> failure =
            new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable exception) {
                failure.set(exception);
            } finally {
                completion.countDown();
            }
        });

        waitForCompletion(
            completion,
            failure
        );
    }

    public static void shutdown() {
        synchronized (LOCK) {
            if (!started || terminated) {
                return;
            }

            Platform.exit();

            started = false;
            terminated = true;
        }
    }

    private static void waitForCompletion(
            CountDownLatch completion,
            AtomicReference<Throwable> failure) {

        try {
            completion.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                "JavaFX operation was interrupted",
                exception
            );
        }

        Throwable exception = failure.get();

        if (exception == null) {
            return;
        }

        if (exception
                instanceof RuntimeException runtimeException) {

            throw runtimeException;
        }

        if (exception instanceof Error error) {
            throw error;
        }

        throw new RuntimeException(
            "JavaFX operation failed",
            exception
        );
    }
}