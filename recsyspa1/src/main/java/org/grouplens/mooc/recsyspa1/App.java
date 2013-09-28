package org.grouplens.mooc.recsyspa1;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class App {
    public static void main(String[] args) throws IOException {
    	//SimpleRanking simpleRanking = new SimpleRanking();
    	SimpleRanking simpleRanking = new AdvancedRanking();
        try {
        	simpleRanking.loadRatings();
        	int[] myMovies = new int[] { 161, 1894, 153 };
        	//int[] myMovies = new int[] { 11, 121, 8587 };
        	for(int myMovie : myMovies) {
            	List<Result> results = simpleRanking.getRankingForTheMovie(myMovie);
            	System.out.print(myMovie + ",");
            	printResults(results);
            	System.out.println();
        	}
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printResults(List<Result> results){
        Collections.sort(results, new ResultComparator());
        int i = 0;
    	for(Result result : results) {
    		System.out.print(String.format(Locale.ENGLISH, "%s,%.2f", result.MovieId, result.Score));
    		if(++i >= 5)
    			break;
    		System.out.print(",");
    	}
    }
}
