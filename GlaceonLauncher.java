/*
 * Written by matega
 */
package glaceonlauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
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
    static Method mainmethod;
    static File cacerts;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0 && "--second-stage".equals(args[0])) {
            secondstage(args);
            System.exit(0);
        }
        cacerts = new File("./cacerts");
        if(!cacerts.exists()) {
            firstrun();
            System.exit(0);
        }
        /*if(args.length > 0 && "--server-jar".equals(args[0])) {
            startjar(args);
            try {
                mainmethod.invoke(null, (Object)new String[0]);
            } catch (Exception ex) {
                Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
        }*/
        if (args.length != 0) {
            firststage(args);
            System.exit(0);
        }
        try {
            System.setProperty("javax.net.ssl.trustStore", "./cacerts");
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
    /**
     * @param args the original command line arguments
     */
    private static void firststage(String[] args) {
        try {
            String javapath = System.getProperty("java.home") + File.separator + "bin" + File.separator + ((System.getProperty("java.home").startsWith("Win"))?"java.exe":"java");
            String ownjarpath = GlaceonLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
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
            System.exit(0);
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
    
    /*private static void startjar(String[] args) {
        try {
            System.out.println(System.getProperty("java.home"));
            PrintWriter pw = new PrintWriter("args.txt", "UTF-8");
            pw.print(System.getProperty("java.home"));
            pw.flush();
            pw.close();
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
            mainmethod = bootstrap.getDeclaredMethod("main", new Class[]{String[].class});
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
        }
    } */

    private static void firstrun() {
        try {
            String oldcacerts = System.getProperty("javax.net.ssl.trustStore");
            if(oldcacerts == null) oldcacerts = System.getProperty("java.home")+File.separator+"lib"+File.separator+"security"+File.separator+"cacerts";
            
            InputStream newcertis = GlaceonLauncher.class.getResourceAsStream("cacert.pem");
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream cacertsis = new FileInputStream(oldcacerts);
            ks.load(cacertsis, null);
            cacertsis.close();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate newcacert = cf.generateCertificate(newcertis);
            ks.setCertificateEntry("GlaceonCA", newcacert);
            FileOutputStream out = new FileOutputStream(cacerts);
            ks.store(out, "changeit".toCharArray());
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(GlaceonLauncher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CertificateException ex) {
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
