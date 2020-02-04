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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fi.ct.mist.mist.R;

public class SystemFragment extends Fragment {

    public SystemFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String info = "Lorem ipsum dolor sit amet, convallis semper ut sociis. In morbi sed eget scelerisque dictumst augue.";

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
