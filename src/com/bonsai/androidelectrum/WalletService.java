package com.bonsai.androidelectrum;

import java.math.BigInteger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.WalletTransaction;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;

public class WalletService extends Service
{
    private Logger mLogger;

    private WalletAppKit mKit;
    private NetworkParameters mParams;
    private SetupWalletTask mTask;

    private class SetupWalletTask extends AsyncTask<Void, Void, Void>
    {
		@Override
		protected Void doInBackground(Void... arg0)
        {
            mLogger.info("getting network parameters");

            mParams = MainNetParams.get();

            String filePrefix = "android-electrum";

            Context context = getApplicationContext();

            mLogger.info("creating new wallet app kit");

            byte[] seed = Hex.decode("4a34f8fe74f81723ab07ff1d73af91e2");
            final HDWallet hdwallet = new HDWallet(mParams, seed);

            mKit =
                new WalletAppKit(mParams, context.getFilesDir(), filePrefix)
                {
                    @Override
                    protected void onSetupCompleted() {
                        mLogger.info("adding keys");
                        hdwallet.addAllKeys(wallet());
                    }
                };

            mLogger.info("waiting for blockchain setup");

            // Download the block chain and wait until it's done.
            mKit.startAndWait();

            mLogger.info("blockchain setup finished");

            BigInteger bal0 = mKit.wallet().getBalance(BalanceType.AVAILABLE);
            BigInteger bal1 = mKit.wallet().getBalance(BalanceType.ESTIMATED);

            mLogger.info("avail balance = " + bal0.toString());
            mLogger.info("estim balance = " + bal1.toString());

            // Compute balances and transaction counts.
            Iterable<WalletTransaction> iwt =
                mKit.wallet().getWalletTransactions();
            hdwallet.applyAllTransactions(iwt);

            // Send a transaction.
            if (false) {
                try {
                    int acctnum = 1;
                    Address dest =
                        new Address(mParams,
                                    "19jh2GWRBJ5ktjyMeoe2scghwqYK3enJQH");
                    BigInteger value = BigInteger.valueOf(100000);
                    BigInteger fee = BigInteger.valueOf(20000);
                    hdwallet.sendCoins(mKit.wallet(), acctnum, dest, value, fee);

                } catch (WrongNetworkException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (AddressFormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // FIXME - Need to add Progress and Result.
			return null;
		}
    }

    public WalletService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate()
    {
        mLogger = LoggerFactory.getLogger(WalletService.class);

        mLogger.info("WalletService created");
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        mTask = new SetupWalletTask();
        mTask.execute();

        mLogger.info("WalletService started");
    }

    @Override
    public void onDestroy() {
    }
}
