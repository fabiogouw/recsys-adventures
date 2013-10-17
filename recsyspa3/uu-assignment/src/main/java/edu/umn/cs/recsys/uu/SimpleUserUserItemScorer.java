package edu.umn.cs.recsys.uu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

import org.apache.commons.lang3.ArrayUtils;
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
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final UserEventDAO userDao;
    private final ItemEventDAO itemDao;

    // 928:1892 928:274 928:568 928:98 928:9741 4734:5503 4734:22 4734:604 4734:807 4734:10020 1964:3049 1964:664 1964:24 1964:105 1964:641 4554:581 4554:1892 4554:114 4554:197 4554:752 4305:280 4305:4327 4305:786 4305:788 4305:581
    
    @Inject
    public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao) {
        userDao = udao;
        itemDao = idao;
    }

    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
        SparseVector userVector = getUserRatingVector(user);
        double averageRatingForUser = getAverageRating(userVector);
        // TODO Score items for this user using user-user collaborative filtering

        // This is the loop structure to iterate over items to score
        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
        	long item = e.getKey();
        	LongSet usersForItem = itemDao.getUsersForItem(item);
        	
            Map<Long, Double> similarities = new HashMap<Long, Double>(usersForItem.size());        
            Map<Long, Double> ratingFromOtherUsers = new HashMap<Long, Double>(usersForItem.size());
            Map<Long, Double> averageRatingFromOtherUsers = new HashMap<Long, Double>(usersForItem.size());
            
            CosineVectorSimilarity cos = new CosineVectorSimilarity();
            for(long otherUser : usersForItem){
            	if(otherUser != user) {
            		SparseVector otherUserVector = getUserRatingVector(otherUser);
            		double averageFromOtherUser = getAverageRating(otherUserVector);
            		
            		ratingFromOtherUsers.put(otherUser, otherUserVector.get(item));
            		averageRatingFromOtherUsers.put(otherUser, averageFromOtherUser);
            		
            		double similarity = cos.similarity(getUserMeanCenteredRatingVector(userVector),
            				getUserMeanCenteredRatingVector(otherUserVector));
            		similarities.put(otherUser, similarity);
            	}
            }
            Map<Long, Double> sorted = getTop30Users(similarities);
            
            double sumOfSimilarities = 0;
            double sumOfWeightedRating = 0;
            
            for(long otherUser : sorted.keySet()){
            	double similarity = sorted.get(otherUser);
            	double ratingOfOtherUser = ratingFromOtherUsers.get(otherUser);
            	double averageRatingFromOtherUser = averageRatingFromOtherUsers.get(otherUser);
            	sumOfWeightedRating += similarity * (ratingOfOtherUser - averageRatingFromOtherUser);
            	sumOfSimilarities += Math.abs(similarity);
            }
            scores.set(item, averageRatingForUser + (sumOfWeightedRating / sumOfSimilarities));
        }
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
    
    private SparseVector getUserMeanCenteredRatingVector(SparseVector vector) {
        MutableSparseVector ret = vector.mutableCopy();
        double average = getAverageRating(ret);
        for (VectorEntry e: vector.fast(VectorEntry.State.EITHER)) {
        	double value = e.getValue() - average;
        	ret.set(e.getKey(), value);
        }
        return ret;
    }
    
    private double getAverageRating(SparseVector userVector){
    	double sum = 0;
        for (VectorEntry e: userVector.fast(VectorEntry.State.EITHER)) {
        	sum = sum + e.getValue();
        }
        return sum / userVector.size();
    }
    
    public Map<Long, Double> getTop30Users(Map<Long, Double> map) {
        List<Map.Entry<Long, Double>> list = new LinkedList<Map.Entry<Long, Double>>(map.entrySet());
        Collections.sort(list,
                new Comparator<Map.Entry<Long, Double>>() {
                    public int compare(Map.Entry<Long, Double> o1, Map.Entry<Long, Double> o2) {
                    	return java.lang.Double.compare((java.lang.Double)o2.getValue(), (java.lang.Double)o1.getValue());
                    }
                });

        Map<Long, Double> result = new LinkedHashMap<Long, Double>();
        int limit = 30;
        for (Iterator<Map.Entry<Long, Double>> it = list.iterator(); it.hasNext();) {
            Map.Entry<Long, Double> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
            limit--;
            if(limit == 0)
            	break;
        }
        return result;
    }
    
}
