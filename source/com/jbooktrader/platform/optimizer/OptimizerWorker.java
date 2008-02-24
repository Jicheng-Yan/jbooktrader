package com.jbooktrader.platform.optimizer;

import com.jbooktrader.platform.backtest.BackTester;
import com.jbooktrader.platform.marketdepth.MarketDepth;
import com.jbooktrader.platform.model.*;
import com.jbooktrader.platform.position.PositionManager;
import com.jbooktrader.platform.strategy.Strategy;
import com.jbooktrader.platform.util.MessageDialog;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 */
public class OptimizerWorker implements Runnable {
    private final List<Result> results;
    private final int minTrades;
    private final CountDownLatch remainingTasks;
    private final Constructor<?> strategyConstructor;
    private final LinkedList<StrategyParams> tasks;
    private final List<MarketDepth> marketDepths;

    public OptimizerWorker(List<MarketDepth> marketDepths, Constructor<?> strategyConstructor, LinkedList<StrategyParams> tasks, List<Result> results, int minTrades, CountDownLatch remainingTasks) throws IOException, JBookTraderException {
        this.marketDepths = marketDepths;
        this.results = results;
        this.minTrades = minTrades;
        this.remainingTasks = remainingTasks;
        this.strategyConstructor = strategyConstructor;
        this.tasks = tasks;
    }

    public void run() {
        StrategyParams params;

        try {
            while (true) {
                synchronized (tasks) {
                    if (tasks.isEmpty()) {
                        break;
                    }
                    params = tasks.removeFirst();
                }

                Strategy strategy = (Strategy) strategyConstructor.newInstance(params);
                BackTester backTester = new BackTester(strategy, marketDepths);
                backTester.execute();

                PositionManager positionManager = strategy.getPositionManager();
                int trades = positionManager.getTrades();

                if (trades >= minTrades) {
                    Result result = new Result(params, positionManager);
                    synchronized (results) {
                        results.add(result);
                    }
                }

                synchronized (remainingTasks) {
                    remainingTasks.countDown();
                }

            }
        } catch (Throwable t) {
            Dispatcher.getReporter().report(t);
            MessageDialog.showError(null, t.getMessage());
        }
    }
}