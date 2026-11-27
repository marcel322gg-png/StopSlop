/* Copyright 2026 Hushfeed contributors */
package app.morphe.extension.tiktok.settings.preference;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import app.morphe.extension.tiktok.feedfilter.BlockedVideosHistory;
import app.morphe.extension.tiktok.feedfilter.BlockedVideosHistory.Entry;
import app.morphe.extension.tiktok.settings.L10n;
import app.morphe.extension.tiktok.settings.preference.SettingsUi;

/** Settings-only review list; no playback surface is created. */
@SuppressWarnings("deprecation")
public final class BlockedVideosPreference extends Preference {
    public BlockedVideosPreference(Context context) {
        super(context);
        setKey("stop_slop_blocked_videos_history");
        setTitle(L10n.t(context, "Blocked Videos"));
        setSummary(L10n.t(context, "Review videos hidden by StopSlop"));
        setOnPreferenceClickListener(p -> { show(); return true; });
    }

    private void show() {
        Context c = getContext();
        LinearLayout root = new LinearLayout(c); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(8),dp(18),dp(8));
        Button clear = new Button(c); clear.setText(L10n.t(c,"Clear history"));
        clear.setTextColor(SettingsUi.accent());
        clear.setOnClickListener(v -> new AlertDialog.Builder(c).setMessage(L10n.t(c,"Clear all blocked video history? Restore exceptions will be kept.")).setNegativeButton(android.R.string.cancel,null).setPositiveButton(L10n.t(c,"Clear history"),(d,w)->{BlockedVideosHistory.clear();show();}).show());
        root.addView(clear);
        Button limit = new Button(c); limit.setText(L10n.f(c,"History limit: %1$s", BlockedVideosHistory.limit()));
        limit.setTextColor(SettingsUi.accent());
        limit.setOnClickListener(v -> { String[] options={"5","10","20","50","100"}; new AlertDialog.Builder(c).setTitle(L10n.t(c,"History limit")).setSingleChoiceItems(options,indexOfLimit(),(d,w)->{BlockedVideosHistory.setLimit(BlockedVideosHistory.LIMITS[w]);d.dismiss();show();}).setNegativeButton(android.R.string.cancel,null).show(); }); root.addView(limit);
        List<Entry> entries=BlockedVideosHistory.list();
        if(entries.isEmpty()){TextView empty=new TextView(c);empty.setText(L10n.t(c,"No blocked videos yet"));empty.setPadding(0,dp(20),0,dp(20));root.addView(empty);}
        for(Entry e:entries) root.addView(row(c,e));
        ScrollView scroll=new ScrollView(c); scroll.addView(root);
        new AlertDialog.Builder(c).setTitle(L10n.t(c,"Were these videos blocked correctly?")).setView(scroll).setPositiveButton(android.R.string.ok,null).show();
    }
    private int indexOfLimit(){int value=BlockedVideosHistory.limit();for(int i=0;i<BlockedVideosHistory.LIMITS.length;i++)if(BlockedVideosHistory.LIMITS[i]==value)return i;return 2;}
    private LinearLayout row(Context c, Entry e){
        LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,dp(10),0,dp(10));
        TextView details=new TextView(c);String when=DateFormat.getMediumDateFormat(c).format(e.timestamp)+" "+DateFormat.getTimeFormat(c).format(e.timestamp);
        details.setText("ID: "+e.id+(e.username==null?"":"\n@"+e.username)+"\n"+humanReason(e.reason)+"\n"+when+("CORRECT".equals(e.review)?"\n👍 Correct":""));box.addView(details);
        LinearLayout actions=new LinearLayout(c);actions.setOrientation(LinearLayout.HORIZONTAL);
        if (e.username != null) {
            Button open=new Button(c);open.setText(L10n.t(c,"Open in TikTok"));open.setOnClickListener(v->openVideo(e));actions.addView(open);
        } else {
            Button copy=new Button(c);copy.setText(L10n.t(c,"Copy ID"));copy.setOnClickListener(v->{ClipboardManager clipboard=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText(L10n.t(c,"Video ID"),e.id));Toast.makeText(c,L10n.t(c,"Video ID copied"),Toast.LENGTH_SHORT).show();}});actions.addView(copy);
        }
        Button correct=new Button(c);correct.setText(L10n.t(c,"👍 Correct"));correct.setOnClickListener(v->{BlockedVideosHistory.correct(e.id);show();});actions.addView(correct);
        Button delete=new Button(c);delete.setText(L10n.t(c,"🗑 Delete"));delete.setOnClickListener(v->{BlockedVideosHistory.delete(e.id);show();});actions.addView(delete);
        Button restore=new Button(c);restore.setText(L10n.t(c,"👎 Restore"));restore.setOnClickListener(v->{BlockedVideosHistory.restore(e.id);show();});actions.addView(restore);box.addView(actions);
        correct.setTextColor(0xff2e7d32);
        restore.setTextColor(0xffc62828);
        return box;
    }
    private String humanReason(String reason){
        if (reason == null || reason.isEmpty()) return L10n.t(getContext(),"Other filter");
        if (reason.endsWith("Filter") && reason.length() > 6) reason = reason.substring(0, reason.length() - 6);
        StringBuilder readable = new StringBuilder();
        for (int i=0;i<reason.length();i++) { char ch=reason.charAt(i); if(i>0 && Character.isUpperCase(ch)) readable.append(' '); readable.append(ch); }
        String value=readable.toString(); return value.isEmpty()?L10n.t(getContext(),"Other filter"):Character.toUpperCase(value.charAt(0))+value.substring(1);
    }
    private void openVideo(Entry e){
        Context c=getContext();try{String url="https://www.tiktok.com/@"+Uri.encode(e.username)+"/video/"+Uri.encode(e.id);c.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(ActivityNotFoundException ex){Toast.makeText(c,L10n.t(c,"No app can open this video link"),Toast.LENGTH_SHORT).show();}
    }
    private int dp(int value){return (int)(value*getContext().getResources().getDisplayMetrics().density+0.5f);}
}
