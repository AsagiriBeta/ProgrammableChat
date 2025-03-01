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
    private static long lastSentTime = 0;
    private static boolean isSending = false;
    private static boolean isAutoEating = false;
    private static Vec3d lastPlayerPos = Vec3d.ZERO;
    private static boolean hasUpdatedPosition = false;
    private static long eatStartTime = 0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "programmablechat.json");

    @Override
    public void onInitialize() {
        loadConfig();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> AutoMessageCommand.register(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isSending && interval > 0 && System.currentTimeMillis() - lastSentTime >= interval * 1000L) {
                if (!message.isEmpty() && MinecraftClient.getInstance().player != null) {
                    if (message.startsWith("/")) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(message.substring(1));
                    } else {
                        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(message);
                    }
                    lastSentTime = System.currentTimeMillis();
                }
            }

            // 自动进食逻辑
            if (isAutoEating && MinecraftClient.getInstance().player != null) {
                if (MinecraftClient.getInstance().currentScreen != null) {
                    return;
                }

                Vec3d currentPos = MinecraftClient.getInstance().player.getPos();
                if (!hasUpdatedPosition) {
                    lastPlayerPos = currentPos;
                    hasUpdatedPosition = true;
                }
                if (!currentPos.equals(lastPlayerPos)) {
                    int foodLevel = MinecraftClient.getInstance().player.getHungerManager().getFoodLevel();
                    if (foodLevel <= 10) {
                        if (eatStartTime == 0) {
                            for (int i = 0; i < 9; i++) {
                                var stack = MinecraftClient.getInstance().player.getInventory().getStack(i);
                                if (stack.getItem() == Items.COOKED_PORKCHOP) {
                                    MinecraftClient.getInstance().player.getInventory().selectedSlot = i;
                                    MinecraftClient.getInstance().options.useKey.setPressed(true);
                                    eatStartTime = System.currentTimeMillis();
                                    break;
                                }
                            }
                        } else if (System.currentTimeMillis() - eatStartTime >= 6000) {
                            MinecraftClient.getInstance().options.useKey.setPressed(false);
                            eatStartTime = 0;
                        }
                    } else if (foodLevel >= 20) {
                        MinecraftClient.getInstance().options.useKey.setPressed(false);
                        eatStartTime = 0;
                    }
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screen.option.OptionsScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.of("ProgrammableChat"), button -> MinecraftClient.getInstance().setScreen(new AutoMessageScreen()))
                    .dimensions(10, scaledHeight - 30, 150, 20).build());
            }
        });
    }

    public static void setMessage(String msg) {
        message = msg;
        saveConfig(); // 自动保存设置
    }

    public static void setInterval(int intervalSeconds) {
        interval = intervalSeconds;
        saveConfig(); // 自动保存设置
    }

    public static void startSending() {
        isSending = true;
        saveConfig(); // 自动保存设置
    }

    public static void stopSending() {
        isSending = false;
        saveConfig(); // 自动保存设置
    }

    public static void setAutoEating(boolean autoEating) {
        isAutoEating = autoEating;
        hasUpdatedPosition = false;
        saveConfig(); // 自动保存设置
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

    private static void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                Config config = GSON.fromJson(reader, Config.class);
                message = config.message;
                interval = config.interval;
                isSending = config.isSending;
                isAutoEating = config.isAutoEating;
            } catch (IOException e) {
                // 使用Minecraft的日志系统记录异常
                net.minecraft.util.Util.throwOrPause(e);
            }
        }
    }

    private static void saveConfig() {
        Config config = new Config(message, interval, isSending, isAutoEating);
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            // 使用Gson的toJson方法确保特殊字符和转义字符被正确处理
            GSON.toJson(config, writer);
        } catch (IOException e) {
            // 使用Minecraft的日志系统记录异常
            net.minecraft.util.Util.throwOrPause(e);
        }
    }

    private static class Config {
        String message;
        int interval;
        boolean isSending;
        boolean isAutoEating;

        Config(String message, int interval, boolean isSending, boolean isAutoEating) {
            this.message = message;
            this.interval = interval;
            this.isSending = isSending;
            this.isAutoEating = isAutoEating;
        }
    }
}