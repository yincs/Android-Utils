package changs.android.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.R.attr.tag;


/**
 * Created by yincs on 2016/11/21.
 */

public class MyLog {

    private int LEVEL_NONE = 0;
    /**
     * 打印>=该等级的日志
     */
    private static int currentLogLevel = 0;

    interface Logger {
        void println(int priority, String tag, Object msg);
    }

    private static Logger logger;

    public static void init(Logger logger) {
        MyLog.logger = logger;
    }

    private static void check() {
        if (logger == null)
            logger = new JavaLogger();
    }

    /**
     * DEBUG = 3
     */
    public static void d(Object msg) {
        check();
        logger.println(Log.DEBUG, "MyLog", msg.toString());
    }

    /**
     * DEBUG = 3
     */
    public static void d(String tag, Object msg) {
        check();
        logger.println(Log.DEBUG, tag, msg.toString());
    }

    /**
     * INFO = 4
     * 网络请求相关日志记录
     */
    public static void h(String tag, Object msg) {
        check();
        logger.println(Log.INFO, tag, msg);
    }

    /**
     * 错误类日志记录
     * ERROR = 6
     */
    public static void e(String tag, Object msg) {
        check();
        logger.println(Log.ERROR, tag, msg);
    }

    /**
     * ASSERT = 7
     * 崩溃类日志记录
     */
    public static void a(String tag, String msg) {
        check();
        logger.println(Log.ASSERT, tag, msg);
    }


    public static class AndroidLogger implements Logger {
        private static final String logHttpFileName = "log-http.txt";
        private static final String logNormalFileName = "log-debug.txt";
        private static final String logCrashFileName = "log-crash.txt";

        private File logHttpFile;
        private File logNormalFile;
        private File logCrashFile;
        private ExecutorService executorService;
        private final Date logTime = new Date();

        public AndroidLogger(Context ctx) {
            String appName = AppUtils.getAppName(ctx);
            if (!SDCardUtils.isSDCardEnable()) {
                return;
            }

            final File parentFile = new File(String.format("%s%s%s",
                    Environment.getExternalStorageDirectory().getAbsolutePath(), File.separator, appName));
            logHttpFile = new File(parentFile, logHttpFileName);
            logCrashFile = new File(parentFile, logCrashFileName);
            logNormalFile = new File(parentFile, logNormalFileName);

            executorService = Executors.newSingleThreadExecutor();
        }

        @Override
        public void println(int priority, String tag, Object msg) {
            if (priority < currentLogLevel) {
                return;
            }
            final String msgStr;
            if (msg == null) {
                msgStr = "null";
            } else {
                msgStr = msg.toString();
            }
            Log.println(priority, tag, msgStr);

            if (executorService == null) return;

            final File logFile;
            if (priority == Log.INFO) {
                logFile = logHttpFile;
            } else if (priority == Log.ASSERT || priority == Log.ERROR) {
                logFile = logCrashFile;
            } else {
                logFile = logNormalFile;
            }
            executorService.execute(new LogTask(logFile, tag, msgStr));
        }

        private void writeLog2File(LogTask logTask) {
            try {
                FileUtils.createOrExistsFile(logTask.file);
                FileWriter fileWriter = new FileWriter(logTask.file, true);
                BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
                StackTraceElement funcParent = Thread.currentThread().getStackTrace()[4];
                logTime.setTime(System.currentTimeMillis());
                String log = String.format("%s%n%s[%s][%s]:%s%n", logTime, tag, funcParent.getMethodName(), funcParent.getLineNumber(), logTask.msg);
                bufferWriter.write(log);
                bufferWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        class LogTask implements Runnable {
            File file;
            String tag;
            String msg;

            LogTask(File file, String tag, String msg) {
                this.file = file;
                this.tag = tag;
                this.msg = msg;
            }

            @Override
            public void run() {
                writeLog2File(this);
            }
        }
    }

    private static class JavaLogger implements Logger {

        @Override
        public void println(int priority, String tag, Object msg) {
            if (priority <= Log.INFO) {
                System.out.println(String.format("%s:%s", tag, msg));
            } else {
                System.err.println(String.format("%s:%s", tag, msg));
            }
        }
    }
}
