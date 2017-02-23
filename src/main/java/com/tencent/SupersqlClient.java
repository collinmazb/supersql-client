package com.tencent;

/**
 * Hello world!
 *
 */
import static io.airlift.airline.SingleCommand.singleCommand;
public class SupersqlClient
{
    public static void main( String[] args )
    {
        Console console = singleCommand(Console.class).parse(args);

//        if (console.helpOption.showHelpIfRequested() ||
//                console.versionOption.showVersionIfRequested()) {
//            return;
//        }

        console.run();
    }
}
