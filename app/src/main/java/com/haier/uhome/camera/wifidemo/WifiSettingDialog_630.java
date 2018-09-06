package com.haier.uhome.camera.wifidemo;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;

/*
import com.haiersmart.sfnation.R;
import com.haiersmart.sfnation630.adapter.wifiSetup.WifiSettingAdapter_630;
import com.unilife.common.utils.SystemUtils;
import com.unilife.common.utils.ToastMng;
import com.unilife.common.utils.WifiSystem;
*/

import java.util.List;

/**
 * WifiSettingDialog：Wifi设置Dialog
 *
 */
public class WifiSettingDialog_630 extends Dialog{
    private Context context;
    private final int DELAY_FLUSH_WIFI = 5000;//5s刷新一次
    private final int DELAY_WIFI_ERROR = 10000;//10s没有连接上，则提示连接失败
    private final int MSG_FLUSH_WIFI = 1;
    private final int MSG_WIFI_ERROR = 3;
    private static int flag = 1;//计数，N次后重置wifi

    private ListView mWifiListView;//wifi列表-listview
    private WifiManager mWifiManager;
    private WifiSystem mWifiAdmin;
    private WifiSettingAdapter_630 mWifiAdapter;

    private WifiSystem.WifiSystemListener m_WifiSystemListener = new WifiSystem.WifiSystemListener() {
        @Override
        public void onScanEnd(List<WifiSystem.WifiSystemItem> scanResultList) {
            m_Handler.removeMessages(MSG_FLUSH_WIFI);
            m_Handler.sendEmptyMessageDelayed(MSG_FLUSH_WIFI, DELAY_FLUSH_WIFI);
            if(mWifiAdapter != null) {
                if(mWifiAdapter.getSelected() == -1) {
                    mWifiAdapter.updateData(scanResultList);
                    mWifiAdapter.notifyDataSetChanged();
                    if (scanResultList.size() == 0 && flag > 10) {
                        flag = 0;
                        SystemUtils.resetWiFi(mWifiAdmin.getWifiManager());
                    } else {
                        flag++;
                    }
                }
            }
        }
        @Override
        public void onWifiState(Boolean state) {

        }
    };

    private Handler m_Handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_FLUSH_WIFI:
                    mWifiAdmin.startScan();
                    break;
                case MSG_WIFI_ERROR:
                    if(context == null) {
                        return;
                    }
                    ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if(manager == null) {
                        return;
                    }

                    NetworkInfo mWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if(mWifi == null || !mWifi.isConnected()) {
                        ToastMng.toastShow("连接失败");
                    }
                    break;
            }
        }

        ;
    };


    public WifiSettingDialog_630(Context context) {
        super(context, R.style.myDialog);
        this.context = context;
        setContentView(R.layout.wifi_setting_layout_630);
        initView();
        initDate();
        initListener();
    }

    private void initListener() {
        mWifiAdmin.registerListener(m_WifiSystemListener);
        mWifiAdapter.setOnWifiStartLineListener(new WifiSettingAdapter_630.OnWifiStartLineListener() {
            @Override
            public void startLine(String pwd, WifiSystem.WifiSystemItem item) {
                int pwdType = mWifiAdmin.getWifiPwdType(item.getScanResult().capabilities);
                if (pwdType == 1) { //没有密码就不需要弹出密码框，直接进去连接
                    mWifiAdmin.addNetwork(mWifiAdmin.CreateWifiInfo(item.getScanResult().SSID, "", pwdType));
                    return;
                }
                if (item.isSaved() && "******".equals(pwd)) { //如果已经保存过，则直接连接
                    mWifiAdmin.addNetwork(item.getConfig());
                    m_Handler.removeMessages(MSG_WIFI_ERROR);
                    m_Handler.sendEmptyMessageDelayed(MSG_WIFI_ERROR, DELAY_WIFI_ERROR);
                    return;
                }
                WifiConfiguration config = mWifiAdmin.CreateWifiInfo(item.getScanResult().SSID, pwd, pwdType);
                if (config == null) {
                    return;
                }
                mWifiAdmin.addNetwork(config);
                m_Handler.removeMessages(MSG_WIFI_ERROR);
                m_Handler.sendEmptyMessageDelayed(MSG_WIFI_ERROR, DELAY_WIFI_ERROR);
            }
        });
    }

    private void initDate() {
        //wifi
        mWifiAdmin = WifiSystem.getInstance(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!mWifiManager.isWifiEnabled()) {
            mWifiAdmin.openWifi();
        }

        mWifiAdapter = new WifiSettingAdapter_630(context, WifiSystem.getInstance(context).getScanResultItems());
        mWifiListView.setAdapter(mWifiAdapter);

    }

    private void initView() {
        findViewById(R.id.btn_close_wifi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mWifiListView = (ListView) findViewById(R.id.lv_wifi_item);
    }

    @Override
    public void setOnDismissListener(OnDismissListener listener) {
        super.setOnDismissListener(listener);
        mWifiAdmin.unregisterListener(m_WifiSystemListener);
        m_Handler.removeMessages(MSG_FLUSH_WIFI);
        m_Handler.removeMessages(MSG_WIFI_ERROR);
    }
}
