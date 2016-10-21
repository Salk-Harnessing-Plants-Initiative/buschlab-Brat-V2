package at.ac.oeaw.gmi.brat.gui.swing.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SingleLineFormatter extends Formatter{
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
//	private SimpleDateFormat dateFormat=new SimpleDateFormat("dd/MM/yy_HH:mm:ss.SSS");
	private SimpleDateFormat dateFormat=new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS");
	
	@Override
	public String format(LogRecord record)
	{
		StringBuilder sb = new StringBuilder();

		sb.append(dateFormat.format(new Date(record.getMillis())))
//		.append(" [")
//		.append(record.getThreadID())
//		.append("] ")
//		.append(record.getSourceClassName())
//		.append(".")
//		.append(record.getSourceMethodName())
		.append(" ")
		.append(String.format("%-7s",record.getLevel().getName()))
		.append(": ")
		.append(formatMessage(record))
		.append(LINE_SEPARATOR);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                // ignore
            }
        }

        return sb.toString();
	}


}
