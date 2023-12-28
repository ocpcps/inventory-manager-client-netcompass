package com.osstelecom.db.inventory.manager.client.netcompass;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;


@Component
public class NetcompassClientRunner implements CommandLineRunner, ExitCodeGenerator {

    private final NetcompassClient client;

    private final IFactory factory; // auto-configured to inject PicocliSpringFactory

    private int exitCode;

    public NetcompassClientRunner(NetcompassClient client, IFactory factory) {
        this.client = client;
        this.factory = factory;
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(client, factory).execute(args);

        // commandLine.parseWithHandler(new CommandLine.RunLast(), args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
