import java.util.Random;

/**
 * Created by Andreas on 21.02.2019.
 */
public class Main {
    public static double odWeight = 0.1;  //overall deviation -
    public static double cmWeight = 0.1*80;   // connectivity measure
    public static double evWeight = 0.1;  // edge value


    // Lowet and highest number of possible segmentations for current image
    public static int lowestNumSegments = 2;
    public static int highestNumSegments = 200;



    public static Random random = new Random();

    public static int numIterations = 100;

    public static int populationSize = 10;


    public static int initEdgeRemoveNum = 30000;


    public static int mergeSegmentsSmallerThan = 4000;
    public static int mergeSegmentsMultiplier = 1;
    public static int removeCMlowerThan = 200;
    public static int removeODhigherThan = 9000000;

    public static int numChildrenToGenerate = 5;

    public static double crossoverRate = 0.8;
    public static double mutationRate = 0.8;



    public static String imageUrl = "./Test Images Project 2/original_image/test image_3.jpg";
    //public static String imageUrl = "./Test Images Project 2/147091/Test image.jpg";



    public static void main(String[] args) {



        NSGA2 sys = new NSGA2();

        //sys.run();

        sys.weightedSumRun();



    }
}


//TODO:
/**
 *  - implement evaluation function of solution
 *
 *
 *
 */









