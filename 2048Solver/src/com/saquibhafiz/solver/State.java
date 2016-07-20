package com.saquibhafiz.solver;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class State {
    protected final static int MAX_DEPTH = 2;

    protected int[][] _cells;
    protected Map<Long, State> _children;
    protected long _code;
    protected float _emptyCells;
    protected float _likelihood;
    protected float _max;
    protected float _numOfChildren;
    protected float _score;

    protected abstract float measureRecursiveScore(int depth, float likelihood);

    public abstract State getChild(long key);

    private static final Map<Integer, Long> POWERS;
    static {
        Map<Integer, Long> powers = new HashMap<>();
        int two = 2;
        for (long i = 1; i <= 15; i++) {
            powers.put(two, i);
            two *= 2;
        }
        POWERS = Collections.unmodifiableMap(powers);
    }

    protected State() {
        _children = new HashMap<>();
        _emptyCells = 0f;
        _likelihood = 1f;
        _max = 0f;
        _numOfChildren = 0f;
        _score = 0f;
    }

    protected State(int[][] cells) {
        this();
        _cells = cells;
        cellsToCode();
    }

    protected State(State previous) {
        this();
        _cells = new int[4][4];
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                _cells[i][j] = previous._cells[i][j];
                if (_cells[i][j] == 0) {
                    _emptyCells++;
                }
                if (_cells[i][j] > _max) {
                    _max = _cells[i][j];
                }
            }
        }
        cellsToCode();
    }

    protected Collection<State> getChildren() {
        return _children.values();
    }

    public void printCells() {
        System.out.println("--------");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.print(_cells[j][i] + " ");
            }
            System.out.println();
        }
        System.out.println("--------");
    }

    private long cellToCode(int value, int i, int j) {
        if (value == 0)
            return 0l;
        else
            return (15l & POWERS.get(value)) << (i * 16 + j * 4);
    }

    protected long cellsToCode() {
        _code = 0l;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                _code |= cellToCode(_cells[i][j], i, j);
            }
        }
        return _code;
    }

    public long getCode() {
        return _code;
    }

    public void drawCodes() {
        drawCodes(0);
    }

    private void drawCodes(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("___|");
        }
        System.out.println(_code);
        for (State state : getChildren()) {
            state.drawCodes(depth + 1);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return prime + (int) (_code ^ (_code >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        State other = (State) obj;
        if (_code != other._code)
            return false;
        return true;
    }

    public int getMaxCellValue() {
        return (int) _max;
    }
}
