package com.example.marvel.ui.common;

import android.app.Activity;
import android.content.Intent;

import com.example.marvel.MainActivity;
import com.example.marvel.R;
import com.example.marvel.ui.album.AlbumActivity;
import com.example.marvel.ui.heroes.MyHeroesActivity;
import com.example.marvel.ui.market.MarketActivity;
import com.example.marvel.ui.ranks.RankTrailActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class MainNav {

    private MainNav() {
    }

    public static void setup(Activity activity, BottomNavigationView nav, int currentItemId,
                             Runnable onReselect) {
        nav.setSelectedItemId(currentItemId);
        nav.setOnItemSelectedListener(item -> {
            open(activity, item.getItemId());
            return false;
        });
        nav.setOnItemReselectedListener(item -> {
            if (onReselect != null) onReselect.run();
        });
    }

    public static void openRanks(Activity from) {
        open(from, R.id.nav_ranks);
    }

    private static void open(Activity from, int itemId) {
        Class<?> target;
        if (itemId == R.id.nav_market) {
            target = MarketActivity.class;
        } else if (itemId == R.id.nav_my_heroes) {
            target = MyHeroesActivity.class;
        } else if (itemId == R.id.nav_album) {
            target = AlbumActivity.class;
        } else if (itemId == R.id.nav_ranks) {
            target = RankTrailActivity.class;
        } else {
            target = MainActivity.class;
        }
        Intent intent = new Intent(from, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (from.getClass() == target) return;
        from.startActivity(intent);
        if (isTab(from) && !(from instanceof MainActivity)) {
            from.finish();
        }
    }

    private static boolean isTab(Activity activity) {
        return activity instanceof MainActivity || activity instanceof MarketActivity
                || activity instanceof MyHeroesActivity || activity instanceof AlbumActivity
                || activity instanceof RankTrailActivity;
    }
}
