package billiards.database;

public final class PictureStable {
    public final String points;
    public final String equations;

    public PictureStable(final String points, final String equations) {
        this.points = points;
        this.equations = equations;
        
        //george aug 24,2019 I don't know what makes this print
        //System.out.print("equations " + equations + "\n");
        //System.out.print("points " + points + "\n");
    }
}
