package wtf.myer.nbtviewer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class NBTViewer implements ModInitializer {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private ItemStack lastItem;
    @Override
    public void onInitialize() {
        KeyBinding showNBT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Show NBT Data",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "NBTViewer"
        ));
        KeyBinding copyToClipboard = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Copy Item NBT To Clipboard",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "NBTViewer"
        ));
        // held item
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (copyToClipboard.wasPressed()) {
                if (client.player != null) {
                    final ItemStack stack = client.player.getMainHandStack();
                    if (stack.getTag() == null) return;
                    if (isLastItem(stack)) return;
                    client.player.sendMessage(new LiteralText("Copied NBT Data of minecraft:" + stack.toString().split(" ")[1] + " to clipboard"), false);
                    StringSelection selection = new StringSelection(this.format(stack.getTag().toString()));
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }
            }
        });
        // in inventory (show)
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if ((!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), KeyBindingHelper.getBoundKeyOf(showNBT).getCode())) || (stack.getTag() == null)) return;
            String nbt = format(stack.getTag().toString());
            lines.add(new LiteralText(""));
            for (String s : nbt.split("\n")) {
                lines.add(new LiteralText(s));
            }
        });
        // in inventory (copy to clipboard)
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if ((!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), KeyBindingHelper.getBoundKeyOf(copyToClipboard).getCode())) || (stack.getTag() == null)) return;
            if (isLastItem(stack)) return;
            if (client.player != null) client.player.sendMessage(new LiteralText("Copied NBT Data of minecraft:" + stack.toString().split(" ")[1] + " to clipboard"), false);
            StringSelection selection = new StringSelection(format(stack.getTag().toString()));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });
    }

    public String format(String nbt) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        for (char c : nbt.toCharArray()) {
            boolean newline = false;
            if (c == '{' || c == '[') {
                indent++;
                newline = true;
            } else if (c == '}' || c == ']') {
                indent--;
                sb.append("\n");
                for (int i = 0; i < indent; i++) sb.append("  ");
            } else if (c == ',') newline = true;
            else if (c == '\"') sb.append(Formatting.RESET.toString()).append(Formatting.GRAY);
            else if (c == ':') {
                sb.append(c).append(" ");
                continue;
            }
            sb.append(c);
            if (newline) {
                sb.append("\n");
                for (int i = 0; i < indent; i++) sb.append("  ");
            }
        } return sb.toString();
    }
    public boolean isLastItem(ItemStack item) {
        if (item == lastItem) return true;
        else {
            lastItem = item;
            return false;
        }
    }
}
