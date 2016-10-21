package at.ac.oeaw.gmi.brat.gui.fx.log;

import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.LogRecord;

/**
 * Created by christian.goeschl on 10/17/16.
 *
 */
public class LogQueue {
    private final BlockingDeque<LogRecord> log;

        public LogQueue(Integer maxLogEntries){
            int MAX_LOG_ENTRIES;
            if(maxLogEntries!=null){
                MAX_LOG_ENTRIES = maxLogEntries;
            }
            else{
                MAX_LOG_ENTRIES = 1_000_000;
            }
            log = new LinkedBlockingDeque<>(MAX_LOG_ENTRIES);
        }

        void drainTo(Collection<? super LogRecord> collection) {
            log.drainTo(collection);
        }
        void offer(LogRecord record) {
            log.offer(record);
        }
}
