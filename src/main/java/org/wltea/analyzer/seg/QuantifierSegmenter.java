/**
 *
 */
package org.wltea.analyzer.seg;

import org.wltea.analyzer.Context;
import org.wltea.analyzer.Lexeme;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;
import org.wltea.analyzer.help.CharacterHelper;

import java.util.HashSet;
import java.util.Set;

public class QuantifierSegmenter implements ISegmenter {

    public static String Arabic_Num_Pre = "-+$￥";
    private static Set<Character> ArabicNumPreChars = new HashSet<Character>();

    static {
        char[] ca = Arabic_Num_Pre.toCharArray();
        for (char nChar : ca) {
            ArabicNumPreChars.add(nChar);
        }
    }

    public static final int NC_ANP = 01;
    public static final int NC_ARABIC = 02;
    public static String Arabic_Num_Mid = ",./:Ee";
    private static Set<Character> ArabicNumMidChars = new HashSet<Character>();

    static {
        char[] ca = Arabic_Num_Mid.toCharArray();
        for (char nChar : ca) {
            ArabicNumMidChars.add(nChar);
        }
    }

    public static final int NC_ANM = 03;
    public static String Arabic_Num_End = "%‰";
    public static final int NC_ANE = 04;

    public static String Chn_Num_Pre = "第";
    public static final int NC_CNP = 11;
    public static String Chn_Num = "○一二两三四五六七八九十零壹贰叁肆伍陆柒捌玖拾百千万亿拾佰仟万亿兆卅廿";
    private static Set<Character> ChnNumberChars = new HashSet<Character>();

    static {
        char[] ca = Chn_Num.toCharArray();
        for (char nChar : ca) {
            ChnNumberChars.add(nChar);
        }
    }

    public static final int NC_CHINESE = 12;
    public static String Chn_Num_Mid = "点";
    public static final int NC_CNM = 13;
    public static String Chn_Num_End = "几多余半";
    private static Set<Character> ChnNumEndChars = new HashSet<Character>();

    static {
        char[] ca = Chn_Num_End.toCharArray();
        for (char nChar : ca) {
            ChnNumEndChars.add(nChar);
        }
    }

    public static final int NC_CNE = 14;

    public static String Rome_Num = "ⅠⅡⅢⅣⅤⅥⅧⅨⅩⅪ";
    private static Set<Character> RomeNumChars = new HashSet<Character>();

    static {
        char[] ca = Rome_Num.toCharArray();
        for (char nChar : ca) {
            RomeNumChars.add(nChar);
        }
    }

    public static final int NC_ROME = 22;

    public static final int NaN = -99;

    private static Set<Character> AllNumberChars = new HashSet<Character>(256);

    static {
        char[] ca = null;

        AllNumberChars.addAll(ArabicNumPreChars);

        for (char nChar = '0'; nChar <= '9'; nChar++) {
            AllNumberChars.add(nChar);
        }

        AllNumberChars.addAll(ArabicNumMidChars);

        ca = Arabic_Num_End.toCharArray();
        for (char nChar : ca) {
            AllNumberChars.add(nChar);
        }

        ca = Chn_Num_Pre.toCharArray();
        for (char nChar : ca) {
            AllNumberChars.add(nChar);
        }

        AllNumberChars.addAll(ChnNumberChars);

        ca = Chn_Num_Mid.toCharArray();
        for (char nChar : ca) {
            AllNumberChars.add(nChar);
        }

        AllNumberChars.addAll(ChnNumEndChars);

        AllNumberChars.addAll(RomeNumChars);

    }


    private int nStart;

    private int nEnd;

    private int nStatus;

    private boolean fCaN;


    private int countStart;

    private int countEnd;


    public QuantifierSegmenter() {
        nStart = -1;
        nEnd = -1;
        nStatus = NaN;
        fCaN = false;

        countStart = -1;
        countEnd = -1;
    }

