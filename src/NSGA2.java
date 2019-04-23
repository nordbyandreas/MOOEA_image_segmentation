import sun.plugin.javascript.navig.Array;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Andreas on 06.03.2019.
 */
public class NSGA2 {




    public ArrayList<Genotype> population = new ArrayList<>();
    public ArrayList<Genotype> childPopulation = new ArrayList<>();

    public ArrayList<Genotype> paretoOptimalFront = new ArrayList<>();

    public ArrayList<ArrayList<Genotype>> paretoFronts = new ArrayList<>();




    public void threadedRemoveLargestEdges(ArrayList<Genotype> front){
        List<Genotype> popInProgress = Collections.synchronizedList(new ArrayList<>(Main.populationSize*2));
        ArrayList<Genotype> r = new ArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for(int i = 0; i < front.size(); i++){
            final int index = i;
            executorService.execute(() -> {

                int tenPercent = (Main.mergeSegmentsSmallerThan  * 10) / 100;
                int current = ThreadLocalRandom.current().nextInt(0, Main.mergeSegmentsMultiplier);
                current = 1 + current;
                front.get(index).mergeSegmentsSmallerThanN2(Main.mergeSegmentsSmallerThan + current, false);

            });
        }

        executorService.shutdown();
        while (! executorService.isTerminated());


    }


