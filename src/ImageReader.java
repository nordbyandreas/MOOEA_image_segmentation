import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by Andreas on 21.02.2019.
 *
 *
 *
 * Reads an image and returns a representation
 *
 *
 *
 */
public class ImageReader {


    public BufferedImage image;
    int width;
    int height;

    public int[] genoType;
    public ArrayList<Color> pixels = new ArrayList<>();



    public ImageReader(String imageUrl){

        try{
            File input = new File(imageUrl);
            image = ImageIO.read(input);
            width = image.getWidth();
            height = image.getHeight();

            for(int i = 0; i < height; i++){

                for( int j= 0; j < width; j++){

                    pixels.add(new Color(image.getRGB(j, i)));
                }

            }
        }
        catch(Exception e){

        }


    }



    public void printPixels(){
        for(int i = 0; i < pixels.size(); i++){

            System.out.println("Pixelnum: " + i +
                    "        Red: " + pixels.get(i).getRed() +
                    "  Green: " + pixels.get(i).getGreen() +
                    "  Blue: " + pixels.get(i).getBlue());
        }

    }



        //GENOTYPE
    //TODO: RETURN 1d vector where length == image.height*image.width



    //TODO: MAKE NEIGHBOR RGB-LIST FOR GENOTYPE




}
