package net.querz.mcaselector.logging;

import net.querz.mcaselector.validation.ShutdownHooks;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(name = "ExceptionBurstFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class ExceptionBurstFilter extends AbstractFilter {

	private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(ExceptionBurstFilter.class);

	private final Map<MappableExceptionInfo, ExceptionInfo> lastExceptions = new ConcurrentHashMap<>();

	private static final int defaultDuration = 20000;

	private final int duration;

	private boolean running;

	private ExceptionBurstFilter(int duration) {
		super(Result.DENY, Result.ACCEPT);
		this.duration = duration;
		running = true;
		new Thread(this::repeatedFlush).start();
		ShutdownHooks.addShutdownHook(this::close);
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
		if (t == null) {
			return Result.NEUTRAL;
		}
		MappableExceptionInfo i = new MappableExceptionInfo(t);
		if (lastExceptions.containsKey(i)) {
			lastExceptions.get(i).update();
			return Result.DENY;
		}
		lastExceptions.put(i, new ExceptionInfo(t));
		return Result.NEUTRAL;
	}

	@Override
	public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
		if (t == null) {
			return Result.NEUTRAL;
		}
		MappableExceptionInfo i = new MappableExceptionInfo(t);
		if (lastExceptions.containsKey(i)) {
			lastExceptions.get(i).update();
			return Result.DENY;
		}
		lastExceptions.put(i, new ExceptionInfo(t));
		return Result.NEUTRAL;
	}

	@PluginFactory
	public static ExceptionBurstFilter createFilter(@PluginAttribute("duration") Integer duration) {
		int actualDuration = duration == null ? defaultDuration : duration;
		return new ExceptionBurstFilter(actualDuration);
	}

	@SuppressWarnings("BusyWait")
	private void repeatedFlush() {
		while (running) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {
				LOGGER.warn("failed repeated flush of ExceptionBurstFilter: {}", ex.getMessage());
			}

			long now = System.currentTimeMillis();
			lastExceptions.entrySet().removeIf(e -> {
				if (now - e.getValue().timestamp > duration) {
					e.getValue().log(LOGGER);
					return true;
				}
				return false;
			});
		}
	}

	private void close() {
		running = false;
		for (Map.Entry<MappableExceptionInfo, ExceptionInfo> e : lastExceptions.entrySet()) {
			e.getValue().log(LOGGER);
		}
		lastExceptions.clear();
	}

}
