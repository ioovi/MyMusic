package cn.bistu.edu.cs.mymusic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private boolean isSeekBarChanging;
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private final List<Music> musicList = new ArrayList<>();//歌曲列表
    private final List<File> musicFile = new ArrayList<>();//MP3文件列表
    private String times;//播放时间
    private int cMusicId = 0;//当前播放的音乐ID
    private ImageButton btnPlay;//播放暂停
    private ImageButton btnPre;//上一首
    private ImageButton btnNext;//下一首
    private ImageButton btnRefresh;//刷新界面
    private ImageButton btnAdd;//添加音乐
    private ListView lv;
    private SeekBar mSeekBar;//进度条
    private Timer timer = new Timer();
    private int currentTime = 0;
    private boolean clickFlag = false;//记录是否点击音乐列表
    private TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //初始化
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv1 = findViewById(R.id.tv1);
        btnPlay = findViewById(R.id.btnPlay);
        btnPre = findViewById(R.id.btnPre);
        btnNext = findViewById(R.id.btnNext);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnAdd = findViewById(R.id.btn_add);
        lv = findViewById(R.id.listWords);
        btnPre.setOnClickListener(this);
        btnNext.setOnClickListener(this);


        mSeekBar = findViewById(R.id.mSeekbar);
        mSeekBar.setOnSeekBarChangeListener(this);


        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.stop();
                btnPlay.setImageResource(android.R.drawable.ic_media_play);
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFlag = true;
                Intent intent = new Intent(MainActivity.this, MusicAdd.class);
                startActivity(intent);
            }
        });

        btnPlay.setOnClickListener(this);
        //长按监听
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,final int position, long id) {
                //长按则显示出一个对话框
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除提示") //无标题
                        .setMessage("是否删除音乐") //内容
                        .setNegativeButton("取消",null) //连个按钮
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            //删除键
                            @SuppressLint("Range")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File file = Environment.getExternalStorageDirectory();//打开的下载路径,/Storage/emulated/0/
                                File StorageFile = new File(file, "Download/"+musicFile.get(position).getName());
                                musicList.remove(position);
                                musicFile.remove(position);
                                System.out.println("File----"+StorageFile);
                                StorageFile.delete();//移除点击的音乐
                                MusicAdapter adapter= new MusicAdapter(MainActivity.this,R.layout.music_item,musicList);
                                lv.setAdapter(adapter);
                                mediaPlayer.stop();

                                btnPlay.setImageResource(android.R.drawable.ic_media_play);
                                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                startActivity(intent);

                            }
                        }).show();
                return true;
            }
        });
        //权限判断，如果没有权限就请求权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            //https://freemusicarchive.org/track/cold-and-warmth/download
            String musicN = getIntent().getStringExtra("musicN");
            String musicP = getIntent().getStringExtra("path");
            clickFlag = false;
            Music m = new Music(musicN, musicP);
            //musicList.add(m);
            initMediaPlayer(m.getPath());//用查找的歌曲初始化音乐播放器

            getMusicList();//得到音乐歌曲列表
            btnPlay.setImageResource(android.R.drawable.ic_media_play);//设置播放后的界面，播放/暂停按钮为暂停显示状态
        }

    }

    /**
     * 获取音乐列表
     */
    public void getMusicList(){
        File file = Environment.getExternalStorageDirectory();//打开的下载路径,/Storage/emulated/0/
        File SdcardFile = new File(file, "Download");
        getSDcardFile(SdcardFile);//得到所有文件列表
        musicList.clear();//清空原始列表
        for(int i=0;i<musicFile.size();i++){
            File c = musicFile.get(i);
            String path = c.getPath();//得到路径
            String name = c.getName();//得到歌曲名字

            if(name.contains("mp3")) {
                name = name.substring(0, name.length() - 4);//数据清洗，将后面的.mp3清洗掉
            }
                Music music = new Music(name, path);
            musicList.add(music);
        }
            MusicAdapter adapter = new MusicAdapter(MainActivity.this, R.layout.music_item, musicList);//适配器
        lv.setAdapter(adapter);//设置适配器
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {//点击事件
                String mpath = musicList.get(i).getPath();
                cMusicId=i;
                mediaPlayer.stop();//停止媒体播放器
                Log.d("path------",mpath);
                initMediaPlayer(mpath);
                mediaPlayer.start();//开始播放
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                clickFlag=true;
            }
        });
    }

    /**
     * 查询目录下所有文件路径
     */
    public void getSDcardFile(File path){
        File[] files = path.listFiles();//得到文件下所有子文件
        System.out.println(files.length);
        for(int i=0;i<=files.length-1;i++){
            musicFile.add(files[i]);
        }
    }

    /**
     * 初始化音乐播放器
     */
    private void initMediaPlayer(String path) {
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();//重启播放器
            File file = new File(path);
            //判断文件夹是否存在,如果不存在则创建文件夹
            if (!file.exists()) {

                Intent intent=new Intent(MainActivity.this,MusicAdd.class);
                intent.putExtra("path",path);
                Toast.makeText(this, "音乐不存在！正在跳转下载...", Toast.LENGTH_LONG).show();
                startActivity(intent);

            }else{
                mediaPlayer.setDataSource(path);//指定音频文件路径
            }
            mediaPlayer.setLooping(true);//设置为循环播放
            mediaPlayer.prepare();//初始化播放器MediaPlayer
            mediaPlayer.start();

            mSeekBar.setMax(mediaPlayer.getDuration());
            currentTime = 0;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isSeekBarChanging&&mediaPlayer.isPlaying()){//如果进度条未改变，并且当前正在播放
                    mSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                    Message msg = new Message();
                    msg.what = 1;
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                    handler.sendMessage(msg);
                }
            }
        },0,1000);

    }

    //对于handle
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    tv1.setText("正在播放：" + getIntent().getStringExtra("musicN"));
                    if(tv1.getText().equals("正在播放：null")) {//默认播放第一首
                        tv1.setText("正在播放：" + musicList.get(cMusicId).getNameM());
                    }else if(tv1.getText().equals("正在播放："+getIntent().getStringExtra("musicN"))&&!clickFlag) {
                        for (int i = 0; i < musicList.size(); i++) {
                            if (getIntent().getStringExtra("musicN").equals(musicList.get(i).getNameM())) {
                                tv1.setText("正在播放：" + musicList.get(i).getNameM());//播放查找到的歌曲
                                cMusicId = i;
                            }
                        }
                    }else if(clickFlag){//播放对应点击的歌曲
                        tv1.setText("正在播放：" + musicList.get(cMusicId).getNameM());
                    }
                    break;
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar mSeekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {//开始移动滚动条
        isSeekBarChanging = true;//开始移动
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {//结束移动滚动条
        isSeekBarChanging = false;//停止移动
        mediaPlayer.seekTo(mSeekBar.getProgress());//找到滚动条对应位置
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnPlay:
                //如果没在播放中，立刻开始播放。
                if(!mediaPlayer.isPlaying()){
                    mediaPlayer.start();
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                }else{  //如果在播放中，立刻暂停。
                    mediaPlayer.pause();
                    btnPlay.setImageResource(android.R.drawable.ic_media_play);
                    v.setTag(0);
                }
                break;

            case R.id.btnPre:
                try{
                    mediaPlayer.stop();//停止
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);//设置播放/停止状态
                    cMusicId=(cMusicId+musicList.size()-1)%musicList.size();//上一首
                    clickFlag=true;
                    initMediaPlayer(musicList.get(cMusicId).getPath());//得到路径，并初始化播放器
                    mediaPlayer.start();//开始播放

                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.btnNext:
                try {
                    mediaPlayer.stop();//停止
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);//设置播放/停止状态
                    cMusicId = (cMusicId + 1) % musicList.size();//下一首
                    Log.d("path",musicList.get(cMusicId).getPath());
                    clickFlag=true;
                    initMediaPlayer(musicList.get(cMusicId).getPath());//得到路径，并初始化播放器
                    mediaPlayer.start();//开始播放

                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }


    @Override
    protected void onDestroy() {
        //结束时释放资源
        super.onDestroy();
        if(mediaPlayer != null){
            mediaPlayer.stop();//停止播放器
            mediaPlayer.reset();
            mediaPlayer.release();//释放资源
        }
    }




}
