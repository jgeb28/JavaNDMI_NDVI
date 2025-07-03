package pw.edu.javandmi_ndvi.core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static void savetoPng(BufferedImage image, String filename) {
        try{
            System.out.println(filename);
            File file = new File(filename);

            ImageIO.write(image, "png", file);
            System.out.println("Image saved as " + filename);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteDirectoryRecursively(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete " + dir.getAbsolutePath());
        }
    }
}
