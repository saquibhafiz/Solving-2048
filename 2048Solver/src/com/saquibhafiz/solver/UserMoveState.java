package com.saquibhafiz.solver;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class UserMoveState extends State {
    private static final float ACCURACY_THRESHOLD = 0.00005f;
    // all the moves are 0 to 3
    public static final int MOVE_UP = 0;
    public static final int MOVE_RIGHT = 1;
    public static final int MOVE_DOWN = 2;
    public static final int MOVE_LEFT = 3;

    private static final float[] SCORE_WEIGHTS = { // score weights
            0.8f, // descending gradient of numbers
            0.1f, // max number
            0.1f // empty spaces
    };
    private static final Cache<Long, Float> CACHE = CacheBuilder.newBuilder().maximumSize(500000).build();

    public UserMoveState(RandomState previous, int move) {
        super(previous);
        transform(move);
        cellsToCode();
    }

    private void transform(int move) {
        switch (move) {
        case MOVE_UP:
            break;
        case MOVE_DOWN:
            for (int i = 0; i < 4; i++) {
                ArrayUtils.reverse(_cells[i]);
            }
            break;
        case MOVE_RIGHT:
            ArrayUtils.reverse(_cells);
        case MOVE_LEFT:
            int[][] rotated2 = new int[4][4];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    rotated2[i][j] = _cells[j][i];
                }
            }
            _cells = rotated2;
            break;
        }

        // slide
        for (int i = 0; i < 4; i++) {
            int a = 0;
            for (int j = 0; j < 4; j++) {
                if (_cells[i][j] != 0) {
                    _cells[i][a++] = _cells[i][j];
                }
            }
            for (int j = a; j < 4; j++) {
                _cells[i][j] = 0;
            }
        }

        // merge
        for (int i = 0; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                if (_cells[i][j] == 0) {
                    break;
                } else {
                    if (_cells[i][j] == _cells[i][j - 1]) {
                        _cells[i][j - 1] = _cells[i][j - 1] << 1;
                        if (_cells[i][j - 1] > _max) {
                            _max = _cells[i][j - 1];
                        }
                        _cells[i][j] = 0;
                    }
                }
            }
        }

        // slide
        _emptyCells = 0f;
        for (int i = 0; i < 4; i++) {
            int a = 0;
            for (int j = 0; j < 4; j++) {
                if (_cells[i][j] != 0) {
                    _cells[i][a++] = _cells[i][j];
                }
            }
            for (int j = a; j < 4; j++) {
                _cells[i][j] = 0;
                _emptyCells++;
            }
        }

        switch (move) {
        case MOVE_UP:
            break;
        case MOVE_DOWN:
            for (int i = 0; i < 4; i++) {
                ArrayUtils.reverse(_cells[i]);
            }
            break;
        case MOVE_LEFT:
            int[][] rotated = new int[4][4];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    rotated[i][j] = _cells[j][i];
                }
            }
            _cells = rotated;
            break;
        case MOVE_RIGHT:
            int[][] rotated2 = new int[4][4];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    rotated2[i][j] = _cells[j][i];
                }
            }
            _cells = rotated2;
            ArrayUtils.reverse(_cells);
            break;
        }
    }

    public void growAllPossibleRandoms() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (_cells[i][j] == 0) {
                    RandomState state2 = new RandomState(this, 2, i, j);
                    state2._likelihood = 0.9f;
                    _children.put(state2._code, state2);

                    RandomState state4 = new RandomState(this, 4, i, j);
                    state4._likelihood = 0.1f;
                    _children.put(state4._code, state4);
                }
            }
        }
        _numOfChildren = _children.size();
    }

    public void measureCurrentScore() {
        if (!CACHE.asMap().containsKey(_code)) {
            _score = 0f;
            _score = SCORE_WEIGHTS[0] * measureGradient();
            _score += SCORE_WEIGHTS[1] * (_max / 4096f);
            _score += SCORE_WEIGHTS[2] * (_emptyCells / 16f);
            CACHE.put(_code, _score);
        } else {
            _score = CACHE.getIfPresent(_code);
        }
    }

    private float measureGradient() {
        float gradientScore = 0f;

        if (_cells[0][0] == _max) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (i + 1 < 4 && _cells[i][j] >= _cells[i + 1][j]) {
                        gradientScore++;
                    }
                    if (j + 1 < 4 && _cells[i][j] >= _cells[i][j + 1]) {
                        gradientScore++;
                    }
                }
            }
        } else if (_cells[3][0] == _max) {
            for (int i = 3; i >= 0; i--) {
                for (int j = 0; j < 4; j++) {
                    if (i - 1 >= 0 && _cells[i][j] >= _cells[i - 1][j]) {
                        gradientScore++;
                    }
                    if (j + 1 < 4 && _cells[i][j] >= _cells[i][j + 1]) {
                        gradientScore++;
                    }
                }
            }
        } else if (_cells[0][3] == _max) {
            for (int i = 0; i < 4; i++) {
                for (int j = 3; j >= 0; j--) {
                    if (i + 1 < 4 && _cells[i][j] >= _cells[i + 1][j]) {
                        gradientScore++;
                    }
                    if (j - 1 >= 0 && _cells[i][j] >= _cells[i][j - 1]) {
                        gradientScore++;
                    }
                }
            }
        } else if (_cells[3][3] == _max) {
            for (int i = 3; i >= 0; i--) {
                for (int j = 3; j >= 0; j--) {
                    if (i - 1 >= 0 && _cells[i][j] >= _cells[i - 1][j]) {
                        gradientScore++;
                    }
                    if (j - 1 >= 0 && _cells[i][j] >= _cells[i][j - 1]) {
                        gradientScore++;
                    }
                }
            }
        }

        return (float) (gradientScore / 40f);
    }

    @Override
    protected float measureRecursiveScore(int depth, float likelihood) {
        if (_children.isEmpty()) {
            if (likelihood < ACCURACY_THRESHOLD) {
                measureCurrentScore();
                return _score;
            } else {
                growAllPossibleRandoms();
            }
        }

        _score = 0f;

        for (State state : getChildren()) {
            final float odds = 2 * state._likelihood / _numOfChildren;
            _score += odds * state.measureRecursiveScore(depth, likelihood * odds);
        }

        CACHE.put(_code, _score);

        return _score;
    }

    @Override
    public State getChild(long key) {
        return _children.get(key);
    }
}
