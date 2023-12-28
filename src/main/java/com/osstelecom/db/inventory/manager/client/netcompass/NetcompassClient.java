package com.osstelecom.db.inventory.manager.client.netcompass;

import com.osstelecom.db.inventory.manager.client.netcompass.commands.NetcompassBackupCommand;
import com.osstelecom.db.inventory.manager.client.netcompass.commands.NetcompassListCommand;
import com.osstelecom.db.inventory.manager.client.netcompass.commands.NetcompassUploadCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "netcompass",
        mixinStandardHelpOptions = true,
        subcommands = {
                NetcompassBackupCommand.class,
                NetcompassUploadCommand.class,
                NetcompassListCommand.class
        })
public class NetcompassClient implements Callable<Integer> {

    @Option(names = "-x", description = "optional option")
    private String x;

    @Parameters(description = "positional params")
    private List<String> positionals;

    @Override
    public Integer call() {
        System.out.printf("mycommand was called with -x=%s and positionals: %s%n", x, positionals);
        return 23;
    }
}
