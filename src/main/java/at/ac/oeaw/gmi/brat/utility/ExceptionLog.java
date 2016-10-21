package at.ac.oeaw.gmi.brat.utility;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by christian.goeschl on 10/19/16.
 */
public class ExceptionLog {
    public static String StackTraceToString(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(new StringWriter());
        e.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }
}
