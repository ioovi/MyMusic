package cn.bistu.edu.cs.mymusic;

public class Music {
    private String name;//歌曲名字
    private String path;//歌曲路径

    public Music(String name, String path){
        this.name = name;
        this.path = path;
    }
    public void setNameM(String nameM) {
        this.name = nameM;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNameM() {
        return name;
    }

    public String getPath() {
        return path;
    }

}
