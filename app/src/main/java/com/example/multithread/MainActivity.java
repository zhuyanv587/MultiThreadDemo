package com.example.multithread;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressBar bar;
    private TextView textView;
    private Button multi, asynchronous, handler, asyncTask, type;
    private ImageView imageView;
    private static final String DOWNLOAD_URL = "https://b-ssl.duitang.com/uploads/blog/201312/04/20131204184148_hhXUT.jpeg";
    private static final String DOWNLOAD_URLS = "https://source.unsplash.com/random/1000x600/?race,car";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        bar = findViewById(R.id.progress);
        textView = findViewById(R.id.tv_count);
        imageView = findViewById(R.id.image);
        multi = findViewById(R.id.btn_multi);
        asynchronous = findViewById(R.id.btn_asynchronous);
        handler = findViewById(R.id.btn_handler);
        asyncTask = findViewById(R.id.btn_asyncTask);
        type = findViewById(R.id.btn_type);

        multi.setOnClickListener(this);
        asynchronous.setOnClickListener(this);
        handler.setOnClickListener(this);
        asyncTask.setOnClickListener(this);
        type.setOnClickListener(this);
    }

    private CalculateThread calculateThread;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_multi:
                calculateThread = new CalculateThread();
                calculateThread.start();
                break;
            case R.id.btn_asynchronous:
                new MyAsyncTask(this).execute(100);
                break;
            case R.id.btn_handler:
                new Thread(new DownloadImageFetcher(DOWNLOAD_URL)).start();
                break;
            case R.id.btn_asyncTask:
                new DownloadImage(this).execute(DOWNLOAD_URLS);
                break;
            case R.id.btn_type:
                break;
        }
    }

    //进度条常量：Message.what类型
    private static final int START_NUM = 1;
    private static final int ADDING_NUM = 2;
    private static final int ENDING_NUM = 3;
    private static final int CANCEL_NUM = 4;
    private MyHandler myHandler = new MyHandler(this);

    //自定义Handler静态类
    static class MyHandler extends Handler {
        //定义弱引用对象
        private WeakReference<Activity> ref;

        //在构造方法中创建此对象
        public MyHandler(Activity activity) {
            this.ref = new WeakReference<>(activity);
        }

        //重写handle方法
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //获取弱引用指向的activity对象
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
            //根据Message的what属性值处理消息
            switch (msg.what) {
                case START_NUM:
                    activity.bar.setVisibility(View.VISIBLE);
                    break;
                case ADDING_NUM:
                    activity.bar.setProgress(msg.arg1);
                    activity.textView.setText("计算已完成" + msg.arg1 + "%");
                    break;
                case ENDING_NUM:
                    activity.bar.setVisibility(View.GONE);
                    activity.textView.setText("计算已完成，结果为：" + msg.arg1);
                    activity.myHandler.removeCallbacks(activity.calculateThread);
                    break;
                case CANCEL_NUM:
                    activity.bar.setProgress(0);
                    activity.bar.setVisibility(View.GONE);
                    activity.textView.setText("计算已取消");
                    break;
            }
        }
    }


    //计算的子线程，实现1+2+...+100的功能
    class CalculateThread extends Thread {
        @Override
        public void run() {
            int result = 0;//存放结果的变量
            boolean isCancel = false;
            //刚开始发送一个空消息
            myHandler.sendEmptyMessage(START_NUM);
            //计算过程，要求：每隔100ms算一次
            for (int i = 0; i <= 100; i++) {
                try {
                    Thread.sleep(100);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isCancel = true;
                    break;
                }
                //发送进度条更新的消息
                if (i % 5 == 0) {
                    //获取消息对象
                    Message msg = Message.obtain();
                    //给消息的参数赋值
                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    //发送消息
                    myHandler.sendMessage(msg);
                }
            }
            if (!isCancel) {
                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage(msg);
            }
        }
    }

    //Handler的消息常量
    private static final int MSG_SHOW_PROGRESS = 11;
    private static final int MSG_SHOW_IMAGE = 12;
    private MyUiHandler uiHandler = new MyUiHandler(this);

    static class MyUiHandler extends Handler {
        //定义弱引用对象
        private WeakReference<Activity> ref;

        //在构造方法中创建此对象
        public MyUiHandler(Activity activity) {
            this.ref = new WeakReference<>(activity);
        }

        //重写handle方法
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //获取弱引用指向的activity对象
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
            //根据Message的what属性值处理消息
            switch (msg.what) {
                case MSG_SHOW_PROGRESS:
                    //显示进度条
                    activity.bar.setVisibility(View.VISIBLE);
                    break;
                case MSG_SHOW_IMAGE:
                    //显示下载的图片
                    activity.bar.setVisibility(View.GONE);
                    activity.imageView.setImageBitmap((Bitmap) msg.obj);
                    break;
            }
        }
    }

    //下载图片的线程
    private class DownloadImageFetcher implements Runnable {
        private String imgUrl;

        public DownloadImageFetcher(String strUri) {
            this.imgUrl = strUri;
        }

        @Override
        public void run() {
            InputStream in = null;
            //发一个空消息到handleMessage()去处理，显示进度条
            uiHandler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();

            try {
                //将url字符串转为URL对象
                URL url = new URL(imgUrl);
                //打开url对象的http连接
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                //获取这个连接的输入流
                in = connection.getInputStream();
                //将输入流解码为Bitmap图片
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                //通过handle发送消息
//                uiHandler.obtainMessage(MSG_SHOW_IMAGE,bitmap).sendToTarget();
                Message msg = uiHandler.obtainMessage();
                msg.what = MSG_SHOW_IMAGE;
                msg.obj = bitmap;
                uiHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * 1. 创建AsyncTask子类继承AsyncTask类
     * 2. 为3个泛型参数指定类型；若不使用，可用java.lang.Void类型代替，
     * 输入参数 = Integer类型、执行进度 = Integer类型、执行结果 = Integer类型
     */
    static class MyAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        private WeakReference<AppCompatActivity> ref;

        public MyAsyncTask(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        // 执行线程任务前的操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
            activity.bar.setVisibility(View.VISIBLE);
        }

        // 接收输入参数、执行任务中的耗时操作、返回线程任务执行的结果
        @Override
        protected Integer doInBackground(Integer... params) {
            int sleep = params[0];
            int result = 0;

            for (int i = 0; i < 101; i++) {
                try {
                    Thread.sleep(sleep);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (i % 5 == 0) {
                    publishProgress(i);
                }

                if (isCancelled()) {
                    break;
                }
            }
            return result;
        }

        // 在主线程中显示线程任务执行的进度
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            MainActivity activity = (MainActivity) this.ref.get();
            activity.bar.setProgress(values[0]);
            activity.textView.setText("计算已完成" + values[0] + "%");
        }

        // 接收线程任务执行结果、将执行结果显示到UI组件
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            MainActivity activity = (MainActivity) this.ref.get();
            activity.textView.setText("已计算完成，结果为：" + result);
            activity.bar.setVisibility(View.GONE);
        }

        // 将异步任务设置为：取消状态
        @Override
        protected void onCancelled() {
            super.onCancelled();

            MainActivity activity = (MainActivity) this.ref.get();
            activity.textView.setText("计算已取消");

            activity.bar.setProgress(0);
            activity.bar.setVisibility(View.GONE);
        }
    }

    static class DownloadImage extends AsyncTask<String, Bitmap, Bitmap> {
        private WeakReference<AppCompatActivity> ref;

        public DownloadImage(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            return downloadImage(url);
        }

        private Bitmap downloadImage(String strUrl) {
            InputStream stream = null;
            Bitmap bitmap = null;

            MainActivity activity = (MainActivity) this.ref.get();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                URL url = new URL(strUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int totalLen = connection.getContentLength();
                if (totalLen == 0) {
                    activity.bar.setProgress(0);
                }

                if (connection.getResponseCode() == 200) {
                    stream = connection.getInputStream();
//                    bitmap = BitmapFactory.decodeStream(stream);

                    int len = -1;
                    int progress = 0;
                    byte[] tmps = new byte[1024];
                    while ((len = stream.read(tmps)) != -1) {
                        progress += len;
                        activity.bar.setProgress(progress);
                        bos.write(tmps, 0, len);
                    }
                    bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            MainActivity activity = (MainActivity) this.ref.get();
            if (bitmap != null) {
                activity.imageView.setImageBitmap(bitmap);
            }
        }
    }
}
