package edu.umn.cs.recsys.ii;

import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.knn.NeighborhoodSize;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.scored.ScoredIdListBuilder;
import org.grouplens.lenskit.scored.ScoredIds;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.List;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final UserEventDAO userEvents;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, UserEventDAO dao,
                                @NeighborhoodSize int nnbrs) {
        model = m;
        userEvents = dao;
        neighborhoodSize = nnbrs;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param scores The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
        SparseVector ratings = getUserRatingVector(user);
        //double averageUserRating = getUserAverageRating(ratings);
        
        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
            long itemId = e.getKey();
            List<ScoredId> neighbors = IntersectNeighbors(model.getNeighbors(itemId), ratings);
            double absTotal = 0;
            double totalWeighted = 0;
            int i = 0;
            for(ScoredId neighbor : neighbors){
            	long comparedItemId = neighbor.getId();
            	double similarity = neighbor.getScore();
            	double userRating = ratings.get(comparedItemId);
            	totalWeighted += similarity * userRating;
            	absTotal += Math.abs(similarity); 
                i++;
                if(i >= neighborhoodSize)
                	break;
            }
            double score = totalWeighted / absTotal;
            scores.set(itemId, score);
            // Score this item and save the score into scores ***
        }
    }
    
    private List<ScoredId> IntersectNeighbors(List<ScoredId> neighbors, SparseVector userRatings) {
    	ScoredIdListBuilder builder = ScoredIds.newListBuilder();
    	for(ScoredId neighbor : neighbors) {
    		long itemId = neighbor.getId();
    		if(userRatings.containsKey(itemId))
    			builder.add(itemId, neighbor.getScore());
    	}
    	return builder.build();
    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private SparseVector getUserRatingVector(long user) {
        UserHistory<Rating> history = userEvents.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }

        return RatingVectorUserHistorySummarizer.makeRatingVector(history);
    }
}
