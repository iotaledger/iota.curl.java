package com.iota.curl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Iota Curl Core mining functions.
 *
 * gianluigi.davassi on 13.10.16.
 */
public class IotaCurlMiner {

    public static final long LMASK1 = (0x5555555555555555l);
    public static final long LMASK2 = (0xAAAAAAAAAAAAAAAAl);
    public static final long LMASK3 = (0xFFFFFFFFFFFFFFFFL);

    static final long[] MAP = {
        (0b11), (0b01), (0b10)
    };

    static final long[] MAP_EX = { LMASK3, LMASK1, LMASK2 };

    // The length of transaction header (before approvalNonce) in trytes.
    public static final int TX_HEADER_SZ = 2430;

    private static final int HASH_SIZE = 3*IotaCurlHash.IOTACURL_HASH_SZ;
    private static final int STATE_SIZE = 3*IotaCurlHash.IOTACURL_STATE_SZ;

    private final long[] midState = new long[3 * IotaCurlHash.IOTACURL_STATE_SZ];

    private final int[] approvalNonce = new int[HASH_SIZE];
    private final int[] trunkTransaction = new int[HASH_SIZE];
    private final int[] branchTransaction = new int[HASH_SIZE];

    private final int PARALLEL = 32;

    protected static long lc(long a) {
        return ((((a) ^ ((a)>>1)) & LMASK1) | (((a)<<1) & LMASK2));
    }

    protected static long ld(long b, long c) {
        return (c) ^ (LMASK2 & (b) & (((b) & (c))<<1)) ^ (LMASK1 & ~(b) & (((b) & (c))>>1));
    }

    protected final void doPowAbsorb(final long[] state, final int[] trits) {
        for (int i=0; i<HASH_SIZE; i++) {
            state[i] = MAP_EX[trits[i]+1];
        }
    }

    protected void doPowTransform(final long [] state) {
        long[] state1 = state; // shallow copy
        long[] state2 = Arrays.copyOf(state, state.length);

        for(int r=0; r<27; r++) {
            {
                final long a = state2[0];
                final long b = state2[364];
                final long c = lc(a);
                state1[0] = ld(b, c);
            }
            for (int i = 0; i < (STATE_SIZE / 2); i++) {
                final long a3 = state2[364 - i - 1];
                final long a1 = state2[364 - i];
                final long a2 = state2[729 - i - 1];
                final long c1 = lc(a1);
                final long c2 = lc(a2);
                state1[2 * i + 1] = ld(a2, c1);
                state1[2 * i + 2] = ld(a3, c2);
            }
            final long[] t = state1;
            state1 = state2;
            state2 = t;
        }
    }

    private long doWork(final int minWeightMagnitude, long offset) {
        final int [] an = Arrays.copyOf(approvalNonce, HASH_SIZE);
        final long [] state = Arrays.copyOf(midState, midState.length);

        IotaCurlUtils.iotaCurlTritsAdd(an, HASH_SIZE, offset);

        // Search. Process approvalNonce.
        for(int i=0; i<PARALLEL; i++) {
            for(int j=0; j<HASH_SIZE; j++) {
                state[j] |= ((MAP[an[j]+1]) << (i*2));
            }
            IotaCurlUtils.iotaCurlTritsIncrement(an, HASH_SIZE);
        }

        doPowTransform(state);

        // Process trunkTransaction/branchTransaction.
        doPowAbsorb(state, trunkTransaction);
        doPowTransform(state);

        doPowAbsorb(state, branchTransaction);
        doPowTransform(state);

        // Check if work is done.
        for(int i=0; i<PARALLEL; i++) {

            boolean complete = true;

            for(int j=HASH_SIZE-minWeightMagnitude; j<HASH_SIZE; j++) {
                long n = state[j] >> ((2*i)) & (MAP[0]);
                complete = complete && (n == (MAP[1]));
            }
            if(!complete) {
                continue;
            }
            return (offset + i); // If the solution has been found.
        }
        return 0;
    }

    private final char[] powInit(final String tx) {

        final IotaCurlHash ctx = new IotaCurlHash(27);
        final char[] trx = tx.toCharArray();
        ctx.doAbsorb(trx, TX_HEADER_SZ);

        for (int i = 0; i < STATE_SIZE; i++) {
            midState[i] = (i < HASH_SIZE) ? 0L : (MAP_EX[ctx.getCurlStateValue(i) + 1]);
        }

        IotaCurlUtils.iotaCurlTrytes2Trits(approvalNonce, 7290 / 3, trx, IotaCurlHash.IOTACURL_HASH_SZ);
        IotaCurlUtils.iotaCurlTrytes2Trits(trunkTransaction, 7533 / 3, trx, IotaCurlHash.IOTACURL_HASH_SZ);
        IotaCurlUtils.iotaCurlTrytes2Trits(branchTransaction, 7776 / 3, trx, IotaCurlHash.IOTACURL_HASH_SZ);
        return trx;
    }

    public void powFinalize(char [] txar, long result) {
        IotaCurlUtils.iotaCurlTritsAdd(approvalNonce, HASH_SIZE, result);
        IotaCurlUtils.iotaCurlTrits2Trytes(txar, TX_HEADER_SZ, approvalNonce, HASH_SIZE);
    }

    public String doCurlPowSingleThread(String tx, final int minWeightMagnitude) {
        final char[] trax = powInit(tx);

        long offset = 0L, result;
        while ((result = doWork(minWeightMagnitude, offset)) == 0) {
            offset += PARALLEL;
        }
        powFinalize(trax, result);
        return new String(trax);
    }

    public String iotaCurlProofOfWork(String tx, final int minWeightMagnitude) {
        try {
            return doCurlPowMultiThread(tx, minWeightMagnitude);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    public String doCurlPowMultiThread(String tx, final int minWeightMagnitude) throws ExecutionException, InterruptedException {
        final char[] trax = powInit(tx);

        final int cpus = Runtime.getRuntime().availableProcessors();
        final ExecutorService executor = Executors.newFixedThreadPool(cpus-1);

        final AtomicLong offset = new AtomicLong(0l);
        final AtomicLong result = new AtomicLong(0l);
        final AtomicBoolean finish = new AtomicBoolean(false);

        final Collection<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i<cpus-1; i++) {
            tasks.add (() -> {
                while (result.get() == 0 && finish.get() == false) {
                    result.compareAndSet(0, doWork(minWeightMagnitude, offset.getAndAdd(32l)));
                }
                finish.set(true);
                return result.get();
            });
        }
        final List<Future<Long>> res = executor.invokeAll(tasks);

        final Long r = res.stream().map(t -> {
            try {
                return t.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return 0;
        }).map(t -> t.longValue()).findFirst().get();
        executor.shutdown();
        powFinalize(trax, r);
        return new String(trax);
    }

}