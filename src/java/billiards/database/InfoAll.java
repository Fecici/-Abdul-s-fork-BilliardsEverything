package billiards.database;

import billiards.wrapper.CInfoAll;
import com.sun.jna.Pointer;

public final class InfoAll {

    public final String initialAngles;
    public final String points;
    public final String equations;
    public final String leftRights;
    public final String codeSeqLR;
    public final String sinEquations;
    public final String cosEquations;
    public final String vectorX;
    public final String vectorY;

    public InfoAll(final CInfoAll cinfoAll) {
        this.initialAngles = cinfoAll.initial_angles.getString(0);
        this.points = cinfoAll.points.getString(0);
        this.equations = cinfoAll.equations.getString(0);
        this.sinEquations = cinfoAll.sinEquations.getString(0);
        this.cosEquations = cinfoAll.cosEquations.getString(0);

        this.leftRights = cinfoAll.left_rights.getString(0);
        this.codeSeqLR = cinfoAll.code_seq_lr.getString(0);
        this.vectorX= cinfoAll.vectorX.getString(0);
        this.vectorY= cinfoAll.vectorY.getString(0);



    }
}
