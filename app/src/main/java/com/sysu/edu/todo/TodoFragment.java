package com.sysu.edu.todo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.CalendarView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentTodoBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TodoFragment extends Fragment {

    final androidx.recyclerview.widget.ConcatAdapter concatAdapter = new androidx.recyclerview.widget.ConcatAdapter(new androidx.recyclerview.widget.ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
    FragmentTodoBinding binding;
    private TodoManager todoManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTodoBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(concatAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.calendarView.setOnCalendarSelectListener(new CalendarView.OnCalendarSelectListener() {
            @Override
            public void onCalendarOutOfRange(Calendar calendar) {
            }

            @Override
            public void onCalendarSelect(Calendar calendar, boolean isClick) {
                todoManager.refresh("due_date = ?", new String[]{new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTimeInMillis())});
            }
        });
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        binding.calendarView.setOnMonthChangeListener((year, month) -> toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", year, month)));
        toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", binding.calendarView.getCurYear(), binding.calendarView.getCurMonth()));
        binding.calendarView.setSelectSingleMode();
        todoManager = new TodoManager(requireActivity(), concatAdapter);
        todoManager.setOnRefreshListener(() -> todoManager.refresh("due_date = ?", new String[]{new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(binding.calendarView.getSelectedCalendar().getTimeInMillis())}));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public TodoManager getInitTodo() {
        return todoManager;
    }
}