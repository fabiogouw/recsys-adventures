package org.grouplens.mooc.recsyspa1;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.grouplens.lenskit.collections.LongUtils;

public class AdvancedRanking extends SimpleRanking {

	public AdvancedRanking() throws FileNotFoundException {
		super();
	}
	
	@Override
	public List<Result> getRankingForTheMovie(long theMovieId) {
    	List<Result> scores = new ArrayList<Result>();
    	LongSet allOtherMovies = getAllMoviesExceptOne(theMovieId);
    	LongSet usersWhoRatedTheMovie = getUsersWhoRatedMovie(theMovieId);	// x
    	LongSet usersWhoNotRatedTheMovie = getUsersWhoNotRatedMovie(theMovieId);	// !x
        for(long movie : allOtherMovies) {
        	LongSet usersWhoRatedOtherMovie = getUsersWhoRatedMovie(movie);	// y
        	LongSet usersWhoRatedBothMovies = getLongSetIntersection(usersWhoRatedTheMovie, usersWhoRatedOtherMovie);	// x + y
        	LongSet usersWhoRatedJustTheOtherMovie = getLongSetIntersection(usersWhoNotRatedTheMovie, usersWhoRatedOtherMovie);	// !x + y
        	float score = ((float) usersWhoRatedBothMovies.size() / (float) usersWhoRatedTheMovie.size()) 
        			/ ((float) usersWhoRatedJustTheOtherMovie.size() / (float) usersWhoNotRatedTheMovie.size());
        	scores.add(new Result(movie, score));        	
        }
        return scores;
	}
	
    protected LongSet getUsersWhoNotRatedMovie(long movieId){
    	LongSet allUsers = getAllusers();
    	LongSet usersWhoRatedTheMovie = getUsersWhoRatedMovie(movieId);
    	LongSortedSet usersWhoNotRatedTheMovie = LongUtils.setDifference(allUsers, usersWhoRatedTheMovie);
    	return usersWhoNotRatedTheMovie;
    	/*List<Long> users = new ArrayList<Long>();
    	for(Ranking ranking : _rakings) {
    		if(movieId != ranking.MovieId && !users.contains(ranking.UserId)){
    			users.add(ranking.UserId);
    		}
    	}
    	long[] array = ArrayUtils.toPrimitive(users.toArray(new Long[users.size()]));
    	return new LongArraySet(array);*/
    }
    
    protected LongSet getAllusers() {
    	List<Long> users = new ArrayList<Long>();
    	for(Ranking ranking : _rakings) {
    		if(!users.contains(ranking.UserId)){
    			users.add(ranking.UserId);
    		}
    	}
    	long[] array = ArrayUtils.toPrimitive(users.toArray(new Long[users.size()]));
    	return new LongArraySet(array);
    }
}