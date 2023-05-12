package com.example.nextface_android;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager {
    private static ThreadPoolProxy mNormalPool = new ThreadPoolProxy(1, 3, 5 * 1000);//param 0  最大线程数，param 1 核心线程数
    private static ThreadPoolProxy mDownloadPool = new ThreadPoolProxy(3, 3, 5 * 1000);

    public static ThreadPoolProxy getNormalPool() {
        return mNormalPool;
    }

    public static ThreadPoolProxy getDownloadPool() {
        return mDownloadPool;
    }

    public static class ThreadPoolProxy {
        private final int mCorePoolSize;
        private final int mMaximumPoolSize;
        private final long mKeepAliveTime;
        private ThreadPoolExecutor mPool;

        public ThreadPoolProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
            this.mCorePoolSize = corePoolSize;
            this.mMaximumPoolSize = maximumPoolSize;
            this.mKeepAliveTime = keepAliveTime;
        }

        private void initPool() {
            if (mPool == null || mPool.isShutdown()) {
                TimeUnit unit = TimeUnit.MILLISECONDS;
                BlockingQueue<Runnable> workQueue = null;
                workQueue = new ArrayBlockingQueue<Runnable>(3);
                ThreadFactory threadFactory = Executors.defaultThreadFactory();
                RejectedExecutionHandler handler = null;
                handler = new ThreadPoolExecutor.DiscardPolicy();

                mPool = new ThreadPoolExecutor(mCorePoolSize,
                        mMaximumPoolSize,
                        mKeepAliveTime,
                        unit,
                        workQueue,
                        threadFactory,
                        handler);
            }
        }

        public void execute(Runnable task) {
            initPool();
            mPool.execute(task);
        }

        public Future<?> submit(Runnable task) {
            initPool();
            return mPool.submit(task);
        }

        public void remove(Runnable task) {
            if (mPool != null && !mPool.isShutdown()) {
                mPool.getQueue()
                        .remove(task);
            }
        }
    }
}
