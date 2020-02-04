/**
 * Copyright (C) 2020, ControlThings Oy Ab
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * @license Apache-2.0
 */
package fi.ct.mist.system;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fi.ct.mist.mist.R;
import fi.ct.mist.main.Main;
import fi.ct.mist.main.MainFragmentListener;

public class MappingsFragment extends Fragment {

    public MappingsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SystemsFragmentListener listener = (SystemsFragmentListener) getActivity();
        String alias = listener.getAlias();;

        String info = "Mappings are subscriptions between endpoints," +
                " and mappings can be made across different systems." +
                " Using mappings, you’re able to setup M2M interactions" +
                " which will continue to run independently even though you disconnect.\n\n\n" +
                "This page provides an overview of all mappings made either from or to "+ alias +"." +
                " If you like to a new mapping, please navigate to the appropriate endpoint first," +
                " from where you will find the add mapping feature.  From this page you’re able to remove mappings.";



        String text = "";

        View v = inflater.inflate(R.layout.fragment_one, container, false);
        TextView myTextView = (TextView) v.findViewById(R.id.one);
        myTextView.setText(info);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("test", "onAttach");

    }
}
