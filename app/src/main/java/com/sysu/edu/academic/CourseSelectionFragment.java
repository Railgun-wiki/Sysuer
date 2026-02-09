package com.sysu.edu.academic;

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseSelectionBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;
import com.sysu.edu.view.CourseAdapter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CourseSelectionFragment extends Fragment {

    final MutableLiveData<String> filter = new MutableLiveData<>();
    final MutableLiveData<Integer> type = new MutableLiveData<>(1);
    final MutableLiveData<Integer> category = new MutableLiveData<>(11);
    final MediatorLiveData<List<Integer>> typeCate = new MediatorLiveData<>();
    FragmentCourseSelectionBinding binding;
    Handler handler;
    String cookie;
    int tmp;
    int page = 1;
    CourseAdapter adp;
    Integer total;
    String term;
    CourseSelectionViewModel vm;
    GridLayoutManager gm;
    Params params;
    HttpManager http;
/*
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        super.onCreate(savedInstanceState);
    }*/

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentCourseSelectionBinding.inflate(inflater, container, false);
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            params = new Params(requireActivity());
            params.setCallback(this, () -> {
                clear();
                getInfo();
            });
            vm.filterValue.observe(requireActivity(), _ -> {
                filter.setValue(vm.getReturnData());
                binding.head.seniorFilter.removeAllViews();
                vm.getFilterName().forEach((_, v) ->
                {
                    if (v != null && !v.isEmpty()) {
                        ItemActionChipBinding item = ItemActionChipBinding.inflate(inflater, binding.head.filter, false);
                        item.getRoot().setText(v);
                        binding.head.seniorFilter.addView(item.getRoot());
                    }
                });
                regetCourseList();
            });
            binding.head.type.setOnCheckedStateChangeListener((chipGroup, _) -> {
                int cid = chipGroup.getCheckedChipId();
                if (cid == R.id.my_major) {
                    selectCategory();
                } else {
                    type.setValue((cid == R.id.college_public_selective) ? 4 : 2);
                }
                if (cid != R.id.my_major && binding.head.category.getHeight() != 0) {
                    tmp = binding.head.category.getHeight();
                }
                ValueAnimator animator = ValueAnimator.ofInt(chipGroup.getCheckedChipId() == R.id.my_major ? new int[]{0, tmp} : new int[]{binding.head.category.getHeight() == 0 ? 0 : tmp, 0});
                animator.addUpdateListener(valueAnimator -> {
                    LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) binding.head.category.getLayoutParams());
                    lp.height = (int) valueAnimator.getAnimatedValue();
                    binding.head.category.setLayoutParams(lp);
                });
                animator.start();
            });
            binding.zoom.setOnClickListener(_ -> binding.head.getRoot().setVisibility(binding.head.getRoot().getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            typeCate.addSource(type, s -> typeCate.setValue(List.of(type.getValue() == null ? 1 : type.getValue(), s)));
            typeCate.addSource(category, s -> typeCate.setValue(List.of(type.getValue() == null ? 11 : type.getValue(), s)));
            typeCate.observe(requireActivity(), _ -> regetCourseList());
            binding.head.category.setOnCheckedStateChangeListener((_, _) -> selectCategory());
            cookie = params.getCookie();
            binding.course.setLayoutManager(gm = new GridLayoutManager(requireContext(), params.getColumn()));
            binding.course.addItemDecoration(new SpacesItemDecoration(params.dpToPx(8)));
            binding.course.setAdapter(adp = new CourseAdapter());
            binding.head.filter.setOnCheckedStateChangeListener((_, _) -> regetCourseList());
            adp.setSelectAction(position -> {
                if (adp.getItem(position).getInteger("selectedStatus") == 3 || adp.getItem(position).getInteger("selectedStatus") == 4) {
                    unselect(adp.convert(position, "courseId"), adp.convert(position, "teachingClassId"));
                } else {
                    select(adp.convert(position, "teachingClassId"));
                }
            });
            adp.setLikeAction(this::like);

            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.obj == null) {
                        //params.toast(R.string.no_wifi_warning);
                        return;
                    }
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    Integer code = response.getInteger("code");
//                    System.out.println(response);
                    if (Objects.equals(code, 200)) {
                        switch (msg.what) {
                            case -1 -> params.toast(R.string.no_wifi_warning);
                            case 0 -> {
                                term = response.getJSONObject("data").getString("semesterYear");
                                getCourseList();
                            }
                            case 1 -> {
                                if (response.getJSONObject("data") != null) {
                                    total = response.getJSONObject("data").getInteger("total");
                                    response.getJSONObject("data").getJSONArray("rows").forEach(e -> adp.add((JSONObject) e));
                                }
                            }
                            case 3 -> {
                                params.toast(response.getString("data"));
                                regetCourseList();
                            }
                        }
                    } else if (Objects.equals(code, 50021000) || Objects.equals(code, 52021104) || Objects.equals(code, 52021100) || Objects.equals(code, 52021133) || Objects.equals(code, 52021170)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    }
                    super.handleMessage(msg);
                }
            };

            binding.course.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                    if (!v.canScrollVertically(1) && total / 10 + 1 > page && dy > 0) {
                        getCourseList();
                    }
                    binding.head.getRoot().setElevation(v.canScrollVertically(-1) ? params.dpToPx(2) : 0);
                    super.onScrolled(v, dx, dy);
                }
            });
            http = new HttpManager(handler);
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE");
            http.setParams(params);
            getInfo();
        }
        return binding.getRoot();
    }

    private void regetCourseList() {
        clear();
        getCourseList();
    }

    private void selectCategory() {
        int cid = binding.head.category.getCheckedChipId();
        if (cid == R.id.major_compulsory) {
            typeCate.setValue(List.of(1, 11));
        } else if (cid == R.id.major_selective) {
            typeCate.setValue(List.of(1, 21));
        } else if (cid == R.id.school_public_selective) {
            typeCate.setValue(List.of(1, 30));
        } else if (cid == R.id.pe) {
            typeCate.setValue(List.of(3, 10));
        } else if (cid == R.id.en) {
            typeCate.setValue(List.of(5, 1));
        } else if (cid == R.id.public_compulsory) {
            typeCate.setValue(List.of(1, 10));
        } else if (cid == R.id.honor) {
            typeCate.setValue(List.of(1, 31));
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            binding.head.addFilter.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.selection_to_filter1, null, new NavOptions.Builder()
                            .setEnterAnim(android.R.animator.fade_in)
                            .setExitAnim(android.R.animator.fade_out)
                            .build(), new FragmentNavigator.Extras(Map.of(v, "miniapp")))
            );
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        gm.setSpanCount(params.getColumn());
    }

    void clear() {
        if (adp != null) adp.clear();
        page = 0;
        total = -1;
    }

    int bool2int(boolean b) {
        return b ? 1 : 0;
    }

    void getCourseList() {
        if (type.getValue() == null || category.getValue() == null || term == null)
            return;
        getCourseList(getType(), getCategory(), term, filter.getValue() == null ? "" : filter.getValue());
    }

    void getCourseList(int selectedType, int selectedCate, String term, String filterText) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/list",
                String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"param\":{\"semesterYear\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"hiddenConflictStatus\":\"0\",\"hiddenSelectedStatus\":\"%d\",\"hiddenEmptyStatus\":\"%d\",\"vacancySortStatus\":\"%d\",\"collectionStatus\":\"%d\"%s}}", ++page, term, selectedType, selectedCate,
                        bool2int(binding.head.hideSelected.isChecked()), bool2int(binding.head.hideVacancy.isChecked()), bool2int(binding.head.vacancy.isChecked()), bool2int(binding.head.onlyCollection.isChecked()),
                        filterText), 1);
    }

    void like(String code) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/stuCollectedCourse/create",
                String.format("{\"classesID\":\"%s\",\"selectedType\":\"1\"}", code),
                3);
    }

    void getInfo() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/selectCourseInfo", 0);
    }

    void select(String code) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/choose",
                String.format(Locale.getDefault(), "{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}", code, getType(), getCategory()),
                3);

    }

    int getType() {
        return Objects.requireNonNull(typeCate.getValue()).get(0) == null ? 1 : typeCate.getValue().get(0);
    }

    int getCategory() {
        return Objects.requireNonNull(typeCate.getValue()).get(1) == null ? 11 : typeCate.getValue().get(1);
    }

    void unselect(String classId, String code) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/back",
                String.format(Locale.getDefault(), "{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}", classId, code, getType()),
                3);

    }

    static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int i) {
            this.space = i;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.top = space / 2;
            outRect.right = space;
            outRect.left = space;
            outRect.bottom = space / 2;
        }
    }
}

