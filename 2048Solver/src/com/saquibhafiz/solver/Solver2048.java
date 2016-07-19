package com.saquibhafiz.solver;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Solver2048 {
	private static final int MAX_ITERATIONS = 1000;

	private static int iterations = 1;
	private static long timerStart = 0L;

	private State _root;
	private Socket _socket;

	private static interface OnFinishListener {
		public void onFinish(int score);
	};

	private final OnFinishListener _onFinishListener;

	private Solver2048(OnFinishListener onFinishListener) {
		_onFinishListener = onFinishListener;
		try {
			_socket = IO.socket("http://localhost:80/bot");
			_socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					System.out.println("\nconnected: starting game " + iterations);
				}
			}).on("result", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					JSONObject jsonResult = (JSONObject) args[0];

					if (isGameOver(jsonResult)) {
						_root = new RandomState(fromJsonToCells(jsonResult));
						int score = (int) _root.getMaxCellValue();
						System.out.println((System.currentTimeMillis() - timerStart) + "ms");
						_root.printCells();
						_root = null;
						_socket.disconnect();
						_onFinishListener.onFinish(score);
						return;
					}

					State currentBoard = new RandomState(fromJsonToCells(jsonResult));

					if (_root == null) { // for the very first run
						_root = currentBoard;
						_root.measureRecursiveScore(0, 1f);
					} else {
						_root = _root.getChild(currentBoard.getCode());
						if (_root == null) {
							throw new RuntimeException("root is null");
						}
					}

					int bestMove = ((RandomState) _root).getBestMove();
					_socket.emit("move", bestMove);
					_root = _root.getChild((long) bestMove);
				}
			});
			_socket.connect();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		play();
	}

	private static void play() {
		timerStart = System.currentTimeMillis();
		new Solver2048(new OnFinishListener() {

			@Override
			public void onFinish(int score) {
				if (iterations++ < MAX_ITERATIONS) {
					play();
				} else {
					System.exit(0);
				}
			}
		});
	}

	public static void lag(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static boolean isGameOver(JSONObject obj) {
		return obj.getBoolean("over");
	}

	private static int[][] fromJsonToCells(JSONObject obj) {
		int[][] cells = new int[4][4];
		JSONArray grid = obj.getJSONArray("grid");

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				cells[i][j] = grid.getJSONArray(i).getInt(j);
			}
		}

		return cells;
	}
}
