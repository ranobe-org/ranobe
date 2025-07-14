package org.ranobe.ranobe.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BionicReadingTextView extends AppCompatTextView {

    private static final Pattern WORD_PATTERN = Pattern.compile("([a-zA-ZÀ-ÿåäöÅÄÖ]+)['’]?[a-zA-ZÀ-ÿåäöÅÄÖ]*");
    private boolean isBionicReading = false;

    public BionicReadingTextView(Context context) {
        super(context);
        init();
    }

    public BionicReadingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BionicReadingTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        applyBionicEffect();
    }

    public boolean isBionicReading() {
        return isBionicReading;
    }

    public void setBionicReading(boolean enabled) {
        this.isBionicReading = enabled;
        applyBionicEffect();
    }

    private void applyBionicEffect() {
        CharSequence text = getText();
        if (!isBionicReading || text == null) {
            setText(text);
            return;
        }

        SpannableString spannable = new SpannableString(text);
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            int wordStart = matcher.start(1);
            int wordEnd = matcher.end(1);
            int wordLength = wordEnd - wordStart;
            int boldLength = calculateBoldLength(wordLength);

            if (boldLength > 0) {
                spannable.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        wordStart,
                        wordStart + boldLength,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        setText(spannable);
    }

    private int calculateBoldLength(int wordLength) {
        if (wordLength <= 3) return 1;
        if (wordLength == 4) return 2;
        return Math.round(wordLength * 0.4f);
    }
}