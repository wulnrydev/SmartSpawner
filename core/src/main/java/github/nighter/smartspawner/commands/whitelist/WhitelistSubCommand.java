package github.nighter.smartspawner.commands.whitelist;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.BaseSubCommand;
import github.nighter.smartspawner.spawner.data.SpawnerManager;
import github.nighter.smartspawner.spawner.properties.SpawnerData;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /spawner whitelist ekle <player>} and {@code /spawner whitelist çıkar <player>}.
 * <p>
 * Lets the owner of a spawner grant (or revoke) another player access to that spawner's menu
 * and storage. Whitelisted players can open the menu but still cannot break the spawner.
 * <p>
 * This command intentionally requires <b>no permission node</b>: any player may run it, but it
 * only affects the spawner they are looking at, and only if they have owner-level control over
 * it (they are the owner, an OP, or hold the ownership bypass permission).
 */
@NullMarked
public class WhitelistSubCommand extends BaseSubCommand {
    private static final int TARGET_DISTANCE = 8;

    // Accepted keywords. ASCII aliases are provided alongside the Turkish words so the command
    // works regardless of keyboard layout / client handling of special characters.
    private static final String[] ADD_LITERALS = {"ekle", "add"};
    private static final String[] REMOVE_LITERALS = {"çıkar", "cikar", "sil", "remove"};

    private final SpawnerManager spawnerManager;

    public WhitelistSubCommand(SmartSpawner plugin) {
        super(plugin);
        this.spawnerManager = plugin.getSpawnerManager();
    }

    @Override
    public String getName() {
        return "whitelist";
    }

    @Override
    public String getPermission() {
        // No dedicated permission node: access is gated by spawner ownership at execution time.
        return "smartspawner.command.use";
    }

    @Override
    public String getDescription() {
        return "Grant or revoke another player's access to a spawner you own";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(getName());

        // Open to everyone – the ownership check happens inside the executor.
        builder.requires(source -> true);
        builder.executes(this::execute);

        for (String add : ADD_LITERALS) {
            builder.then(Commands.literal(add)
                    .then(Commands.argument("player", ArgumentTypes.player())
                            .executes(context -> executeWhitelist(context, true))));
        }
        for (String remove : REMOVE_LITERALS) {
            builder.then(Commands.literal(remove)
                    .then(Commands.argument("player", ArgumentTypes.player())
                            .executes(context -> executeWhitelist(context, false))));
        }

        return builder;
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        logCommandExecution(context);
        plugin.getMessageService().sendMessage(context.getSource().getSender(), "whitelist.usage");
        return 0;
    }

    private int executeWhitelist(CommandContext<CommandSourceStack> context, boolean add) {
        logCommandExecution(context);
        CommandSender sender = context.getSource().getSender();

        if (!(sender instanceof Player player)) {
            plugin.getMessageService().sendMessage(sender, "player_only");
            return 0;
        }

        // The command applies to the spawner the player is looking at.
        SpawnerData spawner = getSpawnerPlayerIsLookingAt(player);
        if (spawner == null) {
            plugin.getMessageService().sendMessage(player, "whitelist.not_looking_at_spawner");
            return 0;
        }

        // Only players with owner-level control may manage the whitelist.
        if (!spawner.canInteract(player)) {
            plugin.getMessageService().sendMessage(player, "spawner_not_owner");
            return 0;
        }

        Player target = resolveTargetPlayer(context);
        if (target == null) {
            plugin.getMessageService().sendMessage(player, "whitelist.player_not_found");
            return 0;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());

        // The owner already has full access; whitelisting them is a no-op.
        if (target.getUniqueId().equals(spawner.getOwnerUuid())) {
            plugin.getMessageService().sendMessage(player, "whitelist.is_owner", placeholders);
            return 0;
        }

        if (add) {
            if (!spawner.addToWhitelist(target.getUniqueId())) {
                plugin.getMessageService().sendMessage(player, "whitelist.already_added", placeholders);
                return 0;
            }
            spawnerManager.markSpawnerModified(spawner.getSpawnerId());
            plugin.getMessageService().sendMessage(player, "whitelist.added", placeholders);
        } else {
            if (!spawner.removeFromWhitelist(target.getUniqueId())) {
                plugin.getMessageService().sendMessage(player, "whitelist.not_in_list", placeholders);
                return 0;
            }
            spawnerManager.markSpawnerModified(spawner.getSpawnerId());
            plugin.getMessageService().sendMessage(player, "whitelist.removed", placeholders);
        }
        return 1;
    }

    private SpawnerData getSpawnerPlayerIsLookingAt(Player player) {
        Block targetBlock = player.getTargetBlockExact(TARGET_DISTANCE, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() != Material.SPAWNER) {
            return null;
        }
        return spawnerManager.getSpawnerByLocation(targetBlock.getLocation());
    }

    private Player resolveTargetPlayer(CommandContext<CommandSourceStack> context) {
        try {
            PlayerSelectorArgumentResolver resolver =
                    context.getArgument("player", PlayerSelectorArgumentResolver.class);
            List<Player> players = resolver.resolve(context.getSource());
            return players.isEmpty() ? null : players.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
