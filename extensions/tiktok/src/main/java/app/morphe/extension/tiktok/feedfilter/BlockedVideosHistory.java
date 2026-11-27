/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 *
 * Built on icysymmetra/tiktok-patches-for-morphe (GPL-3.0).
 */
package app.morphe.extension.tiktok.feedfilter;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

import app.morphe.extension.shared.Utils;

/** Small local record of videos that reached the final feed exclusion point. */
public final class BlockedVideosHistory {
    private static final String PREFS = "stop_slop_blocked_videos";
    private static final String HISTORY = "history_json";
    private static final String RESTORES = "restore_ids_json";
    private static final String LIMIT = "history_limit";
    public static final int DEFAULT_LIMIT = 20;
    public static final int[] LIMITS = {5, 10, 20, 50, 100};

    public static final class Entry {
        public final String id, username, reason, review;
        public final long timestamp;
        Entry(String id, String username, String reason, long timestamp, String review) {
            this.id = id; this.username = username; this.reason = reason;
            this.timestamp = timestamp; this.review = review;
        }
    }

    private static final Object LOCK = new Object();
    private static long clearGeneration;
    private static final ThreadPoolExecutor WRITER = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(256), task -> {
        Thread thread = new Thread(task, "StopSlop-blocked-history");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile Set<String> cachedRestoreIds = Collections.emptySet();
    static {
        // Restore lookup runs from the feed filter, so hydrate its small snapshot off-thread.
        WRITER.execute(BlockedVideosHistory::loadRestoreCache);
    }
    private BlockedVideosHistory() { }

    private static SharedPreferences prefs() {
        Context context = Utils.getContext();
        return context == null ? null : context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void record(String id, String username, String reason) {
        if (id == null || id.trim().isEmpty()) return;
        final String stableId = id;
        final String stableUsername = username;
        final String stableReason = reason;
        final long capturedAt = System.currentTimeMillis();
        final long generation;
        synchronized (LOCK) { generation = clearGeneration; }
        try {
            WRITER.execute(() -> persistRecord(stableId, stableUsername, stableReason, capturedAt, generation));
        } catch (RejectedExecutionException queueFull) {
            // A saturated history writer must not affect ordinary feed filtering.
        }
    }

    private static void persistRecord(String id, String username, String reason, long timestamp, long generation) {
        synchronized (LOCK) {
            if (generation != clearGeneration) return;
            SharedPreferences p = prefs(); if (p == null) return;
            // A queued event from before a Restore action must not recreate its history row.
            if (restoreIds(p).contains(id)) return;
            List<Entry> rows = read(p);
            ArrayList<Entry> next = new ArrayList<>();
            for (Entry row : rows) if (!id.equals(row.id)) next.add(row);
            next.add(0, new Entry(id, username, reason, timestamp, ""));
            write(p, trim(next, p.getInt(LIMIT, DEFAULT_LIMIT)));
        }
    }

    public static List<Entry> list() {
        synchronized (LOCK) { SharedPreferences p = prefs(); return p == null ? Collections.emptyList() : read(p); }
    }

    public static int limit() { SharedPreferences p = prefs(); return p == null ? DEFAULT_LIMIT : p.getInt(LIMIT, DEFAULT_LIMIT); }
    public static void setLimit(int value) {
        if (!validLimit(value)) return;
        synchronized (LOCK) {
            SharedPreferences p = prefs();
            if (p != null) write(p, trim(read(p), value), value);
        }
    }
    private static boolean validLimit(int value) { for (int n : LIMITS) if (n == value) return true; return false; }

    public static void correct(String id) { updateReview(id, "CORRECT"); }
    private static void updateReview(String id, String review) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(); if (p == null) return;
            ArrayList<Entry> rows = new ArrayList<>();
            for (Entry e : read(p)) rows.add(id.equals(e.id) ? new Entry(e.id,e.username,e.reason,e.timestamp,review) : e);
            write(p, rows);
        }
    }
    public static void delete(String id) { remove(id); }
    public static void restore(String id) {
        if (id == null || id.isEmpty()) return;
        synchronized (LOCK) {
            SharedPreferences p = prefs(); if (p == null) return;
            Set<String> ids = restoreIds(p); ids.add(id);
            p.edit().putString(RESTORES, toIds(ids).toString()).apply();
            cachedRestoreIds = immutableCopy(ids);
            removeLocked(p, id);
        }
    }
    /** Memory-only lookup: no preference reads or JSON parsing on the feed-filter thread. */
    public static boolean isRestored(String id) { return id != null && cachedRestoreIds.contains(id); }
    public static void clear() { synchronized (LOCK) { clearGeneration++; SharedPreferences p = prefs(); if (p != null) write(p, Collections.emptyList()); } }
    private static void remove(String id) { synchronized (LOCK) { SharedPreferences p = prefs(); if (p != null) removeLocked(p,id); } }
    private static void removeLocked(SharedPreferences p, String id) { ArrayList<Entry> rows = new ArrayList<>(); for (Entry e : read(p)) if (!id.equals(e.id)) rows.add(e); write(p, rows); }

    private static List<Entry> read(SharedPreferences p) {
        ArrayList<Entry> out = new ArrayList<>();
        try { JSONArray a = new JSONArray(p.getString(HISTORY, "[]")); for (int i=0;i<a.length();i++) { JSONObject o=a.optJSONObject(i); if(o==null) continue; String id=o.optString("id",""); if(id.isEmpty()) continue; out.add(new Entry(id, empty(o.optString("username", "")), o.optString("reason","Other filter"), o.optLong("time",0), o.optString("review",""))); } } catch (JSONException ignored) { }
        return out;
    }
    private static String empty(String s) { return s == null || s.isEmpty() ? null : s; }
    private static List<Entry> trim(List<Entry> rows, int max) { return new ArrayList<>(rows.subList(0, Math.min(Math.max(0,max),rows.size()))); }
    private static void write(SharedPreferences p, List<Entry> rows) { JSONArray a=new JSONArray(); for(Entry e:rows) { JSONObject o=new JSONObject(); try { o.put("id",e.id); o.put("username",e.username == null ? "" : e.username); o.put("reason",e.reason); o.put("time",e.timestamp); o.put("review",e.review); a.put(o); } catch(JSONException ignored) { } } p.edit().putString(HISTORY,a.toString()).apply(); }
    private static void write(SharedPreferences p, List<Entry> rows, int limit) { JSONArray a=new JSONArray(); for(Entry e:rows) { JSONObject o=new JSONObject(); try { o.put("id",e.id); o.put("username",e.username == null ? "" : e.username); o.put("reason",e.reason); o.put("time",e.timestamp); o.put("review",e.review); a.put(o); } catch(JSONException ignored) { } } p.edit().putInt(LIMIT, limit).putString(HISTORY,a.toString()).apply(); }
    private static Set<String> restoreIds(SharedPreferences p) { HashSet<String> ids=new HashSet<>(); try { JSONArray a=new JSONArray(p.getString(RESTORES,"[]")); for(int i=0;i<a.length();i++){String id=a.optString(i,"");if(!id.isEmpty())ids.add(id);} } catch(JSONException ignored){} return ids; }
    private static Set<String> immutableCopy(Set<String> ids) { return Collections.unmodifiableSet(new HashSet<>(ids)); }
    private static void loadRestoreCache() {
        synchronized (LOCK) {
            SharedPreferences p = prefs();
            if (p != null) cachedRestoreIds = immutableCopy(restoreIds(p));
        }
    }
    private static JSONArray toIds(Set<String> ids) { JSONArray a=new JSONArray(); for(String id:ids)a.put(id); return a; }
}
