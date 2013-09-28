package org.grouplens.mooc.recsyspa1;

public class Result {
	public long MovieId;
	public float Score;
	public Result(long movieId, float score){
		this.MovieId = movieId;
		this.Score = score;
	}
}
