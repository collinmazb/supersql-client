package com.tencent;

import jline.console.completer.Completer;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Created by waixingren on 2/22/17.
 */
public class TableNameCompleter
        implements Completer, Closeable
{

    @Override
    public int complete(String s, int i, List<CharSequence> list) {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }
}
