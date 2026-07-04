package ru.raidmine.rbaritone.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class RBaritoneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Navigator::tick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("rbar")
                        .then(literal("goto")
                                .then(argument("x", DoubleArgumentType.doubleArg())
                                        .then(argument("y", DoubleArgumentType.doubleArg())
                                                .then(argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(ctx -> {
                                                            double x = DoubleArgumentType.getDouble(ctx, "x");
                                                            double y = DoubleArgumentType.getDouble(ctx, "y");
                                                            double z = DoubleArgumentType.getDouble(ctx, "z");
                                                            Navigator.gotoPos(x, y, z);
                                                            ctx.getSource().sendFeedback(Text.literal("§6[RBaritone] §fИду к: §e" + fmt(x) + " " + fmt(y) + " " + fmt(z)));
                                                            return 1;
                                                        })))))
                        .then(literal("follow")
                                .then(argument("nick", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String nick = StringArgumentType.getString(ctx, "nick");
                                            Navigator.follow(nick);
                                            ctx.getSource().sendFeedback(Text.literal("§6[RBaritone] §fСледую за игроком: §e" + nick));
                                            return 1;
                                        })))
                        .then(literal("stop")
                                .executes(ctx -> {
                                    Navigator.stop();
                                    ctx.getSource().sendFeedback(Text.literal("§6[RBaritone] §cОстановлено."));
                                    return 1;
                                }))
                        .then(literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(Text.literal("§6[RBaritone] §f" + Navigator.status()));
                                    return 1;
                                }))
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("§6[RBaritone Lite] §fКоманды:"));
                            ctx.getSource().sendFeedback(Text.literal("§e/rbar goto <x> <y> <z> §7- идти к координатам"));
                            ctx.getSource().sendFeedback(Text.literal("§e/rbar follow <nick> §7- следовать за игроком"));
                            ctx.getSource().sendFeedback(Text.literal("§e/rbar stop §7- остановить"));
                            ctx.getSource().sendFeedback(Text.literal("§e/rbar status §7- статус"));
                            return 1;
                        })
        ));
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
