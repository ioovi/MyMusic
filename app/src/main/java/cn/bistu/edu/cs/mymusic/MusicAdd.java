package cn.bistu.edu.cs.mymusic;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicAdd extends AppCompatActivity implements View.OnClickListener{

    private DownloadService.DownloadBinder downloadBinder;
    private  ImageButton pauseDownload;
    private final List<Music> musicList = new ArrayList<>();//歌曲列表
    private final List<File> musicFile = new ArrayList<>();//MP3文件列表

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_add);
        EditText music_url = findViewById(R.id.music_url);
        EditText music_name = findViewById(R.id.music_name);

        Button btnFind = findViewById(R.id.music_find);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton startDownload = findViewById(R.id.start_download);
        pauseDownload =  findViewById(R.id.pause_download);
        ImageButton cancelDownload =  findViewById(R.id.cancel_download);

        music_url.setOnClickListener(this);
        music_name.setOnClickListener(this);
        startDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        cancelDownload.setOnClickListener(this);

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent); // 启动服务
        bindService(intent, connection, BIND_AUTO_CREATE); // 绑定服务
        if (ContextCompat.checkSelfPermission(MusicAdd.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MusicAdd.this, new String[]{ Manifest.permission. WRITE_EXTERNAL_STORAGE }, 1);
        }

        //点击返回
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //点击查找
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMusicFile(music_name.getText().toString());//查询目录下所有匹配的音乐
                if(musicList.isEmpty()){//查找不到则生成下载链接
                    Toast.makeText(MusicAdd.this, "音乐不存在,请先点击下载!", Toast.LENGTH_LONG).show();
                    String musicN = music_name.getText().toString();
                    musicN = musicN.replace(" ","-");
                    musicN = "https://freemusicarchive.org/track/"+musicN+"/download";
                    music_url.setText(musicN);
                }
            }
        });
    }

    /**
     * 查询目录下所有匹配的音乐
     */
    public void getMusicFile(String musicN){
        File file = Environment.getExternalStorageDirectory();//打开sdcard的下载路径,/mnt/sdcard/
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
            if(musicN.equals(name)) {
                Music music = new Music(name, path);
                musicList.add(music);
            }
        }

        MusicAdapter adapter = new MusicAdapter(MusicAdd.this, R.layout.music_item, musicList);//适配器
        ListView lv = findViewById(R.id.listWords);
        lv.setAdapter(adapter);//设置适配器
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {//点击事件
                String mpath = musicList.get(i).getPath();
                Intent intent = new Intent(MusicAdd.this,MainActivity.class);
                intent.putExtra("musicN",musicList.get(i).getNameM());
                intent.putExtra("path",musicList.get(i).getPath());
                startActivity(intent);
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
    @Override
    public void onClick(View v) {
        EditText music_url = findViewById(R.id.music_url);
        music_url.setOnClickListener(this);

        //if(!getIntent().getStringExtra("path").contains("")) {
           // music_url.setText(getIntent().getStringExtra("path"));

        if (downloadBinder == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.start_download:
                //https://freemusicarchive.org/track/in-a-meditative-state/download
               // String url = "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/6iiAzHgTukeycL1ul7O2kFTDu1whRCbMDpWRjZHJ.mp3?download=1&name=Ov%20Moi%20Omm%20-%20In%20A%20Meditative%20State.mp3";
                String url = music_url.getText().toString();
                downloadBinder.startDownload(url);
                break;
            case R.id.pause_download:
                downloadBinder.pauseDownload();

                break;
            case R.id.cancel_download:
                downloadBinder.cancelDownload();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

}
