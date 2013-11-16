package edu.umn.cs.recsys.ii;

import com.google.common.collect.ImmutableMap;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

import org.grouplens.lenskit.collections.LongUtils;
import org.grouplens.lenskit.core.Transient;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.dao.ItemDAO;
import org.grouplens.lenskit.data.dao.UserEventDAO;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.history.RatingVectorUserHistorySummarizer;
import org.grouplens.lenskit.data.history.UserHistory;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.scored.ScoredIdListBuilder;
import org.grouplens.lenskit.scored.ScoredIds;
import org.grouplens.lenskit.vectors.ImmutableSparseVector;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.SparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;
import org.grouplens.lenskit.vectors.similarity.CosineVectorSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemModelBuilder implements Provider<SimpleItemItemModel> {
    private final ItemDAO itemDao;
    private final UserEventDAO userEventDao;
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelBuilder.class);
    private final CosineVectorSimilarity cosCalculator;
    private final ScoredIdComparator scoreComparator;

    @Inject
    public SimpleItemItemModelBuilder(@Transient ItemDAO idao,
                                      @Transient UserEventDAO uedao) {
        itemDao = idao;
        userEventDao = uedao;
        cosCalculator = new CosineVectorSimilarity();
        scoreComparator = new ScoredIdComparator();
    }

    @Override
    public SimpleItemItemModel get() {
        // Get the transposed rating matrix
        // This gives us a map of item IDs to those items' rating vectors
        Map<Long, ImmutableSparseVector> itemVectors = getItemVectors();

        // Get all items - you might find this useful
        LongSortedSet items = LongUtils.packedSet(itemVectors.keySet());
        // Map items to vectors of item similarities
        //Map<Long,MutableSparseVector> itemSimilarities = new HashMap<Long, MutableSparseVector>();
        
        Map<Long,List<ScoredId>> nbrhoods = new HashMap<Long,List<ScoredId>>();
        
        for(long itemId : items){
        	ScoredIdListBuilder builder = ScoredIds.newListBuilder();
        	SparseVector itemUsersRatings = itemVectors.get(itemId);
        	for(long itemIdToCompare : items) {
        		
        		if(itemId != itemIdToCompare) {
        			SparseVector itemUsersRatingsToCompare = itemVectors.get(itemIdToCompare);
        			double similarity = cosCalculator.similarity(itemUsersRatings, itemUsersRatingsToCompare);
	       			if(similarity > 0) {
	       				builder.add(itemIdToCompare, similarity);
	       			}
       			}
        	}
			builder.sort(scoreComparator);
        	nbrhoods.put(itemId, builder.build());
        }
        
        // Compute the similarities between each pair of items ***
        // It will need to be in a map of longs to lists of Scored IDs to store in the model
        return new SimpleItemItemModel(nbrhoods);
    }

    /**
     * Load the data into memory, indexed by item.
     * @return A map from item IDs to item rating vectors. Each vector contains users' ratings for
     * the item, keyed by user ID.
     */
    public Map<Long,ImmutableSparseVector> getItemVectors() {
        // set up storage for building each item's rating vector
        LongSet items = itemDao.getItemIds();
        // map items to maps from users to ratings
        Map<Long,Map<Long,Double>> itemData = new HashMap<Long, Map<Long, Double>>();
        for (long item: items) {
            itemData.put(item, new HashMap<Long, Double>());
        }
        // itemData should now contain a map to accumulate the ratings of each item

        // stream over all user events
        Cursor<UserHistory<Event>> stream = userEventDao.streamEventsByUser();
        try {
            for (UserHistory<Event> evt: stream) {
                MutableSparseVector vector = RatingVectorUserHistorySummarizer.makeRatingVector(evt).mutableCopy();
                // vector is now the user's rating vector
                long userId = evt.getUserId();
                double userAverageRating = getAverageRating(vector);
                for (VectorEntry e: vector.fast(VectorEntry.State.EITHER)) {
                	long itemId = e.getKey();
                	double rating = e.getValue();
                	Map<Long, Double> userRatingsMap = itemData.get(itemId);
                	double weigthedRating = rating - userAverageRating;
                	userRatingsMap.put(userId, weigthedRating);
                }
                // Normalize this vector and store the ratings in the item data ***
            }
        } finally {
            stream.close();
        }

        // This loop converts our temporary item storage to a map of item vectors
        Map<Long,ImmutableSparseVector> itemVectors = new HashMap<Long, ImmutableSparseVector>();
        for (Map.Entry<Long,Map<Long,Double>> entry: itemData.entrySet()) {
            MutableSparseVector vec = MutableSparseVector.create(entry.getValue());
            itemVectors.put(entry.getKey(), vec.immutable());
        }
        return itemVectors;
    }
    
    private double getAverageRating(SparseVector ratings){
    	double total = 0;
    	for (VectorEntry e: ratings.fast(VectorEntry.State.EITHER)) {
    		total += e.getValue();
    	}
    	return total / ratings.size();
    }
    
    private class ScoredIdComparator implements Comparator<ScoredId> {
        public int compare(ScoredId conta, ScoredId outraConta) {
            return Double.compare(outraConta.getScore(), conta.getScore());
        }
    }
}
