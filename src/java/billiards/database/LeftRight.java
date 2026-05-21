package billiards.database;

import java.util.Objects;

public final class LeftRight {

    public final int leftNumber;
    public final int leftBranch;
    public final int rightNumber;
    public final int rightBranch;

    public LeftRight(final int leftNumber, final int leftBranch,
                     final int rightNumber, final int rightBranch) {
        this.leftNumber = leftNumber;
        this.leftBranch = leftBranch;
        this.rightNumber = rightNumber;
        this.rightBranch = rightBranch;
        
      //george aug 26,2019 press show left rights to see (4, 0, 1, 0) in console
       // System.out.print("leftNumber: " + leftNumber + "\n");//4
       //  System.out.print("leftBranch: " + leftBranch + "\n");//0
       //  System.out.print("rightNumber: " + rightNumber + "\n");//1
       //  System.out.print("rightBranch: " + rightBranch + "\n");//0
        //note: it doubles these for some reason and adds the words leftNumber for example 
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d, %d)", this.leftNumber, this.leftBranch, this.rightNumber, this.rightBranch);
    }

    @Override
    public boolean equals(final Object obj) {
        final LeftRight other = (LeftRight) obj;
        return this.leftNumber == other.leftNumber &&
            this.leftBranch == other.leftBranch &&
            this.rightNumber == other.rightNumber &&
            this.rightBranch == other.rightBranch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.leftNumber, this.leftBranch, this.rightNumber, this.rightBranch);
    }
}
