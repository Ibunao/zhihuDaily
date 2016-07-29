package me.bunao.www.zhihudailytest.ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import me.bunao.www.zhihudailytest.R;
import me.bunao.www.zhihudailytest.support.Check;
import me.bunao.www.zhihudailytest.support.Constants;
import me.bunao.www.zhihudailytest.ui.ZhihuDailyApplication;

//配置也自动创建配置xml文件
public class PrefsFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs);
        findPreference("about").setOnPreferenceClickListener(this);
        //如果没有安装知乎客户端这移除using_client选项
        if (!Check.isZhihuClientInstalled()) {
            //找到综合设置
            ((PreferenceCategory) findPreference("settings_settings"))
                    .removePreference(findPreference("using_client?"));
        }

        if (!ZhihuDailyApplication.getSharedPreferences()
                .getBoolean(Constants.SharedPreferencesKeys.KEY_SHOULD_ENABLE_ACCELERATE_SERVER, false)) {
            ((PreferenceScreen) findPreference("preference_screen"))
                    .removePreference(findPreference("settings_network_settings"));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("about")) {
            showApacheLicenseDialog();
            return true;
        }
        return false;
    }

    private void showApacheLicenseDialog() {
        final Dialog apacheLicenseDialog = new Dialog(getActivity());
        apacheLicenseDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        apacheLicenseDialog.setCancelable(true);
        apacheLicenseDialog.setContentView(R.layout.dialog_apache_license);

        TextView textView = (TextView) apacheLicenseDialog.findViewById(R.id.dialog_text);

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.licences_header)).append("\n");

        String[] basedOnProjects = getResources().getStringArray(R.array.apache_licensed_projects);

        for (String str : basedOnProjects) {
            sb.append("> ").append(str).append("\n");
        }


        textView.setText(sb.toString());

        Button closeDialogButton = (Button) apacheLicenseDialog.findViewById(R.id.close_dialog_button);

        closeDialogButton.setOnClickListener(view -> apacheLicenseDialog.dismiss());

        closeDialogButton.setOnLongClickListener(v -> {
            apacheLicenseDialog.dismiss();
            Toast.makeText(getActivity(),
                    getActivity().getString(R.string.accelerate_server_unlock),
                    Toast.LENGTH_SHORT).show();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean(Constants.SharedPreferencesKeys.KEY_SHOULD_ENABLE_ACCELERATE_SERVER, true)
                    .apply();
            return true;
        });

        apacheLicenseDialog.show();
    }
}
