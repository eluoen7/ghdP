package eluoen.ghdp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.zxing.activity.CaptureActivity;
import eluoen.ghdp.util.Constant;
import eluoen.ghdp.util.HttpUtil;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CopyOnWriteArraySet;
import java.lang.ref.WeakReference;
import android.app.ProgressDialog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout lay_download;
    private ProgressBar pb_download;
    private TextView txt_download;
    private TextView txt_version;

    private Button btnQrCode; // 扫码
    private TextView tvResult; // 结果

    private EditText editQrCode;
    private Button btnSearch;
    private TextView txtInfo;//查询结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        //默认不弹出软键盘
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        //检查版本
        checkVersion();

    }


    private void initView() {
        lay_download = (LinearLayout) findViewById(R.id.lay_download);
        txt_version = (TextView) findViewById(R.id.txt_version);
        txt_download = (TextView) findViewById(R.id.txt_download);
        pb_download = (ProgressBar) findViewById(R.id.pb_download);

        btnQrCode = (Button) findViewById(R.id.btn_qrcode);
        btnQrCode.setOnClickListener(this);


        editQrCode = (EditText) findViewById(R.id.edit_qrcode);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(this);
        txtInfo = (TextView) findViewById(R.id.txt_info);

        tvResult = (TextView) findViewById(R.id.txt_result);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_qrcode:
                startQrCode();
                break;
            case R.id.btn_search:
                processThreadSearch();
                break;
        }
    }





    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////以下是开始扫描/////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 开始扫码
    private void startQrCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constant.REQ_PERM_CAMERA);
            return;
        }
        // 二维码扫码
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, Constant.REQ_QR_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);
            //将扫描出的信息显示出来
            tvResult.setText(scanResult);

            editQrCode.setText(scanResult);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode();
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
                }
                break;
            case 11011:
                //11011是自己定义的手机读写存储权限编号
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权，开始下载
                    startDownFile();
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的读写手机存储权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }





    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////以下是查询检索结果//////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    private ProgressDialog progressDialog = null;

    private void processThreadSearch(){

        progressDialog = ProgressDialog.show(this, null, "Loading......");
        progressDialog.setCancelable(true);
        new Thread() {
            public void run() {

                String path = HttpUtil.IP + "/pda_updateGoods";
                String result = HttpUtil.httpClient(null,path,"POST");

                Message msg = new Message();
                msg.obj = result;
                handler.sendMessage(msg);
            };
        }.start();

    }

    private Handler handler = new myHandler(this);

    private final static class myHandler extends Handler{

        private WeakReference<MainActivity> reference = null;

        private myHandler(MainActivity activity){
            reference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            MainActivity activity = reference.get();
            activity.progressDialog.dismiss();

            activity.txtInfo.setText(msg.obj.toString());

        }

    }





    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////以下是版本判断或更新、、/////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    private void checkVersion(){

        String version = HttpUtil.getVersionName(this);
        txt_version.setText("版本号："+version);

        // 如果有网络，检测版本更新
        boolean is = HttpUtil.isNetWorkLink(this);
        if (is) {
            processThreadUpdate();
        }

    }

    private int serverVersionCode = -1;
    private int versionCode = -1;


    private void processThreadUpdate() {

        new Thread() {

            public void run() {
                // 在新线程里执行长耗时方法
                try {

                    HttpUtil.vm = HttpUtil.getServerVersionCode();

                    serverVersionCode = HttpUtil.vm.getVerCode();
                    versionCode = HttpUtil.getVersionCode(MainActivity.this);

                } catch (Exception e) {
                    serverVersionCode = versionCode;
                }
                // 执行完毕后给handler发送一个空消息
                handlerUpdate.sendEmptyMessage(0);
            }

        }.start();
    }

    private Handler handlerUpdate = new Handler() {
        // 当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (versionCode != serverVersionCode) {
                doNewVersionUpdate(MainActivity.this);
            }
        }
    };

    private void doNewVersionUpdate(final Context context) {
        String verName = HttpUtil.getVersionName(this);
        StringBuffer sb = new StringBuffer();
        sb.append("当前版本:");
        sb.append(verName);
        sb.append(", 发现新版本:");
        sb.append(HttpUtil.vm.getVerName());
        sb.append("\n" + HttpUtil.vm.getRemark());
        Dialog dialog = new AlertDialog.Builder(context)
                .setIcon(R.drawable.icon).setTitle("有新版本")
                .setMessage(sb.toString())
                // 设置内容
                .setPositiveButton("立即更新",// 设置确定按钮
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                //如果权限没打开，则申请权限
                                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    // 申请权限，这里自定义手机读取存储权限10011，回调时判断用
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 11011);
                                    return;
                                }else{
                                    //如果已经打开，则直接开始下载
                                    startDownFile();
                                }

                            }
                        }).setNegativeButton("暂不更新", null).create();// 创建
        // 显示对话框
        try {
            dialog.show();
        } catch (Exception e) {
            // TODO: handle exception
            return;
        }
    }


    private void startDownFile(){
        lay_download.setVisibility(View.VISIBLE);

        downFile(HttpUtil.IP + HttpUtil.vm.getApkname());
    }

    private int fileSize;
    private int downloadSize;

    void downFile(final String path) {
        System.out.println("downFile:" + path);
        new Thread() {
            public void run() {

                try {

                    URL url = new URL(path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.connect();

                    long length = conn.getContentLength();

                    fileSize = (int) length;
                    sendMsg(0);
                    InputStream is = conn.getInputStream();
                    FileOutputStream fileOutputStream = null;
                    if (is != null) {
                        File file = new File(
                                Environment.getExternalStorageDirectory(),
                                HttpUtil.vm.getVerName());
                        fileOutputStream = new FileOutputStream(file);
                        byte[] buf = new byte[1024];
                        int ch = -1;
                        int count = 0;
                        while ((ch = is.read(buf)) != -1) {
                            downloadSize = downloadSize + ch;
                            sendMsg(1);
                            fileOutputStream.write(buf, 0, ch);
                            count = count + ch;
                            if (length > 0) {
                            }
                        }
                    }
                    fileOutputStream.flush();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    sendMsg(2);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void sendMsg(int flag) {
        Message msg = new Message();
        msg.what = flag;
        handler2.sendMessage(msg);
    }

    Handler handler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    pb_download.setMax(fileSize);
                    break;

                case 1:
                    pb_download.setProgress(downloadSize);
                    txt_download.setText((downloadSize * 100 / fileSize) + "%");
                    break;

                case 2:
                    update();
                    break;
            }
        }
    };

    void update() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(Environment
                        .getExternalStorageDirectory(), HttpUtil.vm.getVerName())),
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


}
