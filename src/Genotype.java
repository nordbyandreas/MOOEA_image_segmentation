import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Andreas on 21.02.2019.
 */
public class Genotype {


    public int imageWidth;
    public int imageHeight;

    public int numPixelsInImage;

    //OBJECTIVE VALUES
    public double overallDeviation = 0;
    public double connectivityMeasure = 0;
    public double edgeValue = 0;

    // PIXEL NEIGHBORMAP:
    // NORTH = 3
    // SOUTH = 4
    // EAST = 1
    // WEST = 2
    // NORTH_EAST = 5
    // NORTH_WEST = 7
    // SOUTH_EAST = 6
    // SOUTH_WEST = 8
    // NULL = 0



    public int rank;
    public double crowdingDistance = 0;

    public int[] genotype;
    public int[] segmentation;
    public ArrayList<Color> pixels;

    public boolean segmentationOutdated;
    public int numSegments;


    // O.G. Constructor
    public Genotype(int imageWidth, int imageHeight, ImageReader imageReader) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.genotype = new int[imageHeight * imageWidth];
        this.segmentation = new int[imageHeight * imageWidth];
        this.numPixelsInImage = imageHeight * imageWidth;
        this.pixels = imageReader.pixels;
        constructMST();

    }

    //Cloner
    public Genotype(Genotype genotype){
        this.imageWidth = genotype.imageWidth;
        this.imageHeight = genotype.imageHeight;
        this.pixels = genotype.pixels;
        this.numPixelsInImage = genotype.numPixelsInImage;
        this.genotype = new int[numPixelsInImage];
        this.segmentation = new int[numPixelsInImage];

        for(int i = 0; i < genotype.genotype.length; i++) this.genotype[i] = genotype.genotype[i];
    }

    //Crossover
    public Genotype (Genotype g1, Genotype g2){
        this.imageWidth = g1.imageWidth;
        this.imageHeight = g1.imageHeight;
        this.pixels = g1.pixels;
        this.numPixelsInImage = g1.numPixelsInImage;
        this.genotype = new int[numPixelsInImage];
        this.segmentation = new int[numPixelsInImage];

        //TODO change one to random double
        if(Main.random.nextDouble() < Main.crossoverRate){
            for(int i = 0; i < numPixelsInImage; i++){
                if(Main.random.nextBoolean()){
                    genotype[i] = g1.genotype[i];
                }
                else{
                    genotype[i] = g2.genotype[i];
                }
            }
            if(Main.random.nextDouble() < Main.mutationRate) mutate();

        }
        else{
            for(int i = 0; i < g1.genotype.length; i++) this.genotype[i] = g1.genotype[i];
            //TODO uncomment
            if(Main.random.nextDouble() < Main.mutationRate) mutate();
        }
        this.segmentationOutdated = true;
        calculateSegmentation();
        //mergeSegmentsSmallerThanN2(Main.mergeSegmentsSmallerThan, false);
        setObjectiveValues();

    }



    public void setRandomEdge(){
        int pixelIndex =  ThreadLocalRandom.current().nextInt(0, genotype.length);
        ArrayList<Integer> neighbours = getNeighborPixels(pixelIndex);
        int neighbourIndex = ThreadLocalRandom.current().nextInt(0, neighbours.size());
        genotype[pixelIndex] = neighbours.get(neighbourIndex);
    }


    //MUTATION
    //TODO
    public void mutate(){
        double rand = Main.random.nextDouble();

        setRandomEdge();

      /*  if(rand < 0.25){
            mutateMergeSegments();

        }
        else if(rand < 0.5){
            setRandomEdge();
        }
        else if(rand < 0.75){
            removeRandomEdge();
        }
        else{
            mutateAddNewSegmendWithinThreshold(5 + Main.random.nextInt(20));
        }
*/


    }


    public void removeRandomEdge() {
        int pixelIndex =  ThreadLocalRandom.current().nextInt(0, genotype.length);
        genotype[pixelIndex] = pixelIndex;

    }



    private void mutateMergeSegments() {
        if(segmentationOutdated ) calculateSegmentation();
        ArrayList<Integer[]> interSegmentConnections = new ArrayList<>();
        for (int i = 0; i < numPixelsInImage; i++) {
            for (int nb : getNeighborPixels(i))
                if (segmentation[i] != segmentation[nb]) interSegmentConnections.add(new Integer[] {i, nb});
        }
        if (interSegmentConnections.size() > 0) {
            int i = Main.random.nextInt(interSegmentConnections.size());
            genotype[interSegmentConnections.get(i)[0]] = interSegmentConnections.get(i)[1];
        }
    }


    public void mutateAddNewSegmendWithinThreshold(double threshold) {

        ArrayList<Integer> check = new ArrayList<>();
        HashSet<Integer> visited = new HashSet<>();
        check.add(Main.random.nextInt(numPixelsInImage));
        visited.add(check.get(0));
        while (check.size() > 0) {
            int current = check.remove(0);
            for (int nb: getNeighborPixels(current)) {
                if (!visited.contains(nb) && RGBDistance(pixels.get(current), pixels.get(nb)) < threshold) {
                    check.add(nb);
                    visited.add(nb);
                    genotype[nb] = current;
                }
            }
        }

    }


    public void printSegmentation(){
        for(int i = 0; i < segmentation.length; i ++){
            if(segmentation[i] < 10){
                System.out.print(segmentation[i] + ",   ");
            }
            else if(segmentation[i] < 100){
                System.out.print(segmentation[i] + ",  ");
            }
            else{
                System.out.print(segmentation[i] + ", ");
            }

            if((i + 1) % imageWidth == 0){

                System.out.println("\n");
            }
        }



    }


    public void printGenotype(){

        for(int i = 0; i < genotype.length; i ++){
            if(genotype[i] < 10){
                System.out.print(genotype[i] + ",   ");
            }
            else if(genotype[i] < 100){
                System.out.print(genotype[i] + ",  ");
            }
            else{
                System.out.print(genotype[i] + ", ");
            }


            if((i + 1) % imageWidth == 0){

                System.out.println("\n");
            }


        }
    }



    /**
            Returns the indexes of the neighbors of a pixel
     */
    public ArrayList<Integer> getNeighborPixels(int pixelIndex){
        ArrayList<Integer> neighbors = new ArrayList<>();

        if(pixelIndex >= imageWidth) neighbors.add(pixelIndex - imageWidth); //NORTH
        if(Math.floorMod(pixelIndex, imageWidth) != imageWidth - 1) neighbors.add(pixelIndex + 1);  // EAST
        if(pixelIndex < imageWidth * (imageHeight - 1)) neighbors.add(pixelIndex + imageWidth); //SOUTH
        if(Math.floorMod(pixelIndex, imageWidth) != 0) neighbors.add(pixelIndex - 1);  // WEST

        if(pixelIndex >= imageWidth && Math.floorMod(pixelIndex, imageWidth) != imageWidth - 1) neighbors.add(pixelIndex - imageWidth + 1);  //NORTHEAST
        if(pixelIndex >= imageWidth && Math.floorMod(pixelIndex, imageWidth) != 0) neighbors.add(pixelIndex - imageWidth - 1); //NORTHWEST
        if(pixelIndex < imageWidth * (imageHeight - 1) && Math.floorMod(pixelIndex, imageWidth) != imageWidth - 1) neighbors.add(pixelIndex + imageWidth + 1);//SOUTHEAST
        if(pixelIndex < imageWidth * (imageHeight - 1) && Math.floorMod(pixelIndex, imageWidth) != 0) neighbors.add(pixelIndex + imageWidth - 1);//SOUTHWEST


        return neighbors;
    }


    /**
     *  Constructs a Minimum Spanning tree from the image based on RGB distance(weight) between pixels
     */
    public void  constructMST(){
        for (int i = 0; i < genotype.length; i++) genotype[i] = i;

        HashSet<Integer> visited = new HashSet<>(imageHeight*imageWidth);
        PriorityQueue<Edge> priorityQueue = new PriorityQueue<>();


        //int current = genotype.length - 1; // Starts at the last pixel
        int current = ThreadLocalRandom.current().nextInt(0, genotype.length);

        while (visited.size() < imageHeight*imageWidth){
            if (!visited.contains(current)){
                visited.add(current);
                ArrayList<Integer> neighbors = getNeighborPixels(current);
                for (int neighbour : neighbors) {
                    priorityQueue.add(new Edge(current, neighbour, this.pixels));
                }
            }
            Edge edge = priorityQueue.poll();
            if (!visited.contains(edge.to)){
                genotype[edge.to] = edge.from;
            }
            current = edge.to;
        }

    }


