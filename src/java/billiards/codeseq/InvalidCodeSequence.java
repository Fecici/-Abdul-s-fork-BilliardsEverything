package billiards.codeseq;

import org.eclipse.collections.api.list.primitive.IntList;

public enum InvalidCodeSequence {
    EMPTY,
    NEGATIVE_OR_ZERO_NUMBERS,
    ILLEGAL_PATTERN;

    public static String errorMessage(
        final IntList codeNumbers, final InvalidCodeSequence errorCode) {
        final String str = codeNumbers.makeString(" ");

        final String msg;
        if (errorCode == InvalidCodeSequence.EMPTY) {
            msg = String.format("Code sequence is empty: %s", str);
        } else if (errorCode == InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS) {
            msg = String.format("All code numbers must be greater than 0: %s", str);
        } else if (errorCode == InvalidCodeSequence.ILLEGAL_PATTERN) {
            msg = String.format("Code sequence is illegal: %s", str);
        } else {
            throw new RuntimeException("Unknown InvalidCodeSequence " + errorCode);
        }

        return msg;
    }
}
