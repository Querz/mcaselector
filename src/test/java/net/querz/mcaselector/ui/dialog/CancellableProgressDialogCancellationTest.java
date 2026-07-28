package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.ui.ProgressTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CancellableProgressDialogCancellationTest {

	@Test
	void currentTaskScopeSelectsOnlyLocalCancellation() {
		Class<?> scopeType = Arrays.stream(CancellableProgressDialog.class.getDeclaredClasses())
				.filter(type -> type.getSimpleName().equals("CancellationScope"))
				.findFirst()
				.orElse(null);
		assertNotNull(scopeType, "CancellableProgressDialog must expose a cancellation scope");

		try {
			@SuppressWarnings({"unchecked", "rawtypes"})
			Object currentTask = Enum.valueOf((Class<? extends Enum>) scopeType, "CURRENT_TASK");
			Method selector = CancellableProgressDialog.class.getDeclaredMethod(
					"cancellationAction", scopeType, Runnable.class, Runnable.class);
			selector.setAccessible(true);
			AtomicInteger globalCalls = new AtomicInteger();
			AtomicInteger localCalls = new AtomicInteger();

			Runnable selected = (Runnable) selector.invoke(
					null, currentTask, (Runnable) globalCalls::incrementAndGet, (Runnable) localCalls::incrementAndGet);
			selected.run();

			assertEquals(0, globalCalls.get());
			assertEquals(1, localCalls.get());
		} catch (ReflectiveOperationException ex) {
			fail("missing task-scoped cancellation policy", ex);
		}
	}

	@Test
	void progressCancellationTokenIsVisibleAcrossThreads() throws NoSuchFieldException {
		assertTrue(Modifier.isVolatile(ProgressTask.class.getDeclaredField("cancelled").getModifiers()));
	}
}
