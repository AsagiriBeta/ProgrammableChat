package AsagiriBeta.programmablechat.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class AutoMessageCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("automessage")
            .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String message = StringArgumentType.getString(context, "message");
                    AutoMessageMod.setMessage(message);
                    return 1;
                }))
            .then(ClientCommandManager.literal("interval")
                .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        int interval = IntegerArgumentType.getInteger(context, "seconds");
                        AutoMessageMod.setInterval(interval);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("start")
                .executes(context -> {
                    AutoMessageMod.startSending();
                    return 1;
                }))
            .then(ClientCommandManager.literal("stop")
                .executes(context -> {
                    AutoMessageMod.stopSending();
                    return 1;
                }))
            .then(ClientCommandManager.literal("autobuild")
                .executes(context -> {
                    AutoMessageMod.setAutoBuilding(!AutoMessageMod.isAutoBuilding());
                    return 1;
                }))
        );
    }
}