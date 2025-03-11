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
    private static long lastGotoTime = 0;
    private static boolean hasOpenedChest = false;
    private static int chestOpenAttempts = 0;
    private static long lastChestOpenAttemptTime = 0;
    private static String tempCoordinate = ""; // 新增：临时记录坐标
    private static long lastLitematicaTime = 0; // 新增：独立的 #litematica 指令计时变量
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "programmablechat.json");

    public static String getCoordinate() {
        return coordinate;
    }

    private static Vec3d parseCoordinate(String coordinate) {
        String[] parts = coordinate.split(" ");
        if (parts.length == 3) {
            try {
                double targetX = Double.parseDouble(parts[0]);
                double targetY = Double.parseDouble(parts[1]);
                double targetZ = Double.parseDouble(parts[2]);
                return new Vec3d(targetX, targetY, targetZ);
            } catch (NumberFormatException e) {
                System.out.println("坐标格式错误: " + e.getMessage());
            }
        }
        return null;
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

            Vec3d currentPos = mc.player.getPos();

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
                if (mc.player == null) {
                    return;
                }
                if (coordinate != null && !coordinate.isEmpty()) {
                    Vec3d targetPos = parseCoordinate(coordinate);
                    if (targetPos != null) {
                        // 判断玩家实时坐标与预设坐标的差值
                        double deltaX = Math.abs(currentPos.x - targetPos.x);
                        double deltaY = Math.abs(currentPos.y - targetPos.y);
                        double deltaZ = Math.abs(currentPos.z - targetPos.z);

                        // 判断玩家是否在预设坐标2格范围外
                        if (deltaX > 2.0 || deltaY > 2.0 || deltaZ > 2.0) {
                            // 再判断玩家是否保持不动20秒
                            if (currentPos.equals(lastPlayerPos)) {
                                if (System.currentTimeMillis() - lastPlayerMoveTime >= 20000 && lastGotoTime == 0) {
                                    // 记录当前玩家坐标，确保格式为 x y z
                                    tempCoordinate = formatCoordinate(currentPos);
                                    // 发送 #goto 预设坐标
                                    mc.player.networkHandler.sendChatMessage("#goto " + coordinate);
                                    //lastGotoTime = System.currentTimeMillis(); // 记录发送时间，确保功能正常
                                }
                            } else {
                                lastPlayerMoveTime = System.currentTimeMillis();
                                lastPlayerPos = currentPos;
                            }

                            // 如果差值超过2，则重置箱子打开状态
                            hasOpenedChest = false;
                            chestOpenAttempts = 0;
                            lastGotoTime = 0; // 重置 lastGotoTime
                        } else if (deltaX <= 2.0 && deltaY <= 2.0 && deltaZ <= 2.0) {
                            // 判断玩家是否在预设坐标2格范围内保持不动超过10秒钟
                            if (currentPos.equals(lastPlayerPos)) {
                                if (System.currentTimeMillis() - lastPlayerMoveTime >= 10000 && lastGotoTime == 0) {
                                    // 发送 #goto 临时坐标
                                    mc.player.networkHandler.sendChatMessage("#goto " + tempCoordinate);
                                    lastGotoTime = System.currentTimeMillis(); // 记录发送时间
                                }
                            } else {
                                lastPlayerMoveTime = System.currentTimeMillis();
                                lastPlayerPos = currentPos;
                            }

                            // 如果x和z差值不超过1，y差值不超过1，则尝试与箱子交互
                            if (deltaX <= 1.0 && deltaY <= 1.0 && deltaZ <= 1.0) {
                                if (!hasOpenedChest) {
                                    if (System.currentTimeMillis() - lastChestOpenAttemptTime >= 500 && chestOpenAttempts < 1) {
                                        BlockPos chestPos = new BlockPos((int) targetPos.x, (int) targetPos.y - 1, (int) targetPos.z);
                                        BlockHitResult hitResult = new BlockHitResult(new Vec3d(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5), Direction.UP, chestPos, false);
                                        if (mc.interactionManager != null) {
                                            mc.interactionManager.interactBlock(mc.player, net.minecraft.util.Hand.MAIN_HAND, hitResult);
                                        }
                                        chestOpenAttempts++;
                                        lastChestOpenAttemptTime = System.currentTimeMillis();
                                        hasOpenedChest = true;
                                    }
                                }

                                // 独立的 if 语句，确保在箱子打开后执行拿取物品的逻辑
                                if (mc.player.currentScreenHandler != null && mc.interactionManager != null) {
                                    // 增加延迟，确保箱子完全打开
                                    if (System.currentTimeMillis() - lastChestOpenAttemptTime >= 1000) { // 延迟增加到1000毫秒
                                        // 在箱子打开后开始进行物品转移
                                        // 定义每次转移的间隔时间（例如100毫秒）
                                        long transferInterval = 100;
                                        // 计算当前可以转移的物品槽位
                                        if (System.currentTimeMillis() - lastChestOpenAttemptTime >= transferInterval) {
                                            // 获取箱子的槽位数量，固定为27个槽位
                                            int chestSlotCount = 27; // 固定为27个槽位

                                            // 按照箱子槽位顺序将物品转移到背包的对应槽位
                                            for (int chestSlotIndex = 0; chestSlotIndex < chestSlotCount; chestSlotIndex++) {
                                                // 检查槽位索引是否在有效范围内
                                                if (chestSlotIndex < mc.player.currentScreenHandler.slots.size()) {
                                                    var chestStack = mc.player.currentScreenHandler.getSlot(chestSlotIndex).getStack();
                                                    if (!chestStack.isEmpty()) {
                                                        // 计算对应的背包槽位，从54开始
                                                        int playerSlotIndex = 54 + chestSlotIndex; // 从背包的第54个槽位开始
                                                        // 检查背包槽位索引是否在有效范围内
                                                        if (playerSlotIndex < mc.player.currentScreenHandler.slots.size()) {
                                                            // 模拟左键点击箱子槽位，再左键点击背包槽位，将物品转移到背包
                                                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, chestSlotIndex, 0, SlotActionType.PICKUP, mc.player);
                                                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, playerSlotIndex, 0, SlotActionType.PICKUP, mc.player);
                                                        }
                                                    }
                                                }
                                            }

                                            // 转移完成后关闭箱子界面并发送 #goto 临时坐标
                                            if (mc.player != null) {
                                                // 明确关闭箱子界面
                                                mc.player.closeHandledScreen(); // 关闭箱子界面
                                                // 增加延迟，确保箱子完全关闭
                                                if (System.currentTimeMillis() - lastChestOpenAttemptTime >= 1000) {
                                                    // 发送 #goto 临时坐标，确保格式正确
                                                    mc.player.networkHandler.sendChatMessage("#goto " + tempCoordinate);
                                                    lastGotoTime = System.currentTimeMillis(); // 记录发送时间
                                                }
                                            }

                                            // 设置为已打开箱子
                                            hasOpenedChest = true;
                                            chestOpenAttempts = 0;
                                            lastChestOpenAttemptTime = System.currentTimeMillis();
                                            return; // 确保退出循环，防止程序再次尝试打开箱子
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 判断玩家是否到达临时坐标三格范围内
                Vec3d tempPos = parseCoordinate(tempCoordinate);
                if (tempPos != null) {
                    double tempDeltaX = Math.abs(currentPos.x - tempPos.x);
                    double tempDeltaY = Math.abs(currentPos.y - tempPos.y);
                    double tempDeltaZ = Math.abs(currentPos.z - tempPos.z);

                    if (tempDeltaX <= 3.0 && tempDeltaY <= 3.0 && tempDeltaZ <= 3.0) {
                        if (lastLitematicaTime == 0) { // 确保只发送一次
                            mc.player.networkHandler.sendChatMessage("#litematica");
                            lastLitematicaTime = System.currentTimeMillis(); // 记录发送时间
                        }
                    } else {
                        // 重置 lastLitematicaTime 当玩家离开临时坐标范围
                        lastLitematicaTime = 0;
                    }
                }
            }

            // 自动进食逻辑
            if (isAutoEating) {
                if (mc.currentScreen != null) {
                    return;
                }

                if (!hasUpdatedPosition) {
                    lastPlayerPos = currentPos;
                    hasUpdatedPosition = true;
                }
                if (!currentPos.equals(lastPlayerPos)) {
                    if (mc.player != null && mc.player.getHungerManager() != null) { // 增加空值检查
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
                System.out.println("加载配置文件失败: " + e.getMessage());
            }
        }
    }

    private static void saveConfig() {
        Config config = new Config(message, interval, coordinate, isSending, isAutoEating, isAutoBuilding);
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.out.println("保存配置文件失败: " + e.getMessage());
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

    // 修改临时坐标格式，确保x、y、z之间有空格
    private static String formatCoordinate(Vec3d pos) {
        return String.format("%.1f %.1f %.1f", pos.x, pos.y, pos.z);
    }

}
