package com.haribote.hac.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public class CommandHariboteAirCraft extends CommandBase {
    @Override
    public String getName() {
        return "HariboteAirCraft";
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("hariboteaircraft");
        aliases.add("hac");
        return aliases;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/HariboteAirCraft";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Accessible by everyone without OP
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        sender.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "[HariboteAirCraft] " + 
                TextFormatting.YELLOW + "Autor Remake: " + 
                TextFormatting.AQUA + "Patryk1_1"
        ));
    }
}
