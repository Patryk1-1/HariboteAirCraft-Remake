package com.haribote.hac.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class KeyBindings {
    private static final String CATEGORY = "key.category.hac";

    public static KeyBinding keyForward;
    public static KeyBinding keyBackward;
    public static KeyBinding keyLeft;
    public static KeyBinding keyRight;
    public static KeyBinding keyStrafeLeft;
    public static KeyBinding keyStrafeRight;
    public static KeyBinding keyUp;
    public static KeyBinding keyDown;
    public static KeyBinding keyStop;
    public static KeyBinding keyRestore;

    public static void init() {
        keyForward     = new KeyBinding("key.hac.forward", Keyboard.KEY_W, CATEGORY);
        keyBackward    = new KeyBinding("key.hac.backward", Keyboard.KEY_S, CATEGORY);
        keyLeft        = new KeyBinding("key.hac.left", Keyboard.KEY_A, CATEGORY);
        keyRight       = new KeyBinding("key.hac.right", Keyboard.KEY_D, CATEGORY);
        keyStrafeLeft  = new KeyBinding("key.hac.strafe_left", Keyboard.KEY_U, CATEGORY);
        keyStrafeRight = new KeyBinding("key.hac.strafe_right", Keyboard.KEY_O, CATEGORY);
        keyUp          = new KeyBinding("key.hac.up", Keyboard.KEY_SPACE, CATEGORY);
        keyDown        = new KeyBinding("key.hac.down", Keyboard.KEY_C, CATEGORY);
        keyStop        = new KeyBinding("key.hac.stop", Keyboard.KEY_X, CATEGORY);
        keyRestore     = new KeyBinding("key.hac.restore", Keyboard.KEY_R, CATEGORY);

        ClientRegistry.registerKeyBinding(keyForward);
        ClientRegistry.registerKeyBinding(keyBackward);
        ClientRegistry.registerKeyBinding(keyLeft);
        ClientRegistry.registerKeyBinding(keyRight);
        ClientRegistry.registerKeyBinding(keyStrafeLeft);
        ClientRegistry.registerKeyBinding(keyStrafeRight);
        ClientRegistry.registerKeyBinding(keyUp);
        ClientRegistry.registerKeyBinding(keyDown);
        ClientRegistry.registerKeyBinding(keyStop);
        ClientRegistry.registerKeyBinding(keyRestore);
    }
}
