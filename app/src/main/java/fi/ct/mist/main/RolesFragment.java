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
package fi.ct.mist.main;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fi.ct.mist.mist.R;
import wish.Identity;

public class RolesFragment extends Fragment {

    public RolesFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MainFragmentListener listener = (MainFragmentListener) getActivity();
        Identity identity = listener.getSelectedIdentity();

        String alias = "--";
        if (identity != null) {
            alias = identity.getAlias();
        }

        String info = "Here you will be able to manage the roles of " + alias + "." +
                " A list of roles will appear, and by clicking a role you’re able to define the appropriate access on endpoint level." +
                " (In order to define a role on another system, please choose to act as the remote identity first).";

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