    public void nextLexeme(char[] segmentBuff, Context context) {
        fCaN = false;

        processNumber(segmentBuff, context);


        if (countStart == -1) {

            if ((fCaN && nStart == -1)
                    || (nEnd != -1 && nEnd == context.getCursor() - 1)
                    ) {

                processCount(segmentBuff, context);

            }
        } else {

            processCount(segmentBuff, context);
        }


        if (this.nStart == -1 && this.nEnd == -1 && NaN == this.nStatus
                && this.countStart == -1 && this.countEnd == -1) {

            context.unlockBuffer(this);
        } else {
            context.lockBuffer(this);
        }
    }


    private void processNumber(char[] segmentBuff, Context context) {

        int inputStatus = nIdentify(segmentBuff, context);

        if (NaN == nStatus) {

            onNaNStatus(inputStatus, context);

        } else if (NC_ANP == nStatus) {

            onANPStatus(inputStatus, context);

        } else if (NC_ARABIC == nStatus) {

            onARABICStatus(inputStatus, context);

        } else if (NC_ANM == nStatus) {

            onANMStatus(inputStatus, context);

        } else if (NC_ANE == nStatus) {

            onANEStatus(inputStatus, context);

        } else if (NC_CNP == nStatus) {

            onCNPStatus(inputStatus, context);

        } else if (NC_CHINESE == nStatus) {

            onCHINESEStatus(inputStatus, context);

        } else if (NC_CNM == nStatus) {

            onCNMStatus(inputStatus, context);

        } else if (NC_CNE == nStatus) {

            onCNEStatus(inputStatus, context);

        } else if (NC_ROME == nStatus) {

            onROMEStatus(inputStatus, context);

        }


        if (context.getCursor() == context.getAvailable() - 1) {
            if (nStart != -1 && nEnd != -1) {

                outputNumLexeme(context);
            }

            nReset();
        }
    }


