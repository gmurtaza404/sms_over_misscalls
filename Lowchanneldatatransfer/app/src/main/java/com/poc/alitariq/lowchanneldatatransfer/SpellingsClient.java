package com.poc.alitariq.lowchanneldatatransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.TextView;

import java.util.Locale;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

/**
 * Created by ali tariq on 09/02/2018.
 */

public class SpellingsClient extends Activity implements SpellCheckerSession.SpellCheckerSessionListener {

    private TextView suggestions;
    private String suggestionsStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        suggestions = new TextView(this);

        setContentView(suggestions);

        Bundle bundle = getIntent().getExtras();
        setSuggestionsStr("");
        String str = bundle.getString(EXTRA_MESSAGE);
        fetchSuggestionsFor(str);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("RESULTS",suggestionsStr);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        final StringBuffer sb = new StringBuffer("");
        for (SentenceSuggestionsInfo result : results) {
            sb.append(result.getSuggestionsInfoAt(0))
                        .append(" ");
        }
        setSuggestionsStr(sb.toString());
    }

    public void fetchSuggestionsFor(String input) {
        TextServicesManager tsm =
                (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);

        SpellCheckerSession session =
                tsm.newSpellCheckerSession(null, Locale.ENGLISH, this, false);

        if (input != null) {
            if (session != null) {
                session.getSentenceSuggestions(
                        new TextInfo[]{new TextInfo(input)},
                        2
                );
            } else {
                return;
            }
        }
    }

    public String getSuggestionsStr() {
        return suggestionsStr;
    }

    public void setSuggestionsStr(String suggestionsStr) {
        this.suggestionsStr = suggestionsStr;
    }
}