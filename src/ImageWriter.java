import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Andreas on 23.02.2019.
 *
 *
 * Class for writing segmentations onto images
 *
 *
 */
public class ImageWriter {

    private static String PRIpath = "./Evaluator for Segmentation/Segmentation Evaluation/Student_Segmentation_Files/";
    private static String folderPath = "./output_images/";

    public Genotype genotype;



    public ImageWriter(Genotype genotype) {
        this.genotype = genotype;

    }


    public void writeAllSolutionImagesAndPrint(int imageNum, Genotype genotype){
        writeSegmentationEdgesBlackWhite(genotype, imageNum, null);
        writeSegmentationEdgesBlackWhite(genotype, imageNum, PRIpath);
        writeSegmentedImageWithEdges(genotype, imageNum);
        System.out.println(imageNum + ":    " + genotype +   "     Overall Deviation: " + genotype.overallDeviation +
                "       Connectivity Measure: " + genotype.connectivityMeasure + "       Edge Value: " + genotype.edgeValue
                    + "        Segments: " + genotype.numSegments);


    }



    public void writeGridImage(int folderNum){
        try{
            BufferedImage image = new BufferedImage(genotype.imageWidth, genotype.imageHeight, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < genotype.imageWidth; x++) {
                for (int y = 0; y < genotype.imageHeight; y++) {
                    image.setRGB(x, y, genotype.pixels.get(x + (y * genotype.imageWidth)).getRGB());
                }
            }
            File outputFile = new File(folderPath + "original_image.png");
            ImageIO.write(image, "png", outputFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeSegmentedImageRandomColors(Genotype genotype, int folderNum){
        if (genotype.segmentationOutdated) genotype.calculateSegmentation();
//        System.out.println("Writing image " + id);
        try{
            Color[] segmentColors = new Color[genotype.numSegments];

            for (int i = 0; i < genotype.numSegments; i++) {
                segmentColors[i] = new Color((int)(Math.random() * 0x1000000));
            }
            BufferedImage image = new BufferedImage(genotype.imageWidth, genotype.imageHeight, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < genotype.imageWidth; x++) {
                for (int y = 0; y < genotype.imageHeight; y++) {
                    int pixelId = x + (y * genotype.imageWidth);
                    image.setRGB(x, y, segmentColors[genotype.segmentation[pixelId]].getRGB());
                }
            }
            File outputFile = new File(folderPath + String.format("%05d", genotype.numSegments) + "_" + ".png");
            ImageIO.write(image, "png", outputFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeSegmentedImageWithEdges(Genotype genotype, int folderNum){
        String folder = folderPath;
        if (genotype.segmentationOutdated) genotype.calculateSegmentation();
//        System.out.println("Writing image " + id);
        try{
            BufferedImage image = new BufferedImage(genotype.imageWidth, genotype.imageHeight, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < genotype.imageWidth; x++) {
                for (int y = 0; y < genotype.imageHeight; y++) {
                    int pixelId = x + (y * genotype.imageWidth);
                    if (isEdgePixel(genotype, pixelId)) image.setRGB(x, y, Color.GREEN.getRGB());
                    else image.setRGB(x, y, genotype.pixels.get(pixelId).getRGB());
                }
            }
            File dir = new File(folder);
            if(! dir.exists()){
                dir.mkdir();
            }
            String fill = "";
            if(folderNum < 10) fill += "0";
            File outputFile = new File(folderPath + fill + folderNum+"_" +  String.format("%05d", genotype.numSegments) + "_edges.png");
            ImageIO.write(image, "png", outputFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeSegmentationEdgesBlackWhite(Genotype genotype, int folderNum, String otherFolder){

        String folder = folderPath;

        if(otherFolder != null){
            folder = otherFolder;
        }


        if (genotype.segmentationOutdated) genotype.calculateSegmentation();
//        System.out.println("Writing image " + id);
        try{
            BufferedImage image = new BufferedImage(genotype.imageWidth, genotype.imageHeight, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < genotype.imageWidth; x++) {
                for (int y = 0; y < genotype.imageHeight; y++) {
                    int pixelId = x + (y * genotype.imageWidth);
                    if (isEdgePixel(genotype, pixelId)) image.setRGB(x, y, Color.BLACK.getRGB());
                    else image.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
            File dir = new File(folder);
            if(! dir.exists()){
                dir.mkdir();
            }
            String fill = "";
            if(folderNum < 10) fill += "0";
            File outputFile = new File(folder  + fill + folderNum+"_" +   String.format("%05d", genotype.numSegments) + "_BW.png");
            ImageIO.write(image, "png", outputFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean isEdgePixel(Genotype genotype, int pixelId) {
        for (int neighbor : genotype.getNeighborPixels(pixelId))
            if (genotype.segmentation[pixelId] != genotype.segmentation[neighbor]) return true;
        return false;
    }



    public void clearOutputFolder() {
        try{
            File folder = new File(folderPath);
            File[] fileArray = folder.listFiles();
            for (int i = 0; i < fileArray.length; i++){
                if (fileArray[i].getName().equals("actual.png")) continue;

                Files.deleteIfExists(Paths.get(folderPath + fileArray[i].getName()));
//                System.out.println(fileArray[i].getName());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        try{
            File folder = new File(PRIpath);
            File[] fileArray = folder.listFiles();
            for (int i = 0; i < fileArray.length; i++){
                if (fileArray[i].getName().equals("actual.png")) continue;

                Files.deleteIfExists(Paths.get(PRIpath + fileArray[i].getName()));
//                System.out.println(fileArray[i].getName());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
