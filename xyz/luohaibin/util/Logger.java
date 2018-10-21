package xyz.luohaibin.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    public enum Level { DEBUG, INFO, WARN, ERROR, FATAL };

    static Level level = Level.INFO;

    public static void setLevel(Level level) {
        Logger.level = level;
    }

    static public void d(String msg){
        out(Level.DEBUG, msg);
    }
    static public void i(String msg){
        out(Level.INFO, msg);
    }
    static public void w(String msg){
        out(Level.WARN, msg);
    }
    static public void e(String msg){
        out(Level.ERROR, msg);
    }
    static public void f(String msg){
        out(Level.FATAL, msg);
    }

    static void out(Level level,String msg){
        if(Logger.level.ordinal() > level.ordinal())
            return;
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        System.err.println(sdf.format(new Date()) +" "+ level.name() + " "+ msg);
    }
}
