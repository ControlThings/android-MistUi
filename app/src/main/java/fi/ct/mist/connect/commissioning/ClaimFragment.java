package fi.ct.mist.connect.commissioning;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import fi.ct.mist.mist.R;

public class ClaimFragment extends Fragment {

    private final static String TAG = "CommissioningFragment";

    public ClaimFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    ProgressBar progressBar;
    LinearLayout layout;
    TextView textView;
    Button button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.node_claim_fragment, container, false);
        layout = (LinearLayout) view.findViewById(R.id.claim_layout);
        textView = (TextView) view.findViewById(R.id.claim_text);
        button = (Button) view.findViewById(R.id.claim_button);
        progressBar = (ProgressBar) view.findViewById(R.id.claim_progress_bar);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        textView.setText("You took the root account of a new system.\n" +
                "If it was wrong device, please reset it.");
        button.setText("Configure WLAN");

        progressBar.setVisibility(View.GONE);
        layout.setVisibility(View.VISIBLE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClaimListener listener = (ClaimListener) getActivity();
                listener.onClaimButtonClicked();
                progressBar.setVisibility(View.VISIBLE);
            }
        });

    }



    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public interface ClaimListener {
        public void onClaimButtonClicked();
    }
}
