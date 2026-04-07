package com.sysu.edu.academic;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentHomeworkMainBinding;
import com.sysu.edu.databinding.ItemHomeworkBinding;
import com.sysu.edu.template.RecyclerAdapter;
import com.sysu.edu.todo.info.TitleAdapter;

public class HomeworkMainFragment extends Fragment {

    HttpManager http;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentHomeworkMainBinding binding = FragmentHomeworkMainBinding.inflate(getLayoutInflater());
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        ConcatAdapter adapter = new ConcatAdapter();
        Params params = new Params(this);
        ContextUtil contextUtil = new ContextUtil(requireContext());
        AuthorizationJar authorizationJar = new AuthorizationJar(requireContext());
        binding.list.setAdapter(adapter);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                System.out.println(msg.obj);
                if (msg.what == -1)
                    params.toast(R.string.no_net_connected);
                else if (msg.getData().getBoolean("isJSON")) {
                    boolean error = false;
                    for (Object i : JSONArray.parseArray((String) msg.obj)) {
                        JSONObject item = (JSONObject) i;
                        error = item.getBoolean("error");
                        if (error) {
                            params.toast(item.getJSONObject("exception").getString("message"));
                            break;
                        } else {
                            item.getJSONObject("data").getJSONArray("events").forEach(event -> {
                                JSONObject eventItem = (JSONObject) event;
                                TitleAdapter titleAdapter = new TitleAdapter(eventItem.getString("popupname"));
                                titleAdapter.setHeader(2);
                                adapter.addAdapter(titleAdapter);
//                                GymAccountFragment.PreferenceAdapter preferenceAdapter = new GymAccountFragment.PreferenceAdapter();
//                                preferenceAdapter.add(eventItem.getString("name"), eventItem.getString("description"), R.drawable.text);
                                HomeworkAdapter homeworkAdapter = new HomeworkAdapter();
                                homeworkAdapter.add(eventItem);
                                adapter.addAdapter(homeworkAdapter);
                            });
                        }
                    }
                    if (error)
                        contextUtil.login(TargetUrl.LMS, () -> getLmsTask(authorizationJar.getToken("lms.sysu.edu.cn")));
                }
            }
        });
        getLmsTask(authorizationJar.getToken("lms.sysu.edu.cn"));
        return binding.getRoot();
    }

    public void getLmsTask(String key) {
        http.postRequest(String.format("https://lms.sysu.edu.cn/lib/ajax/service.php?sesskey=%s&info=core_calendar_get_calendar_upcoming_view", key), "[{\"index\":0,\"methodname\":\"core_calendar_get_calendar_upcoming_view\",\"args\":{\"courseid\":\"1\",\"categoryid\":\"0\"}}]", 0);
    }


    static class HomeworkAdapter extends RecyclerAdapter<JSONObject> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            return new RecyclerView.ViewHolder(ItemHomeworkBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemHomeworkBinding binding = ItemHomeworkBinding.bind(holder.itemView);
            Context context = holder.itemView.getContext();
            JSONObject item = get(position);
            binding.title.setText(item.getString("name"));
            binding.description.setText(Html.fromHtml(item.getString("description"), Html.FROM_HTML_MODE_COMPACT));
            binding.fold.setOnClickListener(_ -> binding.description.setMaxLines(Integer.MAX_VALUE-1-binding.description.getMaxLines()));
            binding.detail.setText(String.format("%s %s\n%s %s", context.getString(R.string.type), item.getString("normalisedeventtypetext"),
                    context.getString(R.string.link), item.getString("viewurl")));
//            Markwon.create(binding.title.getContext()).setMarkdown(binding.description, item.getString("description"));
        }
    }
}