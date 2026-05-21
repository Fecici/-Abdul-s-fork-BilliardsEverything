package billiards.database;

import billiards.wrapper.CPicture;

public final class Picture {
    public final String initialAngles;
    public final String points;
    public final String equations;

    public Picture(final CPicture cpicture) {
        this.initialAngles = cpicture.initial_angles.getString(0);
        this.points = cpicture.points.getString(0);
        this.equations = cpicture.equations.getString(0);
        
        //george aug 24,2019 when press regular calculate 1 3 3 for example
        //System.out.print("initialAngles " + initialAngles + "\n");//zx for exanple
        //System.out.print("points " + points + "\n");
       // System.out.print("equations " + equations + "\n");
    }
}
