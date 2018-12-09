
package com.iota.curl.miner;

import com.iota.curl.IotaCurlHash;
import com.iota.curl.IotaCurlMiner;

import java.util.Locale;

/**
 * Miner class.
 *
 * This class is a test case for IotaCurlMiner trinary function lib.
 *
 * Ported by gianluigi.davassi on 13.10.16.
 */
public class Miner {

    public static final int TX_LENGTH = 2673;

    public static void main(final String ... args) {

        if (args == null || args.length < 2) {
            stderr("usage: TX_TRYTES [minWeightMagnitude=13]\n");
            stderr("TX_TRYTES:\n" +
                    "\tThe raw transaction data expressed in trytes-encoded string.\n" +
                    "\tShould be " + TX_LENGTH + " letters long and must only include letters [9A-Z].\n" +
                    "minWeightMagnitude:\n\tThe difficulty factor of Curl hash function.\n" +
                    "\tCurrently the default value is set to 13 as in the reference implementation.\n");
            return;
        }

        final String tx = args[1];
        if(tx.length() != TX_LENGTH) {
            stderr("The length of TX_TRYTES is not correct!");
            return;
        }

        if (!tx.matches("[9A-Z]")) {
            stderr("The raw transaction data expressed in trytes-encoded string must contain only [A-Z] and 9 number.");
            return;
        }

        // Load the minWeightMagnitude
	    final int minWeightMagnitude = (args.length>2 ? Integer.parseInt(args[2]) : 13);
        if(minWeightMagnitude <= 0) {
            stderr("minWeightMagnitude (%d) should be greater than zero.", minWeightMagnitude);
            return;
        }

        // Show the data to mine.
        stdout("I: mining for:\nTX_TRYTES: %s\nminWeightMagnitude: %d", tx, minWeightMagnitude);

        // Do PoW.
        stdout("I: mining... ");

        final IotaCurlMiner iotacurl = new IotaCurlMiner();
        iotacurl.iotaCurlProofOfWork(tx, minWeightMagnitude);
        stdout("done.");

        // Print the approvalNonce and the final transaction.
        final String approvalNonce = tx.substring(2430, 2430+81);
        stdout("approvalNonce: %s", approvalNonce);
        stdout("txTrytes: %s", tx);

        // Print hash.
        final String hash = IotaCurlHash.iotaCurlHash(tx, TX_LENGTH, 27);
        stdout("hash: %s", hash);
    }

    private static final void stdout(String str, Object ... args) {
        final String formattedStr = String.format(Locale.ENGLISH, str, args);
        System.out.println(formattedStr);
    }

    private static final void stderr(String str, Object ... args) {
        final String formattedStr = String.format(Locale.ENGLISH, str, args);
        System.err.println("Error: " + formattedStr);
    }
}
