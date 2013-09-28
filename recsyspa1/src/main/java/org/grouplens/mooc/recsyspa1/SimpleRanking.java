package org.grouplens.mooc.recsyspa1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import org.apache.commons.lang3.ArrayUtils;
import org.grouplens.lenskit.collections.LongUtils;

public class SimpleRanking {
	
	public SimpleRanking() throws FileNotFoundException {
	}

	private BufferedReader _reader = new BufferedReader(new FileReader("data\\recsys-data-ratings.csv"));
	protected List<Ranking> _rakings = new ArrayList<Ranking>();
	
	public void loadRatings() throws IOException{
		String line = null;
		_rakings.clear();
		while ((line = _reader.readLine()) != null) {
		    String[] tokens = line.split(",");
		    Ranking ranking = new Ranking();
		    ranking.UserId = Long.parseLong(tokens[0]);
		    ranking.MovieId = Long.parseLong(tokens[1]);
		    ranking.Score = Float.parseFloat(tokens[2]);
		    _rakings.add(ranking);
		}
	}
	
    public List<Result> getRankingForTheMovie(long theMovieId) {
    	List<Result> scores = new ArrayList<Result>();
    	LongSet allOtherMovies = getAllMoviesExceptOne(theMovieId);
    	LongSet usersWhoRatedTheMovie = getUsersWhoRatedMovie(theMovieId);   	
        for(long movie : allOtherMovies) {
        	LongSet usersWhoRatedOtherMovie = getUsersWhoRatedMovie(movie);
        	LongSet intersection = getLongSetIntersection(usersWhoRatedTheMovie, usersWhoRatedOtherMovie);
        	float score = (float) intersection.size() / usersWhoRatedTheMovie.size();
        	scores.add(new Result(movie, score));        	
        }
        return scores;
    }
    
    protected LongSet cloneLongSet(final LongSet original){
    	List<Long> copies = new ArrayList<Long>();
    	for(long item : original) {
    		copies.add(item);
    	}
    	long[] array = ArrayUtils.toPrimitive(copies.toArray(new Long[copies.size()]));
    	return new LongArraySet(array);
    }
    
    protected LongSet getUsersWhoRatedMovie(long movieId){
    	List<Long> users = new ArrayList<Long>();
    	for(Ranking ranking : _rakings) {
    		if(movieId == ranking.MovieId && !users.contains(ranking.UserId)){
    			users.add(ranking.UserId);
    		}
    	}
    	long[] array = ArrayUtils.toPrimitive(users.toArray(new Long[users.size()]));
    	return new LongArraySet(array);
    }
    
    protected LongSet getAllMoviesExceptOne(long movieId){
    	List<Long> movies = new ArrayList<Long>();
    	for(Ranking ranking : _rakings) {
    		if(!movies.contains(ranking.MovieId) && movieId != ranking.MovieId){
    			movies.add(ranking.MovieId);
    		}
    	}
    	long[] array = ArrayUtils.toPrimitive(movies.toArray(new Long[movies.size()]));
    	return new LongArraySet(array);
    }
    
    protected LongSet getLongSetIntersection(final LongSet first, final LongSet second) {
        LongSortedSet set3 = LongUtils.setDifference(first, second);
        LongSortedSet set4 = LongUtils.setDifference(second, first);
        LongSet firstCloned = cloneLongSet(first);
        for(Long item : set3) {
            if(firstCloned.contains(item))
            	firstCloned.remove(item);
        }
        for(Long item : set4) {
            if(firstCloned.contains(item)) {
            	firstCloned.remove(item);
            }
        }
        return firstCloned; 
    }
}
