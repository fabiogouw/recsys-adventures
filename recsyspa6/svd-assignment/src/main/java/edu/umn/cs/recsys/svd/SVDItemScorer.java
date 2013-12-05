package edu.umn.cs.recsys.svd;

import org.apache.commons.math3.linear.RealMatrix;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.basic.AbstractItemScorer;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * SVD-based item scorer.
 */
public class SVDItemScorer extends AbstractItemScorer {
    private static final Logger logger = LoggerFactory.getLogger(SVDItemScorer.class);
    private final SVDModel model;
    private final ItemScorer baselineScorer;
    private final UserEventDAO userEvents;

    // 572:671 572:462 572:9802 572:197 572:809 2801:9806 2801:38 2801:8358 2801:857 2801:8467 515:857 515:105 515:1900 515:141 515:7443 1269:640 1269:9802 1269:153 1269:272 1269:77 2895:601 2895:9802 2895:275 2895:7443 2895:629
    
    /**
     * Construct an SVD item scorer using a model.
     * @param m The model to use when generating scores.
     * @param uedao A DAO to get user rating profiles.
     * @param baseline The baseline scorer (providing means).
     */
    @Inject
    public SVDItemScorer(SVDModel m, UserEventDAO uedao,
                         @BaselineScorer ItemScorer baseline) {
        model = m;
        baselineScorer = baseline;
        userEvents = uedao;
    }

    /**
     * Score items in a vector. The key domain of the provided vector is the
     * items to score, and the score method sets the values for each item to
     * its score (or unsets it, if no score can be provided). The previous
     * values are discarded.
     *
     * @param user   The user ID.
     * @param scores The score vector.
     */
    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
    	RealMatrix weights = model.getFeatureWeights();
    	RealMatrix umat = model.getUserVector(user);
    	if(umat != null) {
	        // Score the items in the key domain of scores ***
	        for (VectorEntry e: scores.fast(VectorEntry.State.EITHER)) {
	            long item = e.getKey();
	            RealMatrix imat = model.getItemVector(item).transpose();
	            // Set the scores ***
	            RealMatrix r = umat.multiply(weights).multiply(imat);
	            double prediction = r.getEntry(0, 0);
	            double baseline = baselineScorer.score(user, item);
	            scores.set(item, prediction + baseline);
	        }
    	}
    	else
    		scores.clear();
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
