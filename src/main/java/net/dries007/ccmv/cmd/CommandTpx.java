/*
 * Copyright (c) 2014-2016, Dries007 & DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.dries007.ccmv.cmd;

import net.dries007.ccmv.BlockPosDim;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CommandTpx extends CommandBase
{
    @Override
    public String getName()
    {
        return "tpx";
    }

    @Override
    public String getUsage(ICommandSender icommandsender)
    {
        return "/tpx [entity] <dim [x y z]|target>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        try
        {
            if (args.length < 1) throw new WrongUsageException(getUsage(sender));

            if (args.length == 1)
            {
                Entity toTP = getCommandSenderAsPlayer(sender);
                try
                {
                    int target = parseInt(args[0]);
                    WorldServer world = server.getWorld(target);
                    //noinspection ConstantConditions
                    if (world == null) throw new CommandException("Dim " + target + " does not exist.");
                    BlockPosDim pos = teleport(toTP, world.getSpawnPoint(), target);
                    sender.sendMessage(new TextComponentString("Teleported ").appendSibling(toTP.getDisplayName()).appendText(" to ").appendSibling(pos.toClickableChatString()));
                }
                catch (NumberInvalidException e)
                {
                    EntityPlayerMP target = getPlayer(server, sender, args[0]);
                    if (toTP == target) throw new CommandException("You can't TP to you.");
                    BlockPosDim pos = teleport(toTP, target.getPosition(), target.dimension);
                    sender.sendMessage(new TextComponentString("Teleported ").appendSibling(toTP.getDisplayName()).appendText(" to ").appendSibling(pos.toClickableChatString()));
                }

                return;
            }

            if (args.length == 2)
            {
                Entity toTP = getEntity(server, sender, args[0]);
                try
                {
                    int target = parseInt(args[1]);
                    WorldServer world = server.getWorld(target);
                    //noinspection ConstantConditions
                    if (world == null) throw new CommandException("Dim " + target + " does not exist.");
                    BlockPosDim pos = teleport(toTP, world.getSpawnPoint(), target);
                    sender.sendMessage(new TextComponentString("Teleported ").appendSibling(toTP.getDisplayName()).appendText(" to ").appendSibling(pos.toClickableChatString()));
                }
                catch (NumberInvalidException e)
                {
                    EntityPlayerMP target = getPlayer(server, sender, args[1]);
                    if (toTP == target) throw new CommandException("You can't TP to you.");
                    BlockPosDim pos = teleport(toTP, target.getPosition(), target.dimension);
                    sender.sendMessage(new TextComponentString("Teleported ").appendSibling(toTP.getDisplayName()).appendText(" to ").appendSibling(pos.toClickableChatString()));
                }

                return;
            }

            if (args.length == 4 || args.length == 5)
            {
                int i = 0;
                Entity toTP = args.length == 5 ? getEntity(server, sender, args[i++]) : getCommandSenderAsPlayer(sender);

                int target = parseInt(args[i++]);
                int x = parseInt(args[i++], -30000000, 30000000);
                int y = parseInt(args[i++], -30000000, 30000000);
                int z = parseInt(args[i++], -30000000, 30000000);
                WorldServer world = server.getWorld(target);
                //noinspection ConstantConditions
                if (world == null) throw new CommandException("Dim " + target + " does not exist.");
                BlockPosDim pos = teleport(toTP, new BlockPos(x, y, z), target);
                sender.sendMessage(new TextComponentString("Teleported ").appendSibling(toTP.getDisplayName()).appendText(" to ").appendSibling(pos.toClickableChatString()));

                return;
            }

            throw new WrongUsageException("Number of args doesn't match up. 1, 2, 4 or 5 allowed.");
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    private BlockPosDim teleport(Entity target, BlockPos coord, int dim) throws CommandException
    {
        if (target instanceof EntityPlayerMP)
        {
            if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(target, dim)) throw new CommandException("Dim change denied.");
            //noinspection ConstantConditions
            target.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) target, dim, FakeTeleporter.INSTANCE);
            Set<SPacketPlayerPosLook.EnumFlags> set = EnumSet.noneOf(SPacketPlayerPosLook.EnumFlags.class);
            target.dismountRidingEntity();
            ((EntityPlayerMP)target).connection.setPlayerLocation(coord.getX(), coord.getY(), coord.getZ(), target.rotationYaw, target.rotationPitch, set);
        }
        else
        {
            if (dim != target.dimension)
            {
                target = target.changeDimension(dim);
                if (target == null) throw new CommandException("Entity became null??");
            }
            if (dim != target.dimension) throw new CommandException("Dim change denied.");
            target.setLocationAndAngles(coord.getX(), coord.getY(), coord.getZ(), target.rotationYaw, target.rotationPitch);
        }
        return new BlockPosDim(coord.getX(), coord.getY(), coord.getZ(), dim);
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public boolean isUsernameIndex(final String[] args, final int userIndex)
    {
        return userIndex == 0 || userIndex == 1;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
    }

    @SuppressWarnings({"NullableProblems", "ConstantConditions"})
    private static class FakeTeleporter extends Teleporter
    {
        private static final FakeTeleporter INSTANCE = new FakeTeleporter();

        private FakeTeleporter()
        {
            super(FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0]);
        }

        @Override
        public void placeInPortal( Entity entityIn, float rotationYaw)
        {
            // Nop
        }

        @Override
        public boolean placeInExistingPortal(Entity entityIn, float rotationYaw)
        {
            return false;
        }

        @Override
        public boolean makePortal(Entity entityIn)
        {
            return false;
        }

        @Override
        public void removeStalePortalLocations(long worldTime)
        {
            // Nop
        }
    }
}
