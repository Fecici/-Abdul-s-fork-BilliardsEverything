package billiards.wrapper;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.List;

// This class must be public for JNA to work
// Make sure the names and orders of the class and fields
// match those in the struct
// The fields here change obviously, so don't make them final
public final class CString extends Structure {

    public Pointer string;

    @Override
    public List<String> getFieldOrder() {
        return FastList.newListWith("string");
    }
}
