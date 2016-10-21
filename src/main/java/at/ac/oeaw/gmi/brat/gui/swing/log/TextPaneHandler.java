package at.ac.oeaw.gmi.brat.gui.swing.log;

import at.ac.oeaw.gmi.brat.gui.swing.LogPanel;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

public class TextPaneHandler extends Handler{
	private LogPanel logPanel = null;
	private static TextPaneHandler handler = null;
	
	final static SimpleAttributeSet ATTR_DEBUG=new SimpleAttributeSet();
	final static SimpleAttributeSet ATTR_DEFAULT=new SimpleAttributeSet();
	final static SimpleAttributeSet ATTR_SEVERE=new SimpleAttributeSet();
	
	static{
		StyleConstants.setForeground(ATTR_DEBUG,Color.BLUE);
		StyleConstants.setFontFamily(ATTR_DEBUG,"Monospaced");
		StyleConstants.setFontSize(ATTR_DEBUG,10);
		
		StyleConstants.setForeground(ATTR_DEFAULT,Color.BLACK);
		StyleConstants.setFontFamily(ATTR_DEFAULT,"Monospaced");
		StyleConstants.setFontSize(ATTR_DEFAULT,10);
		
		StyleConstants.setForeground(ATTR_SEVERE,Color.RED);
		StyleConstants.setFontFamily(ATTR_SEVERE,"Monospaced");
		StyleConstants.setFontSize(ATTR_SEVERE,10);
		
	}
	
	private TextPaneHandler() {
		LogManager manager = LogManager.getLogManager();
		String className = this.getClass().getName();
		String level = manager.getProperty(className + ".level");
		setLevel(level != null ? Level.parse(level) : Level.INFO);
	}
	
	public void setLogPanel(LogPanel logPanel){
		this.logPanel=logPanel;
	}
	
	@Override
	public void setLevel(Level level){
		super.setLevel(level);
	}
	
	public static synchronized TextPaneHandler getInstance() {
		if (handler == null) {
			handler = new TextPaneHandler();
		}
		return handler;
	}

	@Override
	public synchronized void publish(final LogRecord record) {
		String message = null;
		if (!isLoggable(record))
			return;

		message = getFormatter().format(record);
		SimpleAttributeSet sas=ATTR_DEFAULT;
		if(record.getLevel().intValue()<Level.INFO.intValue()){
			sas=ATTR_DEBUG;
		}
		else if(record.getLevel().intValue()>Level.INFO.intValue()){
			sas=ATTR_SEVERE;
		}
		logPanel.appendText(message,sas);
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws SecurityException {
		// TODO Auto-generated method stub
		
	}

}
