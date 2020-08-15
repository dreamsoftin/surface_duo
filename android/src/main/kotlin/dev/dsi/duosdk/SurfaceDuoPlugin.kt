package dev.dsi.duosdk

import androidx.annotation.NonNull;
import android.app.Activity
import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

import com.microsoft.device.display.DisplayMask
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener


/** SurfaceDuoPlugin */
public class SurfaceDuoPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private lateinit var context: Context
  private lateinit var activity: Activity

  private val HINGE_ANGLE_SENSOR_NAME = "Hinge Angle"
  private var mSensorsSetup : Boolean = false
  private var mSensorManager: SensorManager? = null
  private var mHingeAngleSensor: Sensor? = null
  private var mSensorListener: SensorEventListener? = null
  private var mCurrentHingeAngle: Float = 0.0f

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "surface_duo")
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.applicationContext
  }

  //Activity Aware
  override fun onDetachedFromActivity() {
      //TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
      //TODO("Not yet implemented")
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
      activity = binding.getActivity();
  }

  override fun onDetachedFromActivityForConfigChanges() {
      //TODO("Not yet implemented")
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "surface_duo")
      channel.setMethodCallHandler(SurfaceDuoPlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if(!isDualScreenDevice()) {
        result.success(false)
    } else {
        try {
            if (call.method == "isDualScreenDevice") {
                if (isDualScreenDevice()) {
                    result.success(true)
                } else {
                    result.success(false)
                }
            } else if (call.method == "isAppSpanned") {
                if (isAppSpanned()) {
                    result.success(true)
                } else {
                    result.success(false)
                }
            } else if (call.method == "getHingeAngle") {
                if (!mSensorsSetup) {
                    setupSensors()
                }
                result.success(mCurrentHingeAngle)
            } else if (call.method == "getHingeSize") {
              if (!mSensorsSetup) {
                  setupSensors()
              }
              result.success(getHingeSize())
            }
            
            else {
                result.notImplemented()
            }
        } catch(e: Exception){
            result.success(false)
        }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  fun isDualScreenDevice(): Boolean {
    val feature = "com.microsoft.device.display.displaymask"
    val pm = context.packageManager
    if (pm.hasSystemFeature(feature)) {
        return true
    } else {
        return false
    }
  }

  fun isAppSpanned(): Boolean {
    var displayMask = DisplayMask.fromResourcesRectApproximation(activity);
    var boundings = displayMask.getBoundingRects();
    var first = boundings.get(0);
    var rootView = activity.getWindow().getDecorView().getRootView();
    var drawingRect = android.graphics.Rect();
    rootView.getDrawingRect(drawingRect);
    if (first.intersect(drawingRect)) {
      return true
    } else {
      return false
    }
  }

  private fun setupSensors() {
    mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    val sensorList: List<Sensor> =
      mSensorManager!!.getSensorList(Sensor.TYPE_ALL)
  
    for (sensor in sensorList) {
      if (sensor.getName().contains(HINGE_ANGLE_SENSOR_NAME)) {
        mHingeAngleSensor = sensor
        break
      }
    }
  
    mSensorListener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor === mHingeAngleSensor) {
          mCurrentHingeAngle = event.values.get(0) as Float
        }
      }
  
      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //TODO – Add support later
      }
    }
  
    mSensorManager?.registerListener(
      mSensorListener, 
      mHingeAngleSensor, 
      SensorManager.SENSOR_DELAY_NORMAL)
  
    mSensorsSetup = true
  }

  fun getHingeSize(): Int {
      //The size will always be the same,
      // it will either be the height (Double Landscape)
      // or the width (Double portrait)
      val displayMask = DisplayMask.fromResourcesRectApproximation(activity)
      val boundings = displayMask.boundingRects

      if (boundings.isEmpty()) return 0

      val first = boundings[0]

      val density: Float = activity!!.resources.displayMetrics.density

      val height = ((first.right / density) - (first.left / density)).toInt()
      val width = ((first.bottom / density) - (first.top / density)).toInt()

      if (width < height) {
          return width
      } else {
          return height
      }
  }
}
