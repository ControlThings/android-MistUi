package fi.ct.mist;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.bson.BsonDocument;

import java.util.ArrayList;
import java.util.List;

import addon.AddonReceiver;
import fi.ct.mist.main.Main;
import fi.ct.mist.mist.R;
import wish.Request;
import wish.request.Identity;
import mist.api.request.Mist;

import static fi.ct.mist.settings.SettingsFragment.NOTIFICATION_KEY;
import static fi.ct.mist.settings.SettingsFragment.SETTINGS_KEY;


/**
 * Created by jeppe on 2/10/17.
 */

public class NotificationService extends Service implements AddonReceiver.Receiver {
    private static String TAG = "NotificationService";

    int signalsId = 0;

    private int friendSignalsId = 0;


    private List<Request> friends = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        SharedPreferences preferences = this.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE);

        if (preferences.getBoolean(NOTIFICATION_KEY, true)) {
            AddonReceiver mistReceiver = new AddonReceiver(this);

            Intent mistService = new Intent(this, mist.api.Service.class);
            mistService.putExtra("receiver", mistReceiver);
            startService(mistService);

        } else {
            if (signalsId != 0) {
                Mist.cancel(signalsId);
            }
            stopSelf();
        }

        return START_STICKY;

    }

    private void subscribeMistReady() {

        Mist.ready(new Mist.ReadyCb() {
            @Override
            public void cb(boolean b) {
                if (b) {
                    list();
                    signalsFriend();
                }
            }

            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }
        });
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected");
        subscribeMistReady();

        signalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument bsonDocument) {
                if (s.equals("ready")) {
                    subscribeMistReady();
                }
            }

            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
        /* FIXME: do something when disconnected from core */
    }

    private void list() {
        Identity.friendRequestList(new Identity.FriendRequestListCb() {
            @Override
            public void cb(List<Request> list) {
                friends = list;
            }
            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }
        });
    }

    private void signalsFriend() {
        if (friendSignalsId != 0) {
            /* We have already a subscription to friend signals, don't make a new one! */
            return;
        }
        friendSignalsId = Mist.signals(new Mist.SignalsCb() {
            @Override
            public void cb(String s, BsonDocument document) {
                if (s.equals("friendRequest")) {
                    if (friends == null) {
                        return;
                    }
                    Identity.friendRequestList(new Identity.FriendRequestListCb() {
                        @Override
                        public void cb(List<Request> list) {
                            if (list.size() <= friends.size()) {
                                friends = list;
                                return;
                            } else if (list.size() - friends.size() > 1) {
                                friendRequest("more");
                            } else {
                                for (Request request : list) {
                                    boolean exists = false;
                                    for (Request friend : friends) {
                                        if (friend.equals(request)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        friendRequest(request.getAlias());
                                        break;
                                    }
                                }
                            }
                        }

                        @Override
                        public void err(int i, String s) {
                        }

                        @Override
                        public void end() {
                        }
                    });
                }
            }

            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }
        });
    }

    private void friendRequest(final String name) {
        Identity.list(new Identity.ListCb() {
            @Override
            public void cb(List<wish.Identity> list) {
                if (list.size() > 0) {
                    wish.Identity selectedIdentity;
                    String alias = "--";
                    for (wish.Identity identity : list) {
                        if (identity.isPrivkey()) {
                            selectedIdentity = identity;
                            alias = selectedIdentity.getAlias();
                            break;
                        }
                    }

                    Intent intent = new Intent(getApplicationContext(), Main.class);
                    intent.putExtra("type", "tag");
                    intent.putExtra("tag", getString(R.string.main_users));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    String text;
                    if (name.equals("more")) {
                        text = getString(R.string.notification_friendrequest_title_more);
                    } else {
                        text = String.format(getString(R.string.notification_friendrequest_title), name, alias);
                    }
                    NotificationCompat.Builder builder = notificationBuilder(text,
                            getString(R.string.notification_friendrequest_text),
                            intent);
                    showNotification(builder);

                }
            }

            @Override
            public void err(int i, String s) {
            }

            @Override
            public void end() {
            }

        });
    }


    private NotificationCompat.Builder notificationBuilder(CharSequence title, CharSequence text, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ct_black)
                .setColor(ContextCompat.getColor(this, R.color.ctPrimaryDark))
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(new long[]{1000, 1000})
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        return builder;
    }

    public void showNotification(NotificationCompat.Builder builder) {
        Notification notification = builder.build();
        NotificationManagerCompat.from(this).notify(0, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("NOTI", "onDestroy");
        Mist.cancel(signalsId);
    }
}
