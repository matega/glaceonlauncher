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
import java.net.URISyntaxException;
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
        if (args.length > 0 && "--second-stage".equals(args[0])) {
            secondstage(args);
            System.exit(0);
        }
        if(args.length > 0 && "--server-jar".equals(args[0])) {
            startjar(args);
            System.exit(0);
        }
        if (args.length != 0) {
            firststage(args);
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

    private static void firststage(String[] args) {
        try {
            String javapath = System.getProperty("java.home") + File.separator + "bin" + File.separator + ((System.getProperty("java.home").startsWith("Win"))?"java.exe":"java");
            String ownjarpath = GlaceonLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String mainclassname;
            int i=0;
            for (i = 0; i < args.length; i++) {
                if (args[i].startsWith("-cp")) {
                    i++;
                    args[i] = ownjarpath + ":" + args[i];
                    break;
                }
            }
            for(i++;i<args.length; i++) {
                if(!args[i].startsWith("-")) {
                    mainclassname = args[i];
                    break;
                }
            }
            String[] secondcommand = new String[args.length+3];
            System.arraycopy(args, 0, secondcommand, 1, i);
            System.arraycopy(args, i, secondcommand, i+3, args.length-i);
            secondcommand[0] = javapath;
            secondcommand[i+1] = "glaceonlauncher.GlaceonLauncher";
            secondcommand[i+2] = "--second-stage";
            System.out.println("Will run:\n"+String.join("\n", secondcommand));
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(secondcommand);
            Process p = pb.start();
            while(p.isAlive()) {
                try {
                    p.waitFor();
                } catch (InterruptedException ex) {
                }
            }
            System.exit(255);
        } catch (SecurityException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void secondstage(String[] args) {
        try {
            Field f = InetAddress.class.getDeclaredField("nameServices");
            f.setAccessible(true);
            List<NameService> nsl = (List<NameService>) f.get(null);
            nsl.set(0, new GlaceonNS(nsl.get(0)));
            Class mainclass = Class.forName(args[1]);
            String[] gameargs = new String[args.length-2];
            System.arraycopy(args, 2, gameargs, 0, args.length-2);
            mainclass.getDeclaredMethod("main",  new Class[]{String[].class}).invoke(null, (Object)gameargs);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void startjar(String[] args) {
        try {
            launcher = new File(args[1]);
            jf = new JarFile(launcher);
            Field f = InetAddress.class.getDeclaredField("nameServices");
            f.setAccessible(true);
            List<NameService> nsl = (List<NameService>) f.get(null);
            nsl.set(0, new GlaceonNS(nsl.get(0)));
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.INFO, InetAddress.getByName("authserver.mojang.com").toString(), (Object)null);
            String mainName = jf.getManifest().getMainAttributes().getValue("Main-Class");
            System.out.println(mainName);
            URL[] lu = new URL[]{new URL("jar:file:" + launcher.getAbsolutePath() + "!/")};
            ucl = new URLClassLoader(lu, GlaceonLauncher.class.getClassLoader());
            Class bootstrap = ucl.loadClass(mainName);
            String[] serverargs = new String[args.length-2];
            System.arraycopy(args, 2, serverargs, 0, args.length-2);
            System.out.println(Integer.toString(serverargs.length) + " arguments passed: "+String.join("\n", serverargs));
            System.out.println("Starting.");
            bootstrap.getDeclaredMethod("main", new Class[]{String[].class}).invoke(null, (Object)new String[]{"-v"});
        } catch (IOException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
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
