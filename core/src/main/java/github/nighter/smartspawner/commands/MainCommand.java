package github.nighter.smartspawner.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.clear.ClearSubCommand;
import github.nighter.smartspawner.commands.config.FolderConfigSubCommand;
import github.nighter.smartspawner.commands.give.GiveSubCommand;
import github.nighter.smartspawner.commands.hologram.HologramSubCommand;
import github.nighter.smartspawner.commands.list.ListSubCommand;
import github.nighter.smartspawner.commands.near.NearSubCommand;
import github.nighter.smartspawner.commands.prices.PricesSubCommand;
import github.nighter.smartspawner.commands.reload.ReloadSubCommand;
import github.nighter.smartspawner.commands.set.SetSubCommand;
import github.nighter.smartspawner.commands.whitelist.WhitelistSubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@RequiredArgsConstructor
public class MainCommand {
    private final List<BaseSubCommand> subCommands;

    public MainCommand(SmartSpawner plugin) {
        this.subCommands = List.of(
                new ReloadSubCommand(plugin),
                new GiveSubCommand(plugin),
                new ListSubCommand(plugin),
                new HologramSubCommand(plugin),
                new PricesSubCommand(plugin),
                new ClearSubCommand(plugin),
                new NearSubCommand(plugin, plugin.getSpawnerHighlightManager()),
                new SetSubCommand(plugin),
                new WhitelistSubCommand(plugin),
                new FolderConfigSubCommand(plugin, FolderConfigSubCommand.ConfigOption.LANGUAGE),
                new FolderConfigSubCommand(plugin, FolderConfigSubCommand.ConfigOption.GUI_LAYOUT)
        );
    }

    // Build the main command with all subcommands
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return buildCommandWithName("smartspawner");
    }

    // Build the alias command
    public LiteralCommandNode<CommandSourceStack> buildAliasCommand() {
        return buildCommandWithName("spawner");
    }

    public LiteralCommandNode<CommandSourceStack> buildAliasCommand2() {
        return buildCommandWithName("ss");
    }

    // Helper method to build command with any name
    private LiteralCommandNode<CommandSourceStack> buildCommandWithName(String name) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(name);

        // Allow everyone into the command tree. Each subcommand enforces its own permission
        // via its own requires() (see BaseSubCommand#build / hasPermission), so opening the
        // root does not expose staff subcommands. This is required so the permission-less
        // whitelist subcommand is reachable by regular players who own spawners.
        builder.requires(source -> true);

        // Add all subcommands to the builder
        for (BaseSubCommand subCommand : subCommands) {
            builder.then(subCommand.build());
        }

        return builder.build();
    }
}
