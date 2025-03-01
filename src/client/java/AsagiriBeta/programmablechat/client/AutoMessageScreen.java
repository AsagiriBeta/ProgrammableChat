package AsagiriBeta.programmablechat.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class AutoMessageScreen extends Screen {
    private TextFieldWidget messageField;
    private TextFieldWidget intervalField;
    private final String savedMessage;
    private final String savedInterval;

    protected AutoMessageScreen() {
        super(Text.of("Auto Message Settings"));
        savedMessage = AutoMessageMod.getMessage();
        savedInterval = String.valueOf(AutoMessageMod.getInterval());
    }

    @Override
    protected void init() {
        // 初始化消息输入框
        this.messageField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.of("消息/指令："));
        this.messageField.setMaxLength(256);
        this.messageField.setText(savedMessage);

        // 初始化间隔输入框
        this.intervalField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.of("间隔时间（秒）："));
        this.intervalField.setMaxLength(10);
        this.intervalField.setText(savedInterval);

        // 添加输入框到界面
        this.addSelectableChild(this.messageField);
        this.addSelectableChild(this.intervalField);

        // 添加Start/Stop按钮
        ButtonWidget startStopButton = ButtonWidget.builder(Text.of(AutoMessageMod.isSending() ? "间隔发送已打开" : "打开间隔发送"), button -> {
            if (AutoMessageMod.isSending()) {
                AutoMessageMod.stopSending();
                button.setMessage(Text.of("打开间隔发送"));
            } else {
                AutoMessageMod.startSending();
                button.setMessage(Text.of("间隔发送已打开"));
            }
        }).dimensions(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build();
        this.addDrawableChild(startStopButton);

        // 添加自动进食按钮
        ButtonWidget autoEatButton = ButtonWidget.builder(Text.of(AutoMessageMod.isAutoEating() ? "自动进食已打开" : "打开自动进食"), button -> {
            if (AutoMessageMod.isAutoEating()) {
                AutoMessageMod.setAutoEating(false);
                button.setMessage(Text.of("打开自动进食"));
            } else {
                AutoMessageMod.setAutoEating(true);
                button.setMessage(Text.of("自动进食已打开"));
            }
        }).dimensions(this.width / 2 - 100, this.height / 2 + 70, 200, 20).build();
        this.addDrawableChild(autoEatButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.messageField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.messageField);
            return true;
        }
        if (this.intervalField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.intervalField);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // 绘制文本框的提示文本
        context.drawTextWithShadow(this.textRenderer, Text.of("消息/指令："), this.width / 2 - 100, this.height / 2 - 60, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.of("间隔时间（秒）："), this.width / 2 - 100, this.height / 2 - 30, 0xFFFFFF);

        // 渲染输入框
        this.messageField.render(context, mouseX, mouseY, delta);
        this.intervalField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        // 保存设置到JSON文件
        AutoMessageMod.setMessage(this.messageField.getText());
        AutoMessageMod.setInterval(Integer.parseInt(this.intervalField.getText()));
        super.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理键盘事件，确保文本框能够接收输入
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return this.messageField.keyPressed(keyCode, scanCode, modifiers) || 
               this.intervalField.keyPressed(keyCode, scanCode, modifiers) || 
               super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // 处理字符输入事件，确保文本框能够显示输入的字符
        return this.messageField.charTyped(chr, modifiers) || 
               this.intervalField.charTyped(chr, modifiers) || 
               super.charTyped(chr, modifiers);
    }
}