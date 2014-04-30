/**
 * Low resolution image producer.
 */

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class LowResImgProducer {

  private final String FILE_EXTENSION = "jpg";

  private String filePath;
  private int width;
  private int height;

  /**
   * The constructor for this class.
   * @param filePath The folder path of the images to be stored
   * @param width The width of the images to be stored
   * @param height The height of the images to be stored
   */
  public LowResImgProducer(String filePath, int width, int height) {
    this.filePath = filePath + "/";
    this.width = width;
    this.height = height;
  }

  /**
   * Checks if the given image file exists on disk.
   * @param fileName The file name of the image to look for
   * @return true if the image is found, false otherwise
   */
  public boolean hasImg(String fileName) {
    File file = new File(filePath + fileName);
    return file.exists();
  }

  /**
   * Gets the ImageIcon object of the given file.
   * @param fileName The file name of the image stored on disk
   * @return The ImageIcon object if found, null otherwise
   */
   public ImageIcon getImg(String fileName) {
     try {
       File file = new File(filePath + fileName);
       BufferedImage bufImg = ImageIO.read(file);
       ImageIcon icon = new ImageIcon(bufImg);
       return icon;
     } catch (IOException e) {
       System.out.println("Error when loading image file on disk");
       e.printStackTrace();
     }
     return null;
   }

  /**
   * Saves an image to disk given an image URL.
   *
   * @param imgUrl The URL to the image file
   * @param fileName The name of the file to be save including the image extension
   */
  public ImageIcon saveImg(String imgUrl, String fileName, boolean returnImage) {
    System.out.println("Save to file " + fileName + " from " + imgUrl);
    try {
      URL url = new URL(imgUrl);
      BufferedImage originalBufImg = ImageIO.read(url);
      // Resize the image with the default image scaling algorithm Image.SCALE_DEFAULT
      Image resizedImg = originalBufImg.getScaledInstance(width, height, Image.SCALE_DEFAULT);
      BufferedImage resizedBufImg = new BufferedImage(width, height, originalBufImg.getType());
      Graphics2D graphics = resizedBufImg.createGraphics();
      // Draw the graphics object from coordinate (0, 0) with the ImageObserver set to null
      graphics.drawImage(resizedImg, 0, 0, null);
      // Dispose the graphics when no longer needed
      graphics.dispose();
      // Save the img to disk
      String targetPath = filePath + fileName;
      FileOutputStream out = new FileOutputStream(targetPath);
      ImageIO.write(resizedBufImg, FILE_EXTENSION, out);
      out.close();
      
      //If the saved image is also wanted to be returned
      if(returnImage){
        return new ImageIcon(resizedBufImg);
      }
      
    } catch (IOException e) {
      System.out.println("Error when trying to save an img from URL");
      e.printStackTrace();
    }
    return null;
  }
}
