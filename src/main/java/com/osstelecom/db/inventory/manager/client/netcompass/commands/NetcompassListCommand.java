package com.osstelecom.db.inventory.manager.client.netcompass.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "list", mixinStandardHelpOptions = true)
public class NetcompassListCommand {
}
