package fi.ct.mist;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import fi.ct.mist.main.Device;

/**
 * Created by jeppe on 12/12/16.
 */

public class Menu extends AppCompatActivity {

    public ViewPagerAdapter adapter;
    public OnPageChangeListener changeListener;


    public class ViewPagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> mFragmentList = new ArrayList<>();
        private List<String> mFragmentTitleList = new ArrayList<>();
        private List<View.OnClickListener> mClickListener = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
            if (manager.getFragments() != null) {
                for (Fragment fragment : manager.getFragments()) {
                    manager.beginTransaction().remove(fragment).commit();
                }
            }
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title, View.OnClickListener clickListener) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
            mClickListener.add(clickListener);
        }

        public View.OnClickListener getClickListener(int position) {
            return mClickListener.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    public class OnPageChangeListener implements ViewPager.OnPageChangeListener {

        FloatingActionButton _fab;
        private boolean show = false;

        public OnPageChangeListener(FloatingActionButton fab) {
            this._fab = fab;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (positionOffset == 0.0 && adapter.getClickListener(position) == null) {
                _fab.hide();
            }
        }

        @Override
        public void onPageSelected(int position) {

            _fab.setOnClickListener(adapter.getClickListener(position));
            if (adapter.getClickListener(position) != null) {
                show = true;
                // _fab.show();
            } else {
                show = false;
                // _fab.hide();
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

            if (state == 1) {
                _fab.hide();
            }
            if (state == 0 && show) {
                _fab.show();
            }
            if (state == 2) {
                _fab.hide();
            }
        }


    }


}
