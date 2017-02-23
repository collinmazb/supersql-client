package com.tencent;

import java.io.PrintStream;

/**
 * Created by waixingren on 2/23/17.
 */
public class BeeLine {

//    private OutputFile recordOutputFile = null;
    private PrintStream outputStream = new PrintStream(System.out, true);
    private final BeeLineOpts opts = new BeeLineOpts(this, System.getProperties());
    private boolean isBeeLine = true;


    public BeeLine() {
        this(true);
    }

    public BeeLine(boolean isBeeLine) {
        this.isBeeLine = isBeeLine;
    }

    void output(ColorBuffer msg) {
        output(msg, true);
    }


    void output(String msg, boolean newline, PrintStream out) {
        output(getColorBuffer(msg), newline, out);
    }


    void output(ColorBuffer msg, boolean newline) {
        output(msg, newline, getOutputStream());
    }


    void output(ColorBuffer msg, boolean newline, PrintStream out) {
        if (newline) {
            out.println(msg.getColor());
        } else {
            out.print(msg.getColor());
        }

//        if (recordOutputFile == null) {
//            return;
//        }

        // only write to the record file if we are writing a line ...
        // otherwise we might get garbage from backspaces and such.
//        if (newline) {
//            recordOutputFile.addLine(msg.getMono()); // always just write mono
//        } else {
//            recordOutputFile.print(msg.getMono());
//        }
    }

    ColorBuffer getColorBuffer() {
        return new ColorBuffer(getOpts().getColor());
    }
    PrintStream getOutputStream() {
        return outputStream;
    }

    ColorBuffer getColorBuffer(String msg) {
        return new ColorBuffer(msg, getOpts().getColor());
    }

    public BeeLineOpts getOpts() {
        return opts;
    }
}
