package at.ac.oeaw.gmi.brat.gui.fx.log;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Created by christian.goeschl on 10/17/16.
 *
 */
public class BratFxLogHandler extends Handler{
    private LogQueue logQueue;

    public BratFxLogHandler(LogQueue logQueue) {
		LogManager manager = LogManager.getLogManager();
		String className = this.getClass().getName();
		String level = manager.getProperty(className + ".level");
		setLevel(level != null ? Level.parse(level) : Level.INFO);

        this.logQueue =logQueue;
	}

	public LogQueue getLogQueue(){
        return logQueue;
    }

	@Override
	public void setLevel(Level level){
		super.setLevel(level);
	}

    @Override
    public void publish(final LogRecord logRecord) {
        logRecord.getSourceClassName();
        logRecord.getSourceMethodName();
		if (isLoggable(logRecord))
	        logQueue.offer(logRecord);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