/**
    Remove the N largest edges from the MST.
    Essentially creates the first segmentation
 */
    public void removeNLargestEdges(int n) {
        ArrayList<Edge> edges = calculateEdges();
        Collections.sort(edges);
        Collections.reverse(edges);
        for (int i = 0; i < n; i++) {
            Edge edge = edges.get(i);
            genotype[edge.from] = edge.from;
        }
        segmentationOutdated = true;
        calculateSegmentation();
    }



    public ArrayList<Edge> calculateEdges() {
        ArrayList<Edge> edges = new ArrayList<>(genotype.length);
        for (int i = 0; i < genotype.length; i++) {
            edges.add(new Edge(i, genotype[i], pixels)); // TODO Kan utelukke de som er til seg selv om ønskelig
        }
        return edges;
    }



    public void calculateSegmentation() {
        // reset segments
        for (int i = 0; i < segmentation.length; i++) segmentation[i] = -1;
        int curSegmentId = 0;
        ArrayList<Integer> curPath;

        for (int rootPixel = 0; rootPixel < segmentation.length; rootPixel++) {

            curPath = new ArrayList<>();

            if (segmentation[rootPixel] == -1) {
                curPath.add(rootPixel);
                segmentation[rootPixel] = curSegmentId;
                int curPixel = genotype[rootPixel];

                // TODO Variation: Store all looped and set either curSegmentId or segmentation[curPixel] for all instead of one at a time.
                while (segmentation[curPixel] == -1) {
                    curPath.add(curPixel);
                    segmentation[curPixel] = curSegmentId;
                    curPixel = genotype[curPixel];
                }
                if (segmentation[curPixel] != curSegmentId) {
                    for (int segmentPixel : curPath) {
                        segmentation[segmentPixel] = segmentation[curPixel];
                    }
                }
                else curSegmentId++;
            }
        }
        numSegments = curSegmentId;

        segmentationOutdated = false;
    }












    public void mergeSegmentsSmallerThanN2(int minSegmentSize, boolean aimFortargetSegmentations){


        //System.out.println("inside Mergesegmentssmaller than");

        if(segmentationOutdated) calculateSegmentation();
        int[] segmentSizes = new int[numSegments];
        ArrayList<Integer> smallSegments = new ArrayList<>();

        // count size of segments (num of pixels)
        for(int i = 0; i < numPixelsInImage; i++){
            segmentSizes[segmentation[i]] += 1;
        }
        // add index of too small segments to list
        for(int i = 0; i < segmentSizes.length; i++){
            if(segmentSizes[i] < minSegmentSize){
                smallSegments.add(i);
            }
        }

        //System.out.println("inside Mergesegmentssmallerthan  before while loop");


        while(smallSegments.size() > 0){



            ArrayList<Integer> bestFroms = new ArrayList<>();
            ArrayList<Integer> bestTos = new ArrayList<>();


            for( int segmentIndex : smallSegments){
                Edge best = findBestOutgoingEdge(segmentIndex);

                genotype[best.from] = best.to;


            }

            calculateSegmentation();

            segmentSizes = new int[numSegments];
            smallSegments.clear();

            // count size of segments (num of pixels)
            for(int i = 0; i < numPixelsInImage; i++){
                segmentSizes[segmentation[i]] += 1;
            }
            // add index of too small segments to list
            for(int i = 0; i < segmentSizes.length; i++){
                if(segmentSizes[i] < minSegmentSize){
                    smallSegments.add(i);
                }
            }

            //System.out.println("smallsegments size: " + smallSegments.size());






        }
        //System.out.println("inside Mergesegmentssmallerthan AfTER while loop");

        calculateSegmentation();


        if(aimFortargetSegmentations){
            int target = ThreadLocalRandom.current().nextInt(Main.lowestNumSegments, Main.highestNumSegments);
            if(numSegments > target){
                System.out.println("\n\n STart again \n\n");
                mergeSegmentsSmallerThanN2(minSegmentSize + 00, aimFortargetSegmentations);
            }
        }

        setObjectiveValues();
    }









    /**
     * Finds best outgoing edge from a segment
     */
    public Edge findBestOutgoingEdge(int segmentIndex){
        Edge bestOutgoingEdge = new Edge();
        for(int i = 0; i < numPixelsInImage; i++){

            if(segmentation[i] == segmentIndex){

                for(int neighborIndex : getNeighborPixels(i)){
                    if(segmentation[neighborIndex] != segmentIndex){

                        Edge edgeToNeighborSegment = new Edge(i, neighborIndex, this.pixels);

                        if(edgeToNeighborSegment.compareTo(bestOutgoingEdge) < 0){
                            if(Main.random.nextDouble() > 0.05){
                                bestOutgoingEdge = edgeToNeighborSegment;
                            }

                        }
                    }
                }

            }
        }
        return bestOutgoingEdge;
    }





    /***
     *
     *
     *
     *
     *   OBJECTIVE FUNCTIONS:::
     *
     *
     *
     *
     */


    /**
     * Euclidian distance between two colors
     */
    public static double RGBDistance(Color c1, Color c2) {
        return Math.sqrt(Math.pow(c1.getRed() - c2.getRed(), 2)
                + Math.pow(c1.getGreen() - c2.getGreen(), 2)
                + Math.pow(c1.getBlue() - c2.getBlue(), 2));
    }


    /**
     *  a measure of the similarity of the pixels in a segment, for each segment in the image
     *  Should be MINimized
     */
    public double overallDeviation(){
        if(segmentationOutdated) calculateSegmentation();

        float[][] segmentAverageColors = new float[numSegments][3];
        int[] segmentSizes = new int[numSegments];

        //sum colorvalues
        for(int i = 0; i < numPixelsInImage; i++){
            int segmentIndex = segmentation[i];
            segmentAverageColors[segmentIndex][0] += pixels.get(i).getRed();
            segmentAverageColors[segmentIndex][1] += pixels.get(i).getGreen();
            segmentAverageColors[segmentIndex][2] += pixels.get(i).getBlue();
            segmentSizes[segmentIndex] += 1;
        }

        // divide by size of segments to get average(centroid)
        for(int i = 0; i < segmentAverageColors.length; i++){
            for(int j = 0; j < 3; j++){
                segmentAverageColors[i][j] = segmentAverageColors[i][j] / segmentSizes[i];

            }

        }



        //compare pixel-colors to centroid-color of segment
        double overallDeviation = 0;
        for(int i = 0; i < numPixelsInImage; i++){

            Color segmentCentroid = new Color((int)segmentAverageColors[segmentation[i]][0], //red
                    (int)segmentAverageColors[segmentation[i]][1], //green
                    (int)segmentAverageColors[segmentation[i]][2]); //blue
            overallDeviation += RGBDistance(pixels.get(i), segmentCentroid);
        }

        this.overallDeviation = overallDeviation;
        return overallDeviation;

    }


    /**
     *  evaluates the degree to which neighbour pixels have been placed in the same segment
     *  should be MINimized
     */
    public double connectivityMeasure(){

        if(segmentationOutdated) calculateSegmentation();
        double connectivityMeasure = 0.0;
        for(int i = 0; i < numPixelsInImage; i++){
            ArrayList<Integer> neighbours = getNeighborPixels(i);


            for(int neighbourIndex : neighbours){
                //System.out.println("\n current pixelid and neighbour id");
                //System.out.println(segmentation[i]);
                //System.out.println(segmentation[neighbourIndex]);
                if(segmentation[i] != segmentation[neighbourIndex]){

                    connectivityMeasure += 1.0 / 8.0;  //  1/L  instead of 1/j  , as suggested in the FAQ/forum on blackboard.
                }
            }

        }

        this.connectivityMeasure = connectivityMeasure;
        return connectivityMeasure;


    }






    /**
     * a measure of the difference in the boundary between the segments
     * should be MAXimized
     */
    public double edgeValue(){

        if(segmentationOutdated) calculateSegmentation();

        double edgeValue = 0.0;
        for( int i = 0; i < numPixelsInImage; i++){
            ArrayList<Integer> neighbours = getNeighborPixels(i);

            // in the equation in the exercise description it says the 4 neighbouring pixels
            // but I am using all 8 here.
            for(int neighbourIndex : neighbours){
                if(segmentation[i] != segmentation[neighbourIndex]){
                    //System.out.println(RGBDistance(pixels.get(neighbourIndex), pixels.get(i)));
                    edgeValue -= RGBDistance(pixels.get(neighbourIndex), pixels.get(i));
                }
            }
        }

        this.edgeValue = edgeValue;
        return edgeValue;
    }


    public void setObjectiveValues(){
        overallDeviation();
        connectivityMeasure();
        edgeValue();
    }


    /**
     *
     * Returns true if this genotype dominates the param-genotype
     */
    public boolean dominates(Genotype otherGenotype){
        if(overallDeviation > otherGenotype.overallDeviation) return false;
        if(connectivityMeasure > otherGenotype.connectivityMeasure) return false;
        //if(edgeValue > otherGenotype.edgeValue) return false;

        if(overallDeviation < otherGenotype.overallDeviation) return true;
        if(connectivityMeasure < otherGenotype.connectivityMeasure) return true;
        //if(edgeValue < otherGenotype.edgeValue) return true;

        return false;
    }


    static Comparator<Genotype> crowdingDistanceComparator() {
        return (o1, o2) -> {
            if (o1.crowdingDistance > o2.crowdingDistance) return -1;
            if (o1.crowdingDistance < o2.crowdingDistance) return 1;
            return 0;
        };
    }


    static Comparator<Genotype> nonDominationRankAndCrowdingDistanceComparator() {
        return (o1, o2) -> {
            if (o1.rank < o2.rank) return -1;
            if (o1.rank > o2.rank) return 1;
            if (o1.crowdingDistance > o2.crowdingDistance) return -1;
            if (o1.crowdingDistance < o2.crowdingDistance) return 1;
            return 0;
        };
    }

    static Comparator<Genotype> overallDeviationComparator() {
        return (o1, o2) -> {
            if (o1.overallDeviation < o2.overallDeviation) return -1;
            if (o1.overallDeviation > o2.overallDeviation) return 1;
            return 0;
        };
    }



    static Comparator<Genotype> connectivityComparator() {
        return (o1, o2) -> {
            if (o1.connectivityMeasure < o2.connectivityMeasure) return -1;
            if (o1.connectivityMeasure > o2.connectivityMeasure) return 1;
            return 0;
        };
    }


    static Comparator<Genotype> edgeValueComparator() {
        return (o1, o2) -> {
            if (o1.edgeValue < o2.edgeValue) return -1;
            if (o1.edgeValue > o2.edgeValue) return 1;
            return 0;
        };
    }






}
