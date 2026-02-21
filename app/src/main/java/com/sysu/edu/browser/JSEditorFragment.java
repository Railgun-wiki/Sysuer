package com.sysu.edu.browser;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentJsEditorBinding;

import org.eclipse.tm4e.core.registry.IThemeSource;

import java.util.Objects;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;

public class JSEditorFragment extends Fragment {


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentJsEditorBinding binding = FragmentJsEditorBinding.inflate(inflater, container, false);
        binding.editor.setText(requireArguments().getString("script"));

        FileProviderRegistry.getInstance().addFileProvider(new AssetsFileResolver(requireContext().getAssets()));
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
        binding.editor.setEditorLanguage(TextMateLanguage.create("source.js", true));
        var themeRegistry = ThemeRegistry.getInstance();
        var name = "light"; // 主题名称
        var themeAssetsPath = "textmate/" + name + ".json";
        var model = new ThemeModel(IThemeSource.fromInputStream(Objects.requireNonNull(FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath)), themeAssetsPath, null), name);
        try {
            themeRegistry.loadTheme(model);
            ThemeRegistry.getInstance().setTheme(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        binding.editor.setColorScheme(TextMateColorScheme.create(themeRegistry));
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (toolbar.getMenu().size() == 0) {
            requireActivity().getMenuInflater().inflate(R.menu.editor, requireActivity().<MaterialToolbar>findViewById(R.id.toolbar).getMenu());
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.save) {
//                binding.editor
                } else if (item.getItemId() == R.id.redo) {
                    binding.editor.redo();
                } else if (item.getItemId() == R.id.undo) {
                    binding.editor.undo();
                }
                return false;
            });
        } else
            toolbar.getMenu().setGroupVisible(R.id.editor_group,true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setTransitionName("script");
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface));
        setSharedElementEnterTransition(transition);
        setSharedElementReturnTransition(transition);
    }

}