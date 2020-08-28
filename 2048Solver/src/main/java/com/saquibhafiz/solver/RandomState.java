package com.saquibhafiz.solver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RandomState extends State {
  private static final Cache<Long, State> CACHE = CacheBuilder.newBuilder().maximumSize(500000).build();

  public RandomState(int[][] cells) {
    super(cells);
  }

  public RandomState(UserMoveState previous, int value, int a, int b) {
    super(previous);
    _cells[a][b] = value;
    _emptyCells--;
    cellsToCode();
  }

  public int getBestMove() {
    int bestMove = getMoveWithHighestScore(getScoresPerMove());
    CACHE.invalidateAll(); // clear cache
    return bestMove;
  }

  private int getMoveWithHighestScore(float[] scores) {

    int maxMove = 0;
    float maxScore = 0f;
    boolean allZeros = true;

    for (int i = 0; i < 4; i++) {
      if (scores[i] > maxScore) {
        maxScore = scores[i];
        maxMove = i;
        allZeros = false;
      }
    }

    if (allZeros && !_children.isEmpty()) {
      maxMove = _children.keySet().iterator().next().intValue();
    }

    return maxMove;
  }

  private float[] getScoresPerMove() {
    float[] scores = new float[4];
    for (int i = 0; i < 4; i++) {
      scores[i] = 0f;
    }

    final float likelihood = 1f / _numOfChildren;
    for (Entry<Long, State> entry : _children.entrySet()) {
      State state = entry.getValue();
      state.measureRecursiveScore(0, likelihood);
      int move = (int) entry.getKey().longValue();
      scores[move] = state._score;
    }

    return scores;
  }

  public void growUserMoves() {
    for (int move = 0; move < 4; move++) {
      UserMoveState state = new UserMoveState(this, move);
      if (_code != state._code) {
        _children.put((long) move, state);
      }
    }
    _numOfChildren = _children.size();
  }

  @Override
  protected float measureRecursiveScore(int depth, float likelihood) {
    _score = 0f;

    if (_children.isEmpty()) {
      growUserMoves();
    }

    Map<Long, State> replacements = new HashMap<>();

    final float odds = 1f / _numOfChildren;
    for (Entry<Long, State> entry : _children.entrySet()) {
      Long move = entry.getKey();
      State child = entry.getValue();
      State cached = CACHE.getIfPresent(child._code);

      if (cached == null) {
        _score = Math.max(_score, child.measureRecursiveScore(depth + 1, likelihood * odds));
        CACHE.put(child._code, child);
      } else {
        replacements.put(move, cached);
        _score = Math.max(_score, cached._score);
      }
    }

    _children.putAll(replacements);

    return _score;
  }

  @Override
  public State getChild(long key) {
    return _children.get(key);
  }

}
