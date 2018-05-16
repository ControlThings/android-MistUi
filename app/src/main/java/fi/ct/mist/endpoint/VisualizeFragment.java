package fi.ct.mist.endpoint;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fi.ct.mist.mist.R;

public class VisualizeFragment extends Fragment {

    public VisualizeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String info = "Historical data\n" +
                "\n" +
                "The default logging feature is not completely implemented yet. \n\n" +
                "In the future you’ll be able to aggregate data for this endpoint, by activating a timeseries data logging service. It can run locally on same device, or remotely on dedicated device or in a cloud as a service.\n" +
                "\n" +
                "On this tab, you’ll be able to view the visualisation of the historical data, perhaps using Graphana visualisation components.";

        View v = inflater.inflate(R.layout.fragment_one, container, false);
        TextView myTextView = (TextView) v.findViewById(R.id.one);
        myTextView.setText(info);

        return v;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }
}
