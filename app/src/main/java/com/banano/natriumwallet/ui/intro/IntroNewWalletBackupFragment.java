package com.banano.natriumwallet.ui.intro;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.banano.natriumwallet.R;
import com.banano.natriumwallet.bus.CreatePin;
import com.banano.natriumwallet.bus.RxBus;
import com.banano.natriumwallet.databinding.FragmentIntroNewWalletSeedBackupBinding;
import com.banano.natriumwallet.model.Credentials;
import com.banano.natriumwallet.ui.common.ActivityWithComponent;
import com.banano.natriumwallet.ui.common.BaseFragment;
import com.banano.natriumwallet.ui.common.FragmentUtility;
import com.banano.natriumwallet.ui.common.WindowControl;
import com.banano.natriumwallet.ui.home.HomeFragment;
import com.banano.natriumwallet.util.ExceptionHandler;
import com.banano.natriumwallet.util.SharedPreferencesUtil;
import com.hwangjr.rxbus.annotation.Subscribe;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * The Intro Screen to the app
 */

public class IntroNewWalletBackupFragment extends BaseFragment {
    public static String TAG = IntroNewWalletBackupFragment.class.getSimpleName();
    FragmentIntroNewWalletSeedBackupBinding binding;
    @Inject
    Realm realm;

    @Inject
    SharedPreferencesUtil sharedPreferencesUtil;

    private boolean nextTriggered = false;

    /**
     * Create new instance of the fragment (handy pattern if any data needs to be passed to it)
     *
     * @return IntroNewWalletBackupFragment instance
     */
    public static IntroNewWalletBackupFragment newInstance() {
        Bundle args = new Bundle();
        IntroNewWalletBackupFragment fragment = new IntroNewWalletBackupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        nextTriggered = false;
        // init dependency injection
        if (getActivity() instanceof ActivityWithComponent) {
            ((ActivityWithComponent) getActivity()).getActivityComponent().inject(this);
        }

        // inflate the view
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_intro_new_wallet_seed_backup, container, false);
        view = binding.getRoot();

        // subscribe to bus
        RxBus.get().register(this);

        // bind data to view
        binding.setHandlers(new ClickHandlers());

        // Override back button press
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                goToSeed();
                return true;
            }
            return false;
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // unregister from bus
        RxBus.get().unregister(this);
    }

    @Subscribe
    public void receiveCreatePin(CreatePin pinComplete) {
        realm.executeTransaction(realm -> {
            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                credentials.setPin(pinComplete.getPin());
            }
        });
        goToHomeScreen();
    }


    private void goToHomeScreen() {
        // set confirm flag
        sharedPreferencesUtil.setConfirmedSeedBackedUp(true);

        // go to home screen
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).getFragmentUtility().replace(
                    HomeFragment.newInstance(),
                    FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                    FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                    HomeFragment.TAG
            );
        }
    }

    private void goToSeed() {
        // go to seed screen
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).getFragmentUtility().replace(
                    IntroNewWalletFragment.newInstance(false),
                    FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                    FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                    IntroNewWalletFragment.TAG
            );
        }
    }

    private void goToNext() {
        // go to next screen
        if (getActivity() instanceof WindowControl) {
            ((WindowControl) getActivity()).getFragmentUtility().replace(
                    IntroNewWalletWarningFragment.newInstance(),
                    FragmentUtility.Animation.ENTER_LEFT_EXIT_RIGHT,
                    FragmentUtility.Animation.ENTER_RIGHT_EXIT_LEFT,
                    IntroNewWalletWarningFragment.TAG
            );
        }
    }

    public class ClickHandlers {
        public void onClickBack(View v) {
            goToSeed();
        }

        public void onClickYes(View v) {
            if (!nextTriggered) {
                nextTriggered = true;
            } else {
                return;
            }
            Credentials credentials = realm.where(Credentials.class).findFirst();
            if (credentials != null) {
                if (credentials.getPin() == null) {
                    showCreatePinScreen();
                } else {
                    goToHomeScreen();
                }
            } else {
                ExceptionHandler.handle(new Exception("Problem accessing generated seed"));
            }
        }

        public void onClickNo(View v) {
            goToSeed();
        }
    }
}