    public void threadedInitPopulation(){
        List<Genotype> popInProgress = Collections.synchronizedList(new ArrayList<>(Main.populationSize*2));
        ImageReader imageReader = new ImageReader(Main.imageUrl);

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int i = 0; i < Main.populationSize; i++){
            final int index = i;
            executorService.execute(() -> {
                Genotype temp = new Genotype(imageReader.width, imageReader.height, imageReader);
                int tenPercent = (Main.initEdgeRemoveNum  * 10) / 100;
                int current = ThreadLocalRandom.current().nextInt(0, tenPercent);
                temp.removeNLargestEdges(Main.lowestNumSegments + index);
                tenPercent = (Main.mergeSegmentsSmallerThan  * 10) / 100;
                current = ThreadLocalRandom.current().nextInt(0, tenPercent);

                //temp.mergeSegmentsSmallerThanN2(Main.mergeSegmentsSmallerThan + current, false);


                temp.setObjectiveValues();

                popInProgress.add(temp);
            });
        }
        executorService.shutdown();
        while (! executorService.isTerminated());
        this.population.addAll(popInProgress);


    }


    public void initializePopulation(){

        System.out.println("--- INITIALIZING POPULATION ---   ( size = " + Main.populationSize +  " )");

        ImageReader imageReader = new ImageReader(Main.imageUrl);

        for(int i = 0; i < Main.populationSize; i++){
            System.out.println("\n");
            System.out.print(" " + (i + 1)  + "/" + Main.populationSize + ", ");
            System.out.println("");
            Genotype genotype = new Genotype(imageReader.width, imageReader.height, imageReader);

            //TODO:  determine a good heuristic for initialization and use here (numSegments target for start feks.)

            int tenPercent = (Main.initEdgeRemoveNum  * 10) / 100;

            int current = ThreadLocalRandom.current().nextInt(-tenPercent, tenPercent);

            System.out.println(current);


            genotype.removeNLargestEdges(Main.initEdgeRemoveNum + current);

            System.out.println("numsegments after MSTinit and remove N largest edges: " + genotype.numSegments);

            tenPercent = (Main.mergeSegmentsSmallerThan  * 10) / 100;

            current = ThreadLocalRandom.current().nextInt(-tenPercent, tenPercent);
            System.out.println(current);
            genotype.mergeSegmentsSmallerThanN2(Main.mergeSegmentsSmallerThan + current, false);

            System.out.println("numsegments after merge segents smaller than k: " + genotype.numSegments);


            genotype.setObjectiveValues();

            System.out.println("\n");
            System.out.println(genotype.overallDeviation);
            System.out.println(genotype.connectivityMeasure);
            System.out.println(genotype.edgeValue);

            population.add(genotype);

        }
        System.out.println("\n");
    }


    public void weightedSumRun(){

        System.out.println("\n\nINITIALIZTION START\n\n");

        //INITIALIZATION  BEGIN

        // 1. create random startPopulation (some heuristics ofc)
        threadedInitPopulation();


        // INITIALIZATION END
        System.out.println("\n\nINITIALIZTION END \n\n");




        ArrayList<Genotype> parents = new ArrayList<>();

        // EVOLUTION LOOP
        int numIterations = 1;
        while (numIterations < Main.numIterations){

            System.out.println("\n");
            System.out.println("GENERATION NUMBER: " + (numIterations+1));
            System.out.println("\n");



            System.out.println("popsize = " + population.size());
            System.out.println("Childpopsize = " + childPopulation.size());

            // 7. childPopulation = make new populatuion with selection/crossover and mutation from parents
            childPopulation = threadedMakeChildPopulationWeightedSum();



            // 1. Pop = Combine parent and child popul
            population.addAll(childPopulation);


            //SELECT WHO LIVES TO NEXT GENERATION
            ArrayList<Genotype> tempPop = new ArrayList<>();


            //TODO implement elitism

            tempPop = threadedSelectionTorunament();
            population.clear();

            population.addAll(tempPop);

            childPopulation.clear();


            // 8. NEXT iter
            numIterations += 1;

        }

        Genotype g = population.get(0);



        ImageWriter imageWriter = new ImageWriter(g);
        imageWriter.clearOutputFolder();



        System.out.println("remove largest edges start");



        //threadedRemoveLargestEdges(population);

        // get 5 best solutions

        ArrayList<Genotype> bestFive = getBestFive();


        for(int i = 0; i < bestFive.size(); i++){
            imageWriter.writeAllSolutionImagesAndPrint((i+1), bestFive.get(i));

        }


        plottPareto(bestFive, "WEIGHTED SUM");

    }



    public void run(){


        System.out.println("\n\nINITIALIZTION START\n\n");

        //INITIALIZATION  BEGIN

        // 1. create random startPopulation (some heuristics ofc)


        long startTime = System.nanoTime();
        //initializePopulation();
        threadedInitPopulation();
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1000000;

        System.out.println("\n\n\n\n INIT Duration: " + duration + " \n\n\n\n\n ");

        System.out.println("init end");


        // 2. sort population according to nondomination
        nonDominatedSort();

        System.out.println("nondominationsort end");

        // 3. Each solution gets a fitness equal to its non-domination level (rank)
            // DONE IN nonDominatedSort()

        // 4. Use binary tournament, mutation, crossover to create first child populationof Size N
        System.out.println("makechildpop start");
        startTime = System.nanoTime();
        //childPopulation = makeChildPopulation();
        childPopulation = threadedMakeChildPopulation();
        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000;
        System.out.println("\n\n\n\n MAKECHILDPOPULATION Duration: " + duration + " \n\n\n\n\n ");

        System.out.println("makechildpop end");

        // INITIALIZATION END
        System.out.println("\n\nINITIALIZTION END \n\n");



        // EVOLUTION LOOP
        int numIterations = 1;
        while (numIterations < Main.numIterations + 1){


            // 1. Pop = Combine parent and child population
            population.addAll(childPopulation);
            childPopulation.clear();


            System.out.println("\n");
            System.out.println("GENERATION NUMBER: " + (numIterations));
            System.out.println("\n");


            if(numIterations % 10 == 0){

                //threadedRemoveLargestEdges(population);

                for(int i = population.size() - 1; i > -1; i--){
                    if(population.get(i).connectivityMeasure < Main.removeCMlowerThan || population.get(i).numSegments < Main.lowestNumSegments
                            || population.get(i).overallDeviation > Main.removeODhigherThan) {
                        population.remove(i);

                    }

                }



            }




          /*  //TODO REMOVE EXTREMES
            int counter = 0;
            for(int i = 0; i < population.size(); i++){
                Genotype g = population.get(i);

                if(g.numSegments < Main.lowestNumSegments || g.numSegments > Main.highestNumSegments){
                    population.remove(i);
                    i -= 1;
                }
                counter ++;
                if(counter == 10) break;
            }
*/


            // 2. F = Sort pop by non-domination
            nonDominatedSort();


        /*    if(numIterations % 20 == 0){
                threadedRemoveLargestEdges(paretoFronts.get(0));
            }*/



            // 3. parentPopulation = NULL
            // 4. While parentpop.size < N:
            // calculate crowdingdistance in Fi
            // include ith nondomination front in the parentpop
            // 5.  sort in decending order by "crowded comparison operator
            // 6. parentPopulation = parentPopulation[0:N]  - choose N first elements
            selectParentsFromParetoFronts();


            // 7. childPopulation = make new populatuion with selection/crossover and mutation from parents
            //childPopulation = makeChildPopulation();
            childPopulation = threadedMakeChildPopulation();



            // 8. NEXT iter
            numIterations += 1;
        }


        //TODO: PRINT up to 5 pareto solutions


        population.addAll(childPopulation);


      /*  for(int i = 0; i < population.size(); i++){
            Genotype g = population.get(i);

            if(g.numSegments < Main.lowestNumSegments || g.numSegments > Main.highestNumSegments){
                population.remove(i);
                i -= 1;
            }

        }*/




        nonDominatedSort();
        selectParentsFromParetoFronts();
        population.sort(Genotype.nonDominationRankAndCrowdingDistanceComparator());

        System.out.println("population size: " + population.size());


        Genotype g = population.get(0);



        ImageWriter imageWriter = new ImageWriter(g);
        imageWriter.clearOutputFolder();
        ArrayList<Genotype> paretoOptimal = paretoFronts.get(0);



        System.out.println("\nMerging segments..");
        startTime = System.nanoTime();



        threadedRemoveLargestEdges(paretoOptimal);

        for(int i = paretoOptimal.size() - 1; i > -1; i--){
            if(paretoOptimal.get(i).connectivityMeasure < Main.removeCMlowerThan || paretoOptimal.get(i).overallDeviation > Main.removeODhigherThan) {
                paretoOptimal.remove(i);

            }
        }



        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000;
        System.out.println("\n\n\n\n Threaded remove leargest edges Duration: " + duration + " \n\n\n\n\n ");


        for(int i = 0; i < paretoOptimal.size(); i++){
            imageWriter.writeAllSolutionImagesAndPrint((i+1), paretoOptimal.get(i));

        }

        plottPareto(paretoOptimal, "1");


    }

    public void selectParentsFromParetoFronts(){


        population.clear();

        for(ArrayList<Genotype> front : paretoFronts){
            crowdingDistanceAssignment(front);
            if(population.size() + front.size() < Main.populationSize){
                population.addAll(front);
            }
            else{
                ArrayList<Genotype> frontCopy = new ArrayList<>(front);
                frontCopy.sort(Genotype.crowdingDistanceComparator());
                while(population.size() < Main.populationSize){
                    population.add(frontCopy.remove(0));
                }

            }

        }
    }

    public ArrayList<Genotype> makeChildPopulationWeightedSum(){
        childPopulation.clear();
        ArrayList<Genotype> children = new ArrayList<>();

        System.out.println("inside makeChildPopulation");

        for(int i = 0; i < Main.populationSize; i++){
            System.out.println("inside makeChildPopulation loop: " + i);
            Genotype g1 = weightedSumTorunament(population);
            System.out.println("bin1 ran");
            System.out.println(g1);
            Genotype g2 = weightedSumTorunament(population);
            System.out.println("bin2 ran");
            System.out.println(g2);
            Genotype child = new Genotype(g1, g2);
            System.out.println("crossover ran");
            children.add(child);
        }

        return children;

    }



    public ArrayList<Genotype> threadedSelectionTorunament(){

        List<Genotype> childPopInProgress = Collections.synchronizedList(new ArrayList<>(Main.populationSize*2));
        ArrayList<Genotype> newPop = new ArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int i = 0; i < Main.populationSize; i++){
            final int index = i;
            executorService.execute(() -> {

                Genotype g1 = weightedSumTorunament(population);
                Genotype g2 = weightedSumTorunament(population);
                Genotype child = new Genotype(g1, g2);
                childPopInProgress.add(child);
            });
        }
        executorService.shutdown();
        while (! executorService.isTerminated());
        newPop.addAll(childPopInProgress);

        return newPop;
    }


    public ArrayList<Genotype> threadedMakeChildPopulationWeightedSum(){
        childPopulation.clear();
        List<Genotype> childPopInProgress = Collections.synchronizedList(new ArrayList<>(Main.populationSize*2));
        ArrayList<Genotype> children = new ArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int i = 0; i < Main.populationSize; i++){
            final int index = i;
            executorService.execute(() -> {

                Genotype g1 = weightedSumTorunament(population);
                Genotype g2 = weightedSumTorunament(population);
                Genotype child = new Genotype(g1, g2);
                childPopInProgress.add(child);
            });
        }
        executorService.shutdown();
        while (! executorService.isTerminated());
        children.addAll(childPopInProgress);

        return children;
    }


    public ArrayList<Genotype> threadedMakeChildPopulation(){
        childPopulation.clear();
        List<Genotype> childPopInProgress = Collections.synchronizedList(new ArrayList<>(Main.populationSize*2));
        ArrayList<Genotype> children = new ArrayList<>();

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int i = 0; i < Main.populationSize; i++){
            final int index = i;
            executorService.execute(() -> {

                Genotype g1 = quatroTournament(population);
                Genotype g2 = quatroTournament(population);
                Genotype child = new Genotype(g1, g2);
                childPopInProgress.add(child);
            });
        }
        executorService.shutdown();
        while (! executorService.isTerminated());
        children.addAll(childPopInProgress);

        return children;
    }


    public ArrayList<Genotype> makeChildPopulation(){
        childPopulation.clear();
        ArrayList<Genotype> children = new ArrayList<>();

        System.out.println("inside makeChildPopulation");

        for(int i = 0; i < Main.populationSize; i++){
            System.out.println("inside makeChildPopulation loop: " + i);
            Genotype g1 = binaryTournament(population);
            System.out.println("bin1 ran");
            System.out.println(g1);
            Genotype g2 = binaryTournament(population);
            System.out.println("bin2 ran");
            System.out.println(g2);
            Genotype child = new Genotype(g1, g2);
            System.out.println("crossover ran");
            children.add(child);
        }

        return children;
    }


    public ArrayList<Genotype> getBestFive(){
        ArrayList<Genotype> bestFive = new ArrayList<>();


        while(bestFive.size() < 10){
            Genotype best = population.get(0);

            for(int i = 0; i < population.size(); i++){

                /*
                //with edge value
                double ws = population.get(i).overallDeviation * Main.odWeight + population.get(i).connectivityMeasure * Main.cmWeight + population.get(i).edgeValue * Main.evWeight;
                double ows = best.overallDeviation * Main.odWeight + best.connectivityMeasure * Main.cmWeight + best.edgeValue * Main.evWeight;
*/

                double ws = population.get(i).overallDeviation * Main.odWeight + population.get(i).connectivityMeasure * Main.cmWeight ;
                double ows = best.overallDeviation * Main.odWeight + best.connectivityMeasure * Main.cmWeight;


                if(ws < ows){
                    best = population.get(i);
                }
            }
            bestFive.add(best);
            population.remove(population.indexOf(best));

            if(population.size() == 0) break;

        }

        return bestFive;
    }

    public Genotype weightedSumTorunament(ArrayList<Genotype> population){
        int r = ThreadLocalRandom.current().nextInt(0, population.size());
        Genotype best = population.get(r);

        for(int i = 0; i < 5; i++){
            int t = ThreadLocalRandom.current().nextInt(0, population.size());


         /*
            // with edge value
            double ws = population.get(t).overallDeviation * Main.odWeight + population.get(t).connectivityMeasure * Main.cmWeight + population.get(t).edgeValue * Main.evWeight;
            double ows = best.overallDeviation * Main.odWeight + best.connectivityMeasure * Main.cmWeight + best.edgeValue * Main.evWeight;
*/

            double ws = population.get(t).overallDeviation * Main.odWeight + population.get(t).connectivityMeasure * Main.cmWeight ;
            double ows = best.overallDeviation * Main.odWeight + best.connectivityMeasure * Main.cmWeight;


            if(ws < ows){
                best = population.get(t);
            }

        }

        return best;
    }



    public Genotype quatroTournament(ArrayList<Genotype> population){
        int firstIndex = ThreadLocalRandom.current().nextInt(0, population.size());
        int secondIndex = ThreadLocalRandom.current().nextInt(0, population.size());
        int thirdIndex = ThreadLocalRandom.current().nextInt(0, population.size());
        int fourthIndex = ThreadLocalRandom.current().nextInt(0, population.size());

        Genotype g1 = population.get(firstIndex);
        Genotype g2 = population.get(secondIndex);
        Genotype g3 = population.get(thirdIndex);
        Genotype g4 = population.get(fourthIndex);

        Genotype f1;
        Genotype f2;

        if(Genotype.nonDominationRankAndCrowdingDistanceComparator().compare(g1, g2) < 0){
            f1 =  g1;
        }
        else{
            f1 =  g2;
        }
        if(Genotype.nonDominationRankAndCrowdingDistanceComparator().compare(g3, g4) < 0){
            f2 =  g3;
        }
        else{
            f2 =  g4;
        }
        if(Genotype.nonDominationRankAndCrowdingDistanceComparator().compare(f1, f2) < 0){
            return f1;
        }
        else{
            return f2;
        }
    }



    public Genotype binaryTournament(ArrayList<Genotype> population){
        int firstIndex = ThreadLocalRandom.current().nextInt(0, population.size());
        int secondIndex = ThreadLocalRandom.current().nextInt(0, population.size());
        Genotype g1 = population.get(firstIndex);
        Genotype g2 = population.get(secondIndex);

        if(Genotype.nonDominationRankAndCrowdingDistanceComparator().compare(g1, g2) < 0){
            return g1;
        }
        else{
            return g2;
        }

    }

    public void crowdingDistanceAssignment(ArrayList<Genotype> paretoFront){
        for(Genotype g: paretoFront) g.crowdingDistance = 0;

        assignCrowdingDistanceForObjective(paretoFront, 0);
        assignCrowdingDistanceForObjective(paretoFront, 1);
        //assignCrowdingDistanceForObjective(paretoFront, 2);

    }


    public void assignCrowdingDistanceForObjective(ArrayList<Genotype> paretoFront, int objectiveIndex){
        if(objectiveIndex == 0) paretoFront.sort(Genotype.overallDeviationComparator());
        if(objectiveIndex == 1) paretoFront.sort(Genotype.connectivityComparator());
        if(objectiveIndex == 2) paretoFront.sort(Genotype.edgeValueComparator());

        paretoFront.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
        paretoFront.get(paretoFront.size() - 1).crowdingDistance = Double.POSITIVE_INFINITY;

        for(int i = 1; i < paretoFront.size() -1; i++){
            if(objectiveIndex == 0){
                paretoFront.get(i).crowdingDistance += Math.abs(paretoFront.get(i+1).overallDeviation - paretoFront.get(i-1).overallDeviation);
            }
            if(objectiveIndex == 1){
                paretoFront.get(i).crowdingDistance += Math.abs(paretoFront.get(i+1).connectivityMeasure - paretoFront.get(i-1).connectivityMeasure);
            }
            if(objectiveIndex == 2){
                paretoFront.get(i).crowdingDistance += Math.abs(paretoFront.get(i+1).edgeValue - paretoFront.get(i-1).edgeValue);
            }
        }


    }




    /**
     * Sort population by nondomination (solutions that are not dominated are ranked highest)
     */
    public void nonDominatedSort(){

        //create temp copy of population to keep track
        ArrayList<Genotype> tempPopulation = new ArrayList<>();
        for(int i = 0; i < population.size(); i++) tempPopulation.add(population.get(i));

   /*     System.out.println("NON-DOMINATION SORT BEGIN");*/

        paretoFronts.clear();
        int paretoFrontsSize = 0;
        int popSize = tempPopulation.size();
        int rankCounter = 1;

     /*   for(Genotype g : tempPopulation){
            System.out.println(g);
            System.out.println(g.overallDeviation);
            System.out.println(g.connectivityMeasure);
            System.out.println(g.edgeValue);
            System.out.println("\n");
        }*/

        while (paretoFrontsSize < popSize){

          /*  System.out.println(paretoFrontsSize);
            System.out.println(tempPopulation.size());
            System.out.println(paretoFronts.size());
*/

            ArrayList<Genotype> currentRank = new ArrayList<>();

            //add first solution to rank so that we have something to compare with
            currentRank.add(tempPopulation.get(0));


            //loop through remaining population
            for( Genotype g : tempPopulation){
                boolean includeSolution = true;

                //list of solutions in rank that G dominates - to be removed after comparizon
                ArrayList<Genotype> dominatedSolutions = new ArrayList<>();

                if(g.equals(currentRank.get(0))){

                    continue;
                }

                //compare each member of population to all solutions currently in this rank
                for( int i = 0; i < currentRank.size(); i++){

                    // if any solution in this rank dominates this solution we wont include it
                    if(currentRank.get(i).dominates(g)){

                        includeSolution = false;
                    }

                    // if this solution dominates any of the solutions in this rank we should remove them later
                    if(g.dominates(currentRank.get(i))){

                        dominatedSolutions.add(currentRank.get(i));
                    }
                }


                // add solution if no member of currentrank dominates it
                if(includeSolution){
                    currentRank.add(g);
                }


           /*     System.out.println("\n");
                for(Genotype x : currentRank){
                    System.out.println(x);
                }
*/

             /*   System.out.println("curent rank before removal  " + currentRank.size());

                System.out.println("dominated solution len = " + dominatedSolutions.size());
             */

                //remove members of currentrank that are dominated by g
                for(int i = 0; i < dominatedSolutions.size(); i++){
                    currentRank.remove(currentRank.indexOf(dominatedSolutions.get(i)));

                }

              /*  System.out.println("currentrank after removal  " + currentRank.size());

*/
            }


          /*  System.out.println("\n population LOOP \n");*/

            paretoFrontsSize += currentRank.size();
            paretoFronts.add(currentRank);

            //remove already ranked solutions from population, so that they dont get included in next rank
           /* System.out.println("popSize before removal:  " + tempPopulation.size());*/
            for(int i = 0; i < currentRank.size(); i++){
                currentRank.get(i).rank = rankCounter;
                tempPopulation.remove(tempPopulation.indexOf(currentRank.get(i)));
            }

          /*  System.out.println("popSize after removal:   " + tempPopulation.size());*/
            rankCounter += 1;


        }
/*

        System.out.println("temppop size" + tempPopulation.size());
        System.out.println("po size " + population.size());

        System.out.println("NON-DOMINATION SORT END");
*/
/*
        System.out.println(paretoFronts.size());


        for(int i = 0; i < paretoFronts.size(); i++){
            System.out.println("\nRANK " + (i+1));
            for(int j = 0; j < paretoFronts.get(i).size(); j++){
                System.out.print(" Overall Deviation:      " + paretoFronts.get(i).get(j).overallDeviation
                        + "      Connectivity measure:    " +  paretoFronts.get(i).get(j).connectivityMeasure +
                        "     Edge value:    " + paretoFronts.get(i).get(j).edgeValue
                        + "   Rank:    "  + paretoFronts.get(i).get(j).rank);
                System.out.println("");
            }

        }*/

    }



    private void plottPareto(ArrayList<Genotype> solutions, String tittelAddOn) {
        SwingUtilities.invokeLater(() -> {
            Plot plot = new Plot(solutions, tittelAddOn);
            plot.setSize(1000, 600);
            plot.setLocationRelativeTo(null);
            plot.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            plot.setVisible(true);
        });
    }


}
