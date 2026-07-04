package ru.raidmine.rbaritone.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class Navigator {
    private static Mode mode = Mode.IDLE;
    private static Vec3d target;
    private static String followNick;
    private static int targetLostTicks = 0;

    private static final double GOTO_STOP_DISTANCE = 1.25;
    private static final double FOLLOW_STOP_DISTANCE = 3.25;
    private static final double MAX_PITCH = 35.0;

    private Navigator() {
    }

    public static void gotoPos(double x, double y, double z) {
        mode = Mode.GOTO;
        target = new Vec3d(x, y, z);
        followNick = null;
        targetLostTicks = 0;
    }

    public static void follow(String nick) {
        mode = Mode.FOLLOW;
        target = null;
        followNick = nick;
        targetLostTicks = 0;
    }

    public static void stop() {
        mode = Mode.IDLE;
        target = null;
        followNick = null;
        targetLostTicks = 0;
        releaseKeys(MinecraftClient.getInstance());
    }

    public static String status() {
        return switch (mode) {
            case IDLE -> "ничего не делаю.";
            case GOTO -> "иду к координатам " + shortVec(target) + ".";
            case FOLLOW -> "следую за " + followNick + ".";
        };
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            releaseKeys(client);
            return;
        }

        if (mode == Mode.IDLE) {
            releaseKeys(client);
            return;
        }

        ClientPlayerEntity player = client.player;
        Vec3d activeTarget = resolveTarget(client);

        if (activeTarget == null) {
            releaseKeys(client);
            targetLostTicks++;
            if (targetLostTicks == 40 && player != null) {
                player.sendMessage(Text.literal("§6[RBaritone] §cЦель не найдена."), false);
            }
            return;
        }

        targetLostTicks = 0;
        moveTo(client, player, activeTarget, mode == Mode.FOLLOW ? FOLLOW_STOP_DISTANCE : GOTO_STOP_DISTANCE);
    }

    private static Vec3d resolveTarget(MinecraftClient client) {
        if (mode == Mode.GOTO) {
            return target;
        }

        if (mode == Mode.FOLLOW && followNick != null && client.world != null && client.player != null) {
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                String name = player.getName().getString();
                if (player != client.player && name.equalsIgnoreCase(followNick)) {
                    return player.getPos();
                }
            }
        }

        return null;
    }

    private static void moveTo(MinecraftClient client, ClientPlayerEntity player, Vec3d targetPos, double stopDistance) {
        Vec3d pos = player.getPos();
        double dx = targetPos.x - pos.x;
        double dy = targetPos.y - pos.y;
        double dz = targetPos.z - pos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance <= stopDistance && Math.abs(dy) < 1.75) {
            releaseKeys(client);
            if (mode == Mode.GOTO) {
                player.sendMessage(Text.literal("§6[RBaritone] §aДошёл до точки."), false);
                stop();
            }
            return;
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) clamp(-Math.toDegrees(Math.atan2(dy, horizontalDistance)), -MAX_PITCH, MAX_PITCH);
        player.setYaw(yaw);
        player.setPitch(pitch);

        client.options.forwardKey.setPressed(true);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.sprintKey.setPressed(horizontalDistance > 4.0);

        boolean shouldJump = false;
        if (player.horizontalCollision && player.isOnGround()) {
            shouldJump = true;
        }
        if (dy > 0.65 && player.isOnGround()) {
            shouldJump = true;
        }
        if (player.isTouchingWater() || player.isInLava()) {
            shouldJump = true;
        }
        client.options.jumpKey.setPressed(shouldJump);
        client.options.sneakKey.setPressed(false);
    }

    private static void releaseKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String shortVec(Vec3d vec) {
        if (vec == null) {
            return "неизвестно";
        }
        return String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f", vec.x, vec.y, vec.z);
    }

    private enum Mode {
        IDLE,
        GOTO,
        FOLLOW
    }
}
