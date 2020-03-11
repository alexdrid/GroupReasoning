package com.drid.group_reasoning.ui.fragments;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.drid.group_reasoning.R;


public class KnowledgeFragment extends Fragment {

    public static final String TAG = KnowledgeFragment.class.getSimpleName();

    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;

    private FloatingActionButton fab;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_knowledge, container, false);

        fab = view.findViewById(R.id.add_fab);
        viewPager = view.findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        fab.show();
                        break;
                    case ViewPager.SCROLL_STATE_DRAGGING:
                    case ViewPager.SCROLL_STATE_SETTLING:
                        fab.hide();
                        break;
                }
            }
        });

        TabLayout tabLayout = view.findViewById(R.id.sliding_tabs);

        tabLayout.setupWithViewPager(viewPager);


        return view;
    }

    public FloatingActionButton getFab() {
        return fab;
    }


    private static class ViewPagerAdapter extends FragmentPagerAdapter {

        private static final int NUM_ITEMS = 2;

        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new RuleFragment();
            } else {
                return new FactFragment();
            }
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }


        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0) {
                return "Rules";
            } else {
                return "Facts";
            }
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}
