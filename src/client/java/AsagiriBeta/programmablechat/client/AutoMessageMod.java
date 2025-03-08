package AsagiriBeta.programmablechat.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.screen.slot.SlotActionType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AutoMessageMod implements ModInitializer {
    private static String message = "";
    private static int interval = 0;
    private static String coordinate = "";
    private static long lastSentTime = 0;
    private static boolean isSending = false;
    private static boolean isAutoEating = false;
    private static boolean isAutoBuilding = false;
    private static long lastPlayerMoveTime = 0;
    private static Vec3d lastPlayerPos = Vec3d.ZERO;
    private static boolean hasUpdatedPosition = false;
    private static long eatStartTime = 0;
    private static long lastGotoTime = 0; // 新增：记录上次发送#goto的时间
    private static boolean hasOpenedChest = false; // 新增：记录是否已经打开过箱子
    private static int chestOpenAttempts = 0; // 新增：记录尝试打开箱子的次数
    private static long lastChestOpenAttemptTime = 0; // 新增：记录上次尝试打开箱子的时间
    private static int currentSlotIndex = 0; // 新增：记录当前处理的槽位索引
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "programmablechat.json");

    public static String getCoordinate() {
        return coordinate; // 修改：返回实际的坐标值
    }

    @Override
    public void onInitialize() {
        loadConfig();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> AutoMessageCommand.register(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null) {
                return;
            }

            if (isSending && interval > 0 && System.currentTimeMillis() - lastSentTime >= interval * 1000L) {
                if (!message.isEmpty()) {
                    if (message.startsWith("/")) {
                        mc.player.networkHandler.sendChatCommand(message.substring(1));
                    } else {
                        mc.player.networkHandler.sendChatMessage(message);
                    }
                    lastSentTime = System.currentTimeMillis();
                }
            }

            // 自动建造逻辑
            if (isAutoBuilding) {
                Vec3d currentPos = mc.player.getPos();
                if (coordinate != null && !coordinate.isEmpty()) {
                    String[] parts = coordinate.split(" ");
                    if (parts.length == 3) {
                        try {
                            double targetX = Double.parseDouble(parts[0]);
                            double targetY = Double.parseDouble(parts[1]);
                            double targetZ = Double.parseDouble(parts[2]);
                            Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);

                            // 判断玩家实时坐标与预设坐标的差值
                            double deltaX = Math.abs(currentPos.x - targetPos.x);
                            double deltaY = Math.abs(currentPos.y - targetPos.y);
                            double deltaZ = Math.abs(currentPos.z - targetPos.z);

                            // 如果x和z差值不超过1，y差值不超过1，则尝试与箱子交互
                            if (deltaX <= 1.0 && deltaY <= 1.0 && deltaZ <= 1.0) {
                                // 如果差值小于等于1且未打开过箱子，则尝试与箱子交互
                                if (!hasOpenedChest) {
                                    if (System.currentTimeMillis() - lastChestOpenAttemptTime >= 500 && chestOpenAttempts < 1) {
                                        // 获取箱子的位置（预设坐标的x, y-1, z）
                                        BlockPos chestPos = new BlockPos((int) targetX, (int) targetY - 1, (int) targetZ);
                                        // 创建BlockHitResult来模拟与箱子的交互
                                        BlockHitResult hitResult = new BlockHitResult(new Vec3d(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5), Direction.UP, chestPos, false);
                                        // 模拟与箱子的交互
                                        if (mc.interactionManager != null) {
                                            mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hitResult);
                                        }
                                        chestOpenAttempts++;
                                        lastChestOpenAttemptTime = System.currentTimeMillis();
                                        hasOpenedChest = true; // 设置箱子已打开
                                    }
                                } else {
                                    // 在箱子打开后立即开始进行物品转移，但转移过程慢一点
                                    if (mc.player.currentScreenHandler != null && mc.interactionManager != null) {
                                        // 定义每次转移的间隔时间（例如100毫秒）
                                        long transferInterval = 100;
                                        // 计算当前可以转移的物品槽位
                                        if (System.currentTimeMillis() - lastChestOpenAttemptTime >= transferInterval) {
                                            // 获取箱子的槽位数量
                                            int chestSlotCount = 54; // 直接定义为54，确保所有槽位都被尝试
                                            // 获取玩家背包的槽位数量
                                            int playerInventorySlotCount = 27; // 玩家背包有3行9列，共27个槽位
                                            if (currentSlotIndex < chestSlotCount) {
                                                // 确保物品被正确移动到玩家背包
                                                int chestSlotIndex = 36 + currentSlotIndex; // 箱子的槽位从36开始
                                                var chestStack = mc.player.currentScreenHandler.getSlot(chestSlotIndex).getStack();
                                                if (!chestStack.isEmpty()) {
                                                    // 找到玩家背包中的第一个空槽位
                                                    int playerSlotIndex = currentSlotIndex % playerInventorySlotCount;
                                                    // 尝试将物品从箱子移动到玩家背包
                                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, chestSlotIndex, 0, SlotActionType.PICKUP, mc.player);
                                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, playerSlotIndex, 0, SlotActionType.PICKUP, mc.player);
                                                }
                                                currentSlotIndex++;
                                            } else {
                                                // 关闭箱子
                                                mc.player.closeHandledScreen();
                                                hasOpenedChest = false; // 重置箱子打开状态
                                                chestOpenAttempts = 0; // 重置尝试次数
                                                currentSlotIndex = 0; // 重置槽位索引
                                                // 在关闭箱子后发送#litematica
                                                mc.player.networkHandler.sendChatMessage("#litematica");
                                                // 强制离开箱子交互范围，避免再次打开
                                                mc.player.setPosition(targetX, targetY, targetZ + 2);
                                                return; // 添加return语句，确保关闭箱子后不会再次打开
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 如果差值超过1，则重置箱子打开状态
                                hasOpenedChest = false; // 重置箱子打开状态
                                chestOpenAttempts = 0; // 重置尝试次数
                                currentSlotIndex = 0; // 重置槽位索引
                            }

                            // 检测玩家是否保持不动半分钟
                            if (currentPos.equals(lastPlayerPos)) {
                                if (System.currentTimeMillis() - lastPlayerMoveTime >= 30000 && System.currentTimeMillis() - lastGotoTime >= 30000) {
                                    mc.player.networkHandler.sendChatMessage("#goto " + coordinate);
                                    lastGotoTime = System.currentTimeMillis();
                                }
                            } else {
                                lastPlayerMoveTime = System.currentTimeMillis();
                                lastPlayerPos = currentPos;
                            }

                        } catch (NumberFormatException e) {
                            // 坐标格式错误，忽略
                        }
                    }
                }
            }

            // 自动进食逻辑
            if (isAutoEating) {
                if (mc.currentScreen != null) {
                    return;
                }

                Vec3d currentPos = mc.player.getPos();
                if (!hasUpdatedPosition) {
                    lastPlayerPos = currentPos;
                    hasUpdatedPosition = true;
                }
                if (!currentPos.equals(lastPlayerPos)) {
                    int foodLevel = mc.player.getHungerManager().getFoodLevel();
                    if (foodLevel <= 10) {
                        if (eatStartTime == 0) {
                            for (int i = 0; i < 9; i++) {
                                var stack = mc.player.getInventory().getStack(i);
                                if (stack.getItem() == Items.COOKED_PORKCHOP) {
                                    mc.player.getInventory().selectedSlot = i;
                                    mc.options.useKey.setPressed(true);
                                    eatStartTime = System.currentTimeMillis();
                                    break;
                                }
                            }
                        } else if (System.currentTimeMillis() - eatStartTime >= 6000) {
                            mc.options.useKey.setPressed(false);
                            eatStartTime = 0;
                        }
                    } else if (foodLevel >= 20) {
                        mc.options.useKey.setPressed(false);
                        eatStartTime = 0;
                    }
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                return;
            }

            if (screen instanceof net.minecraft.client.gui.screen.option.OptionsScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.of("ProgrammableChat"), button -> mc.setScreen(new AutoMessageScreen()))
                    .dimensions(10, scaledHeight - 30, 150, 20).build());
            }
        });
    }

    public static void setMessage(String msg) {
        message = msg;
        saveConfig();
    }

    public static void setInterval(int intervalSeconds) {
        interval = intervalSeconds;
        saveConfig();
    }

    public static void setCoordinate(String coord) {
        coordinate = coord;
        saveConfig();
    }

    public static void startSending() {
        isSending = true;
        saveConfig();
    }

    public static void stopSending() {
        isSending = false;
        saveConfig();
    }

    public static void setAutoEating(boolean autoEating) {
        isAutoEating = autoEating;
        hasUpdatedPosition = false;
        saveConfig();
    }

    public static void setAutoBuilding(boolean autoBuilding) {
        isAutoBuilding = autoBuilding;
        if (autoBuilding && MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage("#litematica");
            saveConfig(); // 确保坐标保存到JSON
        } else if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage("#stop");
            saveConfig(); // 确保状态保存到JSON
        }
    }

    public static String getMessage() {
        return message;
    }

    public static int getInterval() {
        return interval;
    }

    public static boolean isSending() {
        return isSending;
    }

    public static boolean isAutoEating() {
        return isAutoEating;
    }

    public static boolean isAutoBuilding() {
        return isAutoBuilding;
    }

    private static void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                Config config = GSON.fromJson(reader, Config.class);
                message = config.message;
                interval = config.interval;
                coordinate = config.coordinate;
                isSending = config.isSending;
                isAutoEating = config.isAutoEating;
                isAutoBuilding = config.isAutoBuilding;
            } catch (IOException e) {
                net.minecraft.util.Util.throwOrPause(e);
            }
        }
    }

    private static void saveConfig() {
        Config config = new Config(message, interval, coordinate, isSending, isAutoEating, isAutoBuilding);
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            net.minecraft.util.Util.throwOrPause(e);
        }
    }

    private static class Config {
        String message;
        int interval;
        String coordinate;
        boolean isSending;
        boolean isAutoEating;
        boolean isAutoBuilding;

        Config(String message, int interval, String coordinate, boolean isSending, boolean isAutoEating, boolean isAutoBuilding) {
            this.message = message;
            this.interval = interval;
            this.coordinate = coordinate;
            this.isSending = isSending;
            this.isAutoEating = isAutoEating;
            this.isAutoBuilding = isAutoBuilding;
        }
    }
}