package net.dries007.ccmv;

import com.google.gson.JsonParseException;
import net.dries007.ccmv.cmd.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatisticsManagerServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;

@Mod(
        modid = Ccmv.MOD_ID,
        name = Ccmv.MOD_NAME,
        version = Ccmv.VERSION,
        acceptableRemoteVersions = "*"
)
public class Ccmv
{
    public static final Random RANDOM = new Random();

    public static final String MOD_ID = "ccmv";
    public static final String MOD_NAME = "CCMV";
    public static final String VERSION = "1.0.3";

    public static final String DELAY = MOD_ID + "delay";

    private Logger logger;
    private File loginMessageFile;
    private Configuration configuration;

    private boolean achievementFireworks;
    private boolean lilypad;
    private boolean endDisable;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        loginMessageFile = new File(event.getModConfigurationDirectory(), "loginmessage.txt");
        configuration = new Configuration(event.getSuggestedConfigurationFile());
        updateConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MOD_ID)) updateConfig();
    }

    @Mod.EventHandler
    public void onFMLServerStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandPvp());
        event.registerServerCommand(new CommandExplorers());
        event.registerServerCommand(new CommandFeed());
        event.registerServerCommand(new CommandFireworks());
        event.registerServerCommand(new CommandFly());
        event.registerServerCommand(new CommandGetUUID());
        event.registerServerCommand(new CommandGm());
        event.registerServerCommand(new CommandGod());
        event.registerServerCommand(new CommandHeal());
        event.registerServerCommand(new CommandInvSee());
        event.registerServerCommand(new CommandMem());
        event.registerServerCommand(new CommandPos());
        event.registerServerCommand(new CommandSmite());
        event.registerServerCommand(new CommandSpawn());
        event.registerServerCommand(new CommandTop());
        event.registerServerCommand(new CommandTps());
        event.registerServerCommand(new CommandTpx());
    }

    @SubscribeEvent
    public void onTickPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (!event.player.getEntityData().hasKey(DELAY)) return;
        NBTTagCompound root = event.player.getEntityData();
        int time = root.getInteger(DELAY);
        if (time > 0) root.setInteger(DELAY, time - 1);
        else
        {
            root.removeTag(DELAY);
            NBTTagCompound persist = root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            persist.setBoolean(Ccmv.MOD_ID, false);
            event.player.sendMessage(new TextComponentString("Pvp is now disabled.").setStyle(new Style().setColor(TextFormatting.GREEN)));
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event)
    {
        if (event.getTarget() instanceof EntityPlayer)
        {
            if (!event.getEntity().getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(MOD_ID)) event.setCanceled(true);
            if (!event.getTarget().getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(MOD_ID)) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event)
    {
        if (event.getEntity() instanceof EntityPlayer)
        {
            if (event.getSource().getTrueSource() instanceof EntityPlayer)
            {
                if (!event.getEntity().getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(MOD_ID)) event.setCanceled(true);
                if (!event.getSource().getTrueSource().getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(MOD_ID)) event.setCanceled(true);
            }
            if (event.getSource().getImmediateSource() instanceof EntityPlayer)
            {
                if (!event.getEntity().getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(MOD_ID)) event.setCanceled(true);
                if (!event.getSource().getImmediateSource().getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(MOD_ID)) event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event)
    {
        if (endDisable && event.getDimension() == 1) event.setCanceled(true);
    }

    @SubscribeEvent
    public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (loginMessageFile.exists())
        {
            try
            {
                for (String line : FileUtils.readLines(loginMessageFile))
                {
                    try
                    {
                        event.player.sendMessage(ITextComponent.Serializer.jsonToComponent(line));
                    }
                    catch (JsonParseException jsonparseexception)
                    {
                        event.player.sendMessage(new TextComponentString(line));
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        if (lilypad) lilypad(event.player);
    }

    @SubscribeEvent
    public void playerRespawnEvent(PlayerEvent.PlayerRespawnEvent event)
    {
        if (lilypad) lilypad(event.player);
    }

    @SubscribeEvent
    public void playerDeath(LivingDeathEvent event)
    {
        if (event.getEntityLiving() instanceof EntityPlayer)
        {
            TextComponentString posText = new TextComponentString("X: " + MathHelper.floor(event.getEntityLiving().posX) + " Y: " + MathHelper.floor(event.getEntityLiving().posY + 0.5d) + " Z: " + MathHelper.floor(event.getEntityLiving().posZ));
            try
            {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                if (!server.getCommandManager().getPossibleCommands(event.getEntityLiving()).contains(server.getCommandManager().getCommands().get("tp")))
                {
                    posText.setStyle(new Style().setItalic(true)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to teleport!")))
                            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + event.getEntityLiving().posX + " " + (event.getEntityLiving().posY + 0.5d) + " " + event.getEntityLiving().posZ)));
                }
            }
            catch (Exception ignored)
            {

            }

            event.getEntityLiving().sendMessage(new TextComponentString("You died at ").setStyle(new Style().setColor(TextFormatting.GRAY)).appendSibling(posText));
        }
    }

    public static void spawnRandomFireworks(Entity target, int rad, int rockets)
    {
        while (rockets-- > 0)
        {
            ItemStack itemStack = new ItemStack(Items.FIREWORKS);
            NBTTagCompound fireworks = new NBTTagCompound();
            NBTTagList explosions = new NBTTagList();

            int charges = 1 + RANDOM.nextInt(3);
            while (charges-- > 0)
            {
                NBTTagCompound explosion = new NBTTagCompound();

                if (RANDOM.nextBoolean()) explosion.setBoolean("Flicker", true);
                if (RANDOM.nextBoolean()) explosion.setBoolean("Trail", true);

                int[] colors = new int[1 + RANDOM.nextInt(3)];

                for (int i = 0; i < colors.length; i++)
                {
                    colors[i] = (RANDOM.nextInt(256) << 16) + (RANDOM.nextInt(256) << 8) + RANDOM.nextInt(256);
                }

                explosion.setIntArray("Colors", colors);
                explosion.setByte("Type", (byte) RANDOM.nextInt(5));

                if (RANDOM.nextBoolean())
                {
                    int[] fadeColors = new int[1 + RANDOM.nextInt(3)];

                    for (int i = 0; i < fadeColors.length; i++)
                    {
                        fadeColors[i] = (RANDOM.nextInt(256) << 16) + (RANDOM.nextInt(256) << 8) + RANDOM.nextInt(256);
                    }
                    explosion.setIntArray("FadeColors", fadeColors);
                }

                explosions.appendTag(explosion);
            }
            fireworks.setTag("Explosions", explosions);
            fireworks.setByte("Flight", (byte) (RANDOM.nextInt(2)));

            NBTTagCompound root = new NBTTagCompound();
            root.setTag("Fireworks", fireworks);
            itemStack.setTagCompound(root);
            target.getEntityWorld().spawnEntity(new EntityFireworkRocket(target.getEntityWorld(), target.posX + RANDOM.nextInt(rad) - rad / 2.0, target.posY, target.posZ + RANDOM.nextInt(rad) - rad / 2.0, itemStack));
        }
    }

    private void lilypad(EntityPlayer player)
    {
        World world = player.getEntityWorld();

        BlockPos blockPos = new BlockPos((int)(player.posX),(int)(player.posY),(int)(player.posZ));

        if (blockPos.getX() < 0) blockPos = blockPos.add(-1,0,0);
        if (blockPos.getZ() < 0) blockPos = blockPos.add(0,0,-1);

        int limiter = world.getActualHeight() * 2;

        while (world.getBlockState(blockPos).getMaterial() == Material.WATER && --limiter != 0) blockPos = blockPos.add(0,1,0);
        while (world.getBlockState(blockPos).getMaterial() == Material.AIR && --limiter != 0) blockPos = blockPos.add(0,-1,0);
        if (limiter == 0) return;
        if (world.getBlockState(blockPos).getMaterial() == Material.WATER)
        {
            world.setBlockState(blockPos.add(0,1,0), Blocks.WATERLILY.getDefaultState());
            player.setPositionAndUpdate(blockPos.getX() + 0.5,blockPos.getY() + 2,blockPos.getZ() + 0.5);
        }
    }

    private void updateConfig()
    {
        achievementFireworks = configuration.getBoolean("achievementFireworks", MOD_ID, true, "Achievement = Fireworks");
        lilypad = configuration.getBoolean("lilypad", MOD_ID, true, "Lilypad spawning");
        endDisable = configuration.getBoolean("endDisable", MOD_ID, true, "Disable any travel to the end");

        if (configuration.hasChanged()) configuration.save();
    }
}
