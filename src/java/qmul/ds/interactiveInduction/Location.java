package qmul.ds.interactiveInduction;

public class Location {
    private int x;
    private int y;
    private String name;

    public Location(int x, int y, String name){
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public Location(int x, int y){
        this.x = x;
        this.y = y;
        this.name = "";
    }

    public Location(String locationString){
        String[] location = locationString.split(", ");
        this.x = Integer.parseInt(location[0]);
        this.y = Integer.parseInt(location[1]);
        this.name = location[2];
    }

    public Location(){
        this.x = 0;
        this.y = 0;
        this.name = "";
    }

    public int getX(){
        return this.x;
    }

    public int getY(){
        return this.y;
    }

    public String getName(){
        return this.name;
    }

    public void setX(int x){
        this.x = x;
    }

    public void setY(int y){
        this.y = y;
    }
}
