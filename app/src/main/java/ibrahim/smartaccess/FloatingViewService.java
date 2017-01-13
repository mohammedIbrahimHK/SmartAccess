package ibrahim.smartaccess;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

public class FloatingViewService extends Service {
   private WindowManager mWindowManager;
   private View mFloatingView;
    DevicePolicyManager deviceManger;
    ActivityManager activityManager;
    ComponentName compName;
    ImageView firstButton,secondButton, sound,recent;
    String TAG=FloatingViewService.class.getName();
    public FloatingViewService() {
   }
 
   @Override
   public IBinder onBind(Intent intent) {
       return null;
   }
 
   @Override
   public void onCreate() {
       super.onCreate();


       deviceManger = (DevicePolicyManager)getSystemService(
               Context.DEVICE_POLICY_SERVICE);
       activityManager = (ActivityManager)getSystemService(
               Context.ACTIVITY_SERVICE);
       compName = new ComponentName(this, MyAdmin.class);


       //Inflate the floating view layout we created
       mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);
 
       //Add the view to the window.
       final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
               WindowManager.LayoutParams.WRAP_CONTENT,
               WindowManager.LayoutParams.WRAP_CONTENT,
               WindowManager.LayoutParams.TYPE_PHONE,
               WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
               PixelFormat.TRANSLUCENT);
 
       //Specify the view position
       params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
       params.x = 0;
       params.y = 100;
 
       //Add the view to the window
       mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
       mWindowManager.addView(mFloatingView, params);
 
       //The root element of the collapsed view layout
       final View collapsedView = mFloatingView.findViewById(R.id.collapse_view);
       //The root element of the expanded view layout
       final View expandedView = mFloatingView.findViewById(R.id.expanded_container);
 
       //Set the close button
       ImageView closeButtonCollapsed = (ImageView) mFloatingView.findViewById(R.id.close_btn);
       closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               //close the service and remove the from from the window
               stopSelf();
           }
       });
 
       //Set the view while floating view is expanded.
       //Set the play button.
       ImageView playButton = (ImageView) mFloatingView.findViewById(R.id.play_btn);
       playButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Toast.makeText(FloatingViewService.this, "Playing the song.", Toast.LENGTH_LONG).show();
           }
       });
 
 
       //Set the next button.
       ImageView nextButton = (ImageView) mFloatingView.findViewById(R.id.next_btn);
       nextButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Toast.makeText(FloatingViewService.this, "Playing next song.", Toast.LENGTH_LONG).show();
           }
       });
 
 
       //Set the pause button.
       ImageView prevButton = (ImageView) mFloatingView.findViewById(R.id.prev_btn);
       prevButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Toast.makeText(FloatingViewService.this, "Playing previous song.", Toast.LENGTH_LONG).show();
           }
       });
 
 
       //Set the close button
       ImageView closeButton = (ImageView) mFloatingView.findViewById(R.id.close_button);
       closeButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               collapsedView.setVisibility(View.VISIBLE);
               expandedView.setVisibility(View.GONE);
           }
       });
       firstButton =(ImageView)mFloatingView.findViewById(R.id.first);
       firstButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {

               try {
                   setAutoOrientationEnabled(getApplicationContext(),true);
               } catch (Settings.SettingNotFoundException e) {
                   e.printStackTrace();
               } catch (IllegalAccessException e) {
                   e.printStackTrace();
               } catch (InvocationTargetException e) {
                   e.printStackTrace();
               } catch (ClassNotFoundException e) {
                   e.printStackTrace();
               }
           }
       });
       recent =(ImageView)mFloatingView.findViewById(R.id.recent);
       recent.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               getRecentApps();

           }
       });

       sound =(ImageView)mFloatingView.findViewById(R.id.sound);
       sound.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

               if(am.getRingerMode()==AudioManager.RINGER_MODE_NORMAL){
                   am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                   sound.setImageResource(R.drawable.vibrate);
                   return;
               }
               if(am.getRingerMode()==AudioManager.RINGER_MODE_VIBRATE){
                   am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                   sound.setImageResource(R.drawable.sound);
                   return;
               }
               if(am.getRingerMode()==AudioManager.RINGER_MODE_SILENT){
                   am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                   sound.setImageResource(R.drawable.loud);
                   return;
               }

           }
       });

       //Open the application on thi button click
       ImageView openButton = (ImageView) mFloatingView.findViewById(R.id.open_button);
       openButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               //Open the application  click.
               Intent intent = new Intent(FloatingViewService.this, MainActivity.class);
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               startActivity(intent);
 
 
               //close the service and remove view from the view hierarchy
               stopSelf();
           }
       });
 
       //Drag and move floating view using user's touch action.
       mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
           private int initialX;
           private int initialY;
           private float initialTouchX;
           private float initialTouchY;
 
 
           @Override
           public boolean onTouch(View v, MotionEvent event) {
               switch (event.getAction()) {
                   case MotionEvent.ACTION_DOWN:
 
                       //remember the initial position.
                       initialX = params.x;
                       initialY = params.y;
 
                       //get the touch location
                       initialTouchX = event.getRawX();
                       initialTouchY = event.getRawY();
                       return true;
                   case MotionEvent.ACTION_UP:
                       int Xdiff = (int) (event.getRawX() - initialTouchX);
                       int Ydiff = (int) (event.getRawY() - initialTouchY);
 
 
                       //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                       //So that is click event.
                       if (Xdiff < 10 && Ydiff < 10) {
                           if (isViewCollapsed()) {
                               //When user clicks on the image view of the collapsed layout,
                               //visibility of the collapsed layout will be changed to "View.GONE"
                               //and expanded view will become visible.
                               collapsedView.setVisibility(View.GONE);
                               expandedView.setVisibility(View.VISIBLE);
                           }
                       }
                       return true;
                   case MotionEvent.ACTION_MOVE:
                       //Calculate the X and Y coordinates of the view.
                       params.x = initialX + (int) (event.getRawX() - initialTouchX);
                       params.y = initialY + (int) (event.getRawY() - initialTouchY);
 
 
                       //Update the layout with new X & Y coordinate
                       if(isViewCollapsed())
                       mWindowManager.updateViewLayout(mFloatingView, params);
                       return true;
               }
               return false;
           }
       });
   }
 
 
   /**
    * Detect if the floating view is collapsed or expanded.
    *
    * @return true if the floating view is collapsed.
    */
   private boolean isViewCollapsed() {
       return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
   }
 
 
   @Override
   public void onDestroy() {
       super.onDestroy();
       if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
   }
    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = null;
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

//            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }
    public  void setAutoOrientationEnabled(Context context, boolean enabled) throws Settings.SettingNotFoundException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {



        Log.i("ibbu", "setAutoOrientationEnabled: "+Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION));

        if(Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION)==1){
            firstButton.setImageResource(R.drawable.rotation_enable);
            Settings.System.putInt( context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, !enabled ? 1 : 0);
        }
        else{
            firstButton.setImageResource(R.drawable.rotation_enable);

            Settings.System.putInt( context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);

        }


    }

    public void getRecentApps(){
        Method getService = null;
        try {
            Class serviceManagerClass = Class.forName("android.os.ServiceManager");
            getService = serviceManagerClass.getMethod("getService", String.class);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerClass, "statusbar");
            Class statusBarClass = Class.forName(retbinder.getInterfaceDescriptor());
            Object statusBarObject = statusBarClass.getClasses()[0].getMethod("asInterface", IBinder.class).invoke(null, new Object[] { retbinder });
            Method clearAll = statusBarClass.getMethod("toggleRecentApps");
            clearAll.setAccessible(true);
            clearAll.invoke(statusBarObject);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}