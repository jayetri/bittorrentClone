package p2p;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Logging {

    private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static String LOG_PEER = "log_peer_%d.log";


    public static void setup (int peerid) throws IOException {
        LOGGER.setLevel(Level.INFO);
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new P2PFormatter());
        LOGGER.addHandler(consoleHandler);

        String fileName = String.format(LOG_PEER, peerid);
        Handler fileHandler = new FileHandler(fileName);
        fileHandler.setFormatter(new P2PFormatter());
        LOGGER.addHandler(fileHandler);
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }
}

//Formatter for printing the logs as per project format
class P2PFormatter extends Formatter {
    // Create a DateFormat to format the logger timestamp.
    private static final DateFormat dateFormat = new SimpleDateFormat("[dd/MM/yyyy hh:mm:ss.SSS]");

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(dateFormat.format(new Date(record.getMillis()))).append(": ");
        builder.append(formatMessage(record));
        builder.append("\n");
        return builder.toString();
    }

    public String getHead(Handler handler) {
        return super.getHead(handler);
    }

    public String getTail(Handler handler) {
        return super.getTail(handler);
    }
}
