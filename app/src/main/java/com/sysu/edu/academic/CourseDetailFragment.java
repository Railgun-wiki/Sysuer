package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.trim;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentCourseDetailBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;

public class CourseDetailFragment extends Fragment {

    FragmentCourseDetailBinding binding;
    JSONObject data;
    Params params;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCourseDetailBinding.inflate(inflater);
        params = new Params(requireActivity());
        return binding.getRoot();
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (args != null) {
            JSONObject data = JSONObject.parse(args.getString("data"));
            if (data != null) {
                switch (args.getInt("what")) {
                    case 1:
                        this.data = data;
                        break;
                    case 2:
                        try {
                            binding.intro.setText(trim(this.data.getString("courseContentInChinese")));
                            binding.goal.setText(trim(this.data.getString("courseObjectiveAndRequirement")));
                            binding.method.setText(trim(this.data.getString("teachMethod")));
                            binding.evaluationMethod.setText(trim(this.data.getString("evaluationMethod")));
                            binding.reference.setText(trim(this.data.getString("referenceBook")));
                            binding.resource.setText(trim(this.data.getString("courseResource")));
                            String[] info = new String[]{"courseName", "faceProfessionName", "courseTypeName", "courseNum", "courseId", "subCourseTypeName", "subTypeModuleName", "courseTextBook", "credit", "totalHours", "lecturesCreHours", "labCreHours", "weekHours", "totalHoursComment", "languageName", "establishUnitNumberName", "planClassSize", "teacherName", "intendedAcadYear", "intendedCampusName"};
                            for (int i = 0; i < info.length; i++) {
                                String content = (i == 9 | i == 10 ? data : this.data).getString(info[i]);
                                if (content == null) content = "";
                                Chip chip = ItemActionChipBinding.inflate(getLayoutInflater()).getRoot();
                                chip.setText(String.format("%s：%s", getResources().getStringArray(R.array.course_outline)[i], content));
                                String finalContent = content;
                                chip.setOnLongClickListener(_ -> {
                                    params.copy("courseId", finalContent);
                                    params.toast(R.string.copy_successfully);
                                    return false;
                                });
                                chip.setOnClickListener(a -> Snackbar.make(requireContext(), chip, ((Chip) a).getText(), Snackbar.LENGTH_LONG).show());
                                binding.detail.addView(chip);
                            }
                        } catch (Exception ignored) {
                        }
                        break;
                }

            }
        }
        super.setArguments(args);
    }
}