    private void onNaNStatus(int inputStatus, Context context) {
        if (NaN == inputStatus) {
            return;

        } else if (NC_CNP == inputStatus) {

            nStart = context.getCursor();

            nStatus = inputStatus;

        } else if (NC_CHINESE == inputStatus) {

            nStart = context.getCursor();

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else if (NC_CNE == inputStatus) {

            nStart = context.getCursor();

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else if (NC_ANP == inputStatus) {

            nStart = context.getCursor();

            nStatus = inputStatus;

        } else if (NC_ARABIC == inputStatus) {

            nStart = context.getCursor();

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else if (NC_ROME == inputStatus) {

            nStart = context.getCursor();

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else {

        }
    }

    private void onANPStatus(int inputStatus, Context context) {
        if (NC_ARABIC == inputStatus) {

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else {

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        }
    }


    private void onARABICStatus(int inputStatus, Context context) {
        if (NC_ARABIC == inputStatus) {


            nEnd = context.getCursor();

        } else if (NC_ANM == inputStatus) {

            nStatus = inputStatus;

        } else if (NC_ANE == inputStatus) {

            nStatus = inputStatus;

            nEnd = context.getCursor();

            outputNumLexeme(context);

            nReset();

        } else {

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        }

    }

    private void onANMStatus(int inputStatus, Context context) {
        if (NC_ARABIC == inputStatus) {

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else if (NC_ANP == inputStatus) {

            nStatus = inputStatus;

        } else {

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        }
    }

    private void onANEStatus(int inputStatus, Context context) {

        outputNumLexeme(context);

        nReset();

        onNaNStatus(inputStatus, context);

    }

    private void onCNPStatus(int inputStatus, Context context) {
        if (NC_CHINESE == inputStatus) {

            nEnd = context.getCursor() - 1;

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);


        } else if (NC_ARABIC == inputStatus) {

            nEnd = context.getCursor() - 1;

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        } else if (NC_ROME == inputStatus) {

            nEnd = context.getCursor() - 1;

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        } else {

            nReset();

            onNaNStatus(inputStatus, context);

        }
    }

    private void onCHINESEStatus(int inputStatus, Context context) {
        if (NC_CHINESE == inputStatus) {

            nEnd = context.getCursor();

        } else if (NC_CNM == inputStatus) {

            nStatus = inputStatus;

        } else if (NC_CNE == inputStatus) {

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else {

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        }
    }

    private void onCNMStatus(int inputStatus, Context context) {
        if (NC_CHINESE == inputStatus) {

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else if (NC_CNE == inputStatus) {

            nStatus = inputStatus;

            nEnd = context.getCursor();

        } else {

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        }
    }

    private void onCNEStatus(int inputStatus, Context context) {

        outputNumLexeme(context);

        nReset();

        onNaNStatus(inputStatus, context);

    }


    private void onROMEStatus(int inputStatus, Context context) {
        if (NC_ROME == inputStatus) {

            nEnd = context.getCursor();

        } else {

            outputNumLexeme(context);

            nReset();

            onNaNStatus(inputStatus, context);

        }
    }


    private void outputNumLexeme(Context context) {
        if (nStart > -1 && nEnd > -1) {

            Lexeme newLexeme = new Lexeme(context.getBuffOffset(), nStart, nEnd - nStart + 1, Lexeme.TYPE_NUM);
            context.addLexeme(newLexeme);
            fCaN = true;
        }
    }


    private void outputCountLexeme(Context context) {
        if (countStart > -1 && countEnd > -1) {

            Lexeme countLexeme = new Lexeme(context.getBuffOffset(), countStart, countEnd - countStart + 1, Lexeme.TYPE_NUMCOUNT);
            context.addLexeme(countLexeme);
        }

    }


    private void nReset() {
        this.nStart = -1;
        this.nEnd = -1;
        this.nStatus = NaN;
    }


    private int nIdentify(char[] segmentBuff, Context context) {


        char input = segmentBuff[context.getCursor()];

        int type = NaN;
        if (!AllNumberChars.contains(input)) {
            return type;
        }

        if (CharacterHelper.isArabicNumber(input)) {
            type = NC_ARABIC;

        } else if (ChnNumberChars.contains(input)) {
            type = NC_CHINESE;

        } else if (Chn_Num_Pre.indexOf(input) >= 0) {
            type = NC_CNP;

        } else if (Chn_Num_Mid.indexOf(input) >= 0) {
            type = NC_CNM;

        } else if (ChnNumEndChars.contains(input)) {
            type = NC_CNE;

        } else if (ArabicNumPreChars.contains(input)) {
            type = NC_ANP;

        } else if (ArabicNumMidChars.contains(input)) {
            type = NC_ANM;

        } else if (Arabic_Num_End.indexOf(input) >= 0) {
            type = NC_ANE;

        } else if (RomeNumChars.contains(input)) {
            type = NC_ROME;

        }
        return type;
    }


    private void processCount(char[] segmentBuff, Context context) {
        Hit hit = null;

        if (countStart == -1) {
            hit = Dictionary.matchInQuantifierDict(segmentBuff, context.getCursor(), 1);
        } else {
            hit = Dictionary.matchInQuantifierDict(segmentBuff, countStart, context.getCursor() - countStart + 1);
        }

        if (hit != null) {
            if (hit.isPrefix()) {
                if (countStart == -1) {

                    countStart = context.getCursor();
                }
            }

            if (hit.isMatch()) {
                if (countStart == -1) {
                    countStart = context.getCursor();
                }

                countEnd = context.getCursor();

                outputCountLexeme(context);
            }

            if (hit.isUnmatch()) {
                if (countStart != -1) {

                    countStart = -1;
                    countEnd = -1;
                }
            }
        }


        if (context.getCursor() == context.getAvailable() - 1) {

            countStart = -1;
            countEnd = -1;
        }
    }

    public void reset() {
        nStart = -1;
        nEnd = -1;
        nStatus = NaN;
        fCaN = false;

        countStart = -1;
        countEnd = -1;
    }

}
