package billiards.database;

import billiards.wrapper.CInfo;

public final class Info {

    public final String initialAngles;
    public final String points;
    public final String equations;
    public final String leftRights;
    public final String codeSeqLR;

    public Info(final CInfo cinfo) {
        this.initialAngles = cinfo.initial_angles.getString(0);
        this.points = cinfo.points.getString(0);
        this.equations = cinfo.equations.getString(0);
        this.leftRights = cinfo.left_rights.getString(0);
        this.codeSeqLR = cinfo.code_seq_lr.getString(0);
        
      //george aug 24,2019 press info to see stuff in console
       // System.out.print("equations: " + equations + "\n");
        //System.out.print("initialAngles: " + initialAngles + "\n");
        //System.out.print("points: " + points + "\n");
        //System.out.print("leftRights: " + leftRights + "\n");
        //System.out.print("codeSeqLR: " + codeSeqLR + "\n");

        
    }
}
