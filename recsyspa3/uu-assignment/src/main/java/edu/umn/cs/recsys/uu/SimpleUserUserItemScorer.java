package edu.umn.cs.recsys.uu;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.ItemEventDAO;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * User-user item scorer.
 * @author fabiogouw
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final UserEventDAO userDao;
    private final ItemEventDAO itemDao;
    private final CosineVectorSimilarity cosCalculator;
    private double userAverageRating;
    private SparseVector userMeanCenteredRatingVector;
    private Map<Long, Double> similarities;
    private Map<Long, Double> ratingFromOtherUsers;
    private Map<Long, Double> averageRatingFromOtherUsers;

    // 928:1892 928:274 928:568 928:98 928:9741 4734:5503 4734:22 4734:604 4734:807 4734:10020 1964:3049 1964:664 1964:24 1964:105 1964:641 4554:581 4554:1892 4554:114 4554:197 4554:752 4305:280 4305:4327 4305:786 4305:788 4305:581
    
    @Inject
    public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao) {
        userDao = udao;
        itemDao = idao;
        cosCalculator = new CosineVectorSimilarity();
        ratingFromOtherUsers = new HashMap<Long, Double>();
        averageRatingFromOtherUsers = new HashMap<Long, Double>();
        similarities = new HashMap<Long, Double>(); 
    }

    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
    	calculateUserData(user);
        // This is the loop structure to iterate over items to score
        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
        	long movie = e.getKey(); 
        	clearLists();
        	// get the people who have rated this movie
        	LongSet usersWhoRatedThisItem = itemDao.getUsersForItem(movie);
            for(long otherUser : usersWhoRatedThisItem) {
            	// we don't have to calculate the similarity for the user we're scoring
            	if(otherUser != user) {
            		SparseVector otherUserVector = getUserRatingVector(otherUser);
            		// we'll use the rating for this movie later
            		ratingFromOtherUsers.put(otherUser, otherUserVector.get(movie));
            		// we'll use the mean rating for this user later
            		storeOtherUserRatingAverage(otherUser, otherUserVector);
            		// calculating the similarity for this user
            		similarities.put(otherUser, calculareSimilarity(otherUserVector));
            	}
            }
            // now that we have all information from users who rated the movie, we predict the main user's score
            double score = calculateScore(similarities);
            scores.set(movie, score);
        }
    }
    
    /**
     * Get the information needed to calculate score from the main user.
     * @param user The user ID.
     */
    private void calculateUserData(long user) {
        SparseVector userVector = getUserRatingVector(user);
        userAverageRating = getAverageRating(userVector);
        userMeanCenteredRatingVector = getUserMeanCenteredRatingVector(userVector);
    }
    
    /**
     * Get a user's rating vector.
     * @param user The user ID.
     * @return The rating vector.
     */
    private SparseVector getUserRatingVector(long user) {
        UserHistory<Rating> history = userDao.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }
        return RatingVectorUserHistorySummarizer.makeRatingVector(history);
    }
    
    /**
     * Clear the working lists used to store information from other users.
     */
    private void clearLists() {
        ratingFromOtherUsers.clear();
        averageRatingFromOtherUsers.clear();
        similarities.clear();
    }
    
    /**
     * Calculate the similarity (cosine) between two different user vectors
     * @param otherUserVector The vector of the user we're comparing to the main one.
     */
    private double calculareSimilarity(SparseVector otherUserVector) {
    	SparseVector otherUserMeanCenteredRatingVector = getUserMeanCenteredRatingVector(otherUserVector);
		return cosCalculator.similarity(userMeanCenteredRatingVector, otherUserMeanCenteredRatingVector);
    }
    
    /**
     * Stores the average rating of the user we're comparing to the main one.
     * @param otherUser - Other user Id.
     * @param otherUserVector - The other user's rating vector.
     */
    private void storeOtherUserRatingAverage(long otherUser, SparseVector otherUserVector) {
    	// we calculate if it hasn't been done yet
    	if(!averageRatingFromOtherUsers.containsKey(otherUser)) {
    		double averageFromOtherUser = getAverageRating(otherUserVector);
    		averageRatingFromOtherUsers.put(otherUser, averageFromOtherUser);
    	}
    }
    
    /**
     * Predicts the score for a movie to the main user.
     * @param similarities A list containing the users and their similarities to the main user.
     * @return The predicted score.
     */
    private double calculateScore(Map<Long, Double> similarities) {
        double sumOfSimilarities = 0;
        double sumOfWeightedRating = 0;
        // just the top 30 users that are most similar to the main one 
        similarities = getTop30Users(similarities);
        // for each user, we'll sum the weighted rating and the weight
        for(long otherUser : similarities.keySet()){
        	double similarity = similarities.get(otherUser);
        	double ratingOfOtherUser = ratingFromOtherUsers.get(otherUser);
        	double averageRatingFromOtherUser = averageRatingFromOtherUsers.get(otherUser);
        	sumOfWeightedRating += similarity * (ratingOfOtherUser - averageRatingFromOtherUser);
        	sumOfSimilarities += Math.abs(similarity);
        }
        // this is the formula from the assignment
        return userAverageRating + (sumOfWeightedRating / sumOfSimilarities);
    }
    
    /**
     * Remove the mean rating from the ratings vector.
     * @param vector The vector that will have its ratings mean centered.
     * @return A mean centered rating vector.
     */
    private SparseVector getUserMeanCenteredRatingVector(SparseVector vector) {
    	// we need to change the vector, so we'll ask for a mutable one
        MutableSparseVector meanCenteredRatingVector = vector.mutableCopy();
        double average = getAverageRating(meanCenteredRatingVector);
        for (VectorEntry e: vector.fast(VectorEntry.State.EITHER)) {
        	// removing the average and updaring the vector for a given movie
        	double value = e.getValue() - average;
        	meanCenteredRatingVector.set(e.getKey(), value);
        }
        // work's done, freezing the data
        return meanCenteredRatingVector.freeze();
    }
    
    /**
     * Calculates the rating vector's average. 
     * @param userVector The rating vector.
     * @return The average for the ratings in the vector. 
     */
    private double getAverageRating(SparseVector userVector) {
    	double sum = 0;
        for (VectorEntry e: userVector.fast(VectorEntry.State.EITHER)) {
        	sum = sum + e.getValue();
        }
        return sum / userVector.size();
    }
    
    /**
     * Return the top 30 items from a map ordered by the value descending. 
     * @param map
     * @return A map with at most 30 items in a descending order.
     */
    public Map<Long, Double> getTop30Users(Map<Long, Double> map) {
        List<Map.Entry<Long, Double>> list = new LinkedList<Map.Entry<Long, Double>>(map.entrySet());
        // sorting it
        Collections.sort(list, new DescendingValueComparator());
        // taking the 30 first items
        Map<Long, Double> result = new LinkedHashMap<Long, Double>();
        int limit = 30;
        for(Map.Entry<Long, Double> e : list) {
        	result.put(e.getKey(), e.getValue());
            limit--;
            if(limit == 0)
            	break;	// that's enough, we have 30 users        	
        }
        return result;
    }
    
    /**
     * A comparer used to get the list in the inverse order.
     * @author fabiogouw
     *
     */
    private class DescendingValueComparator implements Comparator<Map.Entry<Long, Double>> {

		@Override
		public int compare(Entry<Long, Double> d1, Entry<Long, Double> d2) {
			return Double.compare(d2.getValue(), d1.getValue());
		}
    
    }
}
