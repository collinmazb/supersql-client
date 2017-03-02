package com.tencent;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import io.airlift.airline.Command;
import io.airlift.units.Duration;

import static java.lang.Integer.parseInt;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jline.internal.Configuration.getUserHome;
//import static com.tencent.Completion.commandCompleter;
//import static com.tencent.Completion.lowerCaseCommandCompleter;
import static com.facebook.presto.sql.parser.StatementSplitter.squeezeStatement;

//import jline.console.completer.Completer;
//import jline.console.completer.StringsCompleter;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

import javax.sound.sampled.Line;
import java.io.File;
import java.util.regex.Pattern;
import com.tencent.supersql.jdbc.SSqlConnection;
import com.tencent.supersql.jdbc.SSqlDriver;

/**
 * Created by waixingren on 2/22/17.
 */
@Command(name = "supersql", description = "SuperSql interactive console")
public class Console implements Runnable{

    private static final Duration EXIT_DELAY = new Duration(3, SECONDS);
    private static final Pattern HISTORY_INDEX_PATTERN = Pattern.compile("!\\d+");
    public String currentLinkName = null;
    public void run() {

        AtomicBoolean exiting = new AtomicBoolean();
        interruptThreadOnExit(Thread.currentThread(), exiting);
        try {
            runConsole(exiting);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static void runConsole(AtomicBoolean exiting) throws ClassNotFoundException {

        String uaeJdbcString = "jdbc:ssql://localhost:7911";
        Class.forName("com.tencent.supersql.jdbc.SSqlDriver");
        Connection con = null;
        SSqlConnection ssqlConnection = null;
        try {
            con = DriverManager.getConnection(uaeJdbcString);
            ssqlConnection = (SSqlConnection)con;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (LineReader reader = new LineReader(getHistory())) {
            String promotSubfix = null;
            while (!exiting.get()) {

                StringBuilder buffer = new StringBuilder();
                String prompt = "supersql";

                if (promotSubfix != null) {
                    prompt += ":" + promotSubfix;
                }
                if (buffer.length() > 0) {
                    prompt = Strings.repeat(" ", prompt.length() - 1) + "-";
                }
                String commandPrompt = prompt + "> ";
                String line = null;
                line = reader.readLine(commandPrompt);


                // add buffer to history and clear on user interrupt
                if (reader.interrupted()) {
                    String partial = squeezeStatement(buffer.toString());
                    if (!partial.isEmpty()) {
                        reader.getHistory().add(partial);
                    }
                    buffer = new StringBuilder();
                    continue;
                }

                // exit on EOF
                if (line == null) {
                    System.out.println();
                    return;
                }

                // check for special commands if this is the first line
                String command = null;
                if (buffer.length() == 0) {
                    command = line.trim();

                    if (HISTORY_INDEX_PATTERN.matcher(command).matches()) {
                        int historyIndex = parseInt(command.substring(1));
                        History history = reader.getHistory();
                        if ((historyIndex <= 0) || (historyIndex > history.index())) {
                            System.err.println("Command does not exist");
                            continue;
                        }
                        line = history.get(historyIndex - 1).toString();
                        System.out.println(commandPrompt + line);
                    }

                    if (command.endsWith(";")) {
                        command = command.substring(0, command.length() - 1).trim();
                    }

                    switch (command.toLowerCase(ENGLISH)) {
                        case "exit":
                        case "quit":
                            return;
                        case "history":
                            for (History.Entry entry : reader.getHistory()) {
                                System.out.printf("%5d  %s%n", entry.index() + 1, entry.value());
                            }
                            continue;
                        case "help":
                            System.out.println();
//                            System.out.println(getHelpText());
                            System.out.println("you need help???");
                            continue;
                    }
                }

//                buffer.append(command).append("\n");
                buffer.append(command);

                // execute any complete statements
                String sql = buffer.toString();
                String isLink = processLink(sql, ssqlConnection);
                if(isLink.equalsIgnoreCase("create link") || isLink.equalsIgnoreCase("show link")){

                    continue;

                }else if(isLink.equalsIgnoreCase("not link")){

                    executeSql(sql, ssqlConnection);
                }
                else{

                    promotSubfix = isLink;
                }


            }
        } catch (IOException e) {
            System.err.println("Readline error: " + e.getMessage());
        }
    }

    private static void executeSql(String sql, Connection con) {

        Statement statement = null;
        try {
            statement = con.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            BeeLine beeLine = new BeeLine();
            TableOutputFormat tableOutputFormat = new TableOutputFormat(beeLine);
            BufferedRows bufferedRows = new BufferedRows(beeLine, resultSet);
            tableOutputFormat.print(bufferedRows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean isCreateLink(String sql) {

        String terms[] = sql.split(" ");
        String first = terms[0].trim();
        String second = terms[1].trim();
        if(first.equalsIgnoreCase("create") && second.equalsIgnoreCase("link")){

            return true;
        }
        return false;
    }
    private static String processLink(String sql, Connection con){

        String terms[] = sql.split(" ");
        SSqlConnection ssqlConnection = (SSqlConnection)con;
        if (isCreateLink(sql)) {
            String linkName = terms[2].trim();
            String userName = terms[5].trim();
            String passwd = terms[8].trim().substring(1);
            passwd = passwd.substring(0,passwd.length()-1);
            String driverJdbcUrl = sql.split("using")[1].trim().substring(1);
            driverJdbcUrl = driverJdbcUrl.substring(0, driverJdbcUrl.length()-1);
            ssqlConnection.createLink(driverJdbcUrl, linkName, userName, passwd);
            return "create link";
        }else if(isUsingLink(sql)){

            String linkName = terms[2].trim();
            ssqlConnection.usingLink(linkName);
            return linkName;
        }else if(isShowLink(sql)){

             List<String> allLinks = ssqlConnection.getAllLinks();
            for (String link : allLinks) {

                System.out.println(link);
            }
            return "show link";
        }
        return "not link";
    }

    private static boolean isShowLink(String sql) {

        String terms[] = sql.split(" ");
        String first = terms[0].trim();
        String second = terms[1].trim();
        if(first.equalsIgnoreCase("show") && second.equalsIgnoreCase("links")){

            return true;
        }
        return false;
    }

    private static boolean isUsingLink(String sql) {

        String terms[] = sql.split(" ");
        String first = terms[0].trim();
        String second = terms[1].trim();
        if(first.equalsIgnoreCase("use") && second.equalsIgnoreCase("link")){

            return true;
        }
        return false;
    }

    private static void interruptThreadOnExit(Thread thread, AtomicBoolean exiting)
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            exiting.set(true);
            thread.interrupt();
            try {
                thread.join(EXIT_DELAY.toMillis());
            }
            catch (InterruptedException ignored) {
            }
        }));
    }

    private static MemoryHistory getHistory()
    {
        MemoryHistory history;
        File historyFile = new File(getUserHome(), ".presto_history");
        try {
            history = new FileHistory(historyFile);
            history.setMaxSize(10000);
        }
        catch (IOException e) {
            System.err.printf("WARNING: Failed to load history file (%s): %s. " +
                            "History will not be available during this session.%n",
                    historyFile, e.getMessage());
            history = new MemoryHistory();
        }
        history.setAutoTrim(true);
        return history;
    }

}
