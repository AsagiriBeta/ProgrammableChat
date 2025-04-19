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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoMessageMod implements ModInitializer {
    private static String message = "";
    private static int interval = 0;
    private static String coordinate = "";
    private static long lastSentTime = 0;
    private static boolean isSending = false;
    private static boolean isAutoEating = false;
    private static boolean isAutoBuilding = false;
    private static long eatStartTime = 0;
    private static String searchText = ""; // 新增：用于存储搜索框的临时字符信息
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "programmablechat.json");
    private static final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Config-Save-Thread");
        t.setDaemon(true);
        return t;
    });

    public static String getCoordinate() {
        return coordinate;
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

            // 调用自动建造逻辑
            AutoBuildHandler.handleTick(mc);

            // 自动进食逻辑
            if (isAutoEating) {
                if (mc.currentScreen != null) {
                    return;
                }

                if (mc.player != null && mc.player.getHungerManager() != null) { // 增加空值检查
                    int foodLevel = mc.player.getHungerManager().getFoodLevel();
                    if (foodLevel <= 10) {
                        if (eatStartTime == 0) {
                            // 发送 #pause 命令暂停 Baritone 任务
                            mc.player.networkHandler.sendChatMessage("#pause");
                            for (int i = 0; i < 9; i++) {
                                var stack = mc.player.getInventory().getStack(i);
                                // 支持更多食物类型
                                if (stack.getItem() == Items.COOKED_PORKCHOP) {
                                    mc.player.getInventory().selectedSlot = i;
                                    mc.options.useKey.setPressed(true);
                                    eatStartTime = System.currentTimeMillis();
                                    break;
                                }
                            }
                        }
                    }
                    if (eatStartTime != 0 && foodLevel >= 20) {
                        mc.options.useKey.setPressed(false);
                        mc.player.networkHandler.sendChatMessage("#resume");
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

        // 创建 restp 文件夹
        Path restpPath = Paths.get("restp");
        if (!Files.exists(restpPath)) {
            try {
                Files.createDirectory(restpPath);
            } catch (IOException e) {
                System.out.println("创建 restp 文件夹失败: " + e.getMessage());
            }
        }
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
        AutoBuildHandler.setCoordinate(coord); // 调用 AutoBuildHandler 的 setCoordinate 方法
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
        saveConfig();
    }

    public static void setAutoBuilding(boolean autoBuilding) {
        isAutoBuilding = autoBuilding;
        AutoBuildHandler.setActive(autoBuilding);
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

    public static String getSearchText() {
        return searchText;
    }

    public static void setSearchText(String text) {
        searchText = text;
        saveConfig();
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
                searchText = config.searchText; // 新增：加载搜索框的临时字符信息
            } catch (IOException e) {
                System.out.println("加载配置文件失败: " + e.getMessage());
            }
        }
    }

    public static void saveConfig() {
        saveExecutor.execute(() -> {
            Config config = new Config(message, interval, coordinate, isSending, isAutoEating, isAutoBuilding, searchText);
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                System.out.println("保存配置文件失败: " + e.getMessage());
            }
        });
    }

    private static class Config {
        String message;
        int interval;
        String coordinate;
        boolean isSending;
        boolean isAutoEating;
        boolean isAutoBuilding;
        String searchText; // 新增：用于存储搜索框的临时字符信息

        Config(String message, int interval, String coordinate, boolean isSending, boolean isAutoEating, boolean isAutoBuilding, String searchText) {
            this.message = message;
            this.interval = interval;
            this.coordinate = coordinate;
            this.isSending = isSending;
            this.isAutoEating = isAutoEating;
            this.isAutoBuilding = isAutoBuilding;
            this.searchText = searchText; // 新增：初始化搜索框的临时字符信息
        }
    }
}
