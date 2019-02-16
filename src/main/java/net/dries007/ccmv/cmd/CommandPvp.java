package net.dries007.ccmv.cmd;

import net.dries007.ccmv.Ccmv;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * @author Dries007
 */
public class CommandPvp extends CommandBase
{
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return sender instanceof EntityPlayer;
    }

    @Override
    public String getName()
    {
        return "pvp";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/pvp to toggle pvp combat";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        NBTTagCompound root = getCommandSenderAsPlayer(sender).getEntityData();
        NBTTagCompound persist = root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        boolean pvp = persist.getBoolean(Ccmv.MOD_ID);
        if (pvp)
        {
            root.setInteger(Ccmv.DELAY, 20 * 15);
            sender.sendMessage(new TextComponentString("Pvp will be disabled in 5 seconds.").setStyle(new Style().setColor(TextFormatting.RED)));
        }
        else
        {
            persist.setBoolean(Ccmv.MOD_ID, true);
            root.setTag(EntityPlayer.PERSISTED_NBT_TAG, persist);
            sender.sendMessage(new TextComponentString("Pvp is now enabled!").setStyle(new Style().setColor(TextFormatting.GREEN)));
        }
    }
}
