package AsagiriBeta.programmablechat.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AutoMessageMod implements ModInitializer {
    private static String message = "";
    private static int interval = 0;
    private static long lastSentTime = 0;
    private static boolean isSending = false;

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AutoMessageCommand.register(dispatcher);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isSending && interval > 0 && System.currentTimeMillis() - lastSentTime >= interval * 1000L) {
                if (!message.isEmpty()) {
                    // 检测消息是否以/开头，如果是则视为指令，否则视为聊天信息
                    if (message.startsWith("/")) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(message.substring(1));
                    } else {
                        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(message);
                    }
                    lastSentTime = System.currentTimeMillis();
                }
            }
        });

        // 修改UI初始化逻辑，将按钮添加到选项界面
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screen.option.OptionsScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.of("ProgrammableChat"), button -> {
                    MinecraftClient.getInstance().setScreen(new AutoMessageScreen());
                }).dimensions(10, scaledHeight - 30, 150, 20).build());
            }
        });
    }

    public static void setMessage(String msg) {
        message = msg;
    }

    public static void setInterval(int intervalSeconds) {
        interval = intervalSeconds;
    }

    public static void startSending() {
        isSending = true;
    }

    public static void stopSending() {
        isSending = false;
    }

    public static boolean isSending() {
        return isSending;
    }
}