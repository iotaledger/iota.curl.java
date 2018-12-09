
package com.iota.curl;

import java.util.Arrays;

/**
 * {@link IotaCurlHash} class definition.
 *
 * This class has an internal state, therefore it has not been designed to be
 * thread-safe. You must access it statically, the main method is
 *
 * IotaCurlMiner.iotaCurlHash(tx,len); // thread safe
 *
 * The hash core functionalities have been ported in this class,
 * utility functions and tables on IotaCurlUtils
 *
 * gianluigi.davassi on 13.10.16.
 */
public class IotaCurlHash {

    // The length of output hash in trytes.
    public static final int IOTACURL_HASH_SZ = 81;

    // The length of internal state buffer in trytes.
    public static final int IOTACURL_STATE_SZ = (3 * IOTACURL_HASH_SZ);

    public static final int IOTACURL_3STATE = (3 * IOTACURL_STATE_SZ);

    private static final class CurlState {
        int[] state = new int[IOTACURL_3STATE];
    };

    private final CurlState curlState = new CurlState();

    private final int rounds;

    public static String iotaCurlHash(final String tx, final int len, int rounds) {
        final IotaCurlHash ctx = new IotaCurlHash(rounds);
        ctx.doAbsorb(tx.toCharArray(), len);
        return ctx.doFinalize();
    }

    protected IotaCurlHash(int rounds) {
        this.rounds = rounds;
    }

    /**
     * Absorb given trytes.
     */
    protected void doAbsorb(final char [] trytes, final int len) {
        for(int i=0; i<len; i+=IOTACURL_HASH_SZ) {
            IotaCurlUtils.iotaCurlTrytes2Trits(curlState.state, i, trytes, IotaCurlUtils.smin(len-i, IOTACURL_HASH_SZ));
            doHashTransform(curlState.state);
        }
    }

    protected void doHashTransform(int [] state1) {
        int [] state2 = Arrays.copyOf(state1, state1.length);

        for(int r=0; r<rounds; r++) {
            state1[0] = IotaCurlUtils.TRUTH_TABLE[state2[0] + (state2[364] << 2) + 5];

            // state1[0] = IotaCurlUtils.invokeBinaryTruthOn(state2[0], state2[364]);

            for(int i=0; i<3*IOTACURL_STATE_SZ/2; i++) {
                state1[2*i+1] = IotaCurlUtils.TRUTH_TABLE[state2[364-i] + (state2[729-(i+1)] << 2) + 5];
                state1[2*i+2] = IotaCurlUtils.TRUTH_TABLE[state2[729-(i+1)] + (state2[364-(i+1)] << 2) + 5];
            }
            int [] t = state1;
            state1 = state2;
            state2 = t;
        }
    }

    private String doFinalize() {
        final char [] hash = new char[IOTACURL_HASH_SZ+1];
        IotaCurlUtils.iotaCurlTrits2Trytes(hash, 0, curlState.state, 3*IOTACURL_HASH_SZ);
        return new String(hash).trim();
    }

    public int getCurlStateValue(int index) {
        return curlState.state[index];
    }
}
