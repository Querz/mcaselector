package net.querz.mcaselector.logging;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingTest {

	@Test
	void uncaughtExceptionsReachTheLogAndStandardError() {
		AtomicReference<String> loggedThread = new AtomicReference<>();
		AtomicReference<Throwable> loggedThrowable = new AtomicReference<>();
		ByteArrayOutputStream standardError = new ByteArrayOutputStream();
		RuntimeException failure = new RuntimeException("builder popup failure");

		Thread.UncaughtExceptionHandler handler = Logging.createUncaughtExceptionHandler(
			(threadName, throwable) -> {
				loggedThread.set(threadName);
				loggedThrowable.set(throwable);
			},
			new PrintStream(standardError, true, StandardCharsets.UTF_8)
		);

		handler.uncaughtException(new Thread("JavaFX Application Thread"), failure);

		assertEquals("JavaFX Application Thread", loggedThread.get());
		assertSame(failure, loggedThrowable.get());
		String capturedError = standardError.toString(StandardCharsets.UTF_8);
		assertTrue(capturedError.contains("java.lang.RuntimeException: builder popup failure"));
		assertTrue(capturedError.contains("LoggingTest.uncaughtExceptionsReachTheLogAndStandardError"));
	}
}
