/*
 * Written by matega
 */
package glaceonlauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import sun.net.spi.nameservice.NameService;

/**
 *
 * @author matega
 */
public class GlaceonLauncher {

    static File launcher;
    static JarFile jf;
    static URLClassLoader ucl;
    final static String NETSOURCE = "http://s3.amazonaws.com/Minecraft.Download/launcher/Minecraft.jar";
    final static String LOCALURL = "./launcher.jar";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            spoofgame(args);
            System.exit(0);
        }
        try {
            launcher = new File(LOCALURL);
            if (!launcher.exists()) {
                System.out.println(launcher.getCanonicalPath());
                launcher.createNewFile();
                URL u = new URL(NETSOURCE);
                try (ReadableByteChannel rbc = Channels.newChannel(u.openStream()); FileOutputStream fos = new FileOutputStream(LOCALURL)) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
            }
            jf = new JarFile(launcher);
            Field f = InetAddress.class.getDeclaredField("nameServices");
            f.setAccessible(true);
            List<NameService> nsl = (List<NameService>) f.get(null);
            nsl.set(0, new GlaceonNS(nsl.get(0)));
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.INFO, InetAddress.getByName("authserver.mojang.com").toString(), (Object) null);
            String mainName = jf.getManifest().getMainAttributes().getValue("Main-Class");
            System.out.println(mainName);
            URL[] lu = new URL[]{new URL("jar:file:" + LOCALURL + "!/")};
            ucl = new URLClassLoader(lu, GlaceonLauncher.class.getClassLoader());
            Class bootstrap = ucl.loadClass(mainName);
            bootstrap.getDeclaredMethod("main", new Class[]{String[].class}).invoke(null, (Object) args);
            // TODO code application logic here
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static URLClassLoader gcl;

    private static void spoofgame(String[] args) {
        try {
            Field f = InetAddress.class.getDeclaredField("nameServices");
            f.setAccessible(true);
            List<NameService> nsl = (List<NameService>) f.get(null);
            nsl.set(0, new GlaceonNS(nsl.get(0)));
            boolean cpnext = false;
            String mainclassname = "";
            String[] gameargs = null;
            for (int i = 0; i < args.length; i++) {
                if (cpnext) {
                    String[] gcp = args[i].split(":");
                    URL[] gcu = new URL[gcp.length];
                    for (int j = 0; j < gcp.length; j++) {
                        gcu[j] = new URL("jar:file:" + gcp[j] + "!/");
                    }
                    JOptionPane.showMessageDialog(null, "classpath = \n" + String.join("\n", gcp));
                    gcl = new URLClassLoader(gcu, Thread.currentThread().getContextClassLoader());
                    cpnext = false;
                    continue;
                }
                if (!args[i].startsWith("-")) {
                    mainclassname = args[i];
                    gameargs = new String[args.length - i - 1];
                    System.arraycopy(args, i + 1, gameargs, 0, gameargs.length);
                    break;
                }
                if ("-cp".equals(args[i])) {
                    cpnext = true;
                    continue;
                }
                if (args[i].startsWith("-D")) {
                    String[] prop = args[i].substring(2).split("=");
                    if("java.library.path".equals(prop[0])) {
                        Field spf = ClassLoader.class.getDeclaredField("usr_paths");
                        spf.setAccessible(true);
                        final String[] paths = (String[]) spf.get(null);
                        String[] newpaths = new String[paths.length];
                        System.arraycopy(paths, 0, newpaths, 0, paths.length);
                        newpaths[newpaths.length-1] = prop[1];
                        spf.set(null, newpaths);
                        JOptionPane.showMessageDialog(null, "added library path.");
                    }
                    System.setProperty(prop[0], prop[1]);
                    continue;
                }
            }
            JOptionPane.showMessageDialog(null, "mainclassname = " + mainclassname);
            JOptionPane.showMessageDialog(null, "cmdlineargs = " + String.join(" # ", gameargs));
            Thread.currentThread().setContextClassLoader(gcl);
            Class mainclass = gcl.loadClass(mainclassname);
            mainclass.getDeclaredMethod("main", new Class[]{String[].class}).invoke(null, (Object) gameargs);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | MalformedURLException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(255);
        }
    }
}

class GlaceonNS implements NameService {

    private NameService ns;

    public GlaceonNS(NameService ns) {
        this.ns = ns;
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
        Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.INFO, "Looking up " + host, (Throwable) null);
        if (host.equals("authserver.mojang.com") || host.equals("sessionserver.mojang.com")) {
            host = "localhost";
        }
        return ns.lookupAllHostAddr(host);
    }

    @Override
    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        return ns.getHostByAddr(bytes);
    }
}
