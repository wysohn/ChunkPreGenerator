import dialogues.Dialogues;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.server.v1_16_R1.*;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;

public class Main {
    private static final String packageName = "net.minecraft.server";
    private static final String targetVersion = "v1_16_R1";

    private static void checkVersion(String version) {
        String className = packageName + "." + version + ".Main";
        try {
            System.out.println("Main located: "+Class.forName(className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to find version "+version+".\n" +
                    "\n" +
                    "Make sure you have appropriate 'spigot.jar' set.");
        }
    }

    private static void setProperties(String worldName, int maxWorldSize, long seed) {
        Properties properties = new Properties();
        File file = new File("temp.properties");
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                properties.load(fr);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        file.deleteOnExit();

        try {
            Random random = new Random();
            properties.setProperty("server-ip", "127.0.0.1");
            properties.setProperty("server-port", String.valueOf(50000 + random.nextInt(5000)));
            properties.setProperty("level-name", worldName);
            properties.setProperty("max-world-size", String.valueOf(maxWorldSize));
            properties.setProperty("spawn-monsters", "false");
            properties.setProperty("spawn-npcs", "false");
            properties.setProperty("online-mode", "false");
            if(seed > 0L)
                properties.setProperty("level-seed", String.valueOf(seed));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try(FileWriter fw = new FileWriter(file)){
            properties.store(fw, "");
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static void bind(ResourcePackRepository<ResourcePackLoader> resourcepackrepository,
                             Convertable.ConversionSession convertable_conversionsession){
        CompletableFuture completablefuture = DataPackResources.a(resourcepackrepository.f(), CommandDispatcher.ServerType.DEDICATED, 2, SystemUtils.f(), Runnable::run);

        DataPackResources datapackresources;

        try {
            datapackresources = (DataPackResources) completablefuture.get();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        datapackresources.i();
    }

    public static int INVALID_NAME = 300;
    public static int INVALID_SIZE = 301;
    public static void main(String[] ar) throws IOException {
        checkVersion(targetVersion);

        String worldName = Dialogues.askWorldName(ar);
        if(worldName == null) {
            System.exit(INVALID_NAME);
            return;
        }

        int max = Dialogues.askMaxWorldSize(ar);
        if(max < 1) {
            System.exit(INVALID_SIZE);
            return;
        }

        System.setProperty("IReallyKnowWhatIAmDoingISwear", "");
        System.setProperty("com.mojang.eula.agree", "true");
        setProperties(worldName, max, -1);

        OptionParser parser = getParser();
        OptionSet optionset = parser.parse("--config=temp.properties");

        Queue<ChunkCoordIntPair> pairs = new LinkedList<>();
        int chunk_max = (int) Math.ceil(max / 16.0);
        for (int x = -chunk_max; x <= chunk_max; x++) {
            for (int z = -chunk_max; z <= chunk_max; z++) {
                pairs.add(new ChunkCoordIntPair(x, z));
            }
        }

        DedicatedServer dedicatedserver = dedicatedServer(worldName, optionset);
        dedicatedserver.executeSync(() -> {
            while (!pairs.isEmpty()) {
                List<ChunkCoordIntPair> selection = new LinkedList<>();

                for (int k = 0; k < 1000 && !pairs.isEmpty(); k++) {
                    selection.add(pairs.poll());
                }

                for (ChunkCoordIntPair pair : selection) {
                    dedicatedserver.getWorlds()
                            .forEach(worldServer -> {
                                ChunkProviderServer chunkProviderServer = worldServer.getChunkProvider();
                                chunkProviderServer.addTicket(TicketType.START, pair, 0, Unit.INSTANCE);
                            });
                }
                executeModerately(dedicatedserver);

                dedicatedserver.saveChunks(false, true, false);
                System.out.println(pairs.size() + " Chunks left.");

                for (ChunkCoordIntPair pair : selection) {
                    dedicatedserver.getWorlds()
                            .forEach(worldServer -> {
                                ChunkProviderServer chunkProviderServer = worldServer.getChunkProvider();
                                chunkProviderServer.removeTicket(TicketType.START, pair, 0, Unit.INSTANCE);
                            });
                }
                executeModerately(dedicatedserver);
            }
        });

        dedicatedserver.safeShutdown(true);
    }

    private static void executeModerately(MinecraftServer server){
        try {
            Method method = MinecraftServer.class.getDeclaredMethod("executeModerately");
            method.setAccessible(true);
            method.invoke(server);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static DedicatedServer dedicatedServer(String worldName, OptionSet optionset) throws IOException {
        IRegistryCustom.Dimension iregistrycustom_dimension = IRegistryCustom.b();

        File file = new File(".");
        Convertable convertable = Convertable.a(file.toPath());
        Convertable.ConversionSession convertable_conversionsession = convertable.c(worldName, WorldDimension.OVERWORLD);

        ResourcePackRepository<ResourcePackLoader> resourcepackrepository = new ResourcePackRepository<>(ResourcePackLoader::new, new ResourcePackSource[]{new ResourcePackSourceVanilla(), new ResourcePackSourceFolder(convertable_conversionsession.getWorldFolder(SavedFile.DATAPACKS).toFile(), PackSource.c)});
        DataPackConfiguration datapackconfiguration1 = MinecraftServer.a(resourcepackrepository, DataPackConfiguration.a, true);

        bind(resourcepackrepository, convertable_conversionsession);

        DedicatedServerSettings dedicatedserversettings = new DedicatedServerSettings(optionset);

        File userCacheFile = new File("temp");
        userCacheFile.deleteOnExit();

        final DedicatedServer dedicatedserver = (DedicatedServer) MinecraftServer.a((thread) -> {
            DedicatedServer dedicatedserver1 = new DedicatedServer(optionset,
                    datapackconfiguration1,
                    thread,
                    iregistrycustom_dimension,
                    convertable_conversionsession,
                    null,
                    new DataPackResources(CommandDispatcher.ServerType.DEDICATED, 2),
                    null,
                    dedicatedserversettings,
                    DataConverterRegistry.a(),
                    null,
                    null,
                    new UserCache((strings, agent, profileLookupCallback) -> {
                    }, userCacheFile),
                    WorldLoadListenerLogger::new);

            if (optionset.has("port")) {
                int port = (Integer) optionset.valueOf("port");
                if (port > 0) {
                    dedicatedserver1.setPort(port);
                }
            }

            return dedicatedserver1;
        });
        return dedicatedserver;
    }

    private static OptionParser getParser() {
        return new OptionParser() {
            {
                acceptsAll(asList("?", "help"), "Show the help");

                acceptsAll(asList("c", "config"), "Properties file to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("server.properties"))
                        .describedAs("Properties file");

                acceptsAll(asList("P", "plugins"), "Plugin directory to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("plugins"))
                        .describedAs("Plugin directory");

                acceptsAll(asList("h", "host", "server-ip"), "Host to listen on")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("Hostname or IP");

                acceptsAll(asList("W", "world-dir", "universe", "world-container"), "World container")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("."))
                        .describedAs("Directory containing worlds");

                acceptsAll(asList("w", "world", "level-name"), "World name")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("World name");

                acceptsAll(asList("p", "port", "server-port"), "Port to listen on")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Port");

                acceptsAll(asList("o", "online-mode"), "Whether to use online authentication")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .describedAs("Authentication");

                acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Server size");

                acceptsAll(asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
                        .withRequiredArg()
                        .ofType(SimpleDateFormat.class)
                        .describedAs("Log date format");

                acceptsAll(asList("log-pattern"), "Specfies the log filename pattern")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("server.log")
                        .describedAs("Log filename");

                acceptsAll(asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(0)
                        .describedAs("Max log size");

                acceptsAll(asList("log-count"), "Specified how many log files to cycle through")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(1)
                        .describedAs("Log count");

                acceptsAll(asList("log-append"), "Whether to append to the log file")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .defaultsTo(true)
                        .describedAs("Log append");

                acceptsAll(asList("log-strip-color"), "Strips color codes from log file");

                acceptsAll(asList("b", "bukkit-settings"), "File for bukkit settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("bukkit.yml"))
                        .describedAs("Yml file");

                acceptsAll(asList("C", "commands-settings"), "File for command settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("commands.yml"))
                        .describedAs("Yml file");

                acceptsAll(asList("forceUpgrade"), "Whether to force a world upgrade");
                acceptsAll(asList("eraseCache"), "Whether to force cache erase during world upgrade");
                acceptsAll(asList("nogui"), "Disables the graphical console");

                acceptsAll(asList("nojline"), "Disables jline and emulates the vanilla console");

                acceptsAll(asList("noconsole"), "Disables the console");

                acceptsAll(asList("v", "version"), "Show the CraftBukkit Version");

                acceptsAll(asList("demo"), "Demo mode");

                // Spigot Start
                acceptsAll(asList("S", "spigot-settings"), "File for spigot settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("spigot.yml"))
                        .describedAs("Yml file");
                // Spigot End
            }
        };
    }
}
