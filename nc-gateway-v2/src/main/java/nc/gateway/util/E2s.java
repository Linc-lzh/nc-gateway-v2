package nc.gateway.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class E2s {
    public static String exception2String(Exception ex) {
        String exceptionMessage = "";
        if (ex != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            try {
                ex.printStackTrace(pw);
                exceptionMessage = sw.toString();
            } finally {
                try {
                    sw.close();
                    pw.close();
                } catch (Exception e) {
                }
            }
        }
        return exceptionMessage;
    }
}
