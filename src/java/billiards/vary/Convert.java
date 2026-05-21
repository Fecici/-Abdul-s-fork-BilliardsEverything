package billiards.vary;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;

public class Convert {
	
    public static Optional<ClassifiedCodeSequence> convert(final IntList codeList) {

        final MutableIntList newCode = IntArrayList.newList(codeList);
        final int len = newCode.size();
        int count = 0;
        
        while (newCode.get(0) == newCode.get(newCode.size() - 1) && count < len + 1) {
            CodeSequence.rotateLeft(newCode);
            count += 1;
            
        }
        if (count >= len) {
        	return Optional.empty();
        }
        
        final MutableIntList finalList = new IntArrayList();

        int counter = 0;
        final int size = newCode.size();
        for (int i = 0; i < size; i++) {
            counter += 1;
            if (newCode.get(i) != newCode.get((i + 1) % size)) {
                finalList.add(counter);
                counter = 0;
            }
        }
        
        try {
        	final ClassifiedCodeSequence codeSeq = ClassifiedCodeSequence.create(finalList).get();
        	return Optional.of(codeSeq);
        	
        } catch(final NoSuchElementException e) {
            return Optional.empty();
            
        }
    }
}
