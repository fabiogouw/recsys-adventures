package org.grouplens.mooc.recsyspa1;

import java.util.Comparator;

public class ResultComparator implements Comparator<Result> {
    @Override
    public int compare(final Result object1, final Result object2) {
    	return object1.Score < object2.Score ? 1 
    		     : object1.Score > object2.Score ? -1 
    		     : 0;
    }
}
