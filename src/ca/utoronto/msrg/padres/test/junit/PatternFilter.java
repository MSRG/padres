package ca.utoronto.msrg.padres.test.junit;

import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class PatternFilter extends Filter {

	String originClass;

	String pattern = null;

	Level level = null;

	public PatternFilter(String originClass, String pattern) {
		this.originClass = originClass;
		this.pattern = pattern;
	}

	public PatternFilter(String originClass) {
		this.originClass = originClass;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	public int decide(LoggingEvent event) {
		if (!originClass.equals(event.getLocationInformation().getClassName())) {
			return Filter.DENY;
		}
		if (level != null && event.getLevel().toInt() < level.toInt()) {
			return Filter.DENY;
		}
		String msg = event.getMessage().toString();
		if (pattern == null || msg.matches(pattern)) {
			return Filter.ACCEPT;
		}
		return Filter.DENY;
	}
}
