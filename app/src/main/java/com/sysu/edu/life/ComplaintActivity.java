package com.sysu.edu.life;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityComplaintBinding;
import com.sysu.edu.view.Pager2Adapter;

import java.util.List;

public class ComplaintActivity extends AppCompatActivity {

    private static int getItem(MenuItem item, List<Integer> itemIds) {
        return itemIds.indexOf(item.getItemId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityComplaintBinding binding = ActivityComplaintBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Pager2Adapter adapter = new Pager2Adapter(this);
        adapter.add(new ComplaintMainFragment());
//        adapter.add(new ComplaintMainFragment());
//        adapter.add(new ComplaintMainFragment());
        binding.pager.setAdapter(adapter);
        List<Integer> itemIds = List.of(R.id.complaint, R.id.response, R.id.square);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position < itemIds.size())
                    binding.bottomNav.setSelectedItemId(itemIds.get(position));
            }
        });
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int currentItem = getItem(item, itemIds);
            if (currentItem >= 0 && currentItem < adapter.getItemCount())
                binding.pager.setCurrentItem(currentItem);
            return true;
        });
    }